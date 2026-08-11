(ns com.greed.ui.app.insights
  (:require [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.core :as c.ui]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.stats :as stats]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.utilities.core :as utilities]))

(defn- safe-pct
  "part as a percentage of whole (0.0 when whole is 0)."
  [part whole]
  (if (and whole (pos? whole))
    (double (* 100.0 (/ (double (or part 0)) whole)))
    0.0))

(defn- pct-str
  "Rounds a percentage to a whole number for display, e.g. 17.03 -> '17%'."
  [p]
  (str (int (Math/round (double p))) "%"))

(defn- hero-substat [label value value-cls]
  [:div {:class "min-w-0"}
   [:p {:class "text-[11px] font-medium text-zinc-500 uppercase tracking-wider whitespace-nowrap"} label]
   [:p {:class (str "mt-1 text-sm font-semibold whitespace-nowrap tabular-nums sm:text-lg "
                    (or value-cls "text-zinc-900"))} value]])

(defn- hero
  "Leads the page with the single most useful health signal — the savings rate —
   and keeps income, expenses and what's left one glance away beneath it."
  [total-income total-expenses leftover overspend? savings-rate]
  (let [rate-num (int (Math/round savings-rate))
        status   (cond
                   (not (pos? total-income))
                   [:span "Add your "
                    [:a {:href "/app/finances"
                         :class "font-semibold text-emerald-600 underline underline-offset-2"}
                     "income"]
                    " in Finances to get a read on your savings."]
                   overspend?
                   [:span {:class "text-rose-600"} "You're spending more than you earn — review your budget in Finances."]
                   (>= rate-num 20) "A strong savings foundation — keep it up."
                   (>= rate-num 10) "On a healthy track. A 20% savings rate is a great goal."
                   (pos? rate-num) "A solid start — every little bit compounds."
                   :else
                   [:span "Nothing set aside yet. "
                    [:a {:href "/app/finances"
                         :class "font-semibold text-emerald-600 underline underline-offset-2"}
                     "Add savings"]
                    " in Finances."])]
    [:div {:class "relative overflow-hidden rounded-2xl bg-white ring-1 ring-zinc-200/70 shadow-card-md"}
     [:div {:class "absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-emerald-500/40 to-transparent"}]
     [:div {:class "absolute -top-24 -right-24 h-72 w-72 rounded-full bg-emerald-400/10 blur-3xl"}]
     [:div {:class "relative px-6 py-6 sm:px-8 sm:py-7"}
      [:div {:class "flex items-center gap-2.5"}
       [:span {:class "h-1.5 w-1.5 rounded-full bg-emerald-500"}]
       [:p {:class "text-[11px] sm:text-xs font-semibold text-emerald-600 uppercase tracking-[0.18em]"}
        "Monthly savings rate"]]
      [:div {:class "mt-5 flex items-baseline gap-2.5 sm:mt-6"}
       [:p {:class "text-5xl sm:text-6xl font-bold text-zinc-900 leading-none tracking-[-0.05em] tabular-nums"}
        (if (pos? total-income) (pct-str savings-rate) "—")]
       [:span {:class "text-sm font-medium text-zinc-400"} "of income saved"]]
      [:p {:class (str "mt-3 text-sm " (if overspend? "text-rose-600" "text-zinc-500"))} status]
      [:div {:class "grid grid-cols-3 gap-4 pt-5 border-t border-zinc-100 mt-6"}
       (hero-substat "Income" (utilities/amount->rands total-income) "text-emerald-600")
       (hero-substat "Expenses" (utilities/amount->rands total-expenses) "text-rose-600")
       (hero-substat (if overspend? "Overspend" "Unallocated")
                     (utilities/amount->rands (Math/abs (long leftover)))
                     (if overspend? "text-rose-600" "text-zinc-900"))]]]))

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
  (let [exp-pct (safe-pct expenses income)
        sav-pct (safe-pct savings income)
        rem-pct (safe-pct leftover income)]
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
         (legend-row "bg-rose-400" "Expenses" expenses (pct-str exp-pct) nil)
         (legend-row "bg-emerald-500" "Savings" savings (pct-str sav-pct) nil)
         (legend-row "bg-zinc-300" (if overspend? "Overspend" "Unallocated")
                     (Math/abs (long leftover))
                     (if overspend? "—" (pct-str rem-pct))
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
        (let [p (safe-pct amount total-expenses)]
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
             (pct-str p)]]]))]
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
        {:keys [total-income total-expenses total-savings]} (c.ui/get-budget-data budget-items)
        expense-items (filterv #(= (:budget-item/type %) :expenses) budget-items)
        leftover      (- total-income total-expenses total-savings)
        overspend?    (neg? leftover)
        savings-rate  (safe-pct total-savings total-income)]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (headers/pages-heading ["Insights"])
      [:p {:class "text-sm text-zinc-500"}
       "A monthly read on your money — where it comes from, where it goes, and what's left over."]
      (if (empty? budget-items)
        (empty-state)
        [:<>
         (hero total-income total-expenses leftover overspend? savings-rate)
         [:div
          (stats/section-header "This month" :href "/app/finances/" :link-label "View in Finances")
          [:div {:class "grid grid-cols-1 items-stretch gap-4 lg:grid-cols-2"}
           (allocation-card total-income total-expenses total-savings leftover overspend?)
           (expense-breakdown-card expense-items total-expenses)]]])])))
