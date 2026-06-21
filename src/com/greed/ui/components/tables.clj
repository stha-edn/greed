(ns com.greed.ui.components.tables
  (:require [com.greed.ui.components.svgs :as svgs]
            [com.greed.ui.components.forms :as forms]
            [com.greed.utilities.core :as utilities]))

(defn add-modal []
  [:div {:x-show "isAddButtonOpen" :x-cloak "true"
         :class "fixed inset-0 z-50 flex items-center justify-center p-4"
         :x-transition:enter "transition ease-out duration-200"
         :x-transition:enter-start "opacity-0 scale-95"
         :x-transition:enter-end "opacity-100 scale-100"
         :x-transition:leave "transition ease-in duration-150"
         :x-transition:leave-start "opacity-100 scale-100"
         :x-transition:leave-end "opacity-0 scale-95"}
   [:div {:class "absolute inset-0 bg-black/50"
          "@click" "isAddButtonOpen = false"}]
   [:div {:class "relative z-10"}
    (forms/budget-item-form)]])

(defn action-modal [item]
  [:div {:x-show "isActionModalOpen" :x-cloak "true"
         :class "fixed inset-0 z-50 flex items-center justify-center p-4"
         :x-transition:enter "transition ease-out duration-200"
         :x-transition:enter-start "opacity-0 scale-95"
         :x-transition:enter-end "opacity-100 scale-100"
         :x-transition:leave "transition ease-in duration-150"
         :x-transition:leave-start "opacity-100 scale-100"
         :x-transition:leave-end "opacity-0 scale-95"}
   [:div {:class "absolute inset-0 bg-black/50"
          "@click" "isActionModalOpen = false"}]
   [:div {:class "relative z-10"}
    (forms/budget-action-form item)]])

(defn add-button []
  [:div {:class "flex justify-end mb-4"}
   [:button {:class "inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors"
             :type "button"
             "@click" "isAddButtonOpen = true"}
    [:svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
     [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M12 4v16m8-8H4"}]]
    "Add item"]
   (add-modal)])

(defn table-row [& {:keys [item salary-budget-item]}]
  (let [{:budget-item/keys [title amount]
         :or {title "Title" amount 0}} item
        salary-item? (= title (:budget-item/title salary-budget-item))]
    [:<>
     [:tr {:class "group hover:bg-gray-50 transition-colors"}
      [:td {:class "px-4 py-3 text-sm text-zinc-700"} title]
      [:td {:class "px-4 py-3 text-sm font-medium text-zinc-900"} (utilities/amount->rands amount)]
      [:td {:class "px-4 py-3 text-right"}
       (when-not salary-item?
         [:button {:class "inline-flex items-center justify-center w-7 h-7 text-zinc-400 hover:text-zinc-600 hover:bg-gray-100 rounded-md transition-colors opacity-0 group-hover:opacity-100"
                   :type "button"
                   "@click" "isActionModalOpen = true"}
          (svgs/action)])]]
     (action-modal item)]))

(defn budget-table [{:keys [title items salary-budget-item]
                     :or {salary-budget-item nil}}]
  (let [badge-class (case title
                      "income"   "text-emerald-700 bg-emerald-50"
                      "expenses" "text-zinc-600 bg-gray-100"
                      "savings"  "text-zinc-600 bg-gray-100"
                      "text-zinc-600 bg-gray-100")]
    [:div {:class "bg-white rounded-xl border border-gray-100 shadow-card overflow-hidden"}
     [:div {:class "flex items-center justify-between px-4 py-3 border-b border-gray-100"}
      [:span {:class (str "text-xs font-semibold uppercase tracking-wide px-2.5 py-1 rounded-full " badge-class)}
       title]
      [:span {:class "text-xs text-zinc-400"} (str (count items) " items")]]
     [:div {:class "overflow-x-auto"}
      [:table {:class "w-full"}
       [:thead
        [:tr {:class "border-b border-gray-50"}
         [:th {:class "px-4 py-2.5 text-left text-xs font-medium text-zinc-400 uppercase tracking-wide"} "Name"]
         [:th {:class "px-4 py-2.5 text-left text-xs font-medium text-zinc-400 uppercase tracking-wide"} "Amount"]
         [:th {:class "px-4 py-2.5 w-12"}]]]
       [:tbody {:class "divide-y divide-gray-50"}
        (if (seq items)
          (for [item items]
            (table-row :item item :salary-budget-item salary-budget-item))
          [:tr
           [:td {:colspan "3" :class "px-4 py-8 text-center text-sm text-zinc-400"}
            "No items yet"]])]]]]))
