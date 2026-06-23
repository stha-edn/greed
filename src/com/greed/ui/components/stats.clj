(ns com.greed.ui.components.stats
  (:require [com.greed.ui.core :as c.ui]
            [com.greed.utilities.core :as utilities]
            [com.greed.ui.components.svgs :as svgs]))

(defn- metric-card [& {:keys [label value icon-bg icon accent]}]
  [:div {:class "relative bg-white rounded-2xl border border-gray-100 shadow-card hover:shadow-card-md transition-shadow duration-200 p-6 overflow-hidden"}
   (when accent
     [:div {:class (str "absolute inset-x-0 top-0 h-1 " accent)}])
   [:div {:class "flex items-start justify-between pt-1"}
    [:div
     [:p {:class "text-xs font-semibold text-zinc-400 uppercase tracking-widest"} label]
     [:p {:class "mt-2 text-2xl font-bold text-zinc-900 tabular-nums"} value]]
    [:div {:class (str "flex-shrink-0 w-12 h-12 rounded-xl flex items-center justify-center " icon-bg)}
     icon]]])

(defn savings-stat [budget-items]
  (let [{:keys [total-savings]} (c.ui/get-budget-data budget-items)]
    (metric-card
     :label "Total Savings"
     :value (utilities/amount->rands total-savings)
     :icon-bg "bg-gray-100"
     :icon [:span {:class "text-zinc-600"} (svgs/money)])))

(defn expense-tracker-stats [budget-items]
  (let [{:keys [total-income total-expenses total-savings]} (c.ui/get-budget-data budget-items)]
    [:div {:class "grid grid-cols-1 sm:grid-cols-3 gap-4"}
     (metric-card
      :label "Monthly Income"
      :value (utilities/amount->rands total-income)
      :accent "bg-emerald-400"
      :icon-bg "bg-emerald-50"
      :icon [:span {:class "text-emerald-600"} (svgs/uptrend)])
     (metric-card
      :label "Monthly Expenses"
      :value (utilities/amount->rands total-expenses)
      :accent "bg-rose-400"
      :icon-bg "bg-rose-50"
      :icon [:span {:class "text-rose-500"} (svgs/downtrend)])
     (metric-card
      :label "Net Savings"
      :value (utilities/amount->rands total-savings)
      :accent "bg-indigo-400"
      :icon-bg "bg-indigo-50"
      :icon [:span {:class "text-indigo-600"} (svgs/banknotes)])]))

(defn- tax-metric [label value sub]
  [:div {:class "bg-white rounded-xl border border-gray-100 shadow-card p-5"}
   [:p {:class "text-xs font-medium text-zinc-400 uppercase tracking-wide"} label]
   [:p {:class "mt-1.5 text-xl font-semibold text-zinc-900"} value]
   (when sub [:p {:class "text-xs text-zinc-400 mt-0.5"} sub])])

(defn tax-stats [income-tax-data]
  (let [{:keys [annual-income gross-tax rebates net-tax net-income effective-rate]} income-tax-data]
    [:div
     [:div {:class "flex items-center justify-between mb-3"}
      [:h2 {:class "text-xs font-medium text-zinc-400 uppercase tracking-wide"} "Tax Overview"]
      [:a {:href "https://www.sars.gov.za/tax-rates/income-tax/rates-of-tax-for-individuals/"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-xs text-emerald-600 hover:underline"}
       "SARS rates"]]
     [:div {:class "grid grid-cols-2 sm:grid-cols-3 gap-4"}
      (tax-metric "Gross Annual Income" (utilities/amount->rands (or annual-income 0)) "Before tax")
      (tax-metric "Gross Tax"           (utilities/amount->rands (or gross-tax 0))     "From brackets")
      (tax-metric "Rebates"             (utilities/amount->rands (or rebates 0))       "Age-based credit")
      (tax-metric "Net Annual Tax"      (utilities/amount->rands (or net-tax 0))       "After rebates")
      (tax-metric "Effective Rate"      (utilities/->percentage (or effective-rate 0)) "Of total income")
      (tax-metric "Net Annual Income"   (utilities/amount->rands (or net-income 0))    "After tax")]]))
