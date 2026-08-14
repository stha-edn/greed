(ns com.greed.ui.pages.home
  (:require [com.greed.ui.components.cards :as cards]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.svgs :as svgs]))

(defn- emerald-dot []
  [:span {:class "w-1.5 h-1.5 bg-emerald-500 rounded-full"}])

(defn- badge [class & content]
  [:div {:class (str "inline-flex items-center gap-2 px-3 py-1.5 text-xs font-semibold text-emerald-700 bg-emerald-50 border border-emerald-100 rounded-full " class)}
   (emerald-dot)
   content])

(defn- trust-stat [value label]
  [:div
   [:p {:class "text-xl font-bold text-zinc-900"} value]
   [:p {:class "mt-0.5 text-xs text-zinc-400"} label]])

(defn- tool-card [& {:keys [icon badge title description detail cta-label cta-href class]}]
  [:a {:href cta-href
       :class (str "reveal group flex flex-col p-8 bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card transition-all duration-200 hover:-translate-y-0.5 hover:shadow-card-hover hover:ring-zinc-300/70 active:scale-[0.98] " class)}
   [:div {:class "mb-5 flex items-start justify-between"}
    [:div {:class "flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 ring-1 ring-emerald-600/10 transition-transform duration-200 group-hover:scale-105"}
     icon]
    (when badge
      [:span {:class "rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700 ring-1 ring-emerald-600/15"} badge])]
   [:h3 {:class "text-lg font-semibold text-zinc-900 tracking-tight group-hover:text-emerald-600 transition-colors"} title]
   [:p {:class "mt-2 text-sm text-zinc-500 leading-relaxed"} description]
   (when detail
     [:ul {:class "mt-5 space-y-2.5"}
      (for [item detail]
        [:li {:class "flex items-start gap-2.5 text-sm text-zinc-600"}
         [:span {:class "mt-0.5 flex h-4 w-4 flex-shrink-0 items-center justify-center rounded-full bg-emerald-50 text-emerald-600"}
          [:svg {:class "size-3" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24" :stroke-width "3"}
           [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M5 13l4 4L19 7"}]]]
         item])])
   [:span {:class "inline-flex items-center gap-2 mt-6 text-sm font-semibold text-zinc-900 transition-colors group-hover:text-emerald-600"}
    cta-label
    (svgs/->next {:class "size-3.5 -translate-x-0.5 transition-transform group-hover:translate-x-0"})]])

(defn- finances-visual []
  [:div {:class "space-y-2.5"}
   [:div {:class "flex items-center justify-between text-sm"}
    [:span {:class "text-zinc-500"} "Take-home"]
    [:span {:class "font-semibold text-zinc-900 tabular-nums"} "R 28,000"]]
   [:div {:class "flex items-center justify-between text-sm"}
    [:span {:class "text-zinc-500"} "Spending"]
    [:span {:class "font-semibold text-zinc-900 tabular-nums"} "R 16,200"]]
   [:div {:class "flex items-center justify-between text-sm"}
    [:span {:class "text-zinc-500"} "Saved"]
    [:span {:class "font-semibold text-emerald-600 tabular-nums"} "R 4,800"]]])

(defn- goals-visual []
  [:<>
   [:div {:class "flex items-end justify-between"}
    [:span {:class "text-sm font-semibold text-zinc-900 tabular-nums"} "R 42,000"]
    [:span {:class "text-xs text-zinc-400 tabular-nums"} "of R 60,000"]]
   [:div {:class "mt-2 h-2 w-full overflow-hidden rounded-full bg-zinc-100"}
    [:div {:class "h-full rounded-full bg-emerald-500" :style {:width "70%"}}]]
   [:div {:class "mt-2 text-xs font-medium text-emerald-600 tabular-nums"} "70% funded"]])

(defn- insights-visual []
  [:<>
   [:div {:class "flex h-2.5 w-full overflow-hidden rounded-full"}
    [:div {:class "h-full bg-rose-400" :style {:width "58%"}}]
    [:div {:class "h-full bg-emerald-500" :style {:width "14%"}}]
    [:div {:class "h-full bg-zinc-200" :style {:width "28%"}}]]
   [:div {:class "mt-3 flex items-center gap-4 text-xs text-zinc-400"}
    [:span {:class "flex items-center gap-1.5"}
     [:span {:class "h-1.5 w-1.5 rounded-full bg-rose-400"}] "Spending"]
    [:span {:class "flex items-center gap-1.5"}
     [:span {:class "h-1.5 w-1.5 rounded-full bg-emerald-500"}] "Saved"]
    [:span {:class "flex items-center gap-1.5"}
     [:span {:class "h-1.5 w-1.5 rounded-full bg-zinc-300"}] "Left"]]])

(defn- calendar-visual []
  [:div {:class "flex gap-1.5"}
   (for [[day kind] [[1 "bg-emerald-500"] [2 nil] [3 "bg-rose-400"] [4 nil] [5 nil] [6 nil] [7 "bg-emerald-500"]]]
     [:div {:class (str "flex flex-1 flex-col items-center justify-center gap-1.5 rounded-md py-1.5 text-xs font-medium "
                        (if kind "bg-zinc-50 text-zinc-900" "text-zinc-400"))}
      day
      (when kind
        [:span {:class (str "h-1 w-1 rounded-full " kind)}])])])

(defn- feature-card [& {:keys [icon title description href visual class]}]
  [:a {:href href
       :class (str "reveal group flex flex-col p-6 bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card transition-all duration-200 hover:-translate-y-0.5 hover:shadow-card-hover hover:ring-zinc-300/70 active:scale-[0.98] " class)}
   [:div {:class "flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 ring-1 ring-emerald-600/10 transition-transform duration-200 group-hover:scale-105"}
    icon]
   [:h3 {:class "mt-4 text-base font-semibold text-zinc-900 tracking-tight group-hover:text-emerald-600 transition-colors"} title]
   [:p {:class "mt-1 text-sm text-zinc-500 leading-relaxed"} description]
   (when visual
     [:div {:class "mt-5" :aria-hidden "true"} visual])])

(defn page [{:keys [session]}]
  (let [signed-in? (some? (:uid session))
        app-href   (if signed-in? "/app" "/signin")]
    [:div {:class "container mx-auto px-6"}

     ;; Hero
     [:div {:class "flex flex-col items-center gap-12 py-16 lg:flex-row lg:py-24"}
      [:div {:class "flex-1 max-w-lg"}
       (badge "mb-6" "Personal finance, simplified")
       [:h1 {:class "text-4xl font-bold text-zinc-900 leading-[1.08] tracking-tight text-balance lg:text-5xl"}
        "Take control of your "
        [:span {:class "text-emerald-600"} "finances."]]
       [:p {:class "mt-4 text-lg text-zinc-500 leading-relaxed"}
        "Greed brings your salary, tax, spending and savings into one clear place — so you always know what you earn, what SARS takes, and what you keep."]
       [:div {:class "flex flex-wrap gap-3 mt-8"}
        (if signed-in?
          (shared/btn :variant :dark :size :lg :href "/app" "Go to dashboard")
          [:div {:class "flex flex-wrap gap-3"}
           (shared/btn :variant :primary :size :lg :href "/signup" "Get started for free")
           (shared/btn :variant :outline :size :lg :href "/signin" :class "hover:border-zinc-500" "Sign in")])]
       [:div {:class "flex items-center gap-6 mt-10"}
        (trust-stat "SARS" "Tax calculator")
        [:div {:class "w-px h-8 bg-zinc-200"}]
        (trust-stat "100%" "Free to use")
        [:div {:class "w-px h-8 bg-zinc-200"}]
        (trust-stat "ZAR" "South African Rand")]]
      [:div {:class "flex-1 flex justify-center lg:justify-end"}
       (cards/note-from-greed)]]

     ;; Free tools
     [:div {:class "pb-20 lg:pb-28"}
      [:div {:class "reveal mb-10"}
       (badge "mb-4" "Free tools")
       [:h2 {:class "text-3xl font-bold text-zinc-900 tracking-tight text-balance lg:text-4xl"}
        "Built for South African "
        [:span {:class "text-emerald-600"} "taxpayers."]]
       [:p {:class "max-w-xl mt-3 text-zinc-500"}
        "No accountant needed. Our calculators use the latest SARS brackets and rebates so you always know where you stand."]]
      [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"}
       (tool-card
        :icon (svgs/percent-badge)
        :badge "2026/27 Year"
        :title "Income Tax Calculator"
        :description "Enter your monthly salary and age to instantly see your gross tax, rebates, effective rate, and take-home pay — no sign-up required."
        :detail ["SARS 2026/27 tax brackets"
                 "Primary, secondary and tertiary rebates"
                 "Effective vs. marginal rate breakdown"
                 "Monthly net income"]
        :cta-label "Calculate your tax"
        :cta-href (if signed-in? "/app/tax/income-tax-calculator" "/signin")
        :class "reveal-1")
       (tool-card
        :icon (svgs/document-text)
        :badge "2026 Year of Assessment"
        :title "Tax Returns Simulator (ITR12)"
        :description "Simulate your full SARS tax return with all common deductions. See whether you are owed a refund or have tax to pay before you file."
        :detail ["Medical aid tax credits (MTC)"
                 "Retirement annuity (RA) deductions"
                 "Travel allowance — logbook or 80% rule"
                 "Out-of-pocket medical expenses (s6B)"
                 "Auto assessment from your salary"]
        :cta-label "Simulate your return"
        :cta-href (if signed-in? "/app/tax/tax-returns" "/signin")
        :class "reveal-2")
       (tool-card
        :icon (svgs/gift)
        :badge "2026/27 Year"
        :title "Bonus Tax Calculator"
        :description "See how much PAYE comes off a bonus or 13th cheque, and what actually lands in your account — taxed correctly at your marginal rate."
        :detail ["Marginal-rate bonus tax"
                 "Compares your tax with and without the bonus"
                 "Net take-home on your bonus"
                 "Effective bonus tax rate"]
        :cta-label "Work out your bonus"
        :cta-href (if signed-in? "/app/tax/bonus-tax-calculator" "/signin")
        :class "reveal-3")]]

     ;; In-app features
     [:div {:class "reveal mb-10"}
      (badge "mb-4" "Inside your account")
      [:h2 {:class "text-3xl font-bold text-zinc-900 tracking-tight text-balance lg:text-4xl"}
       "More than a "
       [:span {:class "text-emerald-600"} "calculator."]]
      [:p {:class "max-w-xl mt-3 text-zinc-500"}
       "Create a free account to track your whole financial picture — your salary and medical aid flow in automatically."]]
     [:div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4"}
      (feature-card
       :icon (svgs/credit-card)
       :title "Finances"
       :description "Income, expenses and savings in one budget — so every rand has a job."
       :href app-href
       :visual (finances-visual)
       :class "reveal-1")
      (feature-card
       :icon (svgs/target)
       :title "Goals"
       :description "Set a target like an emergency fund and watch each one fill up."
       :href app-href
       :visual (goals-visual)
       :class "reveal-2")
      (feature-card
       :icon (svgs/chart-bar)
       :title "Insights"
       :description "See your savings rate and exactly where every rand goes each month."
       :href app-href
       :visual (insights-visual)
       :class "reveal-3")
      (feature-card
       :icon (svgs/calendar)
       :title "Calendar"
       :description "Mark paydays, bills and incoming payments so nothing slips through."
       :href app-href
       :visual (calendar-visual)
       :class "reveal-4")]

     ;; CTA
     [:div {:class "reveal pt-24 lg:pt-28 pb-24 lg:pb-28"}
      [:div {:class "relative overflow-hidden px-8 py-16 bg-zinc-900 rounded-3xl lg:px-16"}
       [:div {:class "absolute -top-32 -right-24 w-96 h-96 rounded-full bg-emerald-500/20 blur-3xl"}]
       [:div {:class "absolute -bottom-32 -left-24 w-96 h-96 rounded-full bg-emerald-500/10 blur-3xl"}]
       [:div {:class "relative text-center max-w-2xl mx-auto"}
        [:h2 {:class "text-3xl font-bold text-white tracking-tight text-balance lg:text-4xl"}
         "Your money should " [:span {:class "text-emerald-500"} "make sense."]]
        [:p {:class "max-w-xl mx-auto mt-4 text-lg text-zinc-400"}
         "Know what you earn, what SARS takes, what's left over and what you can build next — all in one calm, private place."]
        [:a {:href (if signed-in? "/app" "/signup")
             :class "inline-block px-8 py-3.5 mt-8 text-sm font-semibold text-emerald-700 bg-white rounded-xl transition-colors hover:bg-emerald-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 active:bg-emerald-100 active:scale-[0.97]"}
         (if signed-in? "Go to dashboard" "Get started for free")]]]]]))
