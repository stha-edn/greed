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

(defn table-row [& {:keys [item protected-titles]}]
  (let [{:budget-item/keys [title amount]
         :or {title "Title" amount 0}} item
        protected? (contains? (or protected-titles #{}) title)]
    [:<>
     [:tr {:class "group hover:bg-zinc-50 transition-colors"}
      [:td {:class "px-4 py-3 text-sm text-zinc-700"} title]
      [:td {:class "px-4 py-3 text-sm font-medium text-zinc-900 tabular-nums"} (utilities/amount->rands amount)]
      [:td {:class "px-4 py-3 text-right"}
       (if protected?
         [:span {:class "inline-flex items-center text-[10px] font-medium text-zinc-400 uppercase tracking-wide"
                 :title "Managed automatically — edit in Settings"}
          "Auto"]
         [:button {:class "inline-flex items-center justify-center w-7 h-7 text-zinc-400 hover:text-zinc-600 hover:bg-zinc-100 rounded-md transition-colors opacity-0 group-hover:opacity-100"
                   :type "button"
                   "@click" "isActionModalOpen = true"}
          (svgs/action)])]]
     (when-not protected? (action-modal item))]))

(defn budget-table [{:keys [title items protected-titles]
                     :or {protected-titles #{}}}]
  (let [badge-class (case title
                      "income"   "text-emerald-700 bg-emerald-50"
                      "expenses" "text-zinc-600 bg-zinc-100"
                      "savings"  "text-zinc-600 bg-zinc-100"
                      "text-zinc-600 bg-zinc-100")]
    [:div {:class "flex flex-col bg-white rounded-xl border border-zinc-200/70 shadow-card overflow-hidden transition-shadow duration-200 hover:shadow-card-md"}
     [:div {:class "flex items-center justify-between px-4 py-3 border-b border-zinc-100"}
      [:span {:class (str "text-xs font-semibold uppercase tracking-wide px-2.5 py-1 rounded-full " badge-class)}
       title]
      [:span {:class "text-xs font-medium text-zinc-400 tabular-nums"} (str (count items) (if (= 1 (count items)) " item" " items"))]]
     [:div {:class "flex-1 overflow-x-auto"}
      (if (seq items)
        [:table {:class "w-full"}
         [:thead
          [:tr {:class "border-b border-zinc-100"}
           [:th {:class "px-4 py-2.5 text-left text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"} "Name"]
           [:th {:class "px-4 py-2.5 text-left text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"} "Amount"]
           [:th {:class "px-4 py-2.5 w-12"}]]]
         [:tbody {:class "divide-y divide-zinc-100"}
          (for [item items]
            (table-row :item item :protected-titles protected-titles))]]
        [:div {:class "flex flex-col items-center justify-center px-4 py-12 text-center"}
         [:div {:class "w-10 h-10 rounded-full bg-zinc-50 flex items-center justify-center mb-3"}
          [:svg {:class "w-5 h-5 text-zinc-300" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
           [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M12 4v16m8-8H4"}]]]
         [:p {:class "text-sm font-medium text-zinc-500"} "Nothing here yet"]
         [:p {:class "mt-0.5 text-xs text-zinc-400"} (str "Add your first " title " item")]])]]))
