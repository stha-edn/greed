(ns com.greed.ui.app.dashboard
  (:require [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.core :as c.greed]
            [com.greed.ui.components.stats :as stats]
            [com.greed.ui.components.cards :as cards]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.headers :as headers]
            [com.greed.utilities.core :as utilities]))


(defn- section-label [title]
  [:h2 {:class "text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-3"} title])

(defn- hero-substat [label value]
  [:div
   [:p {:class "text-[11px] font-medium uppercase tracking-wider text-zinc-500"} label]
   [:p {:class "mt-1 text-base sm:text-lg font-semibold tabular-nums text-zinc-100"} value]])

(defn- hero
  "Bold feature card leading with monthly net take-home."
  [finances income-tax-data]
  (let [{:finances/keys [salary payday]} finances
        {:keys [net-tax net-income effective-rate]} income-tax-data
        monthly-net (when net-income (/ net-income 12))
        monthly-tax (when net-tax (/ net-tax 12))]
    [:div {:class "relative h-full overflow-hidden rounded-2xl p-6 sm:p-8 text-white bg-gradient-to-br from-zinc-800 via-zinc-900 to-black ring-1 ring-white/10 shadow-card-md"}
     [:div {:class "absolute -top-16 -right-12 w-64 h-64 bg-emerald-500/20 rounded-full blur-3xl"}]
     [:div {:class "absolute bottom-0 left-1/3 w-44 h-44 bg-white/5 rounded-full blur-2xl"}]
     [:div {:class "relative flex h-full flex-col"}
      [:div {:class "flex items-start justify-between gap-3"}
       [:p {:class "text-xs font-medium uppercase tracking-widest text-zinc-400"} "Monthly net take-home"]
       (when payday
         [:span {:class "flex-shrink-0 text-xs font-medium text-emerald-300 bg-emerald-500/10 ring-1 ring-emerald-400/20 px-3 py-1 rounded-full"}
          (str "Payday · " (utilities/ordinal payday))])]
      [:p {:class "mt-3 text-4xl sm:text-5xl font-bold tracking-tight tabular-nums"}
       (if monthly-net (utilities/amount->rands monthly-net) "—")]
      [:div {:class "mt-auto grid grid-cols-3 gap-4 border-t border-white/10 pt-5"}
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
        show-salary-prompt (not (salary-set? finances))]
    (ui/app
     ctx
     [:div {:class "space-y-7"
            :x-data (str "{ showSalaryPrompt: " (boolean show-salary-prompt) " }")}
      (when show-salary-prompt (alerts/salary-prompt-modal))
      (when (:alert params) (alerts/info params))
      (headers/home-heading :user user :date (today-str))

      ;; Hero: net take-home feature card + bank card
      [:div {:class "grid grid-cols-1 lg:grid-cols-3 gap-4 items-stretch"}
       [:div {:class "lg:col-span-2"}
        (hero finances income-tax-data)]
       [:div {:class "lg:col-span-1 flex justify-center lg:justify-end"}
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
