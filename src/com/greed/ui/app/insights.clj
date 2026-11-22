(ns com.greed.ui.app.insights
  (:require [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.stats :as stats]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.ui.app.statements :as spending]
            [com.greed.utilities.core :as utilities]))

(defn- legend-row [dot-cls label amount pct amount-cls]
  [:div {:class "flex items-center gap-3"}
   [:span {:class (str "flex-shrink-0 w-2 h-2 rounded-full " dot-cls)}]
   [:span {:class "min-w-0 flex-1 text-sm text-zinc-600 truncate"} label]
   [:span {:class (str "flex-shrink-0 text-sm font-semibold tabular-nums " (or amount-cls "text-zinc-900"))}
    (utilities/amount->rands amount)]
   [:span {:class "flex-shrink-0 w-10 text-right text-xs text-zinc-500 tabular-nums"} pct]])

(defn- allocation-card
  "A single stacked bar makes the part/whole split of one Rand instantly
   legible. An overspend can't be drawn as a bar (it exceeds 100%), so it's
   surfaced honestly as a warning strip instead."
  [income expenses savings leftover overspend?]
  (let [exp-pct (or (utilities/pct-share expenses income) 0)
        sav-pct (or (utilities/pct-share savings income) 0)
        rem-pct (or (utilities/pct-share leftover income) 0)]
    [:div {:class "p-6 bg-white ring-1 ring-zinc-200/70 rounded-lg"}
     [:h3 {:class "text-sm font-semibold text-zinc-900 tracking-tight"} "Where your income goes"]
     [:p {:class "mt-0.5 mb-5 text-xs text-zinc-500 leading-relaxed"}
      "How each R1 coming in is split between money out, savings and what is left over."]
     (if (pos? income)
       [:<>
        (if overspend?
          [:div {:class "mb-5 rounded-lg bg-rose-50 px-4 py-2.5 text-xs text-rose-700"}
           (str "Expenses and savings are R " (utilities/amount->rands (Math/abs (long leftover)))
             " more than the money coming in.")]
          [:div {:class "overflow-hidden h-3 w-full bg-zinc-100 rounded-full"}
           [:div {:class "h-full w-full overflow-hidden rounded-full"}
            [:div {:class "flex h-full w-full"}
             [:div {:class "h-full bg-zinc-700 greed-bar-grow" :style {:width (str (int exp-pct) "%")}}]
             [:div {:class "h-full bg-brand-600 greed-bar-grow" :style {:width (str (int sav-pct) "%")}}]
             [:div {:class "h-full bg-zinc-200 greed-bar-grow" :style {:width (str (int rem-pct) "%")}}]]]])
        [:div {:class "space-y-2.5 mt-5"}
         (legend-row "bg-zinc-700" "Money out" expenses (utilities/pct-label exp-pct) nil)
         (legend-row "bg-brand-600" "Saved" savings (utilities/pct-label sav-pct) nil)
         (legend-row "bg-zinc-300" (if overspend? "Over budget" "Left over")
           (Math/abs (long leftover))
           (if overspend? "-" (utilities/pct-label rem-pct))
           (when overspend? "text-rose-600"))]]
       [:p {:class "text-center py-6 text-sm text-zinc-500"}
        "Add your "
        [:a {:href "/app/finances" :class "font-medium text-zinc-900 hover:underline"} "income"]
        " in Budget to see this breakdown."])]))

(defn- expense-breakdown-card [expense-items total-expenses]
  [:div {:class "p-6 bg-white ring-1 ring-zinc-200/70 rounded-lg"}
   [:h3 {:class "text-sm font-semibold text-zinc-900 tracking-tight"} "Expense breakdown"]
   [:p {:class "mt-0.5 mb-5 text-xs text-zinc-500 leading-relaxed"}
    "Each expense as a share of everything going out in the month."]
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
             [:div {:class "h-full w-full rounded-full greed-bar-grow bg-zinc-400"
                    :style {:width (str (int p) "%")}}]]
            [:span {:class "flex-shrink-0 w-10 text-right text-xs font-medium text-zinc-500 tabular-nums"}
             (utilities/pct-label p)]]]))]
     [:p {:class "text-center py-6 text-sm text-zinc-500"}
      "No expenses yet. Add them in "
      [:a {:href "/app/finances" :class "font-medium text-zinc-900 hover:underline"} "Budget"]
      "."])])

(defn- empty-state []
  [:div {:class "py-14 px-6 text-center bg-white ring-1 ring-zinc-200/70 rounded-lg"}
   [:div {:class "mx-auto mb-3 flex items-center justify-center w-12 h-12 rounded-full bg-zinc-100"}
    [:span {:class "text-zinc-500"} (svgs/wallet)]]
   [:p {:class "text-sm font-medium text-zinc-600"} "No budget yet"]
   [:p {:class "mt-1 text-xs text-zinc-500"} "Add your income and expenses to see where your money goes."]
   (shared/btn :variant :dark :size :md :class "mt-5" :href "/app/finances" "Add your budget")])

(defn- month-label [m pattern]
  (.format (java.time.YearMonth/parse m) (java.time.format.DateTimeFormatter/ofPattern pattern)))

(defn- months-to-show
  "The months Insights offers: the last six, and any month with a bank
   statement or a budget item from a statement, oldest first."
  [all-items summaries]
  (let [now  (java.time.YearMonth/now)
        last6 (for [i (range 5 -1 -1)] (str (.minusMonths now i)))]
    (->> (concat last6
                 (keep :statement-summary/month summaries)
                 (keep :budget-item/from-statement all-items))
         (remove #(pos? (compare % (str now))))
         distinct sort vec)))

(defn- month-tabs
  "One tab per month, with arrows for the month before and after."
  [months selected]
  (let [i     (.indexOf ^java.util.List months selected)
        prev  (when (pos? i) (months (dec i)))
        next  (when (< -1 i (dec (count months))) (months (inc i)))
        href  #(str "/app/insights?month=" %)
        arrow (fn [m label text]
                (if m
                  [:a {:href (href m) :aria-label label
                       :class "rounded-md border border-zinc-200 bg-white px-3 py-1.5 text-sm text-zinc-900 transition hover:bg-zinc-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"} text]
                  [:span {:aria-hidden "true" :class "rounded-md border border-zinc-100 px-3 py-1.5 text-sm text-zinc-300"} text]))]
    [:nav {:aria-label "Months" :class "flex items-center gap-2"}
     (arrow prev "Previous month" "←")
     [:ul {:class "flex min-w-0 flex-1 gap-2 overflow-x-auto"}
      (for [m months]
        [:li {:class "flex-shrink-0"}
         [:a (cond-> {:href (href m)
                      :class (str "block rounded-md px-3 py-1.5 text-sm transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 "
                                  (if (= m selected) "bg-zinc-900 font-medium text-white" "text-zinc-700 hover:bg-zinc-100"))}
               (= m selected) (assoc :aria-current "page"))
          (month-label m "MMM yyyy")]])]
     (arrow next "Next month" "→")]))

(defn page
  "GET /app/insights?month=yyyy-MM: the current profile's month (Personal's
   or a business's), this month unless another is chosen in the tabs. A
   business has no payday or savings-rate hero."
  [{:keys [session params] :as ctx}]
  (let [user-id      (:uid session)
        owner        (data/owner ctx)
        business?    (some? (:business-id owner))
        all-items    (data/get-owner-budget-items ctx owner)
        summaries    (data/get-statement-summaries ctx owner)
        current      (data/current-month)
        months       (months-to-show all-items summaries)
        month        (let [m (str (:month params))] (if (some #{m} months) m current))
        this-month?  (= month current)
        ;; that month: monthly items and that month's statement items
        budget-items (data/items-for-month all-items month)
        finances     (when-not business? (data/get-finances ctx user-id))
        payday       (:finances/payday finances)
        {:keys [total-income total-expenses total-savings]} (data/get-budget-data budget-items)
        expense-items (filterv #(= (:budget-item/type %) :expenses) budget-items)
        leftover      (- total-income total-expenses total-savings)
        overspend?    (neg? leftover)
        savings-rate  (or (utilities/pct-share total-savings total-income) 0)]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (headers/pages-heading ["Insights"] "A monthly read on the money: where it comes from, where it goes, and what's left over.")
      (month-tabs months month)
      (if (empty? budget-items)
        (empty-state)
        [:<>
         (when-not business?
           (stats/insights-hero total-income total-expenses leftover overspend? savings-rate))
         ;; the donut and savings ring that used to be on Home
         [:div
          (stats/section-header "At a glance")
          (stats/cashflow-donut budget-items)]
         [:div
          (stats/section-header (if this-month? "This month" (month-label month "MMMM yyyy"))
                                :href (str "/app/finances/?month=" month) :link-label "View in Budget")
          [:div {:class "grid grid-cols-1 items-stretch gap-4 md:grid-cols-2 xl:grid-cols-3"}
           (allocation-card total-income total-expenses total-savings leftover overspend?)
           (expense-breakdown-card expense-items total-expenses)
           ;; payday pacing only means something for the month you are in
           (when (and (not business?) this-month?) (stats/savings-pace budget-items payday))]]
         [:div
          (stats/section-header "From your bank statements" :href (str "/app/spending?month=" month) :link-label "View in Spending")
          (spending/planned-vs-spent-card summaries all-items total-expenses :for-month month)]])])))
