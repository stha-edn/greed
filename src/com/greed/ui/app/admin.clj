(ns com.greed.ui.app.admin
  (:require [com.biffweb :as biff]
            [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.headers :as headers]))

(defn- badge [text tone]
  [:span {:class (str "px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide rounded-full " tone)}
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
   (shared/form-label id label)
   [:input (cond-> {:id id :name name :type type
                    :class "block w-full px-3 py-2 text-sm text-zinc-700 bg-white border border-zinc-200 rounded-lg focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500"
                    :required (boolean required?)}
             value (assoc :value (str value)))]])

(defn- role-field [& {:keys [current-role self?]}]
  [:div
   [:label {:class "block mb-1 text-sm font-medium text-zinc-700"} "Role"]
   (if self?
     [:<>
      [:select {:disabled true
                :class "block w-full px-3 py-2 text-sm text-zinc-400 bg-zinc-50 border border-zinc-200 rounded-lg"}
       [:option "Admin"]]
      [:input {:type "hidden" :name "role" :value "admin"}]
      [:p {:class "mt-1 text-xs text-zinc-400"} "You can't change your own role here."]]
     [:select {:name "role"
               :class "block w-full px-3 py-2 text-sm text-zinc-700 bg-white border border-zinc-200 rounded-lg hover:border-zinc-300 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 active:border-zinc-400"}
      [:option (cond-> {:value "user"} (= current-role :user) (assoc :selected true)) "User"]
      [:option (cond-> {:value "admin"} (= current-role :admin) (assoc :selected true)) "Admin"]])])

(defn- edit-modal [user self?]
  (let [{:user/keys [firstname lastname email age roles]} user
        id (str (:xt/id user))
        modal-id (str "user-edit-" id)
        current-role (if (contains? roles :admin) :admin :user)]
    (shared/modal modal-id
     [:div {:class "w-full max-w-md p-6 bg-white rounded-xl shadow-card-md"}
      [:div {:class "flex items-center justify-between mb-4"}
       [:h3 {:class "text-base font-semibold text-zinc-900"} "Edit user"]
[:button {:type "button" :title "Close" :aria-label "Close"
                   :class "flex items-center justify-center w-7 h-7 text-zinc-400 hover:text-zinc-600 hover:bg-zinc-100 rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 active:text-zinc-700 active:bg-zinc-200"
                   :_ (shared/close-actions modal-id)}
          (svgs/close {:class "w-4 h-4"})]]
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
       [:div {:class "flex justify-end gap-3 mt-6"}
[:button {:type "button"
                   :class "px-4 py-2 text-sm font-medium text-zinc-600 hover:text-zinc-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 active:text-zinc-800"
                   :_ (shared/close-actions modal-id)}
          "Cancel"]
         (shared/btn :variant :primary :size :md :type "submit" :class "px-5"
                     "Save changes")])])))

(defn- password-cell [user]
  (if (data/hashed-password? (:user/password user))
    [:span {:class "text-xs text-zinc-400"} "—"]
    [:div {:class "flex items-center gap-2"}
     (badge "Not hashed" "bg-amber-50 text-amber-800")
     (biff/form {:action "/app/admin/users/hash-password" :class "flex"}
       [:input {:type "hidden" :name "user-id" :value (str (:xt/id user))}]
       [:button {:type "submit"
                 :class "px-2 py-0.5 text-[10px] font-medium text-amber-800 uppercase tracking-wide bg-amber-100 rounded-full hover:bg-amber-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amber-400 focus-visible:ring-offset-2 active:bg-amber-300"}
        "Hash now"])]))

(defn- insecure-password-banner [unhashed-count]
  (when (pos? unhashed-count)
    [:div {:class "flex items-center gap-3 px-4 py-3 text-amber-800 bg-amber-50 border border-amber-200 rounded-xl"}
     [:svg {:class "flex-shrink-0 w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
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
                  :class "p-1.5 text-zinc-400 rounded-md transition-colors hover:text-zinc-700 hover:bg-zinc-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 active:text-zinc-700 active:bg-zinc-200"
                  :_ (shared/open-actions (str "user-edit-" id))}
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
                    :data-confirm (str "Delete " (:user/email user)
                                       " and all of their data? This cannot be undone.")
                    :class "p-1.5 text-zinc-400 rounded-md transition-colors hover:text-rose-500 hover:bg-rose-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-400 focus-visible:ring-offset-2 active:text-rose-600 active:bg-rose-100"}
          [:svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
           [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                   :d "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"}]]]))]))

(defn- users-table [users current-user-id]
  [:div {:class "flex flex-col overflow-hidden bg-white border border-zinc-200/70 rounded-xl shadow-card"}
   [:div {:class "flex items-center justify-between px-4 py-3 border-b border-zinc-100"}
    [:span {:class "px-2.5 py-1 text-xs font-semibold text-zinc-600 uppercase tracking-wide bg-zinc-100 rounded-full"}
     "All users"]
    [:span {:class "text-xs font-medium text-zinc-400 tabular-nums"}
     (str (count users) (if (= 1 (count users)) " user" " users"))]]
   [:div {:class "overflow-x-auto min-w-0"}
    [:table {:class "w-full min-w-[720px]"}
     [:thead
      [:tr {:class "border-b border-zinc-100"}
       [:th {:class "sticky left-0 z-20 text-left px-4 py-2.5 text-[11px] font-semibold text-zinc-400 uppercase tracking-wider bg-white"} "Name"]
       [:th {:class "text-left px-4 py-2.5 text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"} "Email"]
       [:th {:class "text-left px-4 py-2.5 text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"} "Age"]
       [:th {:class "text-left px-4 py-2.5 text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"} "Status"]
       [:th {:class "text-left px-4 py-2.5 text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"} "Roles"]
       [:th {:class "text-left px-4 py-2.5 text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"} "Password"]
       [:th {:class "w-20 px-4 py-2.5"}]]]
     [:tbody {:class "divide-y divide-zinc-100"}
      (for [{:user/keys [firstname lastname email age active roles] :as user} (sort-by :user/email users)]
        [:tr {:class "group transition-colors hover:bg-zinc-50"}
         [:td {:class "sticky left-0 z-10 px-4 py-3 text-sm text-zinc-700 whitespace-nowrap bg-white transition-colors group-hover:bg-zinc-50"} (str firstname " " lastname)]
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
[:div {:class "space-y-4"}
       (when (:alert params) (alerts/info params))
      (headers/pages-heading ["Admin" "Users"])
      (insecure-password-banner unhashed-count)
      (users-table users (:uid session))])))
