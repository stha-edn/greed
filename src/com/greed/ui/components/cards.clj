(ns com.greed.ui.components.cards
  (:require [com.greed.ui.core :as c.ui]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.utilities.core :as utilities]))

(defn testiminial [& {:keys [img title body author]}]
  [:div {:class "bg-white rounded-2xl border border-gray-200 shadow-card p-8 max-w-md"}
   [:div {:class "flex items-center gap-4 mb-4"}
    [:img {:class "w-12 h-12 rounded-full object-cover border-2 border-gray-100"
           :alt "Avatar" :src img}]
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
    [:div {:class "relative h-48 w-full max-w-sm rounded-2xl p-6 text-white shadow-card-md bg-zinc-900 overflow-hidden"}
     [:div {:class "absolute top-0 right-0 w-40 h-40 bg-emerald-500/10 rounded-full -translate-y-1/2 translate-x-1/2"}]
     [:div {:class "absolute bottom-0 left-0 w-24 h-24 bg-white/5 rounded-full translate-y-1/2 -translate-x-1/2"}]
     [:div {:class "relative flex justify-between items-start"}
      [:div
       [:p {:class "text-xs font-medium text-zinc-400 uppercase tracking-widest"} (utilities/->string bank)]
       [:p {:class "mt-0.5 text-xs text-zinc-500"} "Debit Card"]]
      [:div {:class "opacity-80"} card-type]]
     [:div {:class "relative absolute bottom-6 left-6 right-6 mt-8"}
      [:div {:class "flex items-center gap-1 mb-4"}
       (for [_ (range 3)]
         [:span {:class "text-zinc-500 text-sm tracking-widest"} "...."])
       [:span {:class "text-sm font-mono text-zinc-300 ml-1"} "4242"]]
      [:div {:class "flex justify-between items-end"}
       [:div
        [:p {:class "text-xs text-zinc-500 uppercase tracking-wide"} "Balance"]
        [:p {:class "text-xl font-bold text-white"} (utilities/amount->rands balance)]]]]]))
