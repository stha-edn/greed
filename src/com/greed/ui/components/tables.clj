(ns com.greed.ui.components.tables
  (:require [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.ui.components.forms :as forms]
            [com.greed.utilities.core :as utilities]))

(defn- budget-row [& {:keys [item protected-titles]}]
  (let [{:budget-item/keys [title amount]
         :or {title "Title" amount 0}} item
        action-modal-id (str "budget-action-" (:xt/id item))
        protected? (contains? (or protected-titles #{}) title)]
    [:li {:class "group flex items-center gap-3 px-5 py-3.5 transition-colors hover:bg-zinc-50/70"}
     [:div {:class "min-w-0 flex-1"}
      [:p {:class "truncate text-sm font-medium text-zinc-800"} title]
      (when protected?
        [:span {:class "mt-0.5 inline-flex items-center text-xs font-medium text-zinc-500"
                :title "Managed automatically. Edit in Settings"}
         "Auto"])]
     [:p {:class "flex-shrink-0 text-sm font-semibold text-zinc-900 tabular-nums"}
      (utilities/amount->rands amount)]
     (when-not protected?
       [:button {:class "inline-flex flex-shrink-0 items-center justify-center w-8 h-8 -mr-1.5 text-zinc-500 hover:text-zinc-600 hover:bg-zinc-100 rounded-lg transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 md:opacity-0 md:group-hover:opacity-100 focus-visible:opacity-100"
                 :type "button"
                 :aria-label (str "Edit " title)
                 :_ (shared/open-actions action-modal-id)}
        (svgs/action)])]))

(defn- budget-row-modal
  "Per-item action modal, rendered outside the row list — a <div>/<form> is
   not valid content directly inside <tbody>, and browsers foster-parent it
   out while silently dropping the <form> wrapper, breaking the delete/update
   buttons inside it."
  [& {:keys [item protected-titles]}]
  (let [{:budget-item/keys [title]} item
        action-modal-id (str "budget-action-" (:xt/id item))
        protected? (contains? (or protected-titles #{}) title)]
    (when-not protected?
      (shared/modal action-modal-id (forms/budget-action-form item)))))

(defn budget-table [{:keys [title items protected-titles]
                     :or {protected-titles #{}}}]
  (let [label (case title
                "income"   "Income"
                "expenses" "Expenses"
                "savings"  "Savings"
                title)
        count (count items)]
    [:div {:class "flex flex-col rounded-lg bg-white ring-1 ring-zinc-200/70 overflow-hidden transition-shadow duration-200"}
     [:div {:class "flex items-center gap-3 px-5 py-4"}
      [:div {:class "min-w-0 flex-1"}
       [:p {:class "text-sm font-semibold text-zinc-900"} label]
       [:p {:class "text-xs text-zinc-500"} (str count (if (= 1 count) " item" " items"))]]
      (shared/btn :variant :outline :size :sm
                  :attrs {"_" (shared/open-actions (str "budget-add-" title "-modal"))}
                  (svgs/plus {:class "w-3.5 h-3.5"})
                  "Add item")]
     (if (seq items)
       [:ul {:class "divide-y divide-zinc-100 border-t border-zinc-100"}
        (for [item items]
          (budget-row :item item :protected-titles protected-titles))]
       [:div {:class "flex flex-col items-center justify-center px-5 py-10 text-center"}
        [:p {:class "text-sm font-medium text-zinc-500"} "Nothing here yet"]
        [:p {:class "mt-0.5 text-xs text-zinc-500"} (str "Add your first " title " item")]])
     (for [item items]
       (budget-row-modal :item item :protected-titles protected-titles))
     (shared/modal (str "budget-add-" title "-modal")
                   (forms/budget-item-form :type (keyword title)))]))
