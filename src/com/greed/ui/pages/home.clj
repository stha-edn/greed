(ns com.greed.ui.pages.home
  (:require [com.greed.ui.components.cards :as cards]))

(defn- tool-card [& {:keys [badge title description detail cta-label cta-href]}]
  [:div {:class "bg-white rounded-2xl border border-gray-100 shadow-card p-8 flex flex-col"}
   (when badge
     [:span {:class "self-start text-xs font-semibold text-emerald-700 bg-emerald-50 border border-emerald-100 px-2.5 py-1 rounded-full mb-5"}
      badge])
   [:h3 {:class "text-xl font-bold text-zinc-900"} title]
   [:p {:class "mt-2 text-sm text-zinc-500 leading-relaxed flex-1"} description]
   (when detail
     [:ul {:class "mt-4 space-y-1.5"}
      (for [item detail]
        [:li {:class "flex items-start gap-2 text-sm text-zinc-600"}
         [:span {:class "mt-0.5 flex-shrink-0 w-4 h-4 rounded-full bg-emerald-50 flex items-center justify-center"}
          [:span {:class "w-1.5 h-1.5 rounded-full bg-emerald-500"}]]
         item])])
   [:a {:href cta-href
        :class "mt-6 inline-flex items-center gap-2 text-sm font-semibold text-zinc-900 hover:text-emerald-600 transition-colors group"}
    cta-label
    [:svg {:class "w-4 h-4 transition-transform group-hover:translate-x-0.5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
     [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M9 5l7 7-7 7"}]]]])

(defn page [{:keys [session]}]
  (let [signed-in? (some? (:uid session))]
  [:div {:class "container mx-auto px-6"}

   ;; Hero
   [:div {:class "flex flex-col items-center gap-12 py-16 lg:flex-row lg:py-24"}
    [:div {:class "flex-1 max-w-lg"}
     [:div {:class "inline-flex items-center gap-2 px-3 py-1.5 bg-emerald-50 text-emerald-700 text-xs font-semibold rounded-full mb-6 border border-emerald-100"}
      [:span {:class "w-1.5 h-1.5 rounded-full bg-emerald-500"}]
      "Personal finance, simplified"]
     [:h1 {:class "text-4xl font-bold text-zinc-900 lg:text-5xl leading-tight"}
      "Take control of your "
      [:span {:class "text-emerald-600"} "finances."]]
     [:p {:class "mt-4 text-lg text-zinc-500 leading-relaxed"}
      "Greed gives you the tools to track your spending, understand your tax obligations, and make smarter financial decisions — all in one place."]
     [:div {:class "flex flex-wrap gap-3 mt-8"}
      [:a {:href "/signup"
           :class "px-6 py-3 text-sm font-semibold text-white bg-emerald-600 rounded-xl hover:bg-emerald-700 transition-colors"}
       "Get started for free"]
      [:a {:href "/signin"
           :class "px-6 py-3 text-sm font-semibold text-zinc-700 border border-gray-300 rounded-xl hover:border-gray-500 hover:bg-gray-50 transition-colors"}
       "Sign in"]]
     [:div {:class "flex items-center gap-6 mt-10"}
      [:div
       [:p {:class "text-xl font-bold text-zinc-900"} "SARS"]
       [:p {:class "text-xs text-zinc-400 mt-0.5"} "Tax calculator"]]
      [:div {:class "w-px h-8 bg-gray-200"}]
      [:div
       [:p {:class "text-xl font-bold text-zinc-900"} "100%"]
       [:p {:class "text-xs text-zinc-400 mt-0.5"} "Free to use"]]
      [:div {:class "w-px h-8 bg-gray-200"}]
      [:div
       [:p {:class "text-xl font-bold text-zinc-900"} "ZAR"]
       [:p {:class "text-xs text-zinc-400 mt-0.5"} "South African Rand"]]]]
    [:div {:class "flex-1 flex justify-center lg:justify-end"}
     (cards/testiminial
      :img "/img/avatar.jpg"
      :title "Changed how I manage money"
      :body "Greed helped me understand exactly how much tax I owe each month and where my money is going. I use the tax returns tool every filing season."
      :author "Zanele M.")]]

   ;; Tools section
   [:div {:class "pb-20 lg:pb-28"}
    [:div {:class "mb-10"}
     [:div {:class "inline-flex items-center gap-2 px-3 py-1.5 bg-zinc-900 text-white text-xs font-semibold rounded-full mb-4"}
      [:span {:class "w-1.5 h-1.5 rounded-full bg-emerald-500"}]
      "Free tools"]
     [:h2 {:class "text-3xl font-bold text-zinc-900 lg:text-4xl"}
      "Built for South African "
      [:span {:class "text-emerald-600"} "taxpayers."]]
     [:p {:class "mt-3 text-zinc-500 max-w-xl"}
      "No accountant needed. Our calculators use the latest SARS brackets and rebates so you always know where you stand."]]
    [:div {:class "grid grid-cols-1 md:grid-cols-2 gap-6"}
     (tool-card
      :badge "2026/27 Year"
      :title "Income Tax Calculator"
      :description "Enter your monthly salary and age to instantly see your gross tax, rebates, effective rate, and take-home pay — no sign-up required."
      :detail ["SARS 2026/27 tax brackets"
               "Primary, secondary and tertiary rebates"
               "Effective vs. marginal rate breakdown"
               "Monthly net income"]
      :cta-label "Calculate your tax"
      :cta-href (if signed-in? "/app/tools/income-tax-calculator" "/signin"))
     (tool-card
      :badge "2026 Year of Assessment"
      :title "Tax Returns Simulator (ITR12)"
      :description "Simulate your full SARS tax return with all common deductions. See whether you are owed a refund or have tax to pay before you file."
      :detail ["Medical aid tax credits (MTC)"
               "Retirement annuity (RA) deductions"
               "Travel allowance — logbook or 80% rule"
               "Out-of-pocket medical expenses (s6B)"
               "Auto assessment from your salary"]
      :cta-label "Simulate your return"
      :cta-href (if signed-in? "/app/tools/tax-returns" "/signin"))]]]))
