(ns com.greed.ui.app.insights
  (:require [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.core :as c.ui]
            [com.greed.ui.components.headers :as headers]
            [com.greed.utilities.core :as utilities]))

(defn- safe-pct [part whole]
  (if (and whole (pos? whole))
    (double (* 100.0 (/ (double (or part 0)) whole)))
    0.0))

(defn- metric [label value sub value-cls]
  [:div {:class "bg-white rounded-xl border border-zinc-200/70 shadow-card p-5"}
   [:p {:class "text-xs font-medium text-zinc-400 uppercase tracking-wider"} label]
   [:p {:class (str "mt-2 text-2xl font-semibold tabular-nums " (or value-cls "text-zinc-900"))} value]
   (when sub [:p {:class "mt-1 text-xs text-zinc-400"} sub])])

(defn- bar-row [& {:keys [label amount pct colour]}]
  [:div
   [:div {:class "flex items-center justify-between mb-1"}
    [:span {:class "text-sm text-zinc-600 truncate pr-2"} label]
    [:span {:class "text-sm font-medium text-zinc-900 tabular-nums flex-shrink-0"} (utilities/amount->rands amount)]]
   [:div {:class "h-2 w-full rounded-full bg-zinc-100 overflow-hidden"}
    [:div {:class (str "h-full rounded-full " colour)
           :style {:width (str (int (min 100 (max 0 (Math/round (double pct))))) "%")}}]]])

(defn- allocation-card [income expenses savings]
  (let [leftover (max 0 (- income expenses savings))]
    [:div {:class "bg-white rounded-xl border border-zinc-200/70 shadow-card p-6"}
     [:h3 {:class "text-sm font-semibold text-zinc-900"} "Where your income goes"]
     [:p {:class "text-xs text-zinc-400 mt-0.5 mb-5"}
      "How your monthly income splits across expenses, savings, and what's left over."]
     (if (pos? income)
       [:div {:class "space-y-4"}
        (bar-row :label "Expenses" :amount expenses :pct (safe-pct expenses income) :colour "bg-rose-400")
        (bar-row :label "Savings"  :amount savings  :pct (safe-pct savings income)  :colour "bg-indigo-400")
        (bar-row :label "Unallocated" :amount leftover :pct (safe-pct leftover income) :colour "bg-zinc-300")]
       [:p {:class "text-sm text-zinc-400 py-6 text-center"} "Add your income in Finances to see this breakdown."])]))

(defn- expense-breakdown-card [expense-items total-expenses]
  [:div {:class "bg-white rounded-xl border border-zinc-200/70 shadow-card p-6"}
   [:h3 {:class "text-sm font-semibold text-zinc-900"} "Expense breakdown"]
   [:p {:class "text-xs text-zinc-400 mt-0.5 mb-5"} "Each expense as a share of your total monthly spending."]
   (if (seq expense-items)
     [:div {:class "space-y-4"}
      (for [{:budget-item/keys [title amount]} (sort-by :budget-item/amount > expense-items)]
        (bar-row :label title :amount amount
                 :pct (safe-pct amount total-expenses) :colour "bg-rose-400"))]
     [:p {:class "text-sm text-zinc-400 py-6 text-center"} "No expenses recorded yet."])])

(defn page [{:keys [session] :as ctx}]
  (let [user-id      (:uid session)
        budget-items (data/get-budget-items ctx user-id)
        {:keys [total-income total-expenses total-savings]} (c.ui/get-budget-data budget-items)
        expense-items (filterv #(= (:budget-item/type %) :expenses) budget-items)
        leftover      (- total-income total-expenses total-savings)
        savings-rate  (safe-pct total-savings total-income)
        expense-rate  (safe-pct total-expenses total-income)]
    (ui/app
     ctx
     [:div {:class "space-y-4"}
      (headers/pages-heading ["Insights"])
      [:p {:class "text-sm text-zinc-500"}
       "A read on your monthly money based on your budget in Finances."]
      [:div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4"}
       (metric "Monthly Income" (utilities/amount->rands total-income) nil "text-emerald-600")
       (metric "Savings rate" (utilities/->percentage savings-rate)
               "Share of income saved" (cond (>= savings-rate 20) "text-emerald-600"
                                             (>= savings-rate 10) "text-amber-500"
                                             :else "text-rose-500"))
       (metric "Expense rate" (utilities/->percentage expense-rate) "Share of income spent" "text-zinc-900")
       (metric (if (neg? leftover) "Overspending" "Unallocated")
               (utilities/amount->rands (Math/abs (long leftover)))
               (if (neg? leftover) "Expenses + savings exceed income" "Income not yet budgeted")
               (if (neg? leftover) "text-rose-500" "text-zinc-900"))]
      [:div {:class "grid grid-cols-1 lg:grid-cols-2 gap-4 items-start"}
       (allocation-card total-income total-expenses total-savings)
       (expense-breakdown-card expense-items total-expenses)]])))
