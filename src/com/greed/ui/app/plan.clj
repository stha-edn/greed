(ns com.greed.ui.app.plan
  "GET /app/plan: the person's plan in the same header card as every tab,
   then the three plans with one action each. Starting the trial is here;
   paying arrives with Paystack (stage 2)."
  (:require [com.biffweb :as biff]
            [com.greed.ui :as ui]
            [com.greed.data.plans :as plans]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.hero :as hero]
            [com.greed.ui.components.plans :as plan-cards]
            [com.greed.ui.components.shared :as shared])
  (:import (java.text SimpleDateFormat)))

(defn- date [d] (.format (SimpleDateFormat. "d MMMM yyyy") d))

(defn- plan-hero [ctx {:keys [tier trial? days-left admin? subscription]}]
  (let [uid    (get-in ctx [:session :uid])
        months (count (plans/statement-months ctx uid))
        limit  (plans/free-statement-months)
        total  (plans/trial-days)
        ends   (:subscription/period-ends subscription)]
    (hero/card
     {:label "Your plan"
      :badge (cond admin? (hero/badge "Admin")
                   trial? (hero/badge "Trial" :tone :brand))
      :value (str (plans/tier-name tier) (when trial? " trial"))
      :unit (cond trial? (str days-left " day" (when (not= 1 days-left) "s") " left")
                  (= :free tier) "for ever"
                  ends (str "until " (date ends)))
      :reading (cond
                 admin? "Admins have every feature, without a plan."
                 trial? (str "Everything is unlocked until " (date (:subscription/trial-ends subscription))
                             ". No card needed: afterwards you are on Free unless you choose a plan.")
                 (= :free tier) "Budget, goals, calendar, insights and the tax tools are free. Statements beyond one month, and businesses, need a plan."
                 :else "Thank you for supporting greed.")
      :picture (cond
                 trial? (hero/bars {:title "Your trial" :of total
                                    :rows [{:label "Days used" :value (str (- total days-left) " of " total)
                                            :amount (- total days-left) :cls "bg-zinc-400"}
                                           {:label "Days left" :value (str days-left) :amount days-left :cls "bg-brand-600"}]})
                 :else (hero/bars {:title "Statement months read" :of (max 1 limit months)
                                   :rows [{:label "Read" :value (str months) :amount months :cls "bg-brand-600"}
                                          {:label "Free allowance" :value (str limit) :amount limit :cls "bg-zinc-400"}]
                                   :note (if (= :free tier)
                                           (str "Free reads " limit " month. Premium reads every month.")
                                           "Unlimited on your plan.")}))
      :stats [["Plan" (plans/tier-name tier)]
              ["Statements" (if (#{:premium :business} tier) "Unlimited" (str limit " month"))]
              ["Businesses" (if (= :business tier) "Included" "Business plan")]]})))

(defn- action [{:keys [tier trial-used? admin?]} interval]
  (fn [t]
    (cond
      admin? nil
      (= t tier) (shared/btn :variant :outline :size :md :class "w-full" :attrs {:disabled true} "Your plan")
      (= :free t) nil
      :else
      [:div {:class "space-y-2"}
       (biff/form {:action "/app/plan/checkout"}
                  [:input {:type "hidden" :name "tier" :value (name t)}]
                  [:input {:type "hidden" :name "interval" :value (name interval)}]
                  (shared/btn :variant (if (= :premium t) :dark :outline) :size :md :type "submit" :class "w-full"
                              (str "Choose " (plans/tier-name t))))
       (when (and (= :business t) (not trial-used?))
         (biff/form {:action "/app/plan/trial"}
                    (shared/btn :variant :ghost :size :md :type "submit" :class "w-full"
                                (str "Try free for " (plans/trial-days) " days"))))])))

(defn page [{:keys [session params] :as ctx}]
  (let [st       (plans/status ctx (:uid session))
        interval (plan-cards/interval-of params)]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (headers/pages-heading ["Plan"] "What your plan includes, and how to change it.")
      (plan-hero ctx st)
      [:div
       [:div {:class "mb-4 flex flex-wrap items-center justify-between gap-3"}
        [:h2 {:class "text-xs font-medium text-zinc-500"} "Plans"]
        (plan-cards/interval-switch "/app/plan" interval)]
       (plan-cards/cards {:interval interval :current (when-not (:trial? st) (:tier st)) :action (action st interval)})
       [:p {:class "mt-4 text-xs text-zinc-500"}
        "Prices in rand. Paid plans renew by themselves until you cancel, and you can cancel at any time."]]])))

(defn start-trial
  "POST /app/plan/trial: the one free trial."
  [{:keys [session] :as ctx}]
  (let [result (plans/start-trial! ctx (:uid session))]
    {:status 303
     :headers {"location" (if (= :started result) "/app?alert=trial-started" "/app/plan?alert=trial-used")}}))

(defn checkout
  "POST /app/plan/checkout: Paystack arrives in stage 2."
  [_ctx]
  {:status 303 :headers {"location" "/app/plan?alert=payments-soon"}})
