(ns com.greed.ui.tools.income-tax-calculator
  (:require [com.biffweb :as biff]
            [com.greed.ui :as ui]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.ui.tools.core :as tools]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.tax :as tax]))

(defn- ->n [s]
  (try (double (BigDecimal. (or s "0")))
       (catch Exception _ 0.0)))

(def tiers
  [["under-65" "Under 65"]
   ["65-74"    "65–74"]
   ["75-plus"  "75+"]])

(defn- tier->age [tier]
  (case tier
    "under-65" 30
    "65-74"    70
    "75-plus"  80
    30))

(defn- age->tier [age]
  (cond
    (>= age 75) "75-plus"
    (>= age 65) "65-74"
    :else       "under-65"))

(defn- guide []
  [:div {:class "space-y-4"}
   (tools/panel
    (tools/panel-heading "How this tool works")
    [:div {:class "px-5 pb-5 sm:px-6"}
     [:p {:class "text-sm text-zinc-500 leading-relaxed"}
      "Enter your gross monthly salary, pick your rebate tier, then click Calculate. The calculator applies the SARS 2026/27 tax brackets and rebates to show your take-home pay."]
     [:p {:class "mt-2 text-sm text-zinc-500 leading-relaxed"}
      "Use this to quickly understand how much of your salary you actually keep, or to compare offers at different salary levels."]])

   (tools/panel
    (tools/panel-heading "Understanding your results")
    [:div {:class "divide-y divide-zinc-100 border-t border-zinc-100"}
     (tools/glossary-item "Gross tax" "The raw tax calculated from the SARS brackets before any rebates are applied.")
     (tools/glossary-item "Rebates" "A fixed annual credit that reduces your tax bill. Under-65s get the primary rebate (R17,820); the 65–74 tier adds a secondary rebate and the 75+ tier a tertiary one.")
     (tools/glossary-item "Effective tax rate" "The percentage of your total income that goes to tax after rebates. This is lower than your marginal rate (the rate on your top bracket) because lower portions of income are taxed at lower rates.")
     (tools/glossary-item "Net income" "Your take-home pay after income tax. Note: UIF and medical aid contributions are not deducted here — this is purely the income tax effect.")])

   (tools/notice
    "This calculator does not account for medical aid credits, retirement annuity deductions, or travel allowances. Use the "
    [:a {:href "/app/tax/tax-returns" :class "font-semibold text-amber-900 underline underline-offset-2 hover:text-amber-950"} "Tax Returns simulator"]
    " for a more complete picture.")])

(defn- salary-field [value]
  [:div
   [:label {:for "income" :class "block text-sm font-medium text-zinc-700 mb-1"} "Monthly gross salary"]
   [:div {:class "relative mt-1"}
    [:span {:class "absolute left-3.5 top-1/2 -translate-y-1/2 text-sm font-medium text-zinc-400"} "R"]
    [:input {:id "income" :name "income" :type "number" :min "0" :step "any"
             :class "block w-full pl-8 pr-3 py-3 text-lg font-semibold tabular-nums text-zinc-900 placeholder-zinc-400 bg-white border border-zinc-200 rounded-xl transition-colors duration-150 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500"
             :placeholder "0" :value (or value "")}]]])

(defn- tier-field [selected]
  [:div
   [:label {:class "block text-sm font-medium text-zinc-700 mb-1"} "Your rebate tier"]
   [:p {:class "text-xs text-zinc-400 mb-2"} "Age 65+ and 75+ earn extra annual rebates."]
   [:div {:class "grid grid-cols-3 gap-1 rounded-xl bg-zinc-100 p-1"}
    (for [[value label] tiers]
      [:label {:class (str "cursor-pointer rounded-lg px-3 py-2 text-center text-sm font-medium transition-colors active:scale-[0.97] "
                           "focus-within:ring-2 focus-within:ring-emerald-500/50 focus-within:ring-offset-1 "
                           (if (= selected value)
                             "bg-white text-zinc-900 shadow-sm ring-1 ring-zinc-900/5"
                             "text-zinc-500 hover:text-zinc-700"))}
       [:input {:type "radio" :name "rebate-tier" :value value
                :class "sr-only" :checked (= selected value)}]
       label])]])

(defn- form-card [params]
  (let [selected (or (:rebate-tier params)
                     (some-> (:age params) utilities/->int age->tier)
                     "under-65")]
    (tools/panel
     (tools/panel-heading "Calculate your income tax")
     [:div {:class "px-5 pb-6 sm:px-6"}
      (biff/form
       {:hx-post     "/app/tax/income-tax-calculator"
        :hx-target   "#tax-result"
        :hx-swap     "outerHTML"
        :hx-trigger  "submit"}
       [:div {:class "space-y-5"}
        (salary-field (:income params))
        (tier-field selected)]
       [:div {:class "mt-6 flex items-center justify-end"}
        (shared/btn :variant :primary :size :md :class "px-8" :type "submit" "Calculate")])])))

(defn- income-split [take-share tax-share net-income net-tax]
  [:div {:class "mt-4 rounded-xl bg-zinc-50 p-5 ring-1 ring-zinc-200/50"}
   [:p {:class "text-xs font-semibold text-zinc-500 uppercase tracking-wider"} "Where your income goes"]
   [:div {:class "mt-3 flex h-2.5 w-full overflow-hidden rounded-full bg-zinc-200/70"}
    [:div {:class "h-full rounded-l-full bg-emerald-500"
           :style {:width (str take-share "%")}}]
    [:div {:class "h-full rounded-r-full bg-rose-400"
           :style {:width (str tax-share "%")}}]]
   [:div {:class "mt-3 flex items-center justify-between gap-3 text-sm"}
    [:span {:class "flex items-center gap-2 text-zinc-500"}
     [:span {:class "h-2 w-2 rounded-full bg-emerald-500"}] "Take-home"]
    [:span {:class "font-semibold text-zinc-900 tabular-nums"} (utilities/amount->rands net-income)]]
   [:div {:class "mt-1.5 flex items-center justify-between gap-3 text-sm"}
    [:span {:class "flex items-center gap-2 text-zinc-500"}
     [:span {:class "h-2 w-2 rounded-full bg-rose-400"}] "Tax"]
    [:span {:class "font-semibold text-rose-600 tabular-nums"} (utilities/amount->rands net-tax)]]])

(defn- breakdown-panel [income annual-income gross-tax rebates net-tax net-monthly net-income]
  (tools/panel
   (tools/panel-heading "Tax Breakdown")
   (tools/breakdown-section "Income"
     (tools/row "Monthly Gross Income" (utilities/amount->rands income))
     (tools/bold-row "Annual Gross Income" (utilities/amount->rands annual-income)))
   (tools/breakdown-section "Tax"
     (tools/row "Gross Tax" (utilities/amount->rands gross-tax))
     (tools/row "Rebates" (str "(" (utilities/amount->rands rebates) ")"))
     (tools/bold-row "Net Annual Tax" (utilities/amount->rands net-tax)))
   (tools/breakdown-section "Take-home Pay"
     (tools/row "Annual Net Income" (utilities/amount->rands net-income))
     (tools/bold-row "Monthly Net Income" (utilities/amount->rands net-monthly) "text-emerald-600"))))

(defn- result-region [params]
  (let [income        (->n (:income params))
        tier          (or (:rebate-tier params)
                          (some-> (:age params) utilities/->int age->tier)
                          "under-65")
        age           (tier->age tier)
        annual-income (utilities/income->annual-income income)
        {:keys [gross-tax rebates net-tax effective-rate marginal-rate net-income]}
                      (tax/calculate-income-tax annual-income age)
        net-monthly   (utilities/annual-income->monthly-income net-income)
        tax-share     (min 100.0 (max 0.0 (double effective-rate)))
        take-share    (max 0.0 (- 100.0 tax-share))
        status        (if (pos? net-tax)
                        (str "You keep " (utilities/amount->rands net-monthly)
                             " of your " (utilities/amount->rands income)
                             " monthly gross — " (int (Math/round effective-rate)) "c of every rand goes to SARS.")
                        (str "You keep " (utilities/amount->rands net-monthly)
                             " of your " (utilities/amount->rands income)
                             " monthly gross — no income tax this year."))]
    (if (not (pos? income))
      [:div#tax-result
       (tools/panel
        [:div {:class "flex flex-col items-center justify-center px-6 py-16 text-center"}
         [:div {:class "mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-emerald-50"}
          [:span {:class "text-emerald-500"} (svgs/percent-badge)]]
         [:p {:class "text-sm font-medium text-zinc-500"} "Enter your salary to see your take-home pay"]
         [:p {:class "mt-1 max-w-xs text-xs text-zinc-400"} "Enter your salary and click Calculate to see your take-home pay."]])]
      [:div#tax-result {:class "space-y-4"}
       (tools/result-hero
        :eyebrow "Your take-home pay"
        :badge "2026/27"
        :headline (tools/whole->rands net-monthly)
        :suffix "per month"
        :status status
        :body (income-split take-share tax-share net-income net-tax)
        :substats [(tools/hero-substat "Effective rate" (tools/pct effective-rate) "text-rose-600")
                   (tools/hero-substat "Marginal rate" (tools/pct marginal-rate))
                   (tools/hero-substat "Annual take-home" (utilities/amount->rands net-income))])
       (breakdown-panel income annual-income gross-tax rebates net-tax net-monthly net-income)])))

(defn- page-template [ctx params]
  (ui/app
   ctx
   [:div {:class "space-y-7"}
    (headers/pages-heading ["Tax" "Income Tax Calculator"]
                           "See how much of your salary you keep after SARS 2026/27 income tax.")
    [:div {:class "grid grid-cols-1 gap-4 lg:grid-cols-2 lg:items-start"}
     (form-card params)
     (result-region params)]
    (guide)]))

(defn page-get [ctx]
  (page-template ctx {}))

(defn page [{:keys [params] :as ctx}]
  (if (get-in ctx [:headers "hx-request"])
    (result-region params)
    (page-template ctx params)))
