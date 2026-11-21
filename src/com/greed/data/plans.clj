(ns com.greed.data.plans
  "Plans: Free, Premium and Business, and what each one unlocks.

   Free:     Budget, Goals, Calendar, Insights, the tax tools, and one
             statement month.
   Premium:  unlimited statements (Spending, Banking).
   Business: Premium, plus businesses (VAT, company tax, owner's pay, team).

   One :subscription doc per person holds their plan. A trial is a
   :trialing doc with Business for `trial-days`; each person gets one.
   Admins have Business. Months already read stay visible on Free: only new
   uploads are limited. Prices are in common.edn, in cents."
  (:require [com.biffweb :as biff :refer [q]]
            [com.core :as c]
            [com.greed.data.core :as data])
  (:import (java.util Date)
           (java.util.concurrent TimeUnit)))

(def tiers [:free :premium :business])

(defn- rank [tier] (.indexOf ^java.util.List tiers tier))

(defn config [] (:plans c/common-config))

(defn price
  "A tier's price in cents for :monthly or :yearly."
  [tier interval]
  (get-in (config) [:tiers tier interval]))

(defn tier-name [tier] (get-in (config) [:tiers tier :name] "Free"))

(defn trial-days [] (:trial-days (config) 14))

(defn free-statement-months [] (:free-statement-months (config) 1))

;; ---------------------------------------------------------------------------
;; Reading

(defn get-subscription [{:keys [biff/db]} user-id]
  (first (q db
            '{:find (pull s [*])
              :in [user-id]
              :where [[s :subscription/user-id user-id]]}
            user-id)))

(defn- after? [^Date d ^Date now] (and d (.after d now)))

(defn- days-left [^Date until ^Date now]
  (when (after? until now)
    (max 1 (long (Math/ceil (/ (- (.getTime until) (.getTime now)) (double (.toMillis TimeUnit/DAYS 1))))))))

(defn status
  "A person's plan now: {:tier :free|:premium|:business, :trial? bool,
   :days-left n (trial), :subscription doc, :admin? bool, :trial-used? bool}.
   A paid plan counts until its period ends, also while a renewal is
   failing (:attention) or after it was cancelled (:non-renewing)."
  ([ctx user-id] (status ctx user-id (Date.)))
  ([ctx user-id ^Date now]
   (let [sub    (get-subscription ctx user-id)
         admin? (data/admin? (data/get-user ctx user-id))
         st     (:subscription/status sub)
         base   {:subscription sub :admin? admin? :trial-used? (some? sub)}]
     (cond
       admin? (assoc base :tier :business)

       (and (= :trialing st) (after? (:subscription/trial-ends sub) now))
       (assoc base :tier :business :trial? true :days-left (days-left (:subscription/trial-ends sub) now))

       (and (#{:active :non-renewing :attention} st)
            (or (nil? (:subscription/period-ends sub)) (after? (:subscription/period-ends sub) now)))
       (assoc base :tier (:subscription/plan sub))

       :else (assoc base :tier :free)))))

(defn tier [ctx user-id] (:tier (status ctx user-id)))

(defn at-least? [ctx user-id wanted] (>= (rank (tier ctx user-id)) (rank wanted)))

(defn business-allowed?
  "True when the person may work in the business: their own plan is
   Business, or one of the business's owners has Business (so the people an
   owner invites do not each need a plan)."
  [ctx user-id business-id]
  (or (at-least? ctx user-id :business)
      (boolean
       (some #(and (= :owner (:business-member/role %))
                   (at-least? ctx (:business-member/user-id %) :business))
             (map first (data/get-business-members ctx business-id))))))

(defn statement-months
  "The months already read for an owner (a set of \"yyyy-MM\")."
  [ctx o]
  (set (keep :statement-summary/month (data/get-statement-summaries ctx o))))

(defn upload-allowed?
  "May this owner save statement months `new-months`? Premium and up: yes.
   Free: only while the months read, with these, stay within the free
   allowance. A business's statements follow business-allowed?."
  [ctx {:keys [user-id business-id] :as o} new-months]
  (cond
    business-id (business-allowed? ctx user-id business-id)
    (at-least? ctx user-id :premium) true
    :else (<= (count (into (statement-months ctx o) new-months)) (free-statement-months))))

;; ---------------------------------------------------------------------------
;; Writing

(defn start-trial!
  "Starts the person's one trial: Business for trial-days. Returns :started,
   or :used when they already had a trial or a plan."
  ([ctx user-id] (start-trial! ctx user-id (Date.)))
  ([ctx user-id ^Date now]
   (if (get-subscription ctx user-id)
     :used
     (do (biff/submit-tx ctx
                         [{:db/doc-type :subscription
                           :xt/id (random-uuid)
                           :subscription/user-id user-id
                           :subscription/plan :business
                           :subscription/status :trialing
                           :subscription/trial-ends (Date. (+ (.getTime now) (.toMillis TimeUnit/DAYS (trial-days))))
                           :subscription/created-at now
                           :subscription/updated-at now}])
         :started))))

;; ---------------------------------------------------------------------------
;; Paid plans (Paystack). See com.greed.payments.billing.

(defn paid?
  "True when the person's plan is billed through Paystack and has not
   been cancelled for good. A plan just paid for counts from the return
   from checkout (which gives the customer), before the webhook brings the
   subscription's code, so nobody can buy a second plan in between."
  [sub]
  (boolean
   (and (or (:subscription/paystack-code sub) (:subscription/paystack-customer sub))
        (#{:active :non-renewing :attention} (:subscription/status sub)))))

(defn subscription-by
  "The subscription whose `attr` is `value`, e.g.
   (subscription-by ctx :subscription/paystack-code \"SUB_x\")."
  [{:keys [biff/db]} attr value]
  (when value
    (first (q db
              {:find '(pull s [*])
               :in '[v]
               :where [['s attr 'v]]}
              value))))

(defn subscription-tx
  "The tx doc that sets `changes` on the person's subscription, creating it
   when there is none. Fields not in `changes` are kept."
  [ctx user-id changes ^Date now]
  (let [sub (get-subscription ctx user-id)]
    (merge (dissoc sub :xt/id)
           {:db/doc-type :subscription
            :xt/id (or (:xt/id sub) (random-uuid))
            :subscription/user-id user-id
            :subscription/created-at (or (:subscription/created-at sub) now)
            :subscription/updated-at now}
           changes)))

(defn event-seen?
  "True when this payment event (see payment-event-doc) was already handled."
  [{:keys [biff/db]} event-key]
  (some? (first (q db
                   '{:find [e]
                     :in [k]
                     :where [[e :payment-event/key k]]}
                   event-key))))

(defn payment-event-doc
  "An audit record of one payment event: what happened, to whom, when."
  [{:keys [event-key event user-id reference summary]} ^Date now]
  (cond-> {:db/doc-type :payment-event
           :xt/id (random-uuid)
           :payment-event/key event-key
           :payment-event/event event
           :payment-event/received-at now}
    user-id (assoc :payment-event/user-id user-id)
    reference (assoc :payment-event/reference reference)
    summary (assoc :payment-event/summary summary)))

;; ---------------------------------------------------------------------------
;; Reminders and the move to plans

(def reminder-days
  "How many days before a trial ends its reminder goes out."
  3)

(defn trials-to-remind
  "Trials that end within reminder-days and have not been reminded."
  [{:keys [biff/db]} ^Date now]
  (let [soon (Date. (+ (.getTime now) (.toMillis TimeUnit/DAYS reminder-days)))]
    (->> (q db
            '{:find [(pull s [*])]
              :where [[s :subscription/status :trialing]]})
         (map first)
         (filter #(let [^Date ends (:subscription/trial-ends %)]
                    (and ends (.after ends now) (not (.after ends soon))
                         (nil? (:subscription/trial-reminded-at %))))))))

(defn users-without-plan
  "Ids of people who have never had a trial or a plan (accounts from
   before plans existed)."
  [{:keys [biff/db] :as ctx}]
  (let [with (set (map first (q db '{:find [u] :where [[s :subscription/user-id u]]})))]
    (->> (q db '{:find [u] :where [[u :user/email]]})
         (map first)
         (remove with))))

(defn grant-trials!
  "Gives everyone without a plan their free trial, from `now`. Returns how
   many trials started. Safe to run again: it skips anyone with a plan."
  ([ctx] (grant-trials! ctx (Date.)))
  ([ctx ^Date now]
   (let [ids (users-without-plan ctx)]
     (when (seq ids)
       (biff/submit-tx ctx
                       (for [uid ids]
                         {:db/doc-type :subscription
                          :xt/id (random-uuid)
                          :subscription/user-id uid
                          :subscription/plan :business
                          :subscription/status :trialing
                          :subscription/trial-ends (Date. (+ (.getTime now) (.toMillis TimeUnit/DAYS (trial-days))))
                          :subscription/created-at now
                          :subscription/updated-at now})))
     (count ids))))
