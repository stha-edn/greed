(ns com.greed.ui.app.dashboard
  (:require [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.core :as c.greed]
            [com.greed.ui.components.stats :as stats]
            [com.greed.ui.components.cards :as cards]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.headers :as headers]
            [com.greed.utilities.core :as utilities]))


(def finance-tax-prompt-interval-ms
  "How often the finance & tax prompt may appear, in milliseconds."
  (* 24 60 60 1000))

(defn finance-tax-prompt-due?
  "True when the prompt should be shown for this session — i.e. it hasn't
   been dismissed within the last 24 hours."
  [{:keys [session]}]
  (let [dismissed-at (:finance-tax-prompt-dismissed-at session)]
    (or (nil? dismissed-at)
        (>= (- (System/currentTimeMillis) (long dismissed-at))
            finance-tax-prompt-interval-ms))))

(defn- hero-substat [label value]
  [:div {:class "min-w-0"}
   [:p {:class "text-[11px] font-medium text-zinc-400 uppercase tracking-wider whitespace-nowrap"} label]
   [:p {:class "mt-1 text-sm font-semibold text-zinc-900 whitespace-nowrap tabular-nums sm:text-lg"} value]])

(defn- hero
  "Feature card leading with monthly net take-home."
  [finances income-tax-data]
  (let [{:finances/keys [salary payday]} finances
        {:keys [net-tax net-income effective-rate]} income-tax-data
        monthly-net (when net-income (/ net-income 12))
        monthly-tax (when net-tax (/ net-tax 12))]
    [:div {:class "relative overflow-hidden h-full rounded-2xl bg-white ring-1 ring-zinc-200/70 shadow-card-md"}
     [:div {:class "absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-emerald-500/40 to-transparent"}]
     [:div {:class "absolute -top-24 -right-24 h-72 w-72 rounded-full bg-emerald-400/10 blur-3xl"}]
     [:div {:class "relative flex flex-col h-full px-6 py-6 sm:px-8 sm:py-7"}
      [:div {:class "flex items-start justify-between gap-3"}
       [:div {:class "flex items-center gap-2.5"}
        [:span {:class "h-1.5 w-1.5 rounded-full bg-emerald-500"}]
        [:p {:class "text-[11px] sm:text-xs font-semibold text-emerald-600 uppercase tracking-[0.18em]"}
         "Monthly net take-home"]]
       (when payday
         [:span {:class "flex-shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700 ring-1 ring-emerald-600/15"}
          (str "Payday · " (utilities/ordinal payday))])]
      [:div {:class "mt-5 flex items-baseline gap-2.5 sm:mt-6"}
       [:p {:class "text-5xl sm:text-6xl font-bold text-zinc-900 leading-none tracking-[-0.05em] tabular-nums"}
        (if monthly-net (utilities/amount->rands monthly-net) "—")]
       [:span {:class "text-sm font-medium text-zinc-400"} "per month"]]
      [:div {:class "flex-1"}]
      [:div {:class "grid grid-cols-3 gap-4 pt-5 border-t border-zinc-100"}
       (hero-substat "Gross salary"   (utilities/amount->rands (or salary 0)))
       (hero-substat "Est. tax / mo"  (if monthly-tax (utilities/amount->rands monthly-tax) "—"))
       (hero-substat "Effective rate" (utilities/->percentage (or effective-rate 0)))]]]))

(defn- today-str []
  (.format (java.time.LocalDate/now)
           (java.time.format.DateTimeFormatter/ofPattern "EEE, d MMM yyyy")))

(defn salary-set? [finances]
  (let [salary (get finances :finances/salary)]
    (and (some? salary) (pos? (long (or salary 0))))))

(defn page [{:keys [session params] :as ctx}]
  (let [user-id            (:uid session)
        user               (data/get-user ctx user-id)
        finances           (data/get-finances ctx user-id)
        income-tax-data    (c.greed/get-income-tax-data user finances)
        budget-items       (data/get-budget-items ctx user-id)
        payday             (:finances/payday finances)
        events             (data/get-events ctx user-id)
        goals              (data/get-goals ctx user-id)
        show-finance-tax-prompt (and (not (data/admin? user))
                                     (not (salary-set? finances))
                                     (finance-tax-prompt-due? ctx))]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (when show-finance-tax-prompt (alerts/finance-tax-prompt-modal))
      (when (:alert params) (alerts/info params))
      (headers/home-heading :user user :date (today-str))

      ;; Hero: net take-home feature card + bank card
      [:div {:class "grid grid-cols-1 items-stretch gap-4 lg:grid-cols-3"}
       [:div {:class "lg:col-span-2"}
        (hero finances income-tax-data)]
       [:div {:class "flex justify-center lg:col-span-1 lg:justify-end"}
        (cards/bank-card
         :finances finances
         :budget-items budget-items
         :net-monthly-income (when-let [net (:net-income income-tax-data)]
                               (/ net 12)))]]

      ;; Budget overview
      (stats/budget-section budget-items)

      ;; What's next + goals
      [:div {:class "grid grid-cols-1 gap-4 lg:grid-cols-2"}
       (stats/upcoming-section payday events)
       (stats/goals-section goals)]

      ;; Tax
      (stats/tax-stats income-tax-data)])))
