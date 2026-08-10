(ns com.greed.ui.components.tables
  (:require [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.ui.components.forms :as forms]
            [com.greed.utilities.core :as utilities]))

(defn- th [label & {:keys [class]}]
  [:th {:class (str "px-4 py-2.5 text-left text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"
                    (when class (str " " class)))}
   label])

(defn add-button []
  [:div {:class "flex justify-end mb-4"}
   [:button {:class "inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2"
            :type "button"
            :_ (shared/open-actions "budget-add-modal")}
    (svgs/plus {:class "w-4 h-4"})
    "Add item"]
   (shared/modal "budget-add-modal" (forms/budget-item-form))])

(defn table-row [& {:keys [item protected-titles]}]
  (let [{:budget-item/keys [title amount]
         :or {title "Title" amount 0}} item
        action-modal-id (str "budget-action-" (:xt/id item))
        protected? (contains? (or protected-titles #{}) title)]
    [:tr {:class "group hover:bg-zinc-50 transition-colors"}
     [:td {:class "px-4 py-3 text-sm text-zinc-700"} title]
     [:td {:class "px-4 py-3 text-sm font-medium text-zinc-900 tabular-nums"} (utilities/amount->rands amount)]
     [:td {:class "px-4 py-3 text-right"}
      (if protected?
        [:span {:class "inline-flex items-center text-[10px] font-medium text-zinc-400 uppercase tracking-wide"
                :title "Managed automatically — edit in Settings"}
         "Auto"]
        [:button {:class "inline-flex items-center justify-center w-7 h-7 text-zinc-400 hover:text-zinc-600 hover:bg-zinc-100 rounded-md transition-colors md:opacity-0 md:group-hover:opacity-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 focus-visible:opacity-100"
                  :type "button"
                  :_ (shared/open-actions action-modal-id)}
         (svgs/action)])]]))

(defn row-modal
  "Per-item action modal, rendered outside the <table> — a <div>/<form> is not
   valid content directly inside <tbody>, and browsers foster-parent it out
   while silently dropping the <form> wrapper, breaking the delete/update
   buttons inside it."
  [& {:keys [item protected-titles]}]
  (let [{:budget-item/keys [title]} item
        action-modal-id (str "budget-action-" (:xt/id item))
        protected? (contains? (or protected-titles #{}) title)]
    (when-not protected?
      (shared/modal action-modal-id (forms/budget-action-form item)))))

(defn budget-table [{:keys [title items protected-titles]
                     :or {protected-titles #{}}}]
  (let [badge-class (case title
                      "income"   "text-emerald-700 bg-emerald-50"
                      "expenses" "text-rose-700 bg-rose-50"
                      "savings"  "text-indigo-700 bg-indigo-50"
                      "text-zinc-600 bg-zinc-100")]
     [:div {:class "flex flex-col bg-white rounded-xl border border-zinc-200/70 shadow-card overflow-hidden transition-shadow duration-200 hover:shadow-card-hover"}
     [:div {:class "flex items-center justify-between px-4 py-3 border-b border-zinc-100"}
      [:span {:class (str "text-xs font-semibold uppercase tracking-wide px-2.5 py-1 rounded-full " badge-class)}
       title]
      [:span {:class "text-xs font-medium text-zinc-400 tabular-nums"} (str (count items) (if (= 1 (count items)) " item" " items"))]]
     [:div {:class "flex-1 overflow-x-auto"}
      (if (seq items)
        [:table {:class "w-full"}
         [:thead
          [:tr {:class "border-b border-zinc-100"}
           (th "Name")
           (th "Amount")
           [:th {:class "px-4 py-2.5 w-12"}]]]
         [:tbody {:class "divide-y divide-zinc-100"}
          (for [item items]
            (table-row :item item :protected-titles protected-titles))]]
        [:div {:class "flex flex-col items-center justify-center px-4 py-12 text-center"}
         [:div {:class "w-10 h-10 rounded-full bg-zinc-50 flex items-center justify-center mb-3"}
          (svgs/plus {:class "w-5 h-5 text-zinc-300"})]
         [:p {:class "text-sm font-medium text-zinc-500"} "Nothing here yet"]
         [:p {:class "mt-0.5 text-xs text-zinc-400"} (str "Add your first " title " item")]])]
     (for [item items]
       (row-modal :item item :protected-titles protected-titles))]))
