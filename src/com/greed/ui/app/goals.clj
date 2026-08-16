(ns com.greed.ui.app.goals
  (:require [com.biffweb :as biff]
            [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.stats :as stats]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.headers :as headers]
            [com.greed.utilities.core :as utilities])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]))

(defn- format-target-date [date-str]
  (when date-str
    (try
      (.format (LocalDate/parse date-str)
               (DateTimeFormatter/ofPattern "d MMM yyyy"))
      (catch Exception _ date-str))))

(defn- field [& {:keys [id label type hint value required?]}]
  [:div
   (shared/form-label id label)
   (when hint [:p {:class "mb-1 text-xs text-zinc-400"} hint])
   [:input (cond-> {:id id :name id :type type
                    :class (shared/base-input-class)
                    :required (boolean required?)}
             (= type "number") (assoc :min "0" :step "any")
             value (assoc :value (str value)))]])

(defn- goal-form
  "Create form when goal is nil, edit form otherwise."
  [& {:keys [goal modal-id]}]
  (let [{:goal/keys [title target saved target-date] :xt/keys [id]} goal
        editing? (some? goal)]
    [:div {:class "w-full max-w-md p-6 bg-white rounded-2xl shadow-card-md"}
     [:div {:class "flex items-center justify-between mb-4"}
      [:h3 {:class "text-base font-semibold text-zinc-900"}
       (if editing? "Edit goal" "New savings goal")]
      [:button {:type "button" :title "Close" :aria-label "Close"
                :class "flex items-center justify-center w-7 h-7 text-zinc-400 hover:text-zinc-600 hover:bg-zinc-100 rounded-md transition-all active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 active:text-zinc-700 active:bg-zinc-200"
                :_ (shared/close-actions modal-id)}
       (svgs/close {:class "w-4 h-4"})]]
     (biff/form
      {:action (if editing? "/app/goals/update-goal" "/app/goals/create-goal")}
      (when editing?
        [:input {:type "hidden" :name "goal-id" :value (str id)}])
      [:div {:class "space-y-4"}
       (field :id "title" :label "Goal" :type "text"
              :hint "e.g. Emergency fund, New laptop" :value title :required? true)
       (field :id "target" :label "Target amount (R)" :type "number"
              :value target :required? true)
       (field :id "saved" :label "Saved so far (R)" :type "number"
              :hint "How much you've put aside already" :value (or saved 0))
       (field :id "target-date" :label "Target date (optional)" :type "date"
              :value target-date)]
      [:div {:class "flex justify-end gap-3 mt-6"}
       [:button {:type "button"
                 :class "px-4 py-2 text-sm font-medium text-zinc-600 hover:text-zinc-900 transition-colors active:text-zinc-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2"
                 :_ (shared/close-actions modal-id)}
        "Cancel"]
       (shared/btn :variant :primary :size :md :type "submit" :class "px-5"
                   (if editing? "Save changes" "Create goal"))])]))

(defn- goal-card [goal]
  (let [{:goal/keys [title target saved target-date] :xt/keys [id]} goal
        modal-id  (str "goal-edit-" id)
        saved     (or saved 0)
        target    (or target 0)
        p         (utilities/goal-pct saved target)
        remaining (max 0 (- target saved))
        complete? (>= saved target)]
    [:div {:class "p-5 bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card transition-all duration-200 hover:shadow-card-hover"}
     [:div {:class "flex items-start justify-between gap-3"}
      [:div {:class "min-w-0"}
       [:h3 {:class "text-sm font-semibold text-zinc-900 truncate"} title]
       (when target-date
         [:p {:class "mt-0.5 text-xs text-zinc-400"} (str "By " (format-target-date target-date))])]
      [:div {:class "flex flex-shrink-0 items-center gap-1"}
       [:button {:type "button" :title "Edit"
                 :class "p-1.5 text-zinc-400 rounded-md transition-all hover:text-zinc-700 hover:bg-zinc-100 active:scale-95 active:text-zinc-700 active:bg-zinc-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2"
                 :_ (shared/open-actions modal-id)}
        [:svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                 :d "M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"}]]]
       (biff/form {:action "/app/goals/delete-goal" :class "flex"}
                  [:input {:type "hidden" :name "goal-id" :value (str id)}]
                  [:button {:type "submit" :title "Delete"
                            :data-confirm "Delete this goal?"
                            :class "p-1.5 text-zinc-400 rounded-md transition-all hover:text-rose-500 hover:bg-rose-50 active:scale-95 active:text-rose-600 active:bg-rose-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-400 focus-visible:ring-offset-2"}
                   [:svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                    [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                            :d "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"}]]])]]
     [:div {:class "mt-4"}
      [:div {:class "flex items-end justify-between mb-1.5"}
       [:span {:class "text-lg font-semibold text-zinc-900 tabular-nums"} (utilities/amount->rands saved)]
       [:span {:class "text-xs text-zinc-400 tabular-nums"} (str "of " (utilities/amount->rands target))]]
      [:div {:class "overflow-hidden h-2 w-full bg-zinc-100 rounded-full"}
       [:div {:class "h-full overflow-hidden rounded-full" :style {:width (str p "%")}}
        [:div {:class (str "h-full w-full rounded-full greed-bar-grow "
                           (if complete? "bg-emerald-500" "bg-emerald-400"))}]]]
      [:div {:class "flex items-center justify-between mt-2"}
       [:span {:class "text-xs font-medium text-emerald-600 tabular-nums"} (str p "% funded")]
       [:span {:class "text-xs text-zinc-400 tabular-nums"}
        (if complete? "Goal reached" (str (utilities/amount->rands remaining) " to go"))]]]
     (shared/modal modal-id (goal-form :goal goal :modal-id modal-id))]))

(defn- empty-state []
  [:div {:class "text-center py-14 px-6 bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card"}
   [:div {:class "mx-auto mb-3 flex items-center justify-center w-12 h-12 rounded-full bg-emerald-50"}
    [:span {:class "text-emerald-500"} (svgs/target)]]
   [:p {:class "text-sm font-medium text-zinc-600"} "No goals yet"]
   [:p {:class "mt-1 text-xs text-zinc-400"} "Set a savings target and track your progress towards it."]
   (shared/btn :variant :primary :size :md :class "mt-5"
               :attrs {"_" (shared/open-actions "goal-add-modal")}
               "Create your first goal")])

(defn page [{:keys [session params] :as ctx}]
  (let [user-id (:uid session)
        goals   (data/get-goals ctx user-id)]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (when (:alert params) (alerts/info params))
      [:div {:class "flex items-start justify-between gap-4"}
       (headers/pages-heading ["Goals"])
       [:div {:class "mt-1 flex-shrink-0"}
        (shared/btn :variant :primary :size :md
                    :attrs {"_" (shared/open-actions "goal-add-modal")}
                    (svgs/plus {:class "w-4 h-4"})
                    "Add goal")]]
      (if (seq goals)
        [:<>
         (stats/goals-hero goals)
         [:div {:class "grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3"}
          (map goal-card goals)]]
        (empty-state))
      (shared/modal "goal-add-modal" (goal-form :goal nil :modal-id "goal-add-modal"))])))
