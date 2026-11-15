(ns com.greed.ui.components.tables
  (:require [clojure.string :as str]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.statements.categorise :as categorise]
            [com.greed.utilities.vat :as vat]
            [com.greed.ui.components.forms :as forms]
            [com.greed.utilities.core :as utilities]))

(defn- source-tag
  "Where an item came from: added from a bank statement (that month's
   amount) or planned monthly."
  [{:budget-item/keys [from-statement]}]
  (if from-statement
    [:span {:class "flex-shrink-0 rounded-full bg-brand-50 px-2 py-0.5 text-[11px] font-medium text-brand-700 ring-1 ring-brand-600/20"
            :title "Added from a bank statement, at that month's amount"}
     (str "From " (.format (java.time.YearMonth/parse from-statement)
                           (java.time.format.DateTimeFormatter/ofPattern "MMM yyyy"))
          " statement")]
    [:span {:class "flex-shrink-0 rounded-full bg-zinc-100 px-2 py-0.5 text-[11px] font-medium text-zinc-600"
            :title "A planned monthly amount"}
     "Monthly"]))

(defn- budget-row [& {:keys [item protected-titles vat? owners]}]
  (let [{:budget-item/keys [title amount]
         :or {title "Title" amount 0}} item
        action-modal-id (str "budget-action-" (:xt/id item))
        ;; kept in step with Settings, or with what a business pays you
        protected? (or (contains? (or protected-titles #{}) title)
                       (some? (:budget-item/source-business-id item)))
        paid-to    (some #(when (= (:id %) (:budget-item/paid-to item)) (:name %)) owners)
        editable?  (or (not protected?) (not= :income (:budget-item/type item)))
        reference  (:budget-item/reference item)
        category   (some-> (:budget-item/category item) categorise/categories)
        vat-label  (when (and vat? (#{:income :expenses} (:budget-item/type item)))
                     (vat/vat-types (or (:budget-item/vat item) (vat/default-vat-type (:budget-item/type item)))))]
    [:li {:class "group flex items-center gap-3 px-5 py-3.5 transition-colors hover:bg-zinc-50/70"}
     [:div {:class "min-w-0 flex-1"}
      [:p {:class "truncate text-sm font-medium text-zinc-800"} title]
      ;; the tag leads the second line, so long titles keep their width
      [:div {:class "mt-1 flex min-w-0 items-center gap-2"}
       (source-tag item)
       (when (or protected? reference category vat-label paid-to)
         [:p {:class "min-w-0 truncate text-xs text-zinc-500"}
          (str/join " · " (remove nil? [(when protected? "Auto")
                                        vat-label
                                        category
                                        (when paid-to (str "Paid to " paid-to))
                                        (when reference (str "Matches: " reference))]))])]]
     [:p {:class "flex-shrink-0 text-sm font-semibold text-zinc-900 tabular-nums"}
      (utilities/amount->rands amount)]
     (when editable?
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
  [& {:keys [item protected-titles vat? owners]}]
  (let [{:budget-item/keys [title]} item
        action-modal-id (str "budget-action-" (:xt/id item))
        protected? (or (contains? (or protected-titles #{}) title)
                       (some? (:budget-item/source-business-id item)))]
    (when (or (not protected?) (not= :income (:budget-item/type item)))
      (shared/modal action-modal-id (forms/budget-action-form item :auto? protected? :vat? vat? :owners owners)))))

(defn budget-table
  "One list of the budget. :label overrides the heading (a business's
   savings list is \"Set aside\"); :business? gives its Add form the
   business fields."
  [{:keys [title items protected-titles label business? vat? owners]
    :or {protected-titles #{}}}]
  (let [label (or label
                  (case title
                    "income"   "Income"
                    "expenses" "Expenses"
                    "savings"  "Savings"
                    title))
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
          (budget-row :item item :protected-titles protected-titles :vat? vat? :owners owners))]
       [:div {:class "flex flex-col items-center justify-center px-5 py-10 text-center"}
        [:p {:class "text-sm font-medium text-zinc-500"} "Nothing here yet"]
        [:p {:class "mt-0.5 text-xs text-zinc-500"} (str "Add your first " (str/lower-case label) " item")]])
     (for [item items]
       (budget-row-modal :item item :protected-titles protected-titles :vat? vat? :owners owners))
     (shared/modal (str "budget-add-" title "-modal")
                   (forms/budget-item-form :type (keyword title) :business? business? :vat? vat? :owners owners))]))
