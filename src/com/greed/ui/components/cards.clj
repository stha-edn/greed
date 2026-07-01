(ns com.greed.ui.components.cards
  (:require [com.greed.ui.core :as c.ui]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.utilities.core :as utilities]))

(defn testiminial [& {:keys [img title body author]}]
  [:div {:class "bg-white rounded-2xl border border-zinc-200 shadow-card p-8 max-w-md"}
   [:div {:class "flex items-center gap-4 mb-4"}
    [:img {:class "w-12 h-12 rounded-full object-cover border-2 border-zinc-100 flex-shrink-0 bg-zinc-50"
           :alt author :src img}]
    [:div
     [:h3 {:class "font-semibold text-zinc-900"} title]
     [:p {:class "text-sm text-zinc-400"} author]]]
   [:p {:class "text-zinc-600 text-sm leading-relaxed"} body]])

(defn get-card-type [card-type]
  (case card-type
    :visa (svgs/visa)
    :mastercard (svgs/mastercard)
    (svgs/visa)))

(defn bank-card [& {:keys [budget-items finances net-monthly-income]}]
  (let [{:keys [total-income total-expenses]} (c.ui/get-budget-data budget-items)
        {:finances/keys [bank card-type]} finances
        salary-budget-amount (or (some (fn [item]
                                         (when (and (= (:budget-item/type item) :income)
                                                    (= (:budget-item/title item) "Salary"))
                                           (:budget-item/amount item)))
                                       (or budget-items []))
                                 0)
        other-income (- (or total-income 0) salary-budget-amount)
        income       (if net-monthly-income
                       (+ net-monthly-income (max 0 other-income))
                       (or total-income 0))
        balance      (- income total-expenses)
        bank         (or bank :bank)
        card-type    (get-card-type (or card-type :visa))]
    [:div {:class "group relative h-48 w-full max-w-sm rounded-2xl p-6 text-white shadow-card-md ring-1 ring-white/10 bg-gradient-to-br from-zinc-800 via-zinc-900 to-black overflow-hidden transition-all duration-300 hover:shadow-card-hover hover:-translate-y-0.5"}
     [:div {:class "absolute top-0 right-0 w-44 h-44 bg-emerald-500/20 rounded-full blur-2xl -translate-y-1/2 translate-x-1/3 transition-transform duration-500 group-hover:scale-125"}]
     [:div {:class "absolute bottom-0 left-0 w-28 h-28 bg-white/5 rounded-full blur-xl translate-y-1/2 -translate-x-1/2"}]
     [:div {:class "relative flex justify-between items-start"}
      [:div
       [:p {:class "text-xs font-semibold text-zinc-300 uppercase tracking-widest"} (utilities/->string bank)]
       [:p {:class "mt-0.5 text-xs text-zinc-500"} "Debit Card"]]
      [:div {:class "opacity-90"} card-type]]
     [:div {:class "relative absolute bottom-6 left-6 right-6 mt-8"}
      [:div {:class "flex items-center gap-1 mb-4"}
       (for [_ (range 3)]
         [:span {:class "text-zinc-500 text-sm tracking-widest"} "...."])
       [:span {:class "text-sm font-mono text-zinc-300 ml-1"} "4242"]]
      [:div {:class "flex justify-between items-end"}
       [:div
        [:p {:class "text-xs text-zinc-500 uppercase tracking-wider"} "Balance"]
        [:p {:class "mt-0.5 text-2xl font-bold text-white tabular-nums tracking-tight"} (utilities/amount->rands balance)]]]]]))
