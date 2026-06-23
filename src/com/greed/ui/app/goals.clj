(ns com.greed.ui.app.goals
  (:require [com.biffweb :as biff]
            [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.headers :as headers]
            [com.greed.utilities.core :as utilities]))

(defn- pct [saved target]
  (if (and target (pos? target))
    (int (min 100 (Math/round (* 100.0 (/ (double (or saved 0)) target)))))
    0))

(defn- field [& {:keys [id label type hint value required?]}]
  [:div
   [:label {:for id :class "block text-sm font-medium text-zinc-700 mb-1"} label]
   (when hint [:p {:class "text-xs text-zinc-400 mb-1"} hint])
   [:input (cond-> {:id id :name id :type type
                    :class "block w-full px-3 py-2 text-sm text-zinc-700 bg-white border border-zinc-200 rounded-lg focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500"
                    :required (boolean required?)}
             (= type "number") (assoc :min "0" :step "any")
             value (assoc :value (str value)))]])

(defn- goal-form
  "Create form when goal is nil, edit form otherwise."
  [& {:keys [goal toggle-var]}]
  (let [{:goal/keys [title target saved target-date] :xt/keys [id]} goal
        editing? (some? goal)]
    [:div {:class "w-full max-w-md bg-white rounded-xl shadow-card-md p-6"}
     [:div {:class "flex items-center justify-between mb-4"}
      [:h3 {:class "text-base font-semibold text-zinc-900"}
       (if editing? "Edit goal" "New savings goal")]
      [:button {:type "button" :class "text-zinc-400 hover:text-zinc-600"
                "@click" (str toggle-var " = false")}
       "✕"]]
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
      [:div {:class "mt-6 flex justify-end gap-3"}
       [:button {:type "button"
                 :class "px-4 py-2 text-sm font-medium text-zinc-600 hover:text-zinc-900"
                 "@click" (str toggle-var " = false")}
        "Cancel"]
       [:button {:type "submit"
                 :class "px-5 py-2 text-sm font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors"}
        (if editing? "Save changes" "Create goal")]])]))

(defn- modal [& {:keys [toggle-var body]}]
  [:div {:x-show toggle-var :x-cloak "true"
         :class "fixed inset-0 z-50 flex items-center justify-center p-4"
         :x-transition:enter "transition ease-out duration-200"
         :x-transition:enter-start "opacity-0 scale-95"
         :x-transition:enter-end "opacity-100 scale-100"}
   [:div {:class "absolute inset-0 bg-black/50" "@click" (str toggle-var " = false")}]
   [:div {:class "relative z-10"} body]])

(defn- goal-card [goal]
  (let [{:goal/keys [title target saved target-date] :xt/keys [id]} goal
        saved     (or saved 0)
        target    (or target 0)
        p         (pct saved target)
        remaining (max 0 (- target saved))
        complete? (>= saved target)]
    [:div {:x-data "{ editOpen: false }"
           :class "bg-white rounded-xl border border-zinc-200/70 shadow-card p-5 transition-all duration-200 hover:shadow-card-md"}
     [:div {:class "flex items-start justify-between gap-3"}
      [:div {:class "min-w-0"}
       [:h3 {:class "text-sm font-semibold text-zinc-900 truncate"} title]
       (when target-date
         [:p {:class "mt-0.5 text-xs text-zinc-400"} (str "Target date · " target-date)])]
      [:div {:class "flex items-center gap-1 flex-shrink-0"}
       [:button {:type "button" :title "Edit"
                 :class "p-1.5 text-zinc-400 hover:text-zinc-700 hover:bg-zinc-100 rounded-md transition-colors"
                 "@click" "editOpen = true"}
        [:svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                 :d "M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"}]]]
       (biff/form {:action "/app/goals/delete-goal" :class "flex"}
         [:input {:type "hidden" :name "goal-id" :value (str id)}]
         [:button {:type "submit" :title "Delete"
                   :onclick "return confirm('Delete this goal?')"
                   :class "p-1.5 text-zinc-400 hover:text-rose-500 hover:bg-rose-50 rounded-md transition-colors"}
          [:svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
           [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                   :d "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"}]]])]]
     [:div {:class "mt-4"}
      [:div {:class "flex items-end justify-between mb-1.5"}
       [:span {:class "text-lg font-semibold text-zinc-900 tabular-nums"} (utilities/amount->rands saved)]
       [:span {:class "text-xs text-zinc-400 tabular-nums"} (str "of " (utilities/amount->rands target))]]
      [:div {:class "h-2 w-full rounded-full bg-zinc-100 overflow-hidden"}
       [:div {:class (str "h-full rounded-full transition-all " (if complete? "bg-emerald-500" "bg-emerald-400"))
              :style {:width (str p "%")}}]]
      [:div {:class "mt-2 flex items-center justify-between"}
       [:span {:class "text-xs font-medium text-emerald-600 tabular-nums"} (str p "% funded")]
       [:span {:class "text-xs text-zinc-400 tabular-nums"}
        (if complete? "Goal reached 🎉" (str (utilities/amount->rands remaining) " to go"))]]]
     (modal :toggle-var "editOpen" :body (goal-form :goal goal :toggle-var "editOpen"))]))

(defn- summary [goals]
  (let [target-total (reduce + (map #(or (:goal/target %) 0) goals))
        saved-total  (reduce + (map #(or (:goal/saved %) 0) goals))
        p            (pct saved-total target-total)]
    [:div {:class "grid grid-cols-1 sm:grid-cols-3 gap-4"}
     [:div {:class "bg-white rounded-xl border border-zinc-200/70 shadow-card p-5"}
      [:p {:class "text-xs font-medium text-zinc-400 uppercase tracking-wider"} "Active goals"]
      [:p {:class "mt-2 text-2xl font-semibold text-zinc-900 tabular-nums"} (count goals)]]
     [:div {:class "bg-white rounded-xl border border-zinc-200/70 shadow-card p-5"}
      [:p {:class "text-xs font-medium text-zinc-400 uppercase tracking-wider"} "Saved"]
      [:p {:class "mt-2 text-2xl font-semibold text-emerald-600 tabular-nums"} (utilities/amount->rands saved-total)]]
     [:div {:class "bg-white rounded-xl border border-zinc-200/70 shadow-card p-5"}
      [:p {:class "text-xs font-medium text-zinc-400 uppercase tracking-wider"} "Total target"]
      [:p {:class "mt-2 text-2xl font-semibold text-zinc-900 tabular-nums"} (utilities/amount->rands target-total)]
      [:p {:class "mt-1 text-xs text-zinc-400"} (str p "% of all goals funded")]]]))

(defn- empty-state []
  [:div {:class "bg-white rounded-xl border border-zinc-200/70 shadow-card p-12 text-center"}
   [:div {:class "w-12 h-12 mx-auto rounded-full bg-emerald-50 flex items-center justify-center text-emerald-500 mb-4"}
    [:svg {:class "w-6 h-6" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24" :stroke-width "1.8"}
     [:path {:stroke-linecap "round" :stroke-linejoin "round"
             :d "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Zm0-4.5a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9Zm0-3a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z"}]]]
   [:p {:class "text-sm font-medium text-zinc-600"} "No goals yet"]
   [:p {:class "mt-1 text-xs text-zinc-400"} "Set a savings target and track your progress towards it."]
   [:button {:type "button"
             :class "mt-4 inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors"
             "@click" "addOpen = true"}
    "Create your first goal"]])

(defn page [{:keys [session params] :as ctx}]
  (let [user-id (:uid session)
        goals   (data/get-goals ctx user-id)]
    (ui/app
     ctx
     [:div {:class "space-y-4" :x-data "{ addOpen: false }"}
      (when (:alert params) (alerts/info params))
      (headers/pages-heading ["Goals"])
      [:div {:class "flex justify-end"}
       [:button {:type "button"
                 :class "inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors"
                 "@click" "addOpen = true"}
        [:svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M12 4v16m8-8H4"}]]
        "Add goal"]]
      (if (seq goals)
        [:<>
         (summary goals)
         [:div {:class "grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4"}
          (map goal-card goals)]]
        (empty-state))
      (modal :toggle-var "addOpen" :body (goal-form :goal nil :toggle-var "addOpen"))])))
