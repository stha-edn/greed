(ns com.greed.ui.tools.income-tax-calculator
  (:require [com.biffweb :as biff]
            [com.greed.ui :as ui]
            [com.greed.ui.components.headers :as headers]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.tax :as tax]))

(defn- row [label value]
  [:div {:class "flex justify-between py-2 border-b border-gray-100 text-sm"}
   [:span {:class "text-gray-500"} label]
   [:span {:class "text-gray-800"} value]])

(defn- bold-row [label value]
  [:div {:class "flex justify-between py-2 border-b border-gray-200 text-sm font-semibold"}
   [:span {:class "text-gray-700"} label]
   [:span {:class "text-gray-900"} value]])

(defn- guide []
  [:div {:class "space-y-6"}
   [:div {:class "bg-white rounded-lg shadow p-6"}
    [:h3 {:class "text-base font-semibold text-gray-800 mb-1"} "How this tool works"]
    [:p {:class "text-sm text-gray-500"}
     "Enter your gross monthly salary and age. The calculator applies the SARS 2026/27 tax brackets and rebates to show your effective tax rate and take-home pay."]
    [:p {:class "text-sm text-gray-500 mt-2"}
     "Use this to quickly understand how much of your salary you actually keep, or to compare offers at different salary levels."]]

   [:div {:class "bg-white rounded-lg shadow p-6"}
    [:h3 {:class "text-base font-semibold text-gray-800 mb-3"} "Understanding your results"]
    [:div {:class "space-y-4"}
     [:div
      [:p {:class "text-sm font-medium text-gray-800"} "Gross tax"]
      [:p {:class "text-xs text-gray-500 mt-1"}
       "The raw tax calculated from the SARS brackets before any rebates are applied."]]
     [:div
      [:p {:class "text-sm font-medium text-gray-800"} "Rebates"]
      [:p {:class "text-xs text-gray-500 mt-1"}
       "A fixed annual credit that reduces your tax bill. Everyone under 65 gets the primary rebate (R17,820). Additional rebates apply from age 65 and 75."]]
     [:div
      [:p {:class "text-sm font-medium text-gray-800"} "Effective tax rate"]
      [:p {:class "text-xs text-gray-500 mt-1"}
       "The percentage of your total income that goes to tax after rebates. This is lower than your marginal rate (the rate on your top bracket) because lower portions of income are taxed at lower rates."]]
     [:div
      [:p {:class "text-sm font-medium text-gray-800"} "Net income"]
      [:p {:class "text-xs text-gray-500 mt-1"}
       "Your take-home pay after income tax. Note: UIF and medical aid contributions are not deducted here — this is purely the income tax effect."]]]]

   [:div {:class "bg-blue-50 border border-blue-200 rounded-lg p-4"}
    [:p {:class "text-xs text-blue-800"}
     "This calculator does not account for medical aid credits, retirement annuity deductions, or travel allowances. Use the "
     [:a {:href "/app/tools/tax-returns" :class "font-medium underline"} "Tax Returns simulator"]
     " for a more complete picture."]]])

(defn- field [id label type hint]
  [:div
   [:label {:for id :class "block text-sm font-medium text-gray-700 mb-1"} label]
   [:p {:class "text-xs text-gray-400 mb-1"} hint]
   [:input {:id id :name id :type type :min "0" :step "any"
            :class "block w-full px-4 py-2 text-gray-700 bg-white border border-gray-200 rounded-md focus:border-blue-400 focus:outline-none focus:ring focus:ring-blue-300 focus:ring-opacity-40"
            :required true}]])

(defn page-get [ctx]
  (ui/app
   ctx
   [:div {:class "space-y-4"}
    (headers/pages-heading ["Tools" "Income Tax Calculator"])
    [:div {:class "grid grid-cols-1 lg:grid-cols-2 gap-6 items-start"}
     (guide)
     [:div {:class "bg-white rounded-lg shadow p-6"}
      [:h2 {:class "text-lg font-semibold text-gray-800"} "Calculate your income tax"]
      [:p {:class "mt-1 text-sm text-gray-500 mb-6"}
       "Based on SARS 2026/27 brackets and rebates."]
      (biff/form
       {:action "/app/tools/income-tax-calculator"}
       [:div {:class "space-y-5"}
        (field "income" "Monthly Gross Income (R)" "number" "Your salary before any deductions")
        (field "age" "Age" "number" "Determines which rebate tier applies")]
       [:div {:class "mt-6 flex justify-end"}
        [:button {:type "submit"
                  :class "px-8 py-2.5 text-white bg-zinc-900 rounded-md hover:bg-gray-700 focus:outline-none"}
         "Calculate"]])]]]))

(defn page [{:keys [params] :as ctx}]
  (let [->n           #(try (double (BigDecimal. (or % "0")))
                         (catch Exception _ 0.0))
        income        (->n (:income params))
        age           (or (utilities/->int (:age params)) 0)
        annual-income (utilities/income->annual-income income)
        {:keys [gross-tax rebates net-tax
                effective-rate]} (tax/calculate-income-tax annual-income age)
        net-monthly   (utilities/annual-income->monthly-income (- annual-income net-tax))]
    (ui/app
     ctx
     [:div {:class "space-y-4"}
      (headers/pages-heading ["Tools" "Income Tax Calculator"])
      [:div {:class "grid grid-cols-1 lg:grid-cols-2 gap-6 items-start"}
       (guide)
       [:div {:class "bg-white rounded-lg shadow p-6"}
        [:h2 {:class "text-lg font-semibold text-gray-800 mb-4"} "Tax Breakdown"]

        [:p {:class "text-xs font-semibold uppercase tracking-wide text-gray-400 mb-1"} "Income"]
        (row "Monthly Gross Income" (utilities/amount->rands income))
        (bold-row "Annual Gross Income" (utilities/amount->rands annual-income))

        [:p {:class "text-xs font-semibold uppercase tracking-wide text-gray-400 mt-4 mb-1"} "Tax"]
        (row "Gross Tax" (utilities/amount->rands gross-tax))
        (row "Rebates" (str "(" (utilities/amount->rands rebates) ")"))
        (bold-row "Net Annual Tax" (utilities/amount->rands net-tax))

        [:div {:class "flex justify-between items-center py-3 mt-4 px-4 bg-gray-50 rounded-lg"}
         [:span {:class "text-sm text-gray-600"} "Effective Tax Rate"]
         [:span {:class "text-lg font-bold text-gray-900"} (utilities/->percentage effective-rate)]]

        [:p {:class "text-xs font-semibold uppercase tracking-wide text-gray-400 mt-4 mb-1"} "Take-home Pay"]
        (row "Annual Net Income" (utilities/amount->rands (- annual-income net-tax)))
        (bold-row "Monthly Net Income" (utilities/amount->rands net-monthly))

        [:div {:class "mt-6"}
         [:a {:href "/app/tools/income-tax-calculator"
              :class "text-sm text-emerald-600 hover:underline"}
          "<- Calculate again"]]]]])))
