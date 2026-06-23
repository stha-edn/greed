(ns com.greed.ui.tools.bonus-tax-calculator
  (:require [com.biffweb :as biff]
            [com.greed.ui :as ui]
            [com.greed.ui.components.headers :as headers]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.tax :as tax]))

(defn- row [label value]
  [:div {:class "flex justify-between py-2 border-b border-zinc-100 text-sm"}
   [:span {:class "text-zinc-500"} label]
   [:span {:class "text-zinc-800"} value]])

(defn- bold-row [label value]
  [:div {:class "flex justify-between py-2 border-b border-zinc-200 text-sm font-semibold"}
   [:span {:class "text-zinc-700"} label]
   [:span {:class "text-zinc-900"} value]])

(defn- guide []
  [:div {:class "space-y-6"}
   [:div {:class "bg-white rounded-lg shadow p-6"}
    [:h3 {:class "text-base font-semibold text-zinc-800 mb-1"} "How this tool works"]
    [:p {:class "text-sm text-zinc-500"}
     "A bonus (or 13th cheque) is taxed at your marginal rate — the rate on your top slice of income. This calculator works out the tax by comparing your annual tax with and without the bonus; the difference is the PAYE withheld from the bonus."]
    [:p {:class "text-sm text-zinc-500 mt-2"}
     "Enter your regular monthly salary, the once-off bonus amount, and your age. The result shows the tax on the bonus and what you'll actually take home."]]

   [:div {:class "bg-white rounded-lg shadow p-6"}
    [:h3 {:class "text-base font-semibold text-zinc-800 mb-3"} "Understanding your results"]
    [:div {:class "space-y-4"}
     [:div
      [:p {:class "text-sm font-medium text-zinc-800"} "Tax on bonus"]
      [:p {:class "text-xs text-zinc-500 mt-1"}
       "The additional PAYE created by adding the bonus to your annual income. Because the bonus sits on top of your salary, it is taxed at your highest (marginal) rate."]]
     [:div
      [:p {:class "text-sm font-medium text-zinc-800"} "Net bonus"]
      [:p {:class "text-xs text-zinc-500 mt-1"}
       "What lands in your account after PAYE. UIF and other deductions are not included here."]]
     [:div
      [:p {:class "text-sm font-medium text-zinc-800"} "Effective bonus rate"]
      [:p {:class "text-xs text-zinc-500 mt-1"}
       "The percentage of the bonus that goes to tax. This is typically higher than the effective rate on your salary because the whole bonus is taxed at the margin."]]]]

   [:div {:class "bg-blue-50 border border-blue-200 rounded-lg p-4"}
    [:p {:class "text-xs text-blue-800"}
     "This is an estimate based on SARS 2026/27 brackets and the primary/age rebates. It does not account for medical aid credits or retirement contributions. Use the "
     [:a {:href "/app/tax/tax-returns" :class "font-medium underline"} "Tax Returns simulator"]
     " for a fuller picture."]]])

(defn- field [id label hint]
  [:div
   [:label {:for id :class "block text-sm font-medium text-zinc-700 mb-1"} label]
   [:p {:class "text-xs text-zinc-400 mb-1"} hint]
   [:input {:id id :name id :type "number" :min "0" :step "any"
            :class "block w-full px-4 py-2 text-zinc-700 bg-white border border-zinc-200 rounded-md focus:border-blue-400 focus:outline-none focus:ring focus:ring-blue-300 focus:ring-opacity-40"
            :required true :placeholder "0"}]])

(defn page-get [ctx]
  (ui/app
   ctx
   [:div {:class "space-y-4"}
    (headers/pages-heading ["Tax" "Bonus Tax Calculator"])
    [:div {:class "grid grid-cols-1 lg:grid-cols-2 gap-6 items-start"}
     (guide)
     [:div {:class "bg-white rounded-lg shadow p-6"}
      [:h2 {:class "text-lg font-semibold text-zinc-800"} "Calculate your bonus tax"]
      [:p {:class "mt-1 text-sm text-zinc-500 mb-6"}
       "Based on SARS 2026/27 brackets and rebates."]
      (biff/form
       {:action "/app/tax/bonus-tax-calculator"}
       [:div {:class "space-y-5"}
        (field "income" "Monthly Gross Salary (R)" "Your regular salary before the bonus")
        (field "bonus" "Bonus Amount (R)" "The once-off bonus or 13th cheque")
        (field "age" "Age" "Determines which rebate tier applies")]
       [:div {:class "mt-6 flex justify-end"}
        [:button {:type "submit"
                  :class "px-8 py-2.5 text-white bg-zinc-900 rounded-md hover:bg-zinc-700 focus:outline-none"}
         "Calculate"]])]]]))

(defn page [{:keys [params] :as ctx}]
  (let [->n            #(try (double (BigDecimal. (or % "0")))
                          (catch Exception _ 0.0))
        income         (->n (:income params))
        bonus          (->n (:bonus params))
        age            (or (utilities/->int (:age params)) 0)
        annual-salary  (utilities/income->annual-income income)
        tax-without    (:net-tax (tax/calculate-income-tax annual-salary age))
        tax-with       (:net-tax (tax/calculate-income-tax (+ annual-salary bonus) age))
        bonus-tax      (max 0.0 (- tax-with tax-without))
        net-bonus      (- bonus bonus-tax)
        eff-rate       (if (pos? bonus) (* 100.0 (/ bonus-tax bonus)) 0.0)]
    (ui/app
     ctx
     [:div {:class "space-y-4"}
      (headers/pages-heading ["Tax" "Bonus Tax Calculator"])
      [:div {:class "grid grid-cols-1 lg:grid-cols-2 gap-6 items-start"}
       (guide)
       [:div {:class "bg-white rounded-lg shadow p-6"}
        [:h2 {:class "text-lg font-semibold text-zinc-800 mb-4"} "Bonus Breakdown"]

        [:p {:class "text-xs font-semibold uppercase tracking-wide text-zinc-400 mb-1"} "Inputs"]
        (row "Monthly Gross Salary" (utilities/amount->rands income))
        (row "Annual Salary" (utilities/amount->rands annual-salary))
        (bold-row "Bonus Amount" (utilities/amount->rands bonus))

        [:p {:class "text-xs font-semibold uppercase tracking-wide text-zinc-400 mt-4 mb-1"} "Tax on Bonus"]
        (row "Annual tax without bonus" (utilities/amount->rands tax-without))
        (row "Annual tax with bonus" (utilities/amount->rands tax-with))
        (bold-row "Tax on Bonus" (utilities/amount->rands bonus-tax))

        [:div {:class "flex justify-between items-center py-3 mt-4 px-4 bg-zinc-50 rounded-lg"}
         [:span {:class "text-sm text-zinc-600"} "Effective Bonus Tax Rate"]
         [:span {:class "text-lg font-bold text-zinc-900"} (utilities/->percentage eff-rate)]]

        [:div {:class "flex justify-between items-center py-3 mt-3 px-4 bg-emerald-50 rounded-lg"}
         [:span {:class "text-sm font-medium text-emerald-700"} "Net Bonus (take-home)"]
         [:span {:class "text-lg font-bold text-emerald-700"} (utilities/amount->rands net-bonus)]]

        [:div {:class "mt-6"}
         [:a {:href "/app/tax/bonus-tax-calculator"
              :class "text-sm text-emerald-600 hover:underline"}
          "<- Calculate again"]]]]])))
