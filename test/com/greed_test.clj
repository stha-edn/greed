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
            [malli.generator :as mg]
            [rum.core :as rum]
            [xtdb.api :as xt]))

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
        (is (str/includes? csp "https://cdn.jsdelivr.net"))))))

(deftest client-ip-test
  (testing "reads the nginx-provided real IP header"
    (is (= "203.0.113.9" (mid/get-client-ip {:headers {"x-real-ip" "203.0.113.9"}}))))
  (testing "falls back to remote-addr"
    (is (= "127.0.0.1" (mid/get-client-ip {:remote-addr "127.0.0.1"})))))

(defn get-context [node]
  {:biff.xtdb/node  node
   :biff/db         (xt/db node)
   :biff/malli-opts #'main/malli-opts})

(deftest save-user-scoped-to-session-test
  (let [alice-id #uuid "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        bob-id   #uuid "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"]
    (with-open [node (test-xtdb-node [{:xt/id      alice-id
                                       :user/email "alice@example.com"
                                       :user/password (data/hash-password "alice-pass")
                                       :user/firstname "Alice"}
                                      {:xt/id      bob-id
                                       :user/email "bob@example.com"
                                       :user/password (data/hash-password "bob-pass")
                                       :user/firstname "Bob"}])]
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
          (is (some? (data/get-event {:biff/db (xt/db node)} event-id))))))))

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
