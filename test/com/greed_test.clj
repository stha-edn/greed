(ns com.greed-test
  (:require [cheshire.core :as cheshire]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.biffweb :as biff :refer [test-xtdb-node]]
            [com.greed :as main]
            [com.greed.app :as app]
            [com.greed.authentication :as auth]
            [com.greed.data.core :as data]
            [com.greed.home :as home]
            [com.greed.middleware :as mid]
            [com.greed.password :as pwd]
            [com.greed.email :as email]
            [com.biffweb.impl.auth :as biff-auth]
            [com.greed.ui.app.dashboard :as dashboard]
            [malli.generator :as mg]
            [rum.core :as rum]
            [xtdb.api :as xt]))

(deftest send-mailersend-uses-resolved-api-key-test
  (let [captured (atom nil)]
    (with-redefs [clj-http.client/post (fn [_url opts]
                                         (reset! captured opts)
                                         {:status 202})]
      (is (true? (email/send-email {:biff/secret (fn [_] "real-key")
                                    :mailersend/from "admin@example.com"
                                    :mailersend/from-name "Greed"}
                                   {:to "user@example.com"
                                    :subject "Subject"
                                    :text "Body"})))
      (is (= "Bearer real-key" (get-in @captured [:headers "Authorization"]))))))

(deftest example-test
  (is (= 4 (+ 2 2))))

(deftest password-hashing-test
  (let [plaintext "correct horse battery staple"]
    (testing "hashes are bcrypt and not stored in plaintext"
      (let [hash (data/hash-password plaintext)]
        (is (str/starts-with? hash "bcrypt"))
        (is (not (str/includes? hash plaintext)))
        (is (:valid? (auth/validate-password? plaintext hash)))
        (is (not (:valid? (auth/validate-password? "wrong" hash))))))
    (testing "legacy plaintext passwords still verify"
      (is (:valid? (auth/validate-password? plaintext plaintext)))
      (is (not (:valid? (auth/validate-password? "wrong" plaintext)))))))

(deftest rate-limit-test
  (reset! mid/signin-attempts {})
  (testing "allows up to the limit within a window"
    (doseq [n (range 5)]
      (is (not (mid/rate-limit-exceeded? "1.2.3.4" 5 (* 60 1000))))))
  (testing "blocks beyond the limit"
    (is (mid/rate-limit-exceeded? "1.2.3.4" 5 (* 60 1000))))
  (testing "different keys are tracked independently"
    (reset! mid/signin-attempts {})
    (is (not (mid/rate-limit-exceeded? "5.6.7.8" 5 (* 60 1000)))))
  (reset! mid/signin-attempts {}))

(deftest csp-header-test
  (let [wrapped (mid/wrap-security-headers (fn [_] {:status 200 :headers {} :body "ok"}))
        resp (wrapped {:uri "/"})]
    (testing "adds a Content-Security-Policy header"
      (is (some? (get-in resp [:headers "Content-Security-Policy"]))))
    (testing "policy blocks object/embed injection"
      (is (str/includes? (get-in resp [:headers "Content-Security-Policy"]) "object-src 'none'")))
    (testing "policy allows the third-party scripts the app uses"
      (let [csp (get-in resp [:headers "Content-Security-Policy"])]
        (is (str/includes? csp "https://unpkg.com"))
        (is (str/includes? csp "https://cdn.jsdelivr.net"))))
    (testing "connect-src allows the CDNs scripts fetch source maps from"
      (let [connect-src (second (re-find #"connect-src ([^;]+);" (get-in resp [:headers "Content-Security-Policy"])))]
        (is (str/includes? connect-src "https://unpkg.com"))))))

(deftest client-ip-test
  (testing "reads the nginx-provided real IP header"
    (is (= "203.0.113.9" (mid/get-client-ip {:headers {"x-real-ip" "203.0.113.9"}}))))
  (testing "falls back to remote-addr"
    (is (= "127.0.0.1" (mid/get-client-ip {:remote-addr "127.0.0.1"})))))

(defn get-context [node]
  {:biff.xtdb/node  node
   :biff/db         (xt/db node)
   :biff/malli-opts #'main/malli-opts})

(deftest password-reset-flow-test
  (let [uid  #uuid "dddddddd-dddd-dddd-dddd-dddddddddddd"
        sent (atom nil)
        jwt-secret (biff/generate-secret 32)
        secret (fn [k]
                 (case k
                   :biff/jwt-secret jwt-secret
                   :recaptcha/secret-key nil
                   nil))
        capture-send! (fn [_ctx opts]
                        (reset! sent opts)
                        true)]
    (with-open [node (test-xtdb-node [{:xt/id       uid
                                       :user/email  "dave@example.com"
                                       :user/password (data/hash-password "old-pass")
                                       :user/firstname "Dave"
                                       :user/lastname "D"
                                       :user/age 30
                                       :user/active true}])]
      (let [ctx-for (fn [params]
                      (assoc (get-context node)
                             :biff/secret secret
                             :params params))]
        (with-redefs [biff-auth/passed-recaptcha? (constantly true)
                      email/send-email capture-send!]
          (testing "forgot-password always redirects to the generic sent page"
            (let [resp (pwd/forgot-password-action (ctx-for {:email "dave@example.com"}))]
              (is (= 303 (:status resp)))
              (is (= "/forgot-password-sent" (get-in resp [:headers "location"]))))
            (is (some? @sent))
            (is (= :password-reset (:template @sent)))
            (is (str/includes? (:url @sent) "/reset-password?token=")))
          (let [token (second (re-find #"token=([^&]+)" (:url @sent)))]
            (testing "the reset link changes the password and signs the user in"
              (let [resp (pwd/reset-password-action (ctx-for {:token token
                                                              :password "new-pass"
                                                              :confirm-password "new-pass"}))
                    db (xt/db node)
                    user (data/get-user {:biff/db db} uid)]
                (is (= 303 (:status resp)))
                (is (= "/app?success=signin" (get-in resp [:headers "location"])))
                (is (= uid (get-in resp [:session :uid])))
                (is (:valid? (auth/validate-password? "new-pass" (:user/password user))))
                (is (nil? (:user/password-reset-token user)))))
            (testing "a reset token can only be redeemed once"
              (let [resp (pwd/reset-password-action (ctx-for {:token token
                                                              :password "third-pass"
                                                              :confirm-password "third-pass"}))
                    db (xt/db node)
                    user (data/get-user {:biff/db db} uid)]
                (is (= "/reset-password?error=invalid-link" (get-in resp [:headers "location"])))
                (is (:valid? (auth/validate-password? "new-pass" (:user/password user))))))))
            (testing "unknown emails get the generic response and no email is sent"
              (reset! sent nil)
              (let [resp (pwd/forgot-password-action (ctx-for {:email "nobody@example.com"}))]
                (is (= "/forgot-password-sent" (get-in resp [:headers "location"])))
                (is (nil? @sent))))))))

(deftest password-reset-rejects-bad-input-test
  (let [uid  #uuid "dddddddd-dddd-dddd-dddd-dddddddddddd"
        jwt-secret (biff/generate-secret 32)
        secret (fn [k]
                 (case k
                   :biff/jwt-secret jwt-secret
                   :recaptcha/secret-key nil
                   nil))
        make-token (fn [& {:keys [token email exp-in]
                           :or {token "abc123" email "dave@example.com" exp-in 3600}}]
                     (biff/jwt-encrypt {:intent "reset-password"
                                        :email email
                                        :token token
                                        :exp-in exp-in}
                                       jwt-secret))]
    (with-open [node (test-xtdb-node [{:xt/id       uid
                                       :user/email  "dave@example.com"
                                       :user/password (data/hash-password "old-pass")
                                       :user/password-reset-token "abc123"
                                       :user/firstname "Dave"
                                       :user/active true}])]
      (with-redefs [biff-auth/passed-recaptcha? (constantly true)]
        (let [ctx (fn [params]
                    (assoc (get-context node)
                           :biff/secret secret
                           :params params))]
          (testing "a malformed token is rejected"
            (let [resp (pwd/reset-password-action (ctx {:token "garbage"
                                                        :password "x"
                                                        :confirm-password "x"}))]
              (is (= "/reset-password?error=invalid-link" (get-in resp [:headers "location"])))))
          (testing "an expired token is rejected"
            (let [resp (pwd/reset-password-action (ctx {:token (make-token :exp-in -10)
                                                        :password "x"
                                                        :confirm-password "x"}))]
              (is (= "/reset-password?error=invalid-link" (get-in resp [:headers "location"])))))
          (testing "a token that doesn't match the stored one is rejected"
            (let [resp (pwd/reset-password-action (ctx {:token (make-token :token "other-token")
                                                        :password "x"
                                                        :confirm-password "x"}))]
              (is (= "/reset-password?error=invalid-link" (get-in resp [:headers "location"])))))
          (testing "mismatched passwords are rejected and the password is unchanged"
            (let [resp (pwd/reset-password-action (ctx {:token (make-token)
                                                        :password "new-pass"
                                                        :confirm-password "different"}))
                  db (xt/db node)
                  user (data/get-user {:biff/db db} uid)]
              (is (str/starts-with? (get-in resp [:headers "location"])
                                    (str "/reset-password?error=password-mismatch&token=")))
              (is (:valid? (auth/validate-password? "old-pass" (:user/password user))))))
          (testing "a blank password is rejected"
            (let [resp (pwd/reset-password-action (ctx {:token (make-token)
                                                        :password " "
                                                        :confirm-password " "}))]
              (is (str/includes? (get-in resp [:headers "location"]) "error=password-blank")))))))))

(deftest save-user-scoped-to-session-test
  (let [alice-id #uuid "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        bob-id   #uuid "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"]
    (with-open [node (test-xtdb-node [{:xt/id      alice-id
                                       :user/email "alice@example.com"
                                       :user/password (data/hash-password "alice-pass")
                                       :user/firstname "Alice"
                                       :user/active true}
                                      {:xt/id      bob-id
                                       :user/email "bob@example.com"
                                       :user/password (data/hash-password "bob-pass")
                                       :user/firstname "Bob"
                                       :user/active true}])]
      (testing "cannot change another user's password by submitting their email"
        (let [ctx (assoc (get-context node)
                         :session {:uid bob-id}
                         :params {:email "alice@example.com"
                                  :password "hacked"
                                  :firstname "Bob"
                                  :lastname "B"
                                  :age "30"})
              resp (mid/save-user ctx)
              alice (data/get-user (assoc (get-context node) :biff/db (xt/db node)) alice-id)]
          (is (= 303 (:status resp)))
          (is (str/includes? (get-in resp [:headers "location"]) "error=email-taken"))
          (is (not (:valid? (auth/validate-password? "hacked" (:user/password alice)))))
          (is (:valid? (auth/validate-password? "alice-pass" (:user/password alice))))))
      (testing "updates the session user's own record"
        (let [ctx (assoc (get-context node)
                         :session {:uid bob-id}
                         :params {:email "bob@example.com"
                                  :password ""
                                  :firstname "Bobby"
                                  :lastname "B"
                                  :age "31"})
              resp (mid/save-user ctx)
              bob (data/get-user (assoc (get-context node) :biff/db (xt/db node)) bob-id)]
          (is (= 303 (:status resp)))
          (is (= "Bobby" (:user/firstname bob)))
          (is (:valid? (auth/validate-password? "bob-pass" (:user/password bob)))))))))

(deftest ownership-test
  (let [alice-id  #uuid "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        bob-id    #uuid "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        budget-id #uuid "11111111-1111-1111-1111-111111111111"
        goal-id   #uuid "22222222-2222-2222-2222-222222222222"
        event-id  #uuid "33333333-3333-3333-3333-333333333333"]
    (with-open [node (test-xtdb-node [{:xt/id      alice-id
                                       :user/email "alice@example.com"
                                       :user/password (data/hash-password "alice-pass")
                                       :user/firstname "Alice"}
                                      {:xt/id      bob-id
                                       :user/email "bob@example.com"
                                       :user/password (data/hash-password "bob-pass")
                                       :user/firstname "Bob"}
                                      {:xt/id               budget-id
                                       :budget-item/user-id alice-id
                                       :budget-item/title   "Groceries"
                                       :budget-item/type    :expenses
                                       :budget-item/amount  1000}
                                      {:xt/id            goal-id
                                       :goal/user-id     alice-id
                                       :goal/title       "Holiday"
                                       :goal/target      50000
                                       :goal/saved       1000}
                                      {:xt/id           event-id
                                       :event/user-id   alice-id
                                       :event/title     "Dentist"
                                       :event/date      "2026-08-15"}])]
      (testing "a user cannot update another user's budget item"
        (let [ctx (assoc (get-context node)
                         :session {:uid bob-id}
                         :params {:budget-item-id (str budget-id) :title "Hacked" :amount "9999"})]
          (data/update-budget-item ctx)
          (let [item (data/get-budget-item (assoc (get-context node) :biff/db (xt/db node)) budget-id)]
            (is (= "Groceries" (:budget-item/title item)))
            (is (= 1000 (:budget-item/amount item))))))
      (testing "a user cannot delete another user's budget item"
        (let [ctx (assoc (get-context node)
                         :session {:uid bob-id}
                         :params {:budget-item-id (str budget-id)})]
          (data/delete-budget-item ctx)
          (let [db (xt/db node)]
            (is (some? (data/get-budget-item {:biff/db db} budget-id))))))
      (testing "a user can update their own budget item"
        (let [ctx (assoc (get-context node)
                         :session {:uid alice-id}
                         :params {:budget-item-id (str budget-id) :title "Groceries+" :amount "1100"})]
          (data/update-budget-item ctx)
          (let [item (data/get-budget-item (assoc (get-context node) :biff/db (xt/db node)) budget-id)]
            (is (= "Groceries+" (:budget-item/title item)))
            (is (= 1100 (:budget-item/amount item))))))
      (testing "a user cannot update or delete another user's goal"
        (let [ctx (assoc (get-context node)
                         :session {:uid bob-id}
                         :params {:goal-id (str goal-id) :title "Hacked" :target "1" :saved "0"})]
          (data/update-goal ctx)
          (is (= "Holiday" (:goal/title (data/get-goal (assoc (get-context node) :biff/db (xt/db node)) goal-id))))
          (data/delete-goal ctx)
          (is (some? (data/get-goal {:biff/db (xt/db node)} goal-id)))))
      (testing "a user cannot delete another user's event"
        (let [ctx (assoc (get-context node)
                         :session {:uid bob-id}
                         :params {:event-id (str event-id)})]
          (data/delete-event ctx)
          (is (some? (data/get-event {:biff/db (xt/db node)} event-id)))))
      (testing "a user cannot toggle another user's event"
        (let [ctx (assoc (get-context node)
                         :session {:uid bob-id}
                         :params {:event-id (str event-id)})]
          (data/toggle-event ctx)
          (is (nil? (:event/done (data/get-event {:biff/db (xt/db node)} event-id))))))
      (testing "a user can toggle their own event"
        (let [toggle-ctx (fn []
                           (assoc (get-context node)
                                  :session {:uid alice-id}
                                  :params {:event-id (str event-id)}))]
          (data/toggle-event (toggle-ctx))
          (is (true? (:event/done (data/get-event {:biff/db (xt/db node)} event-id))))
          (data/toggle-event (toggle-ctx))
          (is (false? (:event/done (data/get-event {:biff/db (xt/db node)} event-id)))))))))

(deftest create-event-test
  (let [uid #uuid "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"]
    (with-open [node (test-xtdb-node [{:xt/id      uid
                                       :user/email "eve@example.com"
                                       :user/password (data/hash-password "eve-pass")
                                       :user/firstname "Eve"}])]
      (testing "an event without a date or type defaults to an open todo"
        (let [ctx (assoc (get-context node) :session {:uid uid} :params {:title "Buy milk"})]
          (data/create-event ctx)
          (let [events (data/get-events {:biff/db (xt/db node)} uid)]
            (is (= 1 (count events)))
            (is (= :todo (:event/type (first events))))
            (is (nil? (:event/date (first events))))
            (is (false? (:event/done (first events)))))))
      (testing "a dated event keeps its type and date"
        (let [ctx (assoc (get-context node) :session {:uid uid}
                         :params {:title "Rent" :type "bill" :date "2026-09-01"})]
          (data/create-event ctx)
          (let [events (data/get-events {:biff/db (xt/db node)} uid)]
            (is (= 2 (count events)))
            (is (= :bill (:event/type (last events))))
            (is (= "2026-09-01" (:event/date (last events))))
            (is (false? (:event/done (last events)))))))
      (testing "a todo with a date stays a todo and keeps its date"
        (let [ctx (assoc (get-context node) :session {:uid uid}
                         :params {:title "Water plants" :date "2026-08-20"})]
          (data/create-event ctx)
          (let [events (data/get-events {:biff/db (xt/db node)} uid)
                todo   (first (filter #(= "Water plants" (:event/title %)) events))]
            (is (= 3 (count events)))
            (is (= :todo (:event/type todo)))
            (is (= "2026-08-20" (:event/date todo)))
            (is (false? (:event/done todo)))))))))

(deftest signup-auto-signin-test
  (testing "a freshly signed-up user is signed in even though the ctx db is stale"
    (with-open [node (test-xtdb-node [])]
      (let [ctx  (assoc (get-context node)
                        :params {:email "carol@example.com"
                                 :password "carol-pass"
                                 :firstname "Carol"
                                 :lastname "C"
                                 :age "25"}
                        :session {:csrf-token "x"})
            _    (data/upsert-user ctx)
            resp (home/signup-success-page ctx)
            md   (meta (:session resp))]
        (is (contains? md :recreate))
        (is (true? (:recreate md)))
        (is (= (biff/lookup-id (xt/db node) :user/email "carol@example.com")
               (:uid (:session resp))))))))

(deftest session-recreate-test
  (testing "login responses carry :recreate metadata to rotate the session id"
    (with-open [node (test-xtdb-node [{:user/email "alice@example.com"
                                       :user/password (data/hash-password "alice-pass")
                                       :user/firstname "Alice"}])]
      (let [ctx  (assoc (get-context node)
                        :params {:email "alice@example.com"}
                        :session {:csrf-token "x"})
            resp (home/signin-success-page ctx)
            md   (meta (:session resp))]
        (is (contains? md :recreate))
        (is (true? (:recreate md)))
        (is (= (biff/lookup-id (xt/db node) :user/email "alice@example.com")
               (:uid (:session resp))))))))


(deftest user-active-predicate-test
  (testing "a user is active only when explicitly marked active"
    (is (not (data/user-active? {})))
    (is (not (data/user-active? {:user/active nil})))
    (is (data/user-active? {:user/active true}))
    (is (not (data/user-active? {:user/active false})))))

(deftest finance-tax-prompt-due-test
  (testing "due when never dismissed"
    (is (dashboard/finance-tax-prompt-due? {:session {}})))
  (testing "not due when dismissed within the 24 hour window"
    (is (not (dashboard/finance-tax-prompt-due?
              {:session {:finance-tax-prompt-dismissed-at
                         (- (System/currentTimeMillis) (* 1 60 60 1000))}}))))
  (testing "due again after 24 hours"
    (is (dashboard/finance-tax-prompt-due?
         {:session {:finance-tax-prompt-dismissed-at
                    (- (System/currentTimeMillis) (* 25 60 60 1000))}})))
  (testing "the dismiss handler records the dismissal timestamp in the session"
    (let [response (mid/dismiss-finance-tax-prompt {:session {:uid #uuid "ffffffff-ffff-ffff-ffff-ffffffffffff"}})]
      (is (= 303 (:status response)))
      (is (= "/app" (get-in response [:headers "location"])))
      (is (some? (get-in response [:session :finance-tax-prompt-dismissed-at]))))))

(deftest signin-active-status-test
  (testing "deactivated users can't sign in and get a contact-support message"
    (with-open [node (test-xtdb-node [{:xt/id #uuid "cccccccc-cccc-cccc-cccc-cccccccccccc"
                                       :user/email "dave@example.com"
                                       :user/password (data/hash-password "dave-pass")
                                       :user/firstname "Dave"
                                       :user/active false}])]
      (let [ctx (assoc (get-context node)
                       :params {:email "dave@example.com" :password "dave-pass"})
            result (auth/signin? ctx)]
        (is (not (:valid? result)))
        (is (= :account-deactivated (:error result)))
        (is (str/includes? (:message result) "contact support"))
        (is (str/includes? (:message result) "reactivated")))))
  (testing "active users sign in normally"
    (with-open [node (test-xtdb-node [{:xt/id #uuid "cccccccc-cccc-cccc-cccc-cccccccccccc"
                                       :user/email "dave@example.com"
                                       :user/password (data/hash-password "dave-pass")
                                       :user/firstname "Dave"
                                       :user/active true}])]
      (let [ctx (assoc (get-context node)
                       :params {:email "dave@example.com" :password "dave-pass"})]
        (is (:valid? (auth/signin? ctx))))))
  (testing "users missing :user/active are treated as deactivated"
    (with-open [node (test-xtdb-node [{:xt/id #uuid "cccccccc-cccc-cccc-cccc-cccccccccccc"
                                       :user/email "dave@example.com"
                                       :user/password (data/hash-password "dave-pass")
                                       :user/firstname "Dave"}])]
      (let [ctx (assoc (get-context node)
                       :params {:email "dave@example.com" :password "dave-pass"})]
        (is (not (:valid? (auth/signin? ctx))))
        (is (= :account-deactivated (:error (auth/signin? ctx)))))))
  (testing "a wrong password is still invalid-credentials for an active user"
    (with-open [node (test-xtdb-node [{:xt/id #uuid "cccccccc-cccc-cccc-cccc-cccccccccccc"
                                       :user/email "dave@example.com"
                                       :user/password (data/hash-password "dave-pass")
                                       :user/firstname "Dave"
                                       :user/active true}])]
      (let [ctx (assoc (get-context node)
                       :params {:email "dave@example.com" :password "wrong"})]
        (is (not (:valid? (auth/signin? ctx))))
        (is (= :invalid-credentials (:error (auth/signin? ctx))))))))

(deftest deactivated-signin-redirect-test
  (testing "the signin route redirects deactivated users to the contact-support page"
    (with-open [node (test-xtdb-node [{:xt/id #uuid "dddddddd-dddd-dddd-dddd-dddddddddddd"
                                       :user/email "erin@example.com"
                                       :user/password (data/hash-password "erin-pass")
                                       :user/firstname "Erin"
                                       :user/active false}])]
      (let [handler (mid/wrap-authenticate (fn [_] {:status 200}))
            ctx (assoc (get-context node)
                       :uri "/authenticate/signin"
                       :params {:email "erin@example.com" :password "erin-pass"}
                       :biff/secret (constantly nil))
            resp (handler ctx)]
        (reset! mid/signin-attempts {})
        (is (= 303 (:status resp)))
        (is (= "/signin?error=account-deactivated"
               (get-in resp [:headers "location"])))))))

(deftest delete-user-test
  (let [uid #uuid "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"]
    (with-open [node (test-xtdb-node [{:xt/id uid
                                       :user/email "frank@example.com"
                                       :user/password (data/hash-password "frank-pass")
                                       :user/firstname "Frank"
                                       :user/active true}
                                      {:xt/id #uuid "00000000-0000-0000-0000-000000000001"
                                       :finances/user-id uid}
                                      {:xt/id #uuid "00000000-0000-0000-0000-000000000002"
                                       :budget-item/user-id uid}
                                      {:xt/id #uuid "00000000-0000-0000-0000-000000000003"
                                       :tax-profile/user-id uid}
                                      {:xt/id #uuid "00000000-0000-0000-0000-000000000004"
                                       :event/user-id uid}
                                      {:xt/id #uuid "00000000-0000-0000-0000-000000000005"
                                       :goal/user-id uid}])]
      (let [ctx (get-context node)]
        (testing "deletes the user and all of their data"
          (is (= 6 (data/delete-user ctx uid)))
          (let [db (xt/db node)]
            (is (nil? (biff/lookup-id db :user/email "frank@example.com")))
            (is (empty? (biff/q db '{:find [id]
                                     :where [[id :finances/user-id uid]]})))
            (is (empty? (biff/q db '{:find [id]
                                     :where [[id :budget-item/user-id uid]]})))
            (is (empty? (biff/q db '{:find [id]
                                     :where [[id :tax-profile/user-id uid]]})))
            (is (empty? (biff/q db '{:find [id]
                                     :where [[id :event/user-id uid]]})))
            (is (empty? (biff/q db '{:find [id]
                                     :where [[id :goal/user-id uid]]})))))
        (testing "returns nil when the user doesn't exist"
          (is (nil? (data/delete-user ctx #uuid "ffffffff-ffff-ffff-ffff-ffffffffffff"))))))))


(deftest update-finances-purges-legacy-keys-test
  (let [uid #uuid "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"
        finances-id #uuid "11111111-2222-3333-4444-555555555555"]
    (with-open [node (test-xtdb-node [{:xt/id uid
                                       :user/email "frank@example.com"
                                       :user/password (data/hash-password "frank-pass")
                                       :user/firstname "Frank"
                                       :user/age 30
                                       :user/active true
                                       :user/roles #{:user}}
                                      {:xt/id finances-id
                                       :finances/user-id uid
                                       :finances/bank :fnb
                                       :finances/card-type :credit
                                       :finances/salary 50000
                                       :finances/payday 25}
                                      {:xt/id #uuid "22222222-2222-3333-4444-555555555555"
                                       :budget-item/user-id uid
                                       :budget-item/title "Salary"
                                       :budget-item/type :income
                                       :budget-item/amount 41000}])]
      (testing "updating a user with legacy card-type data doesn't throw"
        (let [ctx (assoc (get-context node)
                         :session {:uid uid}
                         :params {:bank "standard-bank"
                                  :account-type "Private Banking"
                                  :salary "52500"
                                  :payday "25"})
              resp (mid/save-finances ctx)
              db (xt/db node)
              finances (first (biff/q db '{:find (pull finances [*])
                                           :in [user-id]
                                           :where [[finances :finances/user-id user-id]]}
                                      uid))]
          (is (= 303 (:status resp)))
          (is (nil? (:finances/card-type finances)))
          (is (= :standard-bank (:finances/bank finances)))
          (is (= "Private Banking" (:finances/account-type finances)))
          (is (= 52500 (:finances/salary finances))))))))


#_(deftest send-message-test
  (with-open [node (test-xtdb-node [])]
    (let [message (mg/generate :string)
          user    (mg/generate :user main/malli-opts)
          ctx     (assoc (get-context node) :session {:uid (:xt/id user)})
          _       (app/send-message ctx {:text (cheshire/generate-string {:text message})})
          db      (xt/db node) ; get a fresh db value so it contains any transactions
                               ; that send-message submitted.
          doc     (biff/lookup db :msg/text message)]
      (is (some? doc))
      (is (= (:msg/user doc) (:xt/id user))))))

#_(deftest chat-test
  (let [n-messages (+ 3 (rand-int 10))
        now        (java.util.Date.)
        messages   (for [doc (mg/sample :msg (assoc main/malli-opts :size n-messages))]
                     (assoc doc :msg/sent-at now))]
    (with-open [node (test-xtdb-node messages)]
      (let [response (app/chat {:biff/db (xt/db node)})
            html     (rum/render-html response)]
        (is (str/includes? html "Messages sent in the past 10 minutes:"))
        (is (not (str/includes? html "No messages yet.")))
        ;; If you add Jsoup to your dependencies, you can use DOM selectors instead of just regexes:
        ;(is (= n-messages (count (.select (Jsoup/parse html) "#messages > *"))))
        (is (= n-messages (count (re-seq #"init send newMessage to #message-header" html))))
        (is (every? #(str/includes? html (:msg/text %)) messages))))))
