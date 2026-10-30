(ns com.greed.ui.components.cards
  (:require [com.greed.data.core :as c.data]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.utilities.core :as utilities]))

(defn bank-card [& {:keys [budget-items finances net-monthly-income]}]
  (let [{:keys [total-income total-expenses]} (c.data/get-budget-data budget-items)
        {:finances/keys [bank user-id account-type]} finances
        last-four (utilities/mock-last-four (or user-id bank))
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
        balance      (- income total-expenses)]
    [:div {:class "relative flex w-full max-w-sm aspect-[1.586/1] flex-col overflow-hidden rounded-lg bg-white p-6 ring-1 ring-zinc-200 lg:bg-zinc-900 lg:ring-white/10"}
     [:div {:class "relative"}
      [:div {:class "flex items-start justify-between"}
       [:span {:class "text-zinc-500 lg:rounded-[5px] lg:bg-zinc-800 lg:p-1 lg:text-zinc-300 lg:ring-1 lg:ring-white/10"} (svgs/card-chip)]
       [:span {:class "text-zinc-500 lg:text-zinc-300"} (svgs/contactless)]]
      [:div {:class "mt-4"}
       (if bank
         [:<>
          [:p {:class "text-xs font-medium text-zinc-500 lg:text-zinc-300"} (utilities/->string bank)]
          [:p {:class "mt-0.5 text-xs text-zinc-500 lg:text-zinc-500"} (or account-type "Debit Card")]]
         [:a {:href "/app/settings"
              :class "inline-flex items-center gap-1 rounded-sm text-xs font-medium text-zinc-900 transition hover:text-zinc-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-zinc-400 active:scale-[0.97] lg:text-zinc-400 lg:hover:text-zinc-200"}
          "Add your bank"
          (svgs/->next {:class "size-3"})])]]
     [:div {:class "relative mt-auto"}
      [:div {:class "flex items-center gap-2"}
       (for [_ (range 3)]
         [:span {:class "text-sm text-zinc-300 lg:text-zinc-500"} "••••"])
       [:span {:class "text-sm font-mono text-zinc-500 lg:text-zinc-300"} last-four]
       ;; Brand mark in the card-network spot: green on the light card, white on the dark (lg) card.
       [:img {:src "/img/greed-symbol-green-tight.svg" :alt "" :aria-hidden "true"
              :width 192 :height 192 :class "ml-auto h-6 w-6 lg:hidden"}]
       [:img {:src "/img/greed-symbol-white-tight.svg" :alt "" :aria-hidden "true"
              :width 192 :height 192 :class "ml-auto hidden h-6 w-6 lg:block"}]]
      [:div {:class "mt-4 flex items-baseline justify-between gap-4"}
       [:p {:class "text-xs text-zinc-500 lg:text-zinc-500"} "Balance"]
       [:p {:class (str "text-2xl font-bold tracking-tight tabular-nums lg:text-3xl "
                        (if (neg? balance) "text-rose-600 lg:text-rose-400" "text-zinc-900 lg:text-white"))}
        (utilities/amount->rands balance)]]]]))
