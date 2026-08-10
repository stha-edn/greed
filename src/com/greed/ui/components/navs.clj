(ns com.greed.ui.components.navs
  (:require [com.greed.data.core :as data]
            [com.greed.ui.components.svgs :as svgs]))

(defn nav [{:keys [session]}]
  (let [signed-in? (some? (:uid session))]
    [:nav {:class "flex items-center justify-between gap-3 py-5"}
     [:a {:href "/"}
      [:span {:class "text-2xl sm:text-3xl font-giza font-bold text-zinc-900"} "greed."]]
     [:div {:class "flex items-center gap-1.5 sm:gap-2"}
      [:a {:href "/about"
           :class "whitespace-nowrap px-2 sm:px-3 py-1.5 text-xs sm:text-sm font-medium text-zinc-600 hover:text-zinc-900 rounded-lg hover:bg-zinc-100 transition-colors"}
       "About"]
      [:a {:href "/team"
           :class "whitespace-nowrap px-2 sm:px-3 py-1.5 text-xs sm:text-sm font-medium text-zinc-600 hover:text-zinc-900 rounded-lg hover:bg-zinc-100 transition-colors"}
       "Team"]
      (if signed-in?
        [:a {:href "/app"
             :class "whitespace-nowrap px-3 sm:px-4 py-1.5 text-xs sm:text-sm font-medium text-white bg-zinc-900 rounded-lg hover:bg-zinc-700 transition-colors"}
         "Dashboard"]
        [:<>
         [:a {:href "/signin"
              :class "whitespace-nowrap px-3 sm:px-4 py-1.5 text-xs sm:text-sm font-medium text-zinc-700 border border-zinc-300 rounded-lg hover:border-zinc-500 hover:bg-zinc-50 transition-colors"}
          "Sign In"]
         [:a {:href "/signup"
              :class "whitespace-nowrap px-3 sm:px-4 py-1.5 text-xs sm:text-sm font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors"}
          "Sign Up"]])]]))

(defn- nav-link [href label icon path-expr]
  [:a {:href href
       :class "group relative flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150 active:opacity-70 active:scale-[0.98]"
       :x-bind:class (str path-expr
                          " ? 'bg-zinc-800/80 text-emerald-400'"
                          " : 'text-zinc-400 hover:bg-zinc-800/60 hover:text-white'")}
   ;; Active indicator bar
   [:span {:class "absolute left-0 top-1/2 -translate-y-1/2 w-1 h-5 rounded-r-full bg-emerald-400 transition-opacity duration-150"
           :x-bind:class (str path-expr " ? 'opacity-100' : 'opacity-0'")}]
   [:span {:class "w-5 h-5 flex-shrink-0 [&_svg]:w-full [&_svg]:h-full"} icon]
   label])

(def ^:private primary-links
  "Primary app destinations, shared by the desktop sidebar, mobile bottom bar,
   and mobile drawer. Items with :bottom? true live in the mobile bottom bar,
   so the mobile drawer only lists the remainder (no duplicated primary nav)."
  [{:href "/app"           :label "Dashboard" :icon (svgs/dashboard)   :path "currentPath === '/app'"
    :bottom? true}
   {:href "/app/finances/" :label "Finances"  :icon (svgs/credit-card) :path "currentPath.startsWith('/app/finances')"
    :bottom? true}
   {:href "/app/goals"     :label "Goals"     :icon (svgs/target)      :path "currentPath.startsWith('/app/goals')"
    :bottom? true}
   {:href "/app/insights"  :label "Insights"  :icon (svgs/chart-bar)   :path "currentPath.startsWith('/app/insights')"
    :bottom? false}
   {:href "/app/calendar"  :label "Calendar"  :icon (svgs/calendar)    :path "currentPath.startsWith('/app/calendar')"
    :bottom? true}
   {:href "/app/tax"       :label "Tax"       :icon (svgs/tools)       :path "currentPath.startsWith('/app/tax')"
    :bottom? true}])

(defn- sidebar-inner [firstname lastname admin?]
  (let [initials (str (or (first firstname) \?) (or (first lastname) \?))]
    [:<>
     [:div {:class "flex items-center h-16 px-5 border-b border-zinc-800 flex-shrink-0"}
      [:a {:href "/"}
       [:span {:class "text-2xl font-giza font-bold text-white"} "greed."]
       [:span {:class "ml-1.5 text-xs font-medium text-emerald-500 align-top mt-1 inline-block"} "beta"]]]
     [:div {:class "flex flex-col flex-1 px-3 py-5 overflow-y-auto min-h-0"}
      [:nav {:class "space-y-0.5"
             :x-data "{ currentPath: window.location.pathname }"}
       (for [{:keys [href label icon path]} primary-links]
         (nav-link href label icon path))
       (when admin?
         (nav-link "/app/admin/users" "Users" (svgs/users) "currentPath.startsWith('/app/admin/users')"))]
      [:div {:class "mt-auto pt-4 border-t border-zinc-800"}
       [:nav {:class "space-y-0.5 mb-4"}
        [:a {:href "/app/settings"
             :class "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-zinc-400 hover:bg-zinc-800 hover:text-white transition-all active:opacity-70 active:scale-[0.98]"}
         [:span {:class "w-5 h-5 flex-shrink-0 [&_svg]:w-full [&_svg]:h-full"} (svgs/cog)]
         "Settings"]]
       [:div {:class "flex items-center justify-between px-2 py-2"}
        [:a {:href "/app/profile" :class "flex items-center gap-2.5 min-w-0 group"}
         [:div {:class "flex-shrink-0 w-8 h-8 rounded-full bg-emerald-600 flex items-center justify-center text-xs font-semibold text-white"}
          (str initials)]
         [:div {:class "min-w-0"}
          [:p {:class "text-xs font-medium text-zinc-300 truncate group-hover:text-white transition-colors"}
           (str firstname " " lastname)]]]
        [:a {:href "/logout"
             :class "flex-shrink-0 p-1.5 text-zinc-500 hover:text-white hover:bg-zinc-800 rounded-md transition-colors"
             :title "Sign out"}
         (svgs/logout)]]]]]))

(defn sidebar [{:keys [session] :as ctx}]
  (let [user-id (:uid session)
        user (data/get-user ctx user-id)
        {:user/keys [firstname lastname]} user]
    [:aside {:class "hidden md:flex flex-col w-64 h-screen bg-black fixed top-0 left-0 z-30"}
     (sidebar-inner firstname lastname (data/admin? user))]))

(defn- bottom-tab [href label icon path-expr]
  [:a {:href href
       :class "flex flex-1 flex-col items-center justify-center gap-0.5 py-2 text-[10px] font-medium transition-colors active:opacity-60 active:scale-[0.96] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-emerald-400"
       :x-bind:class (str path-expr
                          " ? 'text-emerald-400'"
                          " : 'text-zinc-400 hover:text-white'")}
   [:span {:class "w-6 h-6 flex items-center justify-center [&_svg]:w-full [&_svg]:h-full"} icon]
   label])

(defn mobile-bottom-nav []
  [:nav {:class "md:hidden fixed bottom-0 left-0 right-0 z-20 bg-black border-t border-zinc-800 flex pb-[env(safe-area-inset-bottom)]"
         :x-data "{ currentPath: window.location.pathname }"}
   (for [{:keys [href label icon path]} (filter :bottom? primary-links)]
     (bottom-tab href label icon path))])

(defn mobile-sidebar [{:keys [session] :as ctx}]
  (let [user-id (:uid session)
        user (data/get-user ctx user-id)
        {:user/keys [firstname lastname]} user
        admin? (data/admin? user)
        initials (str (or (first firstname) \?) (or (first lastname) \?))]
    [:div {:x-data "{ open: false }" "@keydown.escape.window" "open = false"}
     [:div {:class "md:hidden fixed top-0 left-0 right-0 min-h-14 bg-black z-20 flex items-center justify-between px-4 pt-[env(safe-area-inset-top)]"}
      [:a {:href "/"}
       [:span {:class "text-xl font-giza font-bold text-white"} "greed."]
       [:span {:class "ml-1.5 text-xs font-medium text-emerald-500 align-top mt-1 inline-block"} "beta"]]
      [:button {"@click" "open = true"
                :class "p-2 text-zinc-400 hover:text-white rounded-lg hover:bg-zinc-800 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-emerald-400"
                :aria-label "Open menu"}
       [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
        [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                :d "M4 6h16M4 12h16M4 18h16"}]]]]
     [:div {:x-show "open" :x-cloak "true"
            :class "fixed inset-0 bg-black/60 z-30 md:hidden"
            "@click" "open = false"}]
     [:aside {:class "fixed top-0 left-0 h-full w-64 bg-black z-40 md:hidden flex flex-col"
              :x-show "open" :x-cloak "true"
              :x-transition:enter "transition-transform duration-200 ease-out"
              :x-transition:enter-start "-translate-x-full"
              :x-transition:enter-end "translate-x-0"
              :x-transition:leave "transition-transform duration-150 ease-in"
              :x-transition:leave-start "translate-x-0"
              :x-transition:leave-end "-translate-x-full"}
       [:div {:class "flex items-center justify-between min-h-14 px-4 border-b border-zinc-800 flex-shrink-0 pt-[env(safe-area-inset-top)]"}
       [:a {:href "/"}
        [:span {:class "text-xl font-giza font-bold text-white"} "greed."]
        [:span {:class "ml-1.5 text-xs font-medium text-emerald-500 align-top mt-1 inline-block"} "beta"]]
       [:button {"@click" "open = false"
                 :class "p-2 text-zinc-400 hover:text-white rounded-lg hover:bg-zinc-800 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-emerald-400"
                 :aria-label "Close menu"}
        [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                 :d "M6 18L18 6M6 6l12 12"}]]]]
       [:div {:class "flex flex-col flex-1 px-3 py-5 overflow-y-auto"}
        [:p {:class "px-3 mb-2 text-xs font-semibold text-zinc-500 uppercase tracking-wider"} "More"]
        [:nav {:class "space-y-0.5"
               :x-data "{ currentPath: window.location.pathname }"}
        (for [{:keys [href label icon path]} (remove :bottom? primary-links)]
          (nav-link href label icon path))
        (when admin?
          (nav-link "/app/admin/users" "Users" (svgs/users) "currentPath.startsWith('/app/admin/users')"))]
       [:div {:class "mt-auto pt-4 border-t border-zinc-800"}
        [:nav {:class "space-y-0.5 mb-4"}
         [:a {:href "/app/settings"
              :class "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-zinc-400 hover:bg-zinc-800 hover:text-white transition-all active:opacity-70 active:scale-[0.98]"}
          [:span {:class "w-5 h-5 flex-shrink-0 [&_svg]:w-full [&_svg]:h-full"} (svgs/cog)] "Settings"]]
        [:div {:class "flex items-center justify-between px-2 py-2"}
         [:a {:href "/app/profile" :class "flex items-center gap-2.5"}
          [:div {:class "w-8 h-8 rounded-full bg-emerald-600 flex items-center justify-center text-xs font-semibold text-white"}
           (str initials)]
          [:p {:class "text-xs font-medium text-zinc-300 truncate"} (str firstname " " lastname)]]
         [:a {:href "/logout" :class "p-1.5 text-zinc-500 hover:text-white hover:bg-zinc-800 rounded-md transition-colors"}
          (svgs/logout)]]]]]]))
