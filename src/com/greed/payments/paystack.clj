(ns com.greed.payments.paystack
  "The Paystack API, as greed uses it: plans, checkout, verifying a
   payment, the billing page, cancelling, and checking a webhook's
   signature. Every call returns {:ok data} or {:error message}; nothing
   here writes to the database (see com.greed.data.plans).

   Docs: https://paystack.com/docs/payments/subscriptions/"
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import (java.security MessageDigest)
           (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)))

(def base-url "https://api.paystack.co")

(defn secret-key [{:keys [biff/secret]}]
  (when secret (secret :paystack/secret-key)))

(defn configured? [ctx] (some? (not-empty (secret-key ctx))))

(defn- call [ctx method path & [body]]
  (try
    (let [resp (http/request (cond-> {:method method
                                      :url (str base-url path)
                                      :headers {"Authorization" (str "Bearer " (secret-key ctx))}
                                      :accept :json
                                      ;; greed keeps no cookies from the API. Paystack's
                                      ;; Cloudflare cookie only makes the client log warnings.
                                      :cookie-policy :none
                                      :throw-exceptions false
                                      :connection-timeout 10000
                                      :socket-timeout 20000}
                               body (assoc :content-type :json :body (json/generate-string body))))
          data (try (json/parse-string (:body resp) true) (catch Exception _ nil))]
      (if (and (<= 200 (:status resp) 299) (true? (:status data)))
        {:ok (:data data)}
        (do (log/warn "Paystack" (name method) path "failed:" (:status resp) (:message data))
            {:error (or (:message data) (str "HTTP " (:status resp)))})))
    (catch Exception e
      (log/warn e "Paystack" (name method) path "could not be reached")
      {:error "Paystack could not be reached"})))

;; ---------------------------------------------------------------------------
;; Plans: one Paystack plan per greed tier and interval, found by its name
;; or created the first time someone chooses it.

(def ^:private intervals {:monthly "monthly" :yearly "annually"})

(defn plan-name
  "The Paystack plan name for a tier and interval, e.g. \"greed Premium monthly\".
   Plans are matched on this name, its amount and its interval."
  [tier-name interval]
  (str "greed " tier-name " " (name interval)))

(defn list-plans [ctx]
  (call ctx :get "/plan?perPage=100"))

(defn- matches? [plan nm cents interval]
  (and (= nm (:name plan))
       (= cents (:amount plan))
       (= (intervals interval) (:interval plan))
       (= "ZAR" (:currency plan))))

(defn ensure-plan!
  "The plan code for this name, price (cents) and interval, creating the
   plan in Paystack when there is none."
  [ctx nm cents interval]
  (let [{:keys [ok error]} (list-plans ctx)]
    (if error
      {:error error}
      (if-let [plan (some #(when (matches? % nm cents interval) %) ok)]
        {:ok (:plan_code plan)}
        (let [{:keys [ok error]} (call ctx :post "/plan" {:name nm :amount cents :interval (intervals interval)
                                                         :currency "ZAR" :send_invoices true})]
          (if error {:error error} {:ok (:plan_code ok)}))))))

;; ---------------------------------------------------------------------------
;; Checkout and verifying

(defn initialize!
  "Starts a card checkout that subscribes `email` to `plan-code`. Returns
   {:ok {:authorization_url :reference :access_code}}."
  [ctx {:keys [email cents plan-code callback-url metadata]}]
  (call ctx :post "/transaction/initialize"
        {:email email
         :amount (str cents)
         :currency "ZAR"
         :plan plan-code
         :channels ["card"]
         :callback_url callback-url
         :metadata (json/generate-string metadata)}))

(defn verify [ctx reference]
  (if (re-matches #"[A-Za-z0-9._=-]{1,100}" (str reference))
    (call ctx :get (str "/transaction/verify/" reference))
    {:error "Invalid reference"}))

;; ---------------------------------------------------------------------------
;; Managing a subscription

(defn manage-link
  "A Paystack page where the person can change their card or cancel."
  [ctx subscription-code]
  (call ctx :get (str "/subscription/" subscription-code "/manage/link")))

(defn disable!
  "Stops a subscription from renewing."
  [ctx subscription-code email-token]
  (call ctx :post "/subscription/disable" {:code subscription-code :token email-token}))

;; ---------------------------------------------------------------------------
;; Webhooks

(defn signature
  "HMAC SHA-512 of the raw body with the secret key, as lowercase hex."
  [^String key ^String body]
  (let [mac (doto (Mac/getInstance "HmacSHA512")
              (.init (SecretKeySpec. (.getBytes key "UTF-8") "HmacSHA512")))]
    (str/join (map #(format "%02x" %) (.doFinal mac (.getBytes body "UTF-8"))))))

(defn valid-signature?
  "True when the x-paystack-signature header matches the body. Compared in
   constant time."
  [ctx ^String body ^String header]
  (let [key (secret-key ctx)]
    (boolean
     (and (not-empty key) body header
          (MessageDigest/isEqual (.getBytes (signature key body) "UTF-8")
                                 (.getBytes (str/lower-case header) "UTF-8"))))))
