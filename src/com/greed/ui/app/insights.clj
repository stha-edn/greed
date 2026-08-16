(ns com.greed.ui.app.insights
  (:require [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.stats :as stats]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.utilities.core :as utilities]))

(defn- legend-row [dot-cls label amount pct amount-cls]
  [:div {:class "flex items-center gap-3"}
   [:span {:class (str "flex-shrink-0 w-2 h-2 rounded-full " dot-cls)}]
   [:span {:class "min-w-0 flex-1 text-sm text-zinc-600 truncate"} label]
   [:span {:class (str "flex-shrink-0 text-sm font-semibold tabular-nums " (or amount-cls "text-zinc-900"))}
    (utilities/amount->rands amount)]
   [:span {:class "flex-shrink-0 w-10 text-right text-xs text-zinc-400 tabular-nums"} pct]])

(defn- allocation-card
  "A single stacked bar makes the part/whole split of one Rand instantly
   legible. An overspend can't be drawn as a bar (it exceeds 100%), so it's
   surfaced honestly as a warning strip instead."
  [income expenses savings leftover overspend?]
  (let [exp-pct (or (utilities/pct-share expenses income) 0)
        sav-pct (or (utilities/pct-share savings income) 0)
        rem-pct (or (utilities/pct-share leftover income) 0)]
    [:div {:class "p-6 bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card"}
     [:h3 {:class "text-sm font-semibold text-zinc-900 tracking-tight"} "Where your income goes"]
     [:p {:class "mt-0.5 mb-5 text-xs text-zinc-400 leading-relaxed"}
      "How each Rand of income is split across spending, savings, and what's left over."]
     (if (pos? income)
       [:<>
        (if overspend?
          [:div {:class "mb-5 rounded-lg bg-rose-50 px-4 py-2.5 text-xs text-rose-700"}
           (str "Expenses and savings are R " (utilities/amount->rands (Math/abs (long leftover)))
                " over your income this month.")]
          [:div {:class "overflow-hidden h-3 w-full bg-zinc-100 rounded-full"}
           [:div {:class "h-full w-full overflow-hidden rounded-full greed-bar-grow"}
            [:div {:class "flex h-full w-full"}
             [:div {:class "h-full bg-rose-400" :style {:width (str (int exp-pct) "%")}}]
             [:div {:class "h-full bg-emerald-500" :style {:width (str (int sav-pct) "%")}}]
             [:div {:class "h-full bg-zinc-200" :style {:width (str (int rem-pct) "%")}}]]]])
        [:div {:class "space-y-2.5 mt-5"}
         (legend-row "bg-rose-400" "Expenses" expenses (utilities/pct-label exp-pct) nil)
         (legend-row "bg-emerald-500" "Savings" savings (utilities/pct-label sav-pct) nil)
         (legend-row "bg-zinc-300" (if overspend? "Overspend" "Unallocated")
                     (Math/abs (long leftover))
                     (if overspend? "—" (utilities/pct-label rem-pct))
                     (when overspend? "text-rose-600"))]]
       [:p {:class "text-center py-6 text-sm text-zinc-400"}
        "Add your "
        [:a {:href "/app/finances" :class "font-medium text-emerald-600 hover:underline"} "income"]
        " in Finances to see this breakdown."])]))

(defn- expense-breakdown-card [expense-items total-expenses]
  [:div {:class "p-6 bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card"}
   [:h3 {:class "text-sm font-semibold text-zinc-900 tracking-tight"} "Expense breakdown"]
   [:p {:class "mt-0.5 mb-5 text-xs text-zinc-400 leading-relaxed"}
    "Each expense as a share of your total monthly spending."]
   (if (seq expense-items)
     [:div {:class "divide-y divide-zinc-100"}
      (for [{:budget-item/keys [title amount]} (sort-by :budget-item/amount > expense-items)]
        (let [p (or (utilities/pct-share amount total-expenses) 0)]
          [:div {:class "py-3.5"}
           [:div {:class "flex items-center justify-between gap-3"}
            [:p {:class "min-w-0 text-sm font-medium text-zinc-800 truncate"} title]
            [:span {:class "flex-shrink-0 text-sm font-semibold text-zinc-900 tabular-nums"}
             (utilities/amount->rands amount)]]
           [:div {:class "mt-2 flex items-center gap-3"}
            [:div {:class "flex-1 overflow-hidden h-1.5 bg-zinc-100 rounded-full"}
             [:div {:class "h-full w-full rounded-full greed-bar-grow bg-rose-400"
                    :style {:width (str (int p) "%")}}]]
            [:span {:class "flex-shrink-0 w-10 text-right text-xs font-medium text-rose-500 tabular-nums"}
             (utilities/pct-label p)]]]))]
     [:p {:class "text-center py-6 text-sm text-zinc-400"}
      "No expenses yet. Add them in "
      [:a {:href "/app/finances" :class "font-medium text-emerald-600 hover:underline"} "Finances"]
      "."])])

(defn- empty-state []
  [:div {:class "py-14 px-6 text-center bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card"}
   [:div {:class "mx-auto mb-3 flex items-center justify-center w-12 h-12 rounded-full bg-emerald-50"}
    [:span {:class "text-emerald-500"} (svgs/wallet)]]
   [:p {:class "text-sm font-medium text-zinc-600"} "No budget yet"]
   [:p {:class "mt-1 text-xs text-zinc-400"} "Add your income and expenses to see where your money goes."]
   (shared/btn :variant :primary :size :md :class "mt-5" :href "/app/finances" "Add your budget")])

(defn page [{:keys [session] :as ctx}]
  (let [user-id      (:uid session)
        budget-items (data/get-budget-items ctx user-id)
        finances     (data/get-finances ctx user-id)
        payday       (:finances/payday finances)
        {:keys [total-income total-expenses total-savings]} (data/get-budget-data budget-items)
        expense-items (filterv #(= (:budget-item/type %) :expenses) budget-items)
        leftover      (- total-income total-expenses total-savings)
        overspend?    (neg? leftover)
        savings-rate  (or (utilities/pct-share total-savings total-income) 0)]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (headers/pages-heading ["Insights"])
      [:p {:class "text-sm text-zinc-500"}
       "A monthly read on your money — where it comes from, where it goes, and what's left over."]
      (if (empty? budget-items)
        (empty-state)
        [:<>
         (stats/insights-hero total-income total-expenses leftover overspend? savings-rate)
         [:div
          (stats/section-header "This month" :href "/app/finances/" :link-label "View in Finances")
          [:div {:class "grid grid-cols-1 items-stretch gap-4 md:grid-cols-2 xl:grid-cols-3"}
           (allocation-card total-income total-expenses total-savings leftover overspend?)
           (expense-breakdown-card expense-items total-expenses)
           (stats/savings-pace budget-items payday)]]])])))
