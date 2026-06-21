(ns com.greed.ui.app.dashboard
  (:require [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.core :as c.greed]
            [com.greed.ui.components.stats :as stats]
            [com.greed.ui.components.cards :as cards]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.headers :as headers]
            [com.greed.utilities.core :as utilities]))


(defn- card-detail-item [label value & [highlight?]]
  [:div {:class "bg-white rounded-xl border border-gray-100 shadow-card p-5"}
   [:p {:class "text-xs font-medium text-zinc-400 uppercase tracking-wide"} label]
   [:p {:class (str "mt-1.5 text-xl font-semibold " (if highlight? "text-emerald-600" "text-zinc-900"))} value]])

(defn- card-details [finances income-tax-data]
  (let [{:finances/keys [salary payday]} finances
        {:keys [net-tax net-income]} income-tax-data
        monthly-gross (or salary 0)
        monthly-net   (when net-income (/ net-income 12))
        monthly-tax   (when net-tax (/ net-tax 12))]
    [:div {:class "grid grid-cols-2 sm:grid-cols-4 gap-4"}
     (card-detail-item "Gross Salary"     (utilities/amount->rands monthly-gross) false)
     (card-detail-item "Net Take-home"    (if monthly-net (utilities/amount->rands monthly-net) "-") true)
     (card-detail-item "Est. Monthly Tax" (if monthly-tax (utilities/amount->rands monthly-tax) "-") false)
     (card-detail-item "Pay Day"          (if payday (str (utilities/ordinal payday) " of the month") "-") false)]))

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
     [:div {:class "space-y-4"
            :x-data (str "{ showSalaryPrompt: " (boolean show-salary-prompt) " }")}
      (when show-salary-prompt (alerts/salary-prompt-modal))
      (when (:alert params) (alerts/info params))
      (headers/home-heading :user user)

      ;; Bank card + expense stats
      [:div {:class "grid grid-cols-1 lg:grid-cols-3 gap-4"}
       [:div {:class "lg:col-span-1"}
        (cards/bank-card
         :finances finances
         :budget-items budget-items
         :net-monthly-income (when-let [net (:net-income income-tax-data)]
                               (/ net 12)))]
       [:div {:class "lg:col-span-2"}
        (stats/expense-tracker-stats budget-items)]]

      ;; Card detail strip
      (card-details finances income-tax-data)

      ;; Full tax overview
      (stats/tax-stats income-tax-data)])))
