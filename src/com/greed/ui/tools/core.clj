(ns com.greed.ui.tools.core
  (:require [com.greed.ui.components.svgs :as svgs]))

(defn- tool-card [& {:keys [title description link badge]}]
  [:a {:href link
       :class "group block bg-white rounded-xl border border-gray-100 shadow-card p-6 hover:border-gray-200 hover:shadow-card-md transition-all duration-150"}
   [:div {:class "flex items-start justify-between"}
    [:div {:class "w-10 h-10 rounded-lg bg-zinc-900 flex items-center justify-center text-white mb-4"}
     (svgs/flame)]
    (when badge
      [:span {:class "text-xs font-medium text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full"} badge])]
   [:h3 {:class "text-sm font-semibold text-zinc-900 group-hover:text-emerald-600 transition-colors"} title]
   [:p {:class "mt-1 text-sm text-zinc-500 leading-relaxed"} description]
   [:div {:class "mt-4 flex items-center gap-1 text-xs font-medium text-zinc-400 group-hover:text-emerald-600 transition-colors"}
    "Open tool"
    [:svg {:class "w-3.5 h-3.5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
     [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M9 5l7 7-7 7"}]]]])

(defn tools []
  [:div
   [:p {:class "text-zinc-500 mb-6 text-sm"}
    "Free tools to help you understand and manage your finances."]
   [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3"}
    (tool-card
     :title "Income Tax Calculator"
     :description "Quickly estimate your annual income tax, rebates, and take-home pay based on SARS 2026/27 brackets."
     :link "/app/tools/income-tax-calculator"
     :badge "2026/27")
    (tool-card
     :title "Tax Returns (ITR12)"
     :description "Simulate your SARS tax return including medical aid credits, RA deductions, and travel allowances."
     :link "/app/tools/tax-returns"
     :badge "2026 year")]])
