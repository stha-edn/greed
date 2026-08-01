(ns com.greed-test
  (:require [cheshire.core :as cheshire]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.biffweb :as biff :refer [test-xtdb-node]]
            [com.greed :as main]
            [com.greed.app :as app]
            [com.greed.authentication :as auth]
            [com.greed.data.core :as data]
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

#_(defn get-context [node]
  {:biff.xtdb/node  node
   :biff/db         (xt/db node)
   :biff/malli-opts #'main/malli-opts})

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
