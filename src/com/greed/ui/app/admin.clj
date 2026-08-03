(ns com.greed.ui.app.admin
  (:require [com.biffweb :as biff]
            [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.headers :as headers]))

(defn- badge [text tone]
  [:span {:class (str "text-[10px] font-medium uppercase tracking-wide px-2 py-0.5 rounded-full " tone)}
   text])

(defn- status-badge [active?]
  (if active?
    (badge "Active" "bg-emerald-50 text-emerald-700")
    (badge "Deactivated" "bg-zinc-100 text-zinc-500")))

(defn- role-badges [roles]
  (if (seq roles)
    [:div {:class "flex flex-wrap gap-1"}
     (for [role (sort roles)]
       (badge (name role) "bg-blue-50 text-blue-800"))]
    [:span {:class "text-xs text-zinc-400"} "—"]))

(defn- field [& {:keys [id name label type value required?]}]
  [:div
   [:label {:for id :class "block text-sm font-medium text-zinc-700 mb-1"} label]
   [:input (cond-> {:id id :name name :type type
                    :class "block w-full px-3 py-2 text-sm text-zinc-700 bg-white border border-zinc-200 rounded-lg focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500"
                    :required (boolean required?)}
             value (assoc :value (str value)))]])

(defn- role-field [& {:keys [current-role self?]}]
  [:div
   [:label {:class "block text-sm font-medium text-zinc-700 mb-1"} "Role"]
   (if self?
     [:<>
      [:select {:disabled true
                :class "block w-full px-3 py-2 text-sm text-zinc-400 bg-zinc-50 border border-zinc-200 rounded-lg"}
       [:option "Admin"]]
      [:input {:type "hidden" :name "role" :value "admin"}]
      [:p {:class "text-xs text-zinc-400 mt-1"} "You can't change your own role here."]]
     [:select {:name "role"
               :class "block w-full px-3 py-2 text-sm text-zinc-700 bg-white border border-zinc-200 rounded-lg focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500"}
      [:option (cond-> {:value "user"} (= current-role :user) (assoc :selected true)) "User"]
      [:option (cond-> {:value "admin"} (= current-role :admin) (assoc :selected true)) "Admin"]])])

(defn- edit-modal [user self?]
  (let [{:user/keys [firstname lastname email age roles]} user
        id (str (:xt/id user))
        current-role (if (contains? roles :admin) :admin :user)]
    [:div {:x-show (str "editingUserId === '" id "'") :x-cloak "true"
           :class "fixed inset-0 z-50 flex items-center justify-center p-4"
           :x-transition:enter "transition ease-out duration-200"
           :x-transition:enter-start "opacity-0 scale-95"
           :x-transition:enter-end "opacity-100 scale-100"}
     [:div {:class "absolute inset-0 bg-black/50" "@click" "editingUserId = null"}]
     [:div {:class "relative z-10 w-full max-w-md bg-white rounded-xl shadow-card-md p-6"}
      [:div {:class "flex items-center justify-between mb-4"}
       [:h3 {:class "text-base font-semibold text-zinc-900"} "Edit user"]
       [:button {:type "button" :class "text-zinc-400 hover:text-zinc-600"
                 "@click" "editingUserId = null"}
        "✕"]]
      (biff/form
       {:action "/app/admin/users/update"}
       [:input {:type "hidden" :name "user-id" :value id}]
       [:div {:class "space-y-4"}
        (field :id (str "firstname-" id) :name "firstname" :label "First name" :type "text"
               :value firstname :required? true)
        (field :id (str "lastname-" id) :name "lastname" :label "Last name" :type "text"
               :value lastname :required? true)
        (field :id (str "email-" id) :name "email" :label "Email" :type "text"
               :value email :required? true)
        (field :id (str "age-" id) :name "age" :label "Age" :type "number"
               :value age :required? true)
        (role-field :current-role current-role :self? self?)]
       [:div {:class "mt-6 flex justify-end gap-3"}
        [:button {:type "button"
                  :class "px-4 py-2 text-sm font-medium text-zinc-600 hover:text-zinc-900"
                  "@click" "editingUserId = null"}
         "Cancel"]
        [:button {:type "submit"
                  :class "px-5 py-2 text-sm font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors"}
         "Save changes"]])]]))

(defn- password-cell [user]
  (if (data/hashed-password? (:user/password user))
    [:span {:class "text-xs text-zinc-400"} "—"]
    [:div {:class "flex items-center gap-2"}
     (badge "Not hashed" "bg-amber-50 text-amber-800")
     (biff/form {:action "/app/admin/users/hash-password" :class "flex"}
       [:input {:type "hidden" :name "user-id" :value (str (:xt/id user))}]
       [:button {:type "submit"
                 :class "text-[10px] font-medium uppercase tracking-wide px-2 py-0.5 rounded-full bg-amber-100 text-amber-800"}
        "Hash now"])]))

(defn- insecure-password-banner [unhashed-count]
  (when (pos? unhashed-count)
    [:div {:class "flex items-center gap-3 px-4 py-3 rounded-xl border border-amber-200 bg-amber-50 text-amber-800"}
     [:svg {:class "w-5 h-5 flex-shrink-0" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
      [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
              :d "M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z"}]]
     [:p {:class "text-sm font-medium"}
      (str unhashed-count " account" (when (> unhashed-count 1) "s")
           " " (if (> unhashed-count 1) "have" "has")
           " a password stored without hashing. Use \"Hash now\" below to secure "
           (if (> unhashed-count 1) "them" "it") ".")]]))

(defn- row-actions [user self?]
  (let [id (str (:xt/id user))]
    [:div {:class "flex items-center justify-end gap-1"}
     [:button {:type "button" :title "Edit"
               :class "p-1.5 text-zinc-400 hover:text-zinc-700 hover:bg-zinc-100 rounded-md transition-colors"
               "@click" (str "editingUserId = '" id "'")}
      [:svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
       [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
               :d "M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"}]]]
     (if self?
       [:span {:title "You can't delete your own account from here"
               :class "p-1.5 text-zinc-200 rounded-md"}
        [:svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                 :d "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"}]]]
       (biff/form {:action "/app/admin/users/delete" :class "flex"}
         [:input {:type "hidden" :name "user-id" :value id}]
         [:button {:type "submit" :title "Delete"
                   :onclick (str "return confirm('Delete " (:user/email user) " and all of their data? This cannot be undone.')")
                   :class "p-1.5 text-zinc-400 hover:text-rose-500 hover:bg-rose-50 rounded-md transition-colors"}
          [:svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
           [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                   :d "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"}]]]))]))

(defn- users-table [users current-user-id]
  [:div {:class "flex flex-col bg-white rounded-xl border border-zinc-200/70 shadow-card overflow-hidden"}
   [:div {:class "flex items-center justify-between px-4 py-3 border-b border-zinc-100"}
    [:span {:class "text-xs font-semibold uppercase tracking-wide px-2.5 py-1 rounded-full text-zinc-600 bg-zinc-100"}
     "All users"]
    [:span {:class "text-xs font-medium text-zinc-400 tabular-nums"}
     (str (count users) (if (= 1 (count users)) " user" " users"))]]
   [:div {:class "overflow-auto max-h-[520px]"}
    [:table {:class "w-full min-w-[720px]"}
     [:thead {:class "sticky top-0 z-10 bg-white"}
      [:tr {:class "border-b border-zinc-100"}
       [:th {:class "px-4 py-2.5 text-left text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"} "Name"]
       [:th {:class "px-4 py-2.5 text-left text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"} "Email"]
       [:th {:class "px-4 py-2.5 text-left text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"} "Age"]
       [:th {:class "px-4 py-2.5 text-left text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"} "Status"]
       [:th {:class "px-4 py-2.5 text-left text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"} "Roles"]
       [:th {:class "px-4 py-2.5 text-left text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"} "Password"]
       [:th {:class "px-4 py-2.5 w-20"}]]]
     [:tbody {:class "divide-y divide-zinc-100"}
      (for [{:user/keys [firstname lastname email age active roles] :as user} (sort-by :user/email users)]
        [:tr {:class "hover:bg-zinc-50 transition-colors"}
         [:td {:class "px-4 py-3 text-sm text-zinc-700 whitespace-nowrap"} (str firstname " " lastname)]
         [:td {:class "px-4 py-3 text-sm text-zinc-500 whitespace-nowrap"} email]
         [:td {:class "px-4 py-3 text-sm text-zinc-500 tabular-nums"} age]
         [:td {:class "px-4 py-3"} (status-badge active)]
         [:td {:class "px-4 py-3"} (role-badges roles)]
         [:td {:class "px-4 py-3"} (password-cell user)]
         [:td {:class "px-4 py-3"} (row-actions user (= (:xt/id user) current-user-id))]])]]]
   (for [user users] (edit-modal user (= (:xt/id user) current-user-id)))])

(defn page [{:keys [session params] :as ctx}]
  (let [users (data/get-users ctx)
        unhashed-count (count (remove #(data/hashed-password? (:user/password %)) users))]
    (ui/app
     ctx
     [:div {:class "space-y-4" :x-data "{ editingUserId: null }"}
      (when (:alert params) (alerts/info params))
      (headers/pages-heading ["Admin" "Users"])
      (insecure-password-banner unhashed-count)
      (users-table users (:uid session))])))
