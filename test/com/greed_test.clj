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
            [com.greed.ui.app.profiles :as profiles]
            [com.greed.ui.app.owner-tax :as owner-tax]
            [com.greed.ui.components.navs :as navs]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.utilities.tax :as tax]
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
                                       :goal/user-id uid}
                                      {:xt/id #uuid "00000000-0000-0000-0000-000000000006"
                                       :statement-summary/user-id uid}])]
      (let [ctx (get-context node)]
        (testing "deletes the user and all of their data"
          (is (= 7 (data/delete-user ctx uid)))
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
                                     :where [[id :goal/user-id uid]]})))
            (is (empty? (biff/q db '{:find [id]
                                     :where [[id :statement-summary/user-id uid]]})))))
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
          ;; the bank is set on Banking now, so Settings leaves it as it was
          (is (= :fnb (:finances/bank finances)))
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


;; ---------------------------------------------------------------------------
;; Statement summaries (Spending tab)

(def ^:private sample-summary
  {:bank :fnb :month "2026-10" :period-start "2026-10-12" :period-end "2026-10-31" :line-count 3
   :money-in-cents 705000 :salary-cents 0 :refunds-cents 0 :money-out-cents 42300
   :transfers-in-cents 20000 :transfers-out-cents 0
   :categories {:fuel 40000 :bank-charges 800 :transport 1500} :fees-cents 800 :fee-count 1
   :top-merchants [{:name "Engen Kyalami" :category :fuel :cents 40000 :count 1}]
   :checks-ok true})

(deftest statement-summaries-test
  (let [uid #uuid "cccccccc-0000-0000-0000-000000000001"]
    (with-open [node (test-xtdb-node [{:xt/id uid
                                       :user/email "sum@example.com"
                                       :user/password (data/hash-password "sum-pass")
                                       :user/firstname "Sipho"
                                       :user/lastname "Nkosi"
                                       :user/age 30
                                       :user/active true}])]
      (let [ctx #(assoc (get-context node) :biff/db (xt/db node))
            save! (fn [& summaries] (let [n (data/save-statement-summaries (ctx) uid summaries)] (xt/sync node) n))]
        (testing "saves a month"
          (is (= 1 (save! sample-summary)))
          (let [[s & more] (data/get-statement-summaries (ctx) uid)]
            (is (nil? more))
            (is (= 705000 (:statement-summary/money-in-cents s)))
            (is (= {:fuel 40000 :bank-charges 800 :transport 1500} (:statement-summary/categories s)))))
        (testing "the same month again replaces it, not adds to it"
          (save! (assoc sample-summary :money-in-cents 1))
          (let [all (data/get-statement-summaries (ctx) uid)]
            (is (= 1 (count all)))
            (is (= 1 (:statement-summary/money-in-cents (first all))))))
        (testing "a shorter piece of the same month does not replace a longer one"
          (is (= 0 (save! (assoc sample-summary :period-start "2026-10-25" :money-in-cents 2))))
          (is (= 1 (:statement-summary/money-in-cents (first (data/get-statement-summaries (ctx) uid))))))
        (testing "other months and banks are kept apart, newest month first"
          (save! (assoc sample-summary :month "2026-11" :period-start "2026-11-01" :period-end "2026-11-30")
                 (assoc sample-summary :bank :standard-bank))
          (is (= [["2026-11" :fnb] ["2026-10" :standard-bank] ["2026-10" :fnb]]
                 (map (juxt :statement-summary/month :statement-summary/bank)
                      (data/get-statement-summaries (ctx) uid)))))))))


;; ---------------------------------------------------------------------------
;; Businesses

(def ^:private business-signup
  {:account-type "business" :business-name "  Ngunitek  " :entity-type "pty-ltd"
   :firstname "Sipho" :lastname "Nkosi" :email "sipho@example.com" :password "sipho-pass"})

(defn- fresh [node] (get-context node))

(deftest business-signup-test
  (with-open [node (test-xtdb-node [])]
    (testing "a business sign-up creates the person, the business and the owner link"
      (is (= {:valid? true} (auth/signup! (assoc (fresh node) :params business-signup))))
      (xt/sync node)
      (let [ctx  (fresh node)
            uid  (biff/lookup-id (:biff/db ctx) :user/email "sipho@example.com")
            [b & more] (data/get-user-businesses ctx uid)]
        (is (some? uid))
        (is (nil? (:user/age (data/get-user ctx uid))) "no age is asked of a business sign-up")
        (is (nil? more))
        (is (= ["Ngunitek" :pty-ltd :owner]
               ((juxt :business/name :business/entity-type :business-member/role) b)))
        (testing "and the person starts in their new business"
          (let [resp (with-redefs [email/send-email (constantly true)]
                       (home/signup-success-page (assoc ctx :params business-signup :session {})))]
            (is (= (:xt/id b) (get-in resp [:session :business-id])))))))))

(deftest business-signup-errors-test
  (with-open [node (test-xtdb-node [])]
   (doseq [[params error] [[(assoc business-signup :business-name " ") :business-name-blank]
                          [(assoc business-signup :entity-type "llc") :entity-type-invalid]
                          [(assoc business-signup :business-name (apply str (repeat 121 "x"))) :business-name-too-long]]]
    (is (= {:valid? false :error error} (auth/signup! (assoc (fresh node) :params params))))
    (xt/sync node)
    (is (nil? (biff/lookup-id (xt/db node) :user/email "sipho@example.com"))
        "nothing is created when the business details are wrong"))))

(deftest personal-signup-still-takes-age-test
  (with-open [node (test-xtdb-node [])]
    (is (= {:valid? true}
           (auth/signup! (assoc (fresh node) :params {:firstname "Lerato" :lastname "M" :age "31"
                                                      :email "lerato@example.com" :password "pw"}))))
    (xt/sync node)
    (let [ctx (fresh node)
          uid (biff/lookup-id (:biff/db ctx) :user/email "lerato@example.com")]
      (is (= 31 (:user/age (data/get-user ctx uid))))
      (is (empty? (data/get-user-businesses ctx uid))))))

(deftest choose-profile-test
  (let [uid   #uuid "aaaaaaaa-0000-0000-0000-000000000001"
        other #uuid "aaaaaaaa-0000-0000-0000-000000000002"
        mine  #uuid "bbbbbbbb-0000-0000-0000-000000000001"
        theirs #uuid "bbbbbbbb-0000-0000-0000-000000000002"
        now   (java.util.Date.)]
    (with-open [node (test-xtdb-node [{:xt/id mine :business/name "Mine" :business/entity-type :trust :business/created-at now}
                                      {:xt/id theirs :business/name "Theirs" :business/entity-type :npc :business/created-at now}
                                      {:xt/id (random-uuid) :business-member/business-id mine :business-member/user-id uid
                                       :business-member/role :owner :business-member/created-at now}
                                      {:xt/id (random-uuid) :business-member/business-id theirs :business-member/user-id other
                                       :business-member/role :owner :business-member/created-at now}])]
      (let [ctx (fresh node)]
        (testing "choosing your own business"
          (is (= mine (get-in (profiles/choose (assoc ctx :session {:uid uid} :params {:business-id (str mine)}))
                              [:session :business-id]))))
        (testing "someone else's business is ignored"
          (is (nil? (get-in (profiles/choose (assoc ctx :session {:uid uid :business-id mine}
                                                    :params {:business-id (str theirs)}))
                            [:session :business-id]))))
        (testing "Personal clears the business"
          (is (nil? (get-in (profiles/choose (assoc ctx :session {:uid uid :business-id mine} :params {:business-id ""}))
                            [:session :business-id]))))
        (testing "a business you are not in is not your session's business"
          (is (nil? (data/session-business (assoc ctx :session {:uid uid :business-id theirs}))))
          (is (= mine (:xt/id (data/session-business (assoc ctx :session {:uid uid :business-id mine}))))))))))

(deftest delete-user-with-businesses-test
  (let [uid    #uuid "cccccccc-1111-0000-0000-000000000001"
        partner #uuid "cccccccc-1111-0000-0000-000000000002"
        solo   #uuid "dddddddd-1111-0000-0000-000000000001"
        shared #uuid "dddddddd-1111-0000-0000-000000000002"
        now    (java.util.Date.)
        member (fn [b u] {:xt/id (random-uuid) :business-member/business-id b :business-member/user-id u
                          :business-member/role :owner :business-member/created-at now})]
    (with-open [node (test-xtdb-node [{:xt/id uid :user/email "gone@example.com" :user/password "x"
                                       :user/firstname "G" :user/lastname "One" :user/active true}
                                      {:xt/id solo :business/name "Solo" :business/entity-type :sole-proprietor :business/created-at now}
                                      {:xt/id shared :business/name "Shared" :business/entity-type :partnership :business/created-at now}
                                      (member solo uid) (member shared uid) (member shared partner)])]
      (data/delete-user (fresh node) uid)
      (xt/sync node)
      (let [ctx (fresh node)]
        (testing "a business only they were in goes with them"
          (is (nil? (xt/entity (:biff/db ctx) solo))))
        (testing "a business with another member stays, without them"
          (is (= ["Shared"] (map :business/name (data/get-user-businesses ctx partner))))
          (is (empty? (data/get-user-businesses ctx uid))))))))

(deftest business-money-is-kept-apart-test
  (let [uid   #uuid "eeeeeeee-2222-0000-0000-000000000001"
        stranger #uuid "eeeeeeee-2222-0000-0000-000000000002"
        biz   #uuid "ffffffff-2222-0000-0000-000000000001"
        now   (java.util.Date.)
        item  (fn [id title & [business-id]]
                (cond-> {:xt/id id :budget-item/user-id uid :budget-item/title title
                         :budget-item/type :expenses :budget-item/amount 100}
                  business-id (assoc :budget-item/business-id business-id)))
        month (fn [id & [business-id]]
                (cond-> (assoc sample-summary :xt/id id :statement-summary/user-id uid)
                  true (update-keys #(if (qualified-keyword? %) % (keyword "statement-summary" (name %))))
                  business-id (assoc :statement-summary/business-id business-id)))
        own-item #uuid "11111111-2222-0000-0000-000000000001"
        biz-item #uuid "11111111-2222-0000-0000-000000000002"]
    (with-open [node (test-xtdb-node [{:xt/id biz :business/name "Biz" :business/entity-type :pty-ltd :business/created-at now}
                                      {:xt/id (random-uuid) :business-member/business-id biz :business-member/user-id uid
                                       :business-member/role :owner :business-member/created-at now}
                                      (item own-item "Rent")
                                      (item biz-item "Office" biz)
                                      (month #uuid "22222222-2222-0000-0000-000000000001")
                                      (month #uuid "22222222-2222-0000-0000-000000000002" biz)])]
      (let [ctx      (fresh node)
            personal (assoc ctx :session {:uid uid})
            in-biz   (assoc ctx :session {:uid uid :business-id biz})]
        (testing "personal and business budget items never mix"
          (is (= ["Rent"] (map :budget-item/title (data/get-budget-items ctx uid))))
          (is (= ["Office"] (map :budget-item/title (data/get-owner-budget-items ctx (data/owner in-biz)))))
          (is (= ["Rent"] (map :budget-item/title (data/get-owner-budget-items ctx (data/owner personal))))))
        (testing "nor do statement months"
          (is (= 1 (count (data/get-statement-summaries ctx uid))))
          (is (= [biz] (map :statement-summary/business-id (data/get-statement-summaries ctx (data/owner in-biz))))))
        (testing "a member can change the business's items, others cannot"
          (is (data/can-change-budget-item? personal (data/get-budget-item ctx biz-item)))
          (is (not (data/can-change-budget-item? (assoc ctx :session {:uid stranger}) (data/get-budget-item ctx biz-item))))
          (is (not (data/can-change-budget-item? (assoc ctx :session {:uid stranger}) (data/get-budget-item ctx own-item)))))
        (testing "an item added while in the business belongs to it, with its category"
          (data/upsert-budget-item (assoc in-biz :params {:title "Stock" :type "expenses" :amount "500"
                                                          :category "stock" :reference "makro"}))
          (xt/sync node)
          (let [stock (first (filter #(= "Stock" (:budget-item/title %))
                                     (data/get-business-budget-items (fresh node) biz)))]
            (is (= [biz :stock "makro"] ((juxt :budget-item/business-id :budget-item/category :budget-item/reference) stock)))
            (is (not-any? #(= "Stock" (:budget-item/title %)) (data/get-budget-items (fresh node) uid)))))
        (testing "a personal item keeps a personal category"
          (data/upsert-budget-item (assoc personal :params {:title "Life cover" :type "expenses" :amount "1" :category "insurance"}))
          (xt/sync node)
          (is (= :insurance (:budget-item/category (first (filter #(= "Life cover" (:budget-item/title %))
                                                                  (data/get-budget-items (fresh node) uid)))))))
        (testing "a business category is not kept on a personal item"
          (data/upsert-budget-item (assoc personal :params {:title "Shop" :type "expenses" :amount "1" :category "stock"}))
          (xt/sync node)
          (is (nil? (:budget-item/category (first (filter #(= "Shop" (:budget-item/title %))
                                                          (data/get-budget-items (fresh node) uid)))))))
        (testing "deleting the person takes the business's items and months with it"
          (data/delete-user (fresh node) uid)
          (xt/sync node)
          (let [db (xt/db node)]
            (is (nil? (xt/entity db biz)))
            (is (nil? (xt/entity db biz-item)))
            (is (empty? (biff/q db '{:find [s] :where [[s :statement-summary/business-id]]})))))))))

(deftest business-members-test
  (let [owner  #uuid "aaaaaaaa-3333-0000-0000-000000000001"
        thandi #uuid "aaaaaaaa-3333-0000-0000-000000000002"
        eve    #uuid "aaaaaaaa-3333-0000-0000-000000000003"
        biz    #uuid "bbbbbbbb-3333-0000-0000-000000000001"
        now    (java.util.Date.)
        user   (fn [id email first] {:xt/id id :user/email email :user/password "x" :user/firstname first
                                     :user/lastname "Test" :user/active true})]
    (with-open [node (test-xtdb-node [(user owner "owner@example.com" "Olu")
                                      (user thandi "Thandi@Example.com" "Thandi")
                                      (user eve "eve@example.com" "Eve")
                                      {:xt/id biz :business/name "Biz" :business/entity-type :pty-ltd :business/created-at now}
                                      {:xt/id (random-uuid) :business-member/business-id biz :business-member/user-id owner
                                       :business-member/role :owner :business-member/created-at now}])]
      (let [ctx    #(fresh node)
            sync!  #(xt/sync node)
            member-of (fn [uid] (some #(when (= uid (:business-member/user-id %)) %) (data/get-business-members (ctx) biz)))]
        (testing "only an owner can invite, and the email must be usable"
          (is (= {:error :not-owner} (data/invite-member! (ctx) eve biz {:email "x@example.com" :role "member"})))
          (is (= {:error :invalid-email} (data/invite-member! (ctx) owner biz {:email "not an email" :role "member"})))
          (is (= {:error :invalid-role} (data/invite-member! (ctx) owner biz {:email "x@example.com" :role "admin"})))
          (is (= {:error :already-member} (data/invite-member! (ctx) owner biz {:email "OWNER@example.com" :role "member"}))))
        (let [{:keys [invite]} (data/invite-member! (ctx) owner biz {:email " thandi@example.COM " :role "member"})
              invite-id (:xt/id invite)]
          (sync!)
          (testing "the invitation is saved in lower case, and not twice"
            (is (= "thandi@example.com" (:business-invite/email invite)))
            (is (= {:error :already-invited} (data/invite-member! (ctx) owner biz {:email "thandi@example.com" :role "owner"}))))
          (testing "it waits for the invited person, and only them"
            (is (= [biz] (map (comp :xt/id :business) (data/get-user-invites (ctx) thandi))))
            (is (empty? (data/get-user-invites (ctx) eve)))
            (is (nil? (data/accept-invite! (ctx) eve invite-id)) "someone else cannot accept it")
            (is (nil? (data/decline-invite! (ctx) eve invite-id))))
          (testing "accepting makes them a member and uses the invitation up"
            (is (= biz (data/accept-invite! (ctx) thandi invite-id)))
            (sync!)
            (is (= :member (:business-member/role (member-of thandi))))
            (is (empty? (data/get-user-invites (ctx) thandi)))
            (is (empty? (data/get-business-invites (ctx) biz)))))
        (testing "a member cannot manage the business"
          (is (= :not-owner (data/change-member-role! (ctx) thandi (:xt/id (member-of owner)) "member")))
          (is (= :not-owner (data/update-business! (ctx) thandi biz {:business-name "X" :entity-type "trust"})))
          (is (nil? (data/delete-business! (ctx) thandi biz))))
        (testing "the last owner cannot stop being one, or leave"
          (is (= :last-owner (data/change-member-role! (ctx) owner (:xt/id (member-of owner)) "member")))
          (is (= :last-owner (data/remove-member! (ctx) owner (:xt/id (member-of owner))))))
        (testing "with a second owner, the first can leave"
          (is (= :ok (data/change-member-role! (ctx) owner (:xt/id (member-of thandi)) "owner")))
          (sync!)
          (is (= :ok (data/remove-member! (ctx) owner (:xt/id (member-of owner)))))
          (sync!)
          (is (nil? (member-of owner))))
        (testing "an owner renames the business"
          (is (= :ok (data/update-business! (ctx) thandi biz {:business-name " New name " :entity-type "trust"})))
          (sync!)
          (is (= ["New name" :trust] ((juxt :business/name :business/entity-type) (xt/entity (xt/db node) biz)))))
        (testing "declining and expired invitations"
          (let [{:keys [invite]} (data/invite-member! (ctx) thandi biz {:email "eve@example.com" :role "member"})]
            (sync!)
            (is (true? (data/decline-invite! (ctx) eve (:xt/id invite))))
            (sync!)
            (is (empty? (data/get-user-invites (ctx) eve))))
          (xt/await-tx node (xt/submit-tx node [[::xt/put {:xt/id (random-uuid) :business-invite/business-id biz
                                                            :business-invite/email "eve@example.com" :business-invite/role :member
                                                            :business-invite/invited-by thandi
                                                            :business-invite/created-at (java.util.Date. (- (System/currentTimeMillis)
                                                                                                           (* 15 24 60 60 1000)))}]]))
          (is (empty? (data/get-user-invites (ctx) eve)) "a 15-day-old invitation has expired"))
        (testing "an owner deletes the business and everything in it"
          (is (true? (data/delete-business! (ctx) thandi biz)))
          (sync!)
          (is (nil? (xt/entity (xt/db node) biz)))
          (is (empty? (data/get-user-businesses (ctx) thandi)))
          (is (empty? (biff/q (xt/db node) '{:find [i] :where [[i :business-invite/business-id]]}))))))))

(deftest add-business-test
  (let [uid #uuid "aaaaaaaa-4444-0000-0000-000000000001"]
    (with-open [node (test-xtdb-node [{:xt/id uid :user/email "add@example.com" :user/password "x" :user/firstname "A"
                                       :user/lastname "B" :user/active true}])]
      (is (= {:error :business-name-blank} (data/add-business! (fresh node) uid {:business-name "" :entity-type "trust"})))
      (let [{:keys [business-id]} (data/add-business! (fresh node) uid {:business-name "Side hustle" :entity-type "sole-proprietor"})]
        (xt/sync node)
        (is (= [[business-id :owner]]
               (map (juxt :xt/id :business-member/role) (data/get-user-businesses (fresh node) uid))))))))

(deftest vat-settings-test
  (let [owner #uuid "aaaaaaaa-5555-0000-0000-000000000001"
        member #uuid "aaaaaaaa-5555-0000-0000-000000000002"
        biz   #uuid "bbbbbbbb-5555-0000-0000-000000000001"
        now   (java.util.Date.)
        join  (fn [u role] {:xt/id (random-uuid) :business-member/business-id biz :business-member/user-id u
                            :business-member/role role :business-member/created-at now})]
    (with-open [node (test-xtdb-node [{:xt/id biz :business/name "Biz" :business/entity-type :pty-ltd :business/created-at now}
                                      (join owner :owner) (join member :member)])]
      (let [ctx #(fresh node)
            biz-doc #(xt/entity (xt/db node) biz)]
        (is (= :not-owner (data/update-vat-settings! (ctx) member biz {:vat-registered "true" :vat-number "4123456789" :vat-category "a"})))
        (is (= :vat-number-invalid (data/update-vat-settings! (ctx) owner biz {:vat-registered "true" :vat-number "123" :vat-category "a"})))
        (is (= :vat-category-invalid (data/update-vat-settings! (ctx) owner biz {:vat-registered "true" :vat-number "4123456789"})))
        (is (= :ok (data/update-vat-settings! (ctx) owner biz {:vat-registered "true" :vat-number "412 345 6789" :vat-category "b"})))
        (xt/sync node)
        (is (= [true "4123456789" :b] ((juxt :business/vat-registered :business/vat-number :business/vat-category) (biz-doc))))
        (testing "a VAT type is kept on the business's items"
          (data/upsert-budget-item (assoc (ctx) :session {:uid owner :business-id biz}
                                          :params {:title "Sales" :type "income" :amount "1000" :vat "zero"}))
          (xt/sync node)
          (is (= [:zero] (map :budget-item/vat (data/get-business-budget-items (ctx) biz)))))
        (testing "deregistering clears the number and period"
          (is (= :ok (data/update-vat-settings! (ctx) owner biz {})))
          (xt/sync node)
          (is (= [false nil nil] ((juxt :business/vat-registered :business/vat-number :business/vat-category) (biz-doc)))))))))

(deftest tax-settings-test
  (let [owner #uuid "aaaaaaaa-6666-0000-0000-000000000001"
        member #uuid "aaaaaaaa-6666-0000-0000-000000000002"
        biz   #uuid "bbbbbbbb-6666-0000-0000-000000000001"
        now   (java.util.Date.)
        join  (fn [u role] {:xt/id (random-uuid) :business-member/business-id biz :business-member/user-id u
                            :business-member/role role :business-member/created-at now})]
    (with-open [node (test-xtdb-node [{:xt/id biz :business/name "Biz" :business/entity-type :pty-ltd :business/created-at now}
                                      (join owner :owner) (join member :member)])]
      (is (= :not-owner (data/update-tax-settings! (fresh node) member biz {:year-end-month "6" :sbc "true"})))
      (is (= :year-end-invalid (data/update-tax-settings! (fresh node) owner biz {:year-end-month "13"})))
      (is (= :ok (data/update-tax-settings! (fresh node) owner biz {:year-end-month "6" :sbc "true"})))
      (xt/sync node)
      (is (= [6 true] ((juxt :business/year-end-month :business/sbc) (xt/entity (xt/db node) biz)))))))

(deftest owner-pay-test
  (let [a #uuid "aaaaaaaa-7777-0000-0000-000000000001"
        b #uuid "aaaaaaaa-7777-0000-0000-000000000002"
        m (fn [u role & [share]] (cond-> {:business-member/user-id u :business-member/role role}
                                   share (assoc :business-member/profit-share share)))
        drawings (fn [amount & [to]] (cond-> {:budget-item/type :expenses :budget-item/category :drawings :budget-item/amount amount}
                                       to (assoc :budget-item/paid-to to)))]
    (testing "with one owner, drawings go to them unless someone else is chosen"
      (is (= {a 5000} (data/owner-pay [(drawings 3000) (drawings 2000) {:budget-item/type :expenses :budget-item/amount 999}]
                                       [(m a :owner)])))
      (is (= {a 3000 b 2000} (data/owner-pay [(drawings 3000) (drawings 2000 b)] [(m a :owner) (m b :member)]))))
    (testing "with two owners, only chosen drawings count"
      (is (= {b 2000} (data/owner-pay [(drawings 3000) (drawings 2000 b)] [(m a :owner) (m b :owner)]))))
    (testing "profit shares: equal until set"
      (is (= {a 50.0 b 50.0} (data/profit-shares [(m a :owner) (m b :owner)])))
      (is (= {a 70 b 30} (data/profit-shares [(m a :owner 70) (m b :owner 30)]))))))

(deftest owner-pay-sync-test
  (let [uid #uuid "aaaaaaaa-8888-0000-0000-000000000001"
        biz #uuid "bbbbbbbb-8888-0000-0000-000000000001"
        now (java.util.Date.)]
    (with-open [node (test-xtdb-node [{:xt/id uid :user/email "o@example.com" :user/password "x" :user/firstname "O"
                                       :user/lastname "P" :user/active true}
                                      {:xt/id biz :business/name "Biz" :business/entity-type :sole-proprietor :business/created-at now}
                                      {:xt/id (random-uuid) :business-member/business-id biz :business-member/user-id uid
                                       :business-member/role :owner :business-member/created-at now}])]
      (let [in-biz #(assoc (fresh node) :session {:uid uid :business-id biz})
            from   #(first (filter :budget-item/source-business-id (data/get-budget-items (fresh node) uid)))]
        (testing "drawings in the business become personal income"
          (data/upsert-budget-item (assoc (in-biz) :params {:title "Drawings" :type "expenses" :amount "5000" :category "drawings"}))
          (data/sync-owner-pay! (fresh node) biz)
          (xt/sync node)
          (is (= ["From Biz" :income 5000] ((juxt :budget-item/title :budget-item/type :budget-item/amount) (from)))))
        (testing "it cannot be deleted from the personal side"
          (data/delete-budget-item (assoc (fresh node) :session {:uid uid} :params {:budget-item-id (str (:xt/id (from)))}))
          (xt/sync node)
          (is (some? (from))))
        (testing "renaming the business renames it"
          (data/update-business! (fresh node) uid biz {:business-name "Renamed" :entity-type "sole-proprietor"})
          (xt/sync node)
          (is (= "From Renamed" (:budget-item/title (from)))))
        (testing "without drawings it goes"
          (let [item (first (data/get-business-budget-items (fresh node) biz))]
            (data/delete-budget-item (assoc (in-biz) :params {:budget-item-id (str (:xt/id item))}))
            (xt/sync node)
            (data/sync-owner-pay! (fresh node) biz)
            (xt/sync node)
            (is (nil? (from)))))))))

(deftest profit-shares-settings-test
  (let [a #uuid "aaaaaaaa-9999-0000-0000-000000000001"
        b #uuid "aaaaaaaa-9999-0000-0000-000000000002"
        biz #uuid "bbbbbbbb-9999-0000-0000-000000000001"
        ma #uuid "cccccccc-9999-0000-0000-000000000001"
        mb #uuid "cccccccc-9999-0000-0000-000000000002"
        now (java.util.Date.)]
    (with-open [node (test-xtdb-node [{:xt/id biz :business/name "P" :business/entity-type :partnership :business/created-at now}
                                      {:xt/id ma :business-member/business-id biz :business-member/user-id a :business-member/role :owner :business-member/created-at now}
                                      {:xt/id mb :business-member/business-id biz :business-member/user-id b :business-member/role :owner :business-member/created-at now}])]
      (let [k #(keyword (str "share-" %))]
        (is (= :shares-not-100 (data/update-profit-shares! (fresh node) a biz {(k ma) "60" (k mb) "30"})))
        (is (= :ok (data/update-profit-shares! (fresh node) a biz {(k ma) "60" (k mb) "40"})))
        (xt/sync node)
        (is (= {a 60 b 40} (data/profit-shares (data/get-business-members (fresh node) biz))))))))

(deftest personal-tax-with-business-test
  (testing "the business profit is added to the salary"
    (let [{:keys [with without extra]} (owner-tax/tax-with-business 300000 200000 40)]
      (is (= without (:net-tax (tax/calculate-income-tax 300000 40))))
      (is (= with (:net-tax (tax/calculate-income-tax 500000 40))))
      (is (= extra (- with without)))
      (is (pos? extra))))
  (testing "a loss lowers it, but taxable income never goes below zero"
    (is (= 0 (:with (owner-tax/tax-with-business 0 -50000 40)))))
  (testing "dividends tax is 20%"
    (is (= 20 (owner-tax/dividends-rate)))))

(deftest profile-switcher-for-everyone-test
  (let [uid #uuid "aaaaaaaa-aaaa-0000-0000-000000000001"]
    (with-open [node (test-xtdb-node [{:xt/id uid :user/email "solo@example.com" :user/password "x" :user/firstname "Solo"
                                       :user/lastname "Person" :user/active true}])]
      (let [html #(rum/render-static-markup (navs/sidebar (assoc (fresh node) :session %1 :uri "/app")))]
        (testing "a personal sign-up sees the switcher, with a way to add a business"
          (let [h (html {:uid uid})]
            (is (str/includes? h "Personal"))
            (is (str/includes? h "Add a business"))
            (is (str/includes? h "/app/choose?add=1"))))
        (testing "once they add one, they land in it and can switch"
          (let [{:keys [business-id]} (data/add-business! (fresh node) uid {:business-name "Side Co" :entity-type "pty-ltd"})]
            (xt/sync node)
            (let [h (html {:uid uid :business-id business-id})]
              (is (str/includes? h "Side Co"))
              (is (str/includes? h ">Switch<")))))))))

(deftest business-goals-and-events-test
  (let [uid #uuid "aaaaaaaa-bbbb-0000-0000-000000000001"
        other #uuid "aaaaaaaa-bbbb-0000-0000-000000000002"
        biz #uuid "bbbbbbbb-bbbb-0000-0000-000000000001"
        now (java.util.Date.)]
    (with-open [node (test-xtdb-node [{:xt/id biz :business/name "Biz" :business/entity-type :pty-ltd :business/created-at now}
                                      {:xt/id (random-uuid) :business-member/business-id biz :business-member/user-id uid
                                       :business-member/role :owner :business-member/created-at now}])]
      (let [personal #(assoc (fresh node) :session {:uid uid})
            in-biz   #(assoc (fresh node) :session {:uid uid :business-id biz})]
        (data/upsert-goal (assoc (personal) :params {:title "Holiday" :target "10000" :saved "0"}))
        (data/upsert-goal (assoc (in-biz) :params {:title "Tax reserve" :target "50000" :saved "0"}))
        (data/create-event (assoc (personal) :params {:title "Dentist" :type "bill" :date "2026-12-01"}))
        (data/create-event (assoc (in-biz) :params {:title "Client meeting" :type "bill" :date "2026-12-02"}))
        (xt/sync node)
        (testing "each profile has its own goals and events"
          (is (= ["Holiday"] (map :goal/title (data/get-goals (fresh node) uid))))
          (is (= ["Tax reserve"] (map :goal/title (data/get-goals (fresh node) {:business-id biz}))))
          (is (= ["Dentist"] (map :event/title (data/get-events (fresh node) uid))))
          (is (= ["Client meeting"] (map :event/title (data/get-events (fresh node) {:business-id biz})))))
        (testing "someone outside the business cannot remove its event"
          (let [ev (first (data/get-events (fresh node) {:business-id biz}))]
            (data/delete-event (assoc (fresh node) :session {:uid other} :params {:event-id (str (:xt/id ev))}))
            (xt/sync node)
            (is (= 1 (count (data/get-events (fresh node) {:business-id biz}))))))
        (testing "deleting the business takes its goals and events, not personal ones"
          (data/delete-business! (fresh node) uid biz)
          (xt/sync node)
          (is (empty? (data/get-goals (fresh node) {:business-id biz})))
          (is (empty? (data/get-events (fresh node) {:business-id biz})))
          (is (= ["Holiday"] (map :goal/title (data/get-goals (fresh node) uid)))))))))


(deftest bank-details-test
  (let [uid #uuid "eeeeeeee-1212-0000-0000-000000000001"]
    (with-open [node (test-xtdb-node [{:xt/id uid :user/email "bank@example.com" :user/password "x" :user/firstname "B"
                                       :user/lastname "K" :user/age 30 :user/active true}])]
      (let [ctx #(assoc (fresh node) :session {:uid uid} :params %)
            fin #(data/get-finances (fresh node) uid)
            banks #{:fnb :standard-bank}]
        (testing "the bank can be set before salary and payday"
          (is (= :bank-invalid (data/save-bank-details! (ctx {:bank "monzo"}) banks)))
          (is (= :ok (data/save-bank-details! (ctx {:bank "fnb" :account-type "Aspire"}) banks)))
          (xt/sync node)
          (is (= [:fnb "Aspire" nil] ((juxt :finances/bank :finances/account-type :finances/salary) (fin)))))
        (testing "then Settings adds salary and payday, keeps the bank, and makes the Salary item"
          (mid/save-finances (ctx {:salary "40000" :payday "25"}))
          (xt/sync node)
          (is (= [:fnb 40000 25] ((juxt :finances/bank :finances/salary :finances/payday) (fin))))
          (is (some? (data/get-salary-budget-item (fresh node) uid))))
        (testing "changing bank keeps salary"
          (data/save-bank-details! (ctx {:bank "standard-bank"}) banks)
          (xt/sync node)
          (is (= [:standard-bank nil 40000] ((juxt :finances/bank :finances/account-type :finances/salary) (fin)))))))))

(deftest banking-coverage-and-fees-test
  (let [coverage (requiring-resolve 'com.greed.ui.app.banking/coverage)
        fees     (requiring-resolve 'com.greed.ui.app.banking/monthly-fees)
        s (fn [bank month start end fee n]
            {:statement-summary/bank bank :statement-summary/month month
             :statement-summary/period-start start :statement-summary/period-end end
             :statement-summary/fees-cents fee :statement-summary/fee-count n})
        ss [(s :fnb "2026-10" "2026-10-01" "2026-10-31" 1500 3)
            (s :fnb "2026-11" "2026-11-01" "2026-11-10" 500 1)
            (s :standard-bank "2026-10" "2026-10-01" "2026-10-31" 2000 2)]]
    (testing "each bank's last months: full, part or missing"
      (is (= [:missing :full :partial]
             (map :state (take-last 3 (get (coverage ss 12 (java.time.LocalDate/parse "2026-11-15")) :fnb))))))
    (testing "fees per month, all banks together"
      (is (= [{:month "2026-10" :cents 3500 :count 5} {:month "2026-11" :cents 500 :count 1}] (fees ss 6))))))

(deftest items-added-from-a-statement-are-tagged-test
  (let [uid #uuid "eeeeeeee-3434-0000-0000-000000000001"]
    (with-open [node (test-xtdb-node [{:xt/id uid :user/email "tag@example.com" :user/password "x" :user/firstname "T"
                                       :user/lastname "G" :user/active true}])]
      (let [add! (fn [p] (data/upsert-budget-item (assoc (fresh node) :session {:uid uid} :params p)))]
        (add! {:title "Rent" :type "expenses" :amount "8000"})
        (add! {:title "Netcash" :type "expenses" :amount "100" :from-statement "2026-10"})
        (add! {:title "Odd" :type "expenses" :amount "1" :from-statement "not a month"})
        (xt/sync node)
        (is (= {"Rent" nil "Netcash" "2026-10" "Odd" nil}
               (into {} (map (juxt :budget-item/title :budget-item/from-statement))
                     (data/get-budget-items (fresh node) uid))))))))

(deftest statement-origins-test
  (let [item (fn [id type title amount & [reference]]
               (cond-> {:xt/id id :budget-item/type type :budget-item/title title :budget-item/amount amount}
                 reference (assoc :budget-item/reference reference)))
        items [(item 1 :expenses "ADMYT PARKING" 11 "ADMYT PARKING")   ; added from October
               (item 2 :expenses "Mom" 2000 "Mom")                     ; planned, no such payee
               (item 3 :expenses "Engen" 999 "Engen")                  ; amount differs: planned
               (item 4 :income "Acme Holdings" 5000)                   ; payer in October
               (item 5 :expenses "Rent" 8000)                          ; no reference
               (assoc (item 6 :expenses "Netcash" 100 "Netcash") :budget-item/from-statement "2026-09")]
        months [{:statement-summary/month "2026-10"
                 :statement-summary/payees [{:name "ADMYT PARKING" :text "ADMYT PARKING CHEQUE CARD PURCHASE" :spent-cents 1100 :refund-cents 0}
                                            {:name "Engen" :text "ENGEN" :spent-cents 40000 :refund-cents 0}
                                            {:name "Netcash" :text "Netcash" :spent-cents 10000 :refund-cents 0}]
                 :statement-summary/income-payees [{:name "Acme Holdings" :cents 500000}]}]]
    (is (= {1 "2026-10" 4 "2026-10"} (data/statement-origins items months)))))

(deftest toasts-test
  (let [uid #uuid "eeeeeeee-5656-0000-0000-000000000001"]
    (with-open [node (test-xtdb-node [{:xt/id uid :user/email "t@example.com" :user/password "x" :user/firstname "Thandi"
                                       :user/lastname "Z" :user/active true}])]
      (let [html (fn [params session & opts]
                   (some-> (apply alerts/toasts (assoc (fresh node) :params params :session session :uri "/app/finances/") opts)
                           rum/render-static-markup))
            seen {:uid uid :app-update-seen-version (:app/version com.core/common-config)}]
        (testing "signing in greets the person by name"
          (is (str/includes? (html {:success "signin"} seen :in-app? true) "Welcome back, Thandi.")))
        (testing "a saved notice hides by itself; an error stays"
          (is (str/includes? (html {:alert "goal-saved"} seen :in-app? true) "wait 6s"))
          (is (not (str/includes? (html {:alert "email-taken"} seen :in-app? true) "wait 6s"))))
        (testing "the update notice shows until acknowledged, and returns to the same page"
          (let [h (html {} {:uid uid} :in-app? true)]
            (is (str/includes? h "What's new"))
            (is (str/includes? h "/app/finances/")))
          (is (nil? (html {} seen :in-app? true))))
        (testing "signing out says so on the public page"
          (is (str/includes? (html {:alert "signed-out"} {}) "signed out")))
        (testing "dismissing goes back to an app page only"
          (is (= "/app/finances/" (get-in (mid/dismiss-app-update-banner {:session {} :params {:next "/app/finances/"}}) [:headers "location"])))
          (is (= "/app" (get-in (mid/dismiss-app-update-banner {:session {} :params {:next "https://evil.example"}}) [:headers "location"]))))))))

(deftest budget-periods-test
  (let [items [{:budget-item/title "Rent" :budget-item/type :expenses :budget-item/amount 8000}
               {:budget-item/title "Apple" :budget-item/type :expenses :budget-item/amount 15 :budget-item/from-statement "2026-10"}
               {:budget-item/title "Afrihost" :budget-item/type :expenses :budget-item/amount 9 :budget-item/from-statement "2026-11"}
               {:budget-item/title "Sales" :budget-item/type :income :budget-item/amount 11500}]]
    (testing "monthly items count in every month; statement items only in their month"
      (is (= ["Rent" "Apple" "Sales"] (map :budget-item/title (data/items-for-month items "2026-10"))))
      (is (= ["Rent" "Afrihost" "Sales"] (map :budget-item/title (data/items-for-month items "2026-11"))))
      (is (= ["Rent" "Sales"] (map :budget-item/title (data/items-for-month items "2026-12")))))
    (testing "yearly estimates use monthly items only"
      (is (= (* 12 (- 1150000 800000)) (com.greed.utilities.company-tax/budget-taxable items false))))))

(deftest make-monthly-test
  (let [uid #uuid "eeeeeeee-7878-0000-0000-000000000001"]
    (with-open [node (test-xtdb-node [{:xt/id uid :user/email "m@example.com" :user/password "x" :user/firstname "M"
                                       :user/lastname "N" :user/active true}])]
      (data/upsert-budget-item (assoc (fresh node) :session {:uid uid}
                                      :params {:title "Apple" :type "expenses" :amount "15" :from-statement "2026-10"}))
      (xt/sync node)
      (let [item (first (data/get-budget-items (fresh node) uid))]
        (is (not (data/make-monthly! (assoc (fresh node) :session {:uid (random-uuid)} :params {:budget-item-id (str (:xt/id item))})))
            "someone else cannot change it")
        (is (true? (data/make-monthly! (assoc (fresh node) :session {:uid uid} :params {:budget-item-id (str (:xt/id item))}))))
        (xt/sync node)
        (is (nil? (:budget-item/from-statement (first (data/get-budget-items (fresh node) uid)))))))))
