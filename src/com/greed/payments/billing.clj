(ns com.greed.payments.billing
  "Paying for a plan with Paystack: checkout, the return from checkout, the
   billing page, cancelling, and the webhook that keeps each person's
   subscription up to date.

   The webhook is the source of truth. The return from checkout only
   unlocks the plan straight away, after verifying the payment with
   Paystack, so nobody waits for the webhook. Every applied event is kept
   as a :payment-event (an audit trail), and an event already applied is
   ignored."
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [com.biffweb :as biff]
            [com.greed.data.core :as data]
            [com.greed.data.plans :as plans]
            [com.greed.payments.notices :as notices]
            [com.greed.payments.paystack :as paystack]
            [xtdb.api :as xt])
  (:import (java.time Instant ZoneOffset)
           (java.util Date)))

(defn- redirect [location]
  {:status 303 :headers {"location" location}})

;; ---------------------------------------------------------------------------
;; Plans <-> Paystack

(defn- tier-interval-of-name
  "[tier interval] for a Paystack plan named by paystack/plan-name, or nil."
  [nm]
  (first (for [tier [:premium :business]
               interval [:monthly :yearly]
               :when (= nm (paystack/plan-name (plans/tier-name tier) interval))]
           [tier interval])))

(defn plan-of
  "[tier interval] for a Paystack plan (a map with :name, or a plan code)."
  [ctx plan]
  (cond
    (and (map? plan) (:name plan)) (tier-interval-of-name (:name plan))
    :else (let [code (if (map? plan) (:plan_code plan) plan)
                {:keys [ok]} (when (string? code) (paystack/list-plans ctx))]
            (some #(when (= code (:plan_code %)) (tier-interval-of-name (:name %))) ok))))

(defn- ->date
  "Paystack's ISO time (\"2026-12-21T07:00:00.000Z\") as a Date, or nil."
  [s]
  (when (and (string? s) (not (str/blank? s)))
    (try (Date/from (Instant/parse s)) (catch Exception _ nil))))

(defn- period-after
  "The end of a period paid at `paid` (a Date), with a day's grace."
  [^Date paid interval]
  (let [at (.atOffset (.toInstant paid) ZoneOffset/UTC)]
    (Date/from (.toInstant (.plusDays (if (= :yearly interval) (.plusYears at 1) (.plusMonths at 1)) 1)))))

;; ---------------------------------------------------------------------------
;; Webhook events -> subscription changes

(defn- user-for
  "Whose event this is: the user id in the checkout's metadata, else the
   subscription with this code or customer, else the user with this email."
  [ctx {:keys [metadata customer] :as d}]
  (let [meta (if (string? metadata) (try (json/parse-string metadata true) (catch Exception _ nil)) metadata)
        code (or (get-in d [:subscription :subscription_code]) (:subscription_code d))
        uid  (some-> (:user_id meta) str parse-uuid)]
    (or (when (and uid (data/get-user ctx uid)) uid)
        (:subscription/user-id (plans/subscription-by ctx :subscription/paystack-code code))
        (:subscription/user-id (plans/subscription-by ctx :subscription/paystack-customer (:customer_code customer)))
        (some->> (:email customer) (biff/lookup-id (:biff/db ctx) :user/email)))))

(defn changes
  "The subscription fields one Paystack event sets, or nil for an event
   greed does not act on. `now` is a Date."
  [ctx event d now]
  (let [sub-code  (or (get-in d [:subscription :subscription_code]) (:subscription_code d))
        next-date (->date (or (get-in d [:subscription :next_payment_date]) (:next_payment_date d)))
        customer  (get-in d [:customer :customer_code])
        [tier interval] (when-let [p (or (:plan d) (get-in d [:subscription :plan]))]
                          (when (or (not-empty (:plan_code p)) (string? p)) (plan-of ctx p)))
        base      (cond-> {}
                    customer (assoc :subscription/paystack-customer customer)
                    sub-code (assoc :subscription/paystack-code sub-code)
                    (:email_token d) (assoc :subscription/paystack-email-token (:email_token d))
                    tier (assoc :subscription/plan tier :subscription/interval interval))]
    (case event
      "subscription.create"
      (when tier
        (cond-> (assoc base :subscription/status :active)
          next-date (assoc :subscription/period-ends next-date)))

      "charge.success"
      (when tier
        (assoc base
               :subscription/status :active
               :subscription/period-ends (period-after (or (->date (:paid_at d)) now) interval)))

      ("invoice.update" "invoice.create")
      (when (and sub-code (true? (:paid d)))
        (cond-> (assoc base :subscription/status :active)
          next-date (assoc :subscription/period-ends next-date)))

      "invoice.payment_failed"
      (when sub-code (assoc base :subscription/status :attention))

      "subscription.not_renew"
      (cond-> (assoc base :subscription/status :non-renewing)
        next-date (assoc :subscription/period-ends next-date))

      "subscription.disable"
      (assoc base :subscription/status :cancelled)

      nil)))

(defn- event-key
  "One key per event, so a retried webhook is applied once."
  [event d]
  (str event ":" (or (:id d) (:reference d) (:subscription_code d)
                     (get-in d [:subscription :subscription_code]) "?")
       ":" (or (:status d) "") ":" (or (:next_payment_date d) (get-in d [:subscription :next_payment_date]) "")))

(defn apply-event!
  "Applies one parsed webhook event. Returns :applied, :seen or :ignored."
  ([ctx payload] (apply-event! ctx payload (Date.)))
  ([ctx {:keys [event data]} now]
   (let [k (event-key event data)]
     (cond
       (plans/event-seen? ctx k) :seen
       :else
       (let [uid     (user-for ctx data)
             change  (when uid (changes ctx event data now))
             ;; never start a subscription doc that has no plan
             change  (when (or (:subscription/plan change) (plans/get-subscription ctx uid)) change)]
         (biff/submit-tx ctx
                         (cond-> [(plans/payment-event-doc
                                   {:event-key k :event event :user-id uid
                                    :reference (some-> (:reference data) str)
                                    :summary (when change (pr-str (select-keys change [:subscription/plan :subscription/status
                                                                                       :subscription/interval :subscription/period-ends])))}
                                   now)]
                           change (conj (plans/subscription-tx ctx uid change now))))
         (if change :applied :ignored))))))

(defn webhook
  "POST /paystack/webhook. Checks the signature on the raw body, then
   applies the event. Always 200 for a signed event, so Paystack does not
   retry one greed has chosen to ignore."
  [{:keys [raw-body headers] :as ctx}]
  (if-not (paystack/valid-signature? ctx raw-body (get headers "x-paystack-signature"))
    (do (log/warn "Paystack webhook with a bad signature ignored")
        {:status 401 :body "bad signature"})
    (let [payload (try (json/parse-string raw-body true) (catch Exception _ nil))]
      (when payload
        (try (let [result (apply-event! ctx payload)]
               (log/info "Paystack" (:event payload) result)
               (when (= :applied result)
                 (let [ctx (assoc ctx :biff/db (xt/db (:biff.xtdb/node ctx)))]
                   (when-let [uid (user-for ctx (:data payload))]
                     (notices/after-event! ctx (:event payload) uid)))))
             (catch Exception e (log/error e "Paystack webhook failed" (:event payload)))))
      {:status 200 :body "ok"})))

;; ---------------------------------------------------------------------------
;; Checkout, return, manage

(defn checkout
  "POST /app/plan/checkout: send the person to Paystack to pay for a tier."
  [{:keys [session params biff/base-url] :as ctx}]
  (let [uid      (:uid session)
        tier     (#{:premium :business} (keyword (:tier params)))
        interval (if (= "yearly" (:interval params)) :yearly :monthly)
        sub      (plans/get-subscription ctx uid)
        user     (data/get-user ctx uid)]
    (cond
      (nil? tier) (redirect "/app/plan")
      (plans/paid? sub) (redirect "/app/plan?alert=already-subscribed")
      (not (paystack/configured? ctx)) (redirect "/app/plan?alert=payments-soon")
      :else
      (let [cents (plans/price tier interval)
            {code :ok :as plan} (paystack/ensure-plan! ctx (paystack/plan-name (plans/tier-name tier) interval) cents interval)
            {:keys [ok]} (when code
                           (paystack/initialize! ctx {:email (:user/email user)
                                                      :cents cents
                                                      :plan-code code
                                                      :callback-url (str base-url "/app/plan/return")
                                                      :metadata {:user_id (str uid) :tier (name tier) :interval (name interval)}}))]
        (if-let [url (:authorization_url ok)]
          (redirect url)
          (do (log/warn "Checkout could not start" (:error plan))
              (redirect "/app/plan?alert=payment-failed")))))))

(defn return
  "GET /app/plan/return?reference=...: back from Paystack. Verifies the
   payment and unlocks the plan; the webhook fills in the rest."
  [{:keys [session params] :as ctx}]
  (let [{:keys [ok]} (paystack/verify ctx (:reference params))
        uid (:uid session)
        meta (:metadata ok)
        meta (if (string? meta) (try (json/parse-string meta true) (catch Exception _ nil)) meta)]
    (if (and (= "success" (:status ok)) (= (str uid) (:user_id meta)))
      (do (apply-event! ctx {:event "charge.success"
                             :data (assoc ok :metadata meta :plan (or (:plan_object ok) (:plan ok)))})
          (redirect "/app?alert=plan-active"))
      (redirect "/app/plan?alert=payment-failed"))))

(defn manage
  "GET /app/plan/manage: Paystack's page to change the card or cancel."
  [{:keys [session] :as ctx}]
  (let [sub (plans/get-subscription ctx (:uid session))
        {:keys [ok]} (when-let [code (:subscription/paystack-code sub)] (paystack/manage-link ctx code))]
    (if-let [link (:link ok)]
      (redirect link)
      (redirect "/app/plan?alert=billing-unavailable"))))

(defn cancel-for-deletion!
  "Stops the person's subscription from renewing, before their account is
   deleted. Returns true when there was nothing to stop or it stopped."
  [ctx user-id]
  (let [sub (plans/get-subscription ctx user-id)]
    (if (plans/paid? sub)
      (contains? (paystack/disable! ctx (:subscription/paystack-code sub) (:subscription/paystack-email-token sub)) :ok)
      true)))
