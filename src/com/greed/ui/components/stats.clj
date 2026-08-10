(ns com.greed.ui.components.stats
  (:require [clojure.string :as str]
            [com.greed.ui.core :as c.ui]
            [com.greed.utilities.core :as utilities]
            [com.greed.ui.components.svgs :as svgs]))

(defn- panel
  "Shared soft-gradient panel used across dashboard stats. Extends the
   base card pattern in shared/card with hover feedback."
  [{:keys [padding class]} & children]
  (into [:div
         {:class (str/join " "
                           ["bg-gradient-to-br from-white via-white to-emerald-50/80 border border-zinc-200/70 rounded-xl shadow-card transition-all duration-200 hover:border-emerald-200 hover:shadow-card-hover"
                            (or padding "p-5")
                            class])}]
        children))

(defn- metric-card [& {:keys [label value icon-bg icon]}]
  (panel {}
   [:div {:class "flex items-start justify-between"}
    [:div
     [:p {:class "text-xs font-medium text-zinc-400 uppercase tracking-wider"} label]
     [:p {:class "mt-2 text-2xl font-semibold text-zinc-900 tracking-tight tabular-nums"} value]]
    [:div {:class (str "flex flex-shrink-0 items-center justify-center w-10 h-10 "
                       icon-bg " rounded-xl transition-transform duration-200 group-hover:scale-110")}
     icon]]))

(defn savings-stat [budget-items]
  (let [{:keys [total-savings]} (c.ui/get-budget-data budget-items)]
    (metric-card
     :label "Total Savings"
     :value (utilities/amount->rands total-savings)
     :icon-bg "bg-zinc-100"
     :icon [:span {:class "text-zinc-600"} (svgs/money)])))

(defn expense-tracker-stats [budget-items]
  (let [{:keys [total-income total-expenses total-savings]} (c.ui/get-budget-data budget-items)]
    [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-3"}
     (metric-card
      :label "Monthly Income"
      :value (utilities/amount->rands total-income)
      :icon-bg "bg-emerald-50"
      :icon [:span {:class "text-emerald-600"} (svgs/trending-up)])
     (metric-card
      :label "Monthly Expenses"
      :value (utilities/amount->rands total-expenses)
      :icon-bg "bg-rose-50"
      :icon [:span {:class "text-rose-600"} (svgs/trending-down)])
     (metric-card
      :label "Net Savings"
      :value (utilities/amount->rands total-savings)
      :icon-bg "bg-indigo-50"
      :icon [:span {:class "text-indigo-600"} (svgs/wallet)])]))

(defn- chart-card [& {:keys [title subtitle canvas-id attrs]}]
  (panel {}
   [:h3 {:class "text-sm font-semibold text-zinc-900"} title]
   [:p {:class "mt-0.5 text-xs text-zinc-400"} subtitle]
   [:div {:class "relative h-56 mt-4"}
     [:canvas (merge {:id canvas-id} attrs)]]))

(defn- ->amt [x] (long (Math/round (double (or x 0)))))

(defn tax-charts [{:keys [annual-income gross-tax rebates net-tax net-income effective-rate]}]
  (when (pos? (or annual-income 0))
    [:div {:class "grid grid-cols-1 gap-4 lg:grid-cols-2"}
     (chart-card
      :title "Income split"
      :subtitle "How your gross income divides between take-home pay and tax"
      :canvas-id "incomeSplitChart"
      :attrs {:data-net-income (->amt net-income)
              :data-net-tax     (->amt net-tax)
              :data-effective   (utilities/->percentage (or effective-rate 0))})
     (chart-card
      :title "Tax breakdown"
      :subtitle "Gross tax less age rebates equals your net annual tax"
      :canvas-id "taxBreakdownChart"
      :attrs {:data-gross-tax (->amt gross-tax)
              :data-rebates   (->amt rebates)
              :data-net-tax   (->amt net-tax)})]))

(defn- rand0
  "Compact Rand string with thousands separators, no decimals."
  [n]
  (format "R%,d" (long (Math/round (double (or n 0))))))

(defn- rate-label [rate]
  (str (int (Math/round (* 100 (double (or rate 0))))) "%"))

(defn- bracket-row [i b active-idx brackets]
  (let [active? (= i active-idx)
        from    (get b :threshold 0)
        nxt     (get-in brackets [(inc i) :threshold])
        range-s (if nxt (str (rand0 from) " – " (rand0 (dec nxt))) (str (rand0 from) "+"))]
    [:div {:class (str "flex items-center justify-between px-3 py-2 rounded-lg transition-colors "
                       (if active? "bg-emerald-50 ring-1 ring-emerald-200" "hover:bg-zinc-50"))}
     [:div {:class "flex items-center gap-2.5 min-w-0"}
      [:span {:class (str "flex-shrink-0 w-1.5 h-1.5 rounded-full " (if active? "bg-emerald-500" "bg-zinc-300"))}]
      [:span {:class (str "text-sm truncate tabular-nums " (if active? "font-semibold text-zinc-900" "text-zinc-600"))} range-s]
      (when active?
        [:span {:class "flex-shrink-0 px-1.5 py-0.5 text-[10px] font-semibold text-emerald-700 bg-emerald-100 uppercase tracking-wide rounded"} "You"])]
     [:span {:class (str "flex-shrink-0 text-sm tabular-nums " (if active? "font-semibold text-emerald-700" "text-zinc-500"))}
      (rate-label (get b :rate 0))]]))

(defn tax-bracket-breakdown
  [{:keys [brackets bracket-index marginal-rate annual-income next-threshold income-to-next-bracket]}]
  (when (and (seq brackets) (pos? (or annual-income 0)) bracket-index)
    (let [active   (nth brackets bracket-index)
          from     (get active :threshold 0)
          to       (or next-threshold from)
          span     (max 1 (- to from))
          progress (when next-threshold
                     (min 100.0 (max 0.0 (* 100.0 (/ (- (or annual-income 0) from) span)))))
          next-rate (when next-threshold (rate-label (get-in brackets [(inc bracket-index) :rate] 0)))]
      (panel {}
       [:div {:class "flex items-start justify-between gap-3 mb-4"}
        [:div {:class "min-w-0"}
         [:h3 {:class "text-sm font-semibold text-zinc-900"} "Your tax bracket"]
         [:p {:class "mt-0.5 text-xs text-zinc-400 leading-relaxed"}
          "Income is taxed in slices — your marginal rate applies only to the portion above the bracket threshold."]]
        [:div {:class "flex-shrink-0 text-right"}
         [:p {:class "text-2xl font-bold text-zinc-900 leading-none tracking-tight tabular-nums"} (str (int (or marginal-rate 0)) "%")]
         [:p {:class "mt-1 text-[11px] text-zinc-400 uppercase tracking-wider"} "Marginal rate"]]]
       (when (and next-threshold progress)
         [:div {:class "mb-4"}
          [:div {:class "overflow-hidden h-2 w-full bg-zinc-100 rounded-full"}
           [:div {:class "h-full bg-emerald-500 rounded-full transition-all"
                  :style {:width (str (format "%.0f" progress) "%")}}]]
          [:p {:class "mt-2 text-xs text-zinc-500"}
           [:span {:class "font-semibold text-zinc-700 tabular-nums"} (rand0 income-to-next-bracket)]
           (str " until the " next-rate " bracket")]])
       [:div {:class "space-y-1"}
        (map-indexed (fn [i b] (bracket-row i b bracket-index brackets)) brackets)]))))

(defn tax-stats [income-tax-data]
  (let [has-income? (pos? (or (:annual-income income-tax-data) 0))]
    [:div
     [:div {:class "flex items-center justify-between mb-3"}
      [:h2 {:class "text-xs font-medium text-zinc-400 uppercase tracking-wide"} "Tax Overview"]
      [:a {:href "https://www.sars.gov.za/tax-rates/income-tax/rates-of-tax-for-individuals/"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-xs text-emerald-600 hover:underline focus-visible:underline active:underline"}
       "SARS rates"]]
     (if has-income?
       [:div {:class "space-y-4"}
        (tax-charts income-tax-data)
        (tax-bracket-breakdown income-tax-data)]
       (panel {:padding "p-8" :class "text-center"}
        [:p {:class "text-sm font-medium text-zinc-500"} "No tax data yet"]
        [:p {:class "mt-0.5 text-xs text-zinc-400"} "Add your salary to see your tax breakdown and bracket."]))]))
