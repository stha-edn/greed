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

(defn- section-label [title]
  [:h2 {:class "mb-3 text-xs font-semibold text-zinc-400 uppercase tracking-wider"} title])

(defn- hero-substat [label value]
  [:div {:class "min-w-0"}
   [:p {:class "text-[11px] font-medium text-emerald-700/70 uppercase tracking-wider whitespace-nowrap"} label]
   [:p {:class "mt-1 text-sm font-semibold text-zinc-900 whitespace-nowrap tabular-nums sm:text-lg"} value]])

(defn- hero
  "Bold feature card leading with monthly net take-home."
  [finances income-tax-data]
  (let [{:finances/keys [salary payday]} finances
        {:keys [net-tax net-income effective-rate]} income-tax-data
        monthly-net (when net-income (/ net-income 12))
        monthly-tax (when net-tax (/ net-tax 12))]
    [:div {:class "relative overflow-hidden h-full p-6 bg-gradient-to-br from-white via-emerald-50 to-emerald-100 ring-1 ring-emerald-200/70 rounded-2xl shadow-card-md sm:p-8"}
     [:div {:class "absolute -top-16 -right-12 w-64 h-64 bg-emerald-300/30 rounded-full blur-3xl"}]
     [:div {:class "absolute bottom-0 left-1/3 w-44 h-44 bg-emerald-200/40 rounded-full blur-2xl"}]
     [:div {:class "relative flex flex-col h-full"}
      [:div {:class "flex items-start justify-between gap-3"}
       [:p {:class "text-xs font-medium text-emerald-700 uppercase tracking-widest"} "Monthly net take-home"]
       (when payday
         [:span {:class "flex-shrink-0 px-3 py-1 text-xs font-medium text-emerald-700 bg-emerald-500/10 ring-1 ring-emerald-600/20 rounded-full"}
          (str "Payday · " (utilities/ordinal payday))])]
      [:p {:class "mt-3 text-4xl font-bold text-zinc-900 tracking-tight tabular-nums sm:text-5xl"}
       (if monthly-net (utilities/amount->rands monthly-net) "—")]
      [:div {:class "grid grid-cols-3 gap-2 pt-5 mt-auto border-t border-emerald-200/60 sm:gap-4"}
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
        show-finance-tax-prompt (and (not (data/admin? user))
                                     (not (salary-set? finances))
                                     (finance-tax-prompt-due? ctx))]
    (ui/app
     ctx
     [:div {:class "space-y-7"
            :x-data (str "{ showFinanceTaxPrompt: " (boolean show-finance-tax-prompt) " }")}
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

      ;; Budget snapshot
      [:div
       (section-label "Budget this month")
       (stats/expense-tracker-stats budget-items)]

      ;; Full tax overview (heading + charts + metrics)
      (stats/tax-stats income-tax-data)])))
