(ns com.greed.ui.components.shared
  (:require [com.core :as c]
            [com.greed.data.core :as data]
            [com.greed.utilities.core :as utilities]))

(defn- base-input-class []
  "block w-full px-3 py-2 text-sm text-zinc-700 bg-white border border-zinc-200 rounded-lg focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 placeholder-zinc-400 transition-colors duration-150")

(defn determine-placeholder [id user profile]
  (let [config c/common-config]
    (cond
      (contains? (:user/fields config) id)
      ((keyword (str "user/" id)) user)
      (contains? (:finances/fields config) id)
      ((keyword (str "finances/" id)) profile))))

(defn input [& {:keys [id type label required?]
                :or {required? false}}]
  [:div {:class "mt-4"}
   [:input
    {:class (base-input-class)
     :id id :name id :type type
     :autocomplete type
     :placeholder label
     :required required?}]])

(defn app-input [ctx & {:keys [id type label required? prefix hint]
                        :or {required? false}}]
  (let [{:keys [session]} ctx
        user-id     (:uid session)
        user        (data/get-user ctx user-id)
        finances    (data/get-finances ctx user-id)
        current-val (determine-placeholder id user finances)]
    [:div
     (when (seq label)
       [:label {:class "block text-sm font-medium text-zinc-700 mb-1" :for id} label])
     (if prefix
       [:div {:class "relative flex items-center"}
        [:div {:class "absolute left-3 text-zinc-400 text-sm font-medium pointer-events-none select-none"} prefix]
        [:input {:class "block w-full pl-7 pr-3 py-2 text-sm text-zinc-700 bg-white border border-zinc-200 rounded-lg focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-colors"
                 :id id :name id :type type
                 :value (str current-val)
                 :required required?}]]
       [:input {:class (base-input-class)
                :id id :name id :type type
                :value (str current-val)
                :required required?}])
     (when hint
       [:p {:class "text-xs text-zinc-400 mt-1"} hint])]))

(defn app-select [ctx & {:keys [id label options required? hint]
                         :or {required? false}}]
  (let [{:keys [session]} ctx
        user-id     (:uid session)
        finances    (data/get-finances ctx user-id)
        current-val ((keyword (str "finances/" id)) finances)]
    [:div
     [:label {:class "block text-sm font-medium text-zinc-700 mb-1" :for id} label]
     [:select {:class (base-input-class)
               :id id :name id
               :required required?}
      (for [option options]
        [:option (cond-> {:value option}
                   (= option current-val) (assoc :selected true))
         (utilities/->string option)])]
     (when hint
       [:p {:class "text-xs text-zinc-400 mt-1"} hint])]))

(defn modal-input [& {:keys [id type label required?]
                      :or {required? false}}]
  [:div {:class "mt-3"}
   [:label {:class "block text-sm font-medium text-zinc-700 mb-1" :for id} label]
   [:input {:class (base-input-class)
            :type type :name id :id id :required required?}]])

(defn modal-select [& {:keys [id label options required?]
                       :or {required? false}}]
  [:div {:class "mt-3"}
   [:label {:class "block text-sm font-medium text-zinc-700 mb-1" :for id} label]
   [:select {:class (base-input-class)
             :id id :name id :required required?}
    (for [option options]
      [:option {:value option} (utilities/->string option)])]])
