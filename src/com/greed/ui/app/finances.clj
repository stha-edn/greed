(ns com.greed.ui.app.finances
  (:require [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.stats :as stats]
            [com.greed.ui.components.tables :as tables]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.headers :as headers]
            [com.greed.utilities.core :as utilities]))

(defn budget-lists [& {:keys [budget-items]}]
  [:div {:class "grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3"}
   (tables/budget-table
    {:title "income"
     :items (filterv #(= (:budget-item/type %) :income) budget-items)
     :protected-titles #{"Salary"}})
   (tables/budget-table
    {:title "expenses"
     :items (filterv #(= (:budget-item/type %) :expenses) budget-items)
     :protected-titles #{"Medical Aid" "Annual RA Contributions"}})
   (tables/budget-table
    {:title "savings"
     :items (filterv #(= (:budget-item/type %) :savings) budget-items)})])

(defn- business-lists [budget-items vat?]
  (let [of-type (fn [t] (filterv #(= (:budget-item/type %) t) budget-items))]
    [:div {:class "grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3"}
     (tables/budget-table {:title "income" :items (of-type :income) :business? true :vat? vat?})
     (tables/budget-table {:title "expenses" :items (of-type :expenses) :business? true :vat? vat?})
     (tables/budget-table {:title "savings" :label "Set aside" :items (of-type :savings) :business? true})]))

(defn- business-hero
  "A business's month at a glance: income, what goes out, what is set aside,
   and what is left."
  [budget-items]
  (let [{:keys [total-income total-expenses total-savings]} (data/get-budget-data budget-items)
        left (- total-income total-expenses total-savings)
        stat (fn [label amount & [cls]]
               [:div {:class "min-w-0"}
                [:p {:class "text-sm text-zinc-500"} label]
                [:p {:class (str "mt-1 text-lg font-semibold tabular-nums " (or cls "text-zinc-900"))}
                 (utilities/amount->rands amount)]])]
    [:section {:class "rounded-lg border border-zinc-200 bg-white p-6 sm:p-8" :aria-label "This month's plan"}
     [:p {:class "text-sm text-zinc-500"} (if (neg? left) "Planned beyond its income" "Left after costs and set-aside")]
     [:p {:class (str "mt-2 text-4xl font-semibold tracking-[-0.03em] tabular-nums "
                      (if (neg? left) "text-rose-700" "text-zinc-900"))}
      (utilities/amount->rands left)]
     [:div {:class "mt-6 grid grid-cols-2 gap-6 border-t border-zinc-100 pt-5 sm:grid-cols-3"}
      (stat "Income" total-income)
      (stat "Expenses" total-expenses)
      (stat "Set aside" total-savings)]]))

(defn- business-page [{:keys [params] :as ctx} {:business/keys [name] :as business}]
  (let [budget-items (data/get-business-budget-items ctx (:xt/id business))]
    (ui/app
     ctx
     [:div {:class "space-y-6"}
      (when (:alert params) (alerts/info params))
      (headers/pages-heading ["Budget"] (str name "'s monthly plan. Set aside is money put away for SARS, VAT, reserves and equipment."))
      (business-hero budget-items)
      (business-lists budget-items (:business/vat-registered business))])))

(defn- personal-page [{:keys [session params] :as ctx}]
  (let [user-id      (:uid session)
        budget-items (data/get-budget-items ctx user-id)
        finances     (data/get-finances ctx user-id)
        payday       (:finances/payday finances)]
    (ui/app
     ctx
     [:div {:class "space-y-6"}
      (when (:alert params) (alerts/info params))
      (headers/pages-heading ["Finances"] "Budget")
      (stats/finance-hero budget-items payday)
      (budget-lists :budget-items budget-items)])))

(defn page
  "GET /app/finances/: the chosen business's budget, or the personal one."
  [ctx]
  (if-let [business (data/session-business ctx)]
    (business-page ctx business)
    (personal-page ctx)))
