(ns com.greed.ui.app.plan
  "GET /app/plan: the person's plan in the same header card as every tab,
   then the three plans with one action each. Starting the trial is here;
   paying is com.greed.payments.billing (Paystack)."
  (:require [com.biffweb :as biff]
            [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.data.plans :as plans]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.hero :as hero]
            [com.greed.ui.components.plans :as plan-cards]
            [com.greed.ui.components.shared :as shared])
  (:import (java.text SimpleDateFormat)))

(defn- date [d] (.format (SimpleDateFormat. "d MMMM yyyy") d))

(defn- period-days
  "For a paid plan: [days used, days in the billing period, days left]
   from its end date (a month or a year, less the day of grace)."
  [{:subscription/keys [period-ends interval]}]
  (when period-ends
    (let [end   (.toLocalDate (.atZone (.toInstant ^java.util.Date period-ends) (java.time.ZoneId/of "Africa/Johannesburg")))
          end   (.minusDays end 1)
          start (if (= :yearly interval) (.minusYears end 1) (.minusMonths end 1))
          today (java.time.LocalDate/now)
          total (.between java.time.temporal.ChronoUnit/DAYS start end)
          left  (max 0 (.between java.time.temporal.ChronoUnit/DAYS today end))]
      [(max 0 (- total left)) total left])))

(defn- plan-hero [ctx {:keys [tier trial? days-left admin? subscription]}]
  (let [uid    (get-in ctx [:session :uid])
        months (count (plans/statement-months ctx uid))
        limit  (plans/free-statement-months)
        total  (plans/trial-days)
        status (:subscription/status subscription)
        interval (or (:subscription/interval subscription) :monthly)
        paid?  (and (not admin?) (not trial?) (#{:premium :business} tier))
        ends   (some-> (:subscription/period-ends subscription) date)
        [used period left] (when paid? (period-days subscription))]
    (hero/card
     {:label "Your plan"
      :badge (cond admin? (hero/badge "Admin")
                   trial? (hero/badge "Trial" :tone :brand)
                   paid? (hero/badge (if (= :yearly interval) "Yearly" "Monthly")))
      :value (str (plans/tier-name tier) (when trial? " trial"))
      :unit (cond trial? (str days-left " day" (when (not= 1 days-left) "s") " left")
                  (= :free tier) "free"
                  (and paid? (= :non-renewing status) ends) (str "until " ends)
                  (and paid? ends) (str "renews " ends))
      :verdict (cond
                 admin? nil
                 trial? {:text (str "Trial ends " (date (:subscription/trial-ends subscription))) :tone (if (<= days-left 3) :warn :neutral)}
                 (= :attention status) {:text "Payment failed" :tone :bad}
                 (= :non-renewing status) {:text "Cancelled, won't renew" :tone :warn}
                 paid? {:text "Active" :tone :good})
      :reading (cond
                 admin? "Admins have every feature, without a plan."
                 trial? "Everything is unlocked, with no card on file. When the trial ends you move to Free unless you subscribe."
                 (= :free tier) "Budget, goals, calendar, insights and the income tax and bonus calculators are free. More statement months and the ITR12 simulator need Premium; businesses need Business."
                 (= :attention status) "Your last payment did not go through. Update your card with Manage billing to keep your plan."
                 (= :non-renewing status) "You keep everything until the date above, then move to Free. Nothing you added is deleted."
                 :else "Thank you for supporting greed. Your card is charged on the renewal date; cancel any time in Settings.")
      :picture (cond
                 trial? (hero/bars {:title "Your trial" :of total
                                    :rows [{:label "Days used" :value (str (- total days-left) " of " total)
                                            :amount (- total days-left) :cls "bg-zinc-400"}
                                           {:label "Days left" :value (str days-left) :amount days-left :cls "bg-brand-600"}]})
                 period (hero/bars {:title (if (= :non-renewing status) "Until your plan ends" "This billing period") :of period
                                    :rows [{:label "Days used" :value (str used " of " period) :amount used :cls "bg-zinc-400"}
                                           {:label (if (= :non-renewing status) "Days left" "Days to renewal")
                                            :value (str left) :amount left :cls "bg-brand-600"}]})
                 :else (hero/bars {:title "Statement months" :of (max 1 limit months)
                                   :rows [{:label "Read" :value (str months) :amount months :cls "bg-brand-600"}
                                          {:label "Free allowance" :value (str limit) :amount limit :cls "bg-zinc-400"}]
                                   :note (str "Free reads " limit " month. Premium reads every month.")}))
      :stats (if paid?
               [["Price" (str (plan-cards/rands (plans/price tier interval)) (if (= :yearly interval) " a year" " a month"))]
                [(if (= :non-renewing status) "Ends on" "Renews on") (or ends "–")]
                ["Businesses" (if (= :business tier) "Included" "Not included")]]
               [["Statements" (if (#{:premium :business} tier) "Every month" (str limit " month"))]
                ["ITR12 simulator" (if (#{:premium :business} tier) "Included" "Not included")]
                ["Businesses" (if (= :business tier) "Included" "Not included")]])})))

(defn- action [{:keys [tier trial? trial-used? admin? subscription]} interval]
  (fn [t]
    (cond
      admin? nil
      (and (plans/paid? subscription) (= t tier))
      (shared/btn :variant :outline :size :md :class "w-full" :href "/app/plan/manage" "Manage billing")
      ;; during the trial every paid plan can still be chosen
      (and (= t tier) (not trial?)) (shared/btn :variant :outline :size :md :class "w-full" :attrs {:disabled true} "Your plan")
      ;; Paystack can't switch a plan in place: say how to move instead
      (plans/paid? subscription)
      (when (not= :free t)
        [:p {:class "rounded-md bg-zinc-50 px-3 py-2.5 text-center text-xs text-zinc-600 ring-1 ring-zinc-200/70"}
         (str "To move to " (plans/tier-name t) ", cancel " (plans/tier-name tier) " in ")
         [:a {:href "/app/settings#subscription" :class "font-medium text-zinc-900 underline underline-offset-2"} "Settings"]
         ", then choose it when your current plan ends."])
      (= :free t) nil
      :else
      [:div {:class "space-y-2"}
       ;; Not tried yet: the free trial comes first, then paying
       (when-not trial-used?
         (biff/form {:action "/app/plan/trial"}
                    (shared/btn :variant (if (= :premium t) :dark :outline) :size :md :type "submit" :class "w-full"
                                (str "Try free for " (plans/trial-days) " days"))))
       (biff/form {:action "/app/plan/checkout"}
                  [:input {:type "hidden" :name "tier" :value (name t)}]
                  [:input {:type "hidden" :name "interval" :value (name interval)}]
                  (shared/btn :variant (cond (not trial-used?) :ghost (= :premium t) :dark :else :outline)
                              :size :md :type "submit" :class "w-full"
                              (str (if trial? "Subscribe to " "Choose ") (plans/tier-name t))))])))

(defn page [{:keys [session params] :as ctx}]
  (let [st       (plans/status ctx (:uid session))
        interval (plan-cards/interval-of params)
        business (data/session-business ctx)
        ;; inside a business only the Business plan makes sense; a member
        ;; working under an owner's Business plan needs nothing at all
        covered-by-owner? (and business (not= :business (:tier st)))]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (headers/pages-heading ["Plan"] (if business
                                         (str (:business/name business) " runs on the Business plan.")
                                         "What your plan includes, and how to change it."))
      (if covered-by-owner?
        [:section {:class "rounded-lg bg-white p-6 ring-1 ring-zinc-200/70"}
         [:p {:class "text-sm font-semibold tracking-tight text-zinc-900"} "Covered by the owner's plan"]
         [:p {:class "mt-1 max-w-[60ch] text-sm text-zinc-500"}
          (str (:business/name business) " is on an owner's Business plan, so you can work in it without a plan of your own. Your own plan only matters for your personal money.")]
         [:div {:class "mt-5"}
          (biff/form {:action "/app/choose"}
                     [:input {:type "hidden" :name "business-id" :value ""}]
                     (shared/btn :variant :outline :size :md :type "submit" "See my personal plan"))]]
        (plan-hero ctx st))
      (when-not covered-by-owner?
      [:div
       [:div {:class "mb-4 flex flex-wrap items-center justify-between gap-3"}
        [:h2 {:class "text-xs font-medium text-zinc-500"} (if business "The Business plan" "Plans")]
        (plan-cards/interval-switch "/app/plan" interval)]
       (plan-cards/cards {:interval interval :current (when-not (:trial? st) (:tier st)) :action (action st interval)
                          :tiers (when business [:business])})
       [:p {:class "mt-4 text-xs text-zinc-500"}
        (cond
          (plans/paid? (:subscription st))
          "To change plan, cancel your subscription in Settings. You keep this plan until the end of the period you paid for, then choose the new one."
          (:trial? st)
          "You are on the free trial, with no card on file. Subscribing now starts your plan, and its billing, today. If you do nothing, you move to Free when the trial ends."
          (not (:trial-used? st))
          (str "The free trial unlocks everything for " (plans/trial-days) " days, with no card needed. Prices in rand, paid by card through Paystack.")
          :else
          "Prices in rand. Paid by card through Paystack. Plans renew by themselves until you cancel, and you can cancel at any time.")]
       (when-not (plans/paid? (:subscription st))
         [:p {:class "mt-1 text-xs text-zinc-500"}
          "By subscribing you agree to the "
          [:a {:href "/terms" :class "underline underline-offset-2 hover:text-zinc-900"} "Terms"]
          ", including renewal, cancelling and refunds, and the "
          [:a {:href "/privacy" :class "underline underline-offset-2 hover:text-zinc-900"} "Privacy policy"] "."])])])))

(defn start-trial
  "POST /app/plan/trial: the one free trial."
  [{:keys [session] :as ctx}]
  (let [result (plans/start-trial! ctx (:uid session))]
    {:status 303
     :headers {"location" (if (= :started result) "/app?alert=trial-started" "/app/plan?alert=trial-used")}}))
