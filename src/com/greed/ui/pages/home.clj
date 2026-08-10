(ns com.greed.ui.pages.home
  (:require [com.greed.ui.components.cards :as cards]
            [com.greed.ui.components.shared :as shared]))

(defn- emerald-dot []
  [:span {:class "w-1.5 h-1.5 bg-emerald-500 rounded-full"}])

(defn- tool-card [& {:keys [badge title description detail cta-label cta-href]}]
  [:div {:class "flex flex-col p-8 bg-white border border-zinc-200/70 rounded-2xl shadow-card"}
   (when badge
     [:span {:class "self-start px-2.5 py-1 mb-5 text-xs font-semibold text-emerald-700 bg-emerald-50 border border-emerald-100 rounded-full"}
      badge])
   [:h3 {:class "text-xl font-bold text-zinc-900"} title]
   [:p {:class "flex-1 mt-2 text-sm text-zinc-500 leading-relaxed"} description]
   (when detail
     [:ul {:class "space-y-1.5 mt-4"}
      (for [item detail]
        [:li {:class "flex items-start gap-2 text-sm text-zinc-600"}
         [:span {:class "flex flex-shrink-0 items-center justify-center w-4 h-4 mt-0.5 bg-emerald-50 rounded-full"}
          (emerald-dot)]
         item])])
   [:a {:href cta-href
        :class "inline-flex items-center gap-2 group mt-6 text-sm font-semibold text-zinc-900 transition-colors hover:text-emerald-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 active:text-emerald-700"}
    cta-label
    [:svg {:class "w-4 h-4 transition-transform group-hover:translate-x-0.5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
     [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M9 5l7 7-7 7"}]]]])

(defn- feature-item [title description]
  [:div {:class "p-5 bg-white border border-zinc-200/70 rounded-xl shadow-card"}
   [:div {:class "flex items-center justify-center w-8 h-8 mb-3 bg-emerald-50 rounded-lg"}
    [:span {:class "w-2 h-2 bg-emerald-500 rounded-full"}]]
   [:h4 {:class "text-sm font-semibold text-zinc-900"} title]
   [:p {:class "mt-1 text-sm text-zinc-500 leading-relaxed"} description]])

(defn page [{:keys [session]}]
  (let [signed-in? (some? (:uid session))]
  [:div {:class "container mx-auto px-6"}

   ;; Hero
   [:div {:class "flex flex-col items-center gap-12 py-16 lg:flex-row lg:py-24"}
    [:div {:class "flex-1 max-w-lg"}
     [:div {:class "inline-flex items-center gap-2 px-3 py-1.5 mb-6 text-xs font-semibold text-emerald-700 bg-emerald-50 border border-emerald-100 rounded-full"}
      (emerald-dot)
      "Personal finance, simplified"]
     [:h1 {:class "text-4xl font-bold text-zinc-900 leading-tight tracking-tight lg:text-5xl"}
      "Take control of your "
      [:span {:class "text-emerald-600"} "finances."]]
     [:p {:class "mt-4 text-lg text-zinc-500 leading-relaxed"}
      "Greed gives you the tools to track your spending, understand your tax obligations, and make smarter financial decisions — all in one place."]
     [:div {:class "flex flex-wrap gap-3 mt-8"}
      (if signed-in?
         (shared/btn :variant :dark :size :lg :href "/app" "Go to dashboard")
        [:div {:class "flex flex-wrap gap-3"}
         (shared/btn :variant :primary :size :lg :href "/signup" "Get started for free")
         (shared/btn :variant :outline :size :lg :href "/signin" :class "hover:border-zinc-500" "Sign in")])]
     [:div {:class "flex items-center gap-6 mt-10"}
      [:div
       [:p {:class "text-xl font-bold text-zinc-900"} "SARS"]
       [:p {:class "mt-0.5 text-xs text-zinc-400"} "Tax calculator"]]
      [:div {:class "w-px h-8 bg-zinc-200"}]
      [:div
       [:p {:class "text-xl font-bold text-zinc-900"} "100%"]
       [:p {:class "mt-0.5 text-xs text-zinc-400"} "Free to use"]]
      [:div {:class "w-px h-8 bg-zinc-200"}]
      [:div
       [:p {:class "text-xl font-bold text-zinc-900"} "ZAR"]
       [:p {:class "mt-0.5 text-xs text-zinc-400"} "South African Rand"]]]]
    [:div {:class "flex-1 flex justify-center lg:justify-end"}
     (cards/note-from-greed)]]

   ;; Tools section
   [:div {:class "pb-20 lg:pb-28"}
    [:div {:class "mb-10"}
     [:div {:class "inline-flex items-center gap-2 px-3 py-1.5 mb-4 text-xs font-semibold text-white bg-zinc-900 rounded-full"}
      (emerald-dot)
      "Free tools"]
     [:h2 {:class "text-3xl font-bold text-zinc-900 tracking-tight lg:text-4xl"}
      "Built for South African "
      [:span {:class "text-emerald-600"} "taxpayers."]]
     [:p {:class "max-w-xl mt-3 text-zinc-500"}
      "No accountant needed. Our calculators use the latest SARS brackets and rebates so you always know where you stand."]]
    [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"}
     (tool-card
      :badge "2026/27 Year"
      :title "Income Tax Calculator"
      :description "Enter your monthly salary and age to instantly see your gross tax, rebates, effective rate, and take-home pay — no sign-up required."
      :detail ["SARS 2026/27 tax brackets"
               "Primary, secondary and tertiary rebates"
               "Effective vs. marginal rate breakdown"
               "Monthly net income"]
      :cta-label "Calculate your tax"
      :cta-href (if signed-in? "/app/tax/income-tax-calculator" "/signin"))
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
      :cta-href (if signed-in? "/app/tax/tax-returns" "/signin"))
     (tool-card
      :badge "2026/27 Year"
      :title "Bonus Tax Calculator"
      :description "See how much PAYE comes off a bonus or 13th cheque, and what actually lands in your account — taxed correctly at your marginal rate."
      :detail ["Marginal-rate bonus tax"
               "Compares your tax with and without the bonus"
               "Net take-home on your bonus"
               "Effective bonus tax rate"]
      :cta-label "Work out your bonus"
      :cta-href (if signed-in? "/app/tax/bonus-tax-calculator" "/signin"))]

    ;; In-app features
    [:div {:class "mt-20"}
     [:div {:class "mb-8"}
      [:div {:class "inline-flex items-center gap-2 px-3 py-1.5 mb-4 text-xs font-semibold text-white bg-zinc-900 rounded-full"}
       (emerald-dot)
       "Inside your account"]
      [:h2 {:class "text-3xl font-bold text-zinc-900 tracking-tight lg:text-4xl"}
       "More than a "
       [:span {:class "text-emerald-600"} "calculator."]]
      [:p {:class "max-w-xl mt-3 text-zinc-500"}
       "Create a free account to track your whole financial picture — your salary and medical aid flow in automatically."]]
     [:div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4"}
      (feature-item "Budget Tracker"
                    "Track income, expenses and savings in one place. Your salary and medical aid sync in automatically.")
      (feature-item "Savings Goals"
                    "Set targets like an emergency fund and watch each one fill up with clear progress bars.")
      (feature-item "Insights"
                    "See your savings rate, expense rate, and exactly where every rand goes each month.")
      (feature-item "Financial Calendar"
                    "Mark paydays, bills and incoming payments so nothing slips through the cracks.")]]]]))
