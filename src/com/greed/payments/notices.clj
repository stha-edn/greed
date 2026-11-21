(ns com.greed.payments.notices
  "Emails about a person's plan: their trial ending soon, a payment that
   failed, and a plan that was cancelled. The trial reminder runs as a
   worker task; the other two follow the Paystack webhook."
  (:require [clojure.tools.logging :as log]
            [com.biffweb :as biff]
            [com.greed.data.core :as data]
            [com.greed.data.plans :as plans]
            [com.greed.email :as email])
  (:import (java.text SimpleDateFormat)
           (java.util Date TimeZone)))

(defn date-text
  "\"21 December 2026\", in South African time."
  [^Date d]
  (when d
    (.format (doto (SimpleDateFormat. "d MMMM yyyy") (.setTimeZone (TimeZone/getTimeZone "Africa/Johannesburg"))) d)))

(defn send!
  "Sends one plan email to the person. Returns true when it was sent."
  [{:keys [biff/base-url] :as ctx} user-id template opts]
  (if-let [user (data/get-user ctx user-id)]
    (email/send-email ctx (merge {:template template
                                  :to (:user/email user)
                                  :firstname (:user/firstname user)
                                  :url (str base-url "/app/plan")}
                                 opts))
    false))

(defn after-event!
  "The email, if any, for a webhook event that was just applied."
  [ctx event user-id]
  (let [sub (plans/get-subscription ctx user-id)
        tier-name (plans/tier-name (:subscription/plan sub))]
    (case event
      "invoice.payment_failed" (send! ctx user-id :payment-failed {:tier-name tier-name})
      "subscription.not_renew" (send! ctx user-id :plan-cancelled {:tier-name tier-name
                                                                    :ends (date-text (:subscription/period-ends sub))})
      nil)))

(defn remind-trials
  "Worker task: email each person whose trial ends within a few days, once."
  [ctx]
  (let [now (Date.)]
    (doseq [sub (plans/trials-to-remind ctx now)
            :let [uid (:subscription/user-id sub)]]
      (try
        (when (send! ctx uid :trial-ending {:ends (date-text (:subscription/trial-ends sub))})
          (biff/submit-tx ctx [{:db/doc-type :subscription
                                :db/op :update
                                :xt/id (:xt/id sub)
                                :subscription/trial-reminded-at now}]))
        (catch Exception e
          (log/error e "Trial reminder failed for" uid))))))
