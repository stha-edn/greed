(ns com.greed.ui.app.finances
  (:require [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.stats :as stats]
            [com.greed.ui.components.tables :as tables]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.headers :as headers]))

(defn budget-lists [& {:keys [budget-items]}]
  [:div {:class "grid grid-cols-1 md:grid-cols-3 gap-4"}
   (tables/budget-table
    {:title "income"
     :items (filterv #(= (:budget-item/type %) :income) budget-items)
     :protected-titles #{"Salary"}})
   (tables/budget-table
    {:title "expenses"
     :items (filterv #(= (:budget-item/type %) :expenses) budget-items)
     :protected-titles #{"Medical Aid"}})
   (tables/budget-table
    {:title "savings"
     :items (filterv #(= (:budget-item/type %) :savings) budget-items)})])

(defn page [{:keys [session params] :as ctx}]
  (let [user-id      (:uid session)
        budget-items (data/get-budget-items ctx user-id)]
    (ui/app
     ctx
     [:div {:class "space-y-4" :x-data "{ isAddButtonOpen: false, isActionModalOpen: false }"}
      (when (:alert params) (alerts/info params))
      (headers/pages-heading ["Budget Tracker"])
      (stats/expense-tracker-stats budget-items)
      (tables/add-button)
      (budget-lists
       :budget-items budget-items)])))
