(ns com.greed.ui.components.navs
  (:require [com.greed.data.core :as data]
            [com.greed.ui.components.svgs :as svgs]))

(defn nav [{:keys [session]}]
  (let [signed-in? (some? (:uid session))]
    [:nav {:class "flex items-center justify-between py-5"}
     [:a {:href "/"}
      [:span {:class "text-3xl font-giza font-bold text-zinc-900"} "greed."]]
     [:div {:class "flex items-center gap-2"}
      [:a {:href "/team"
           :class "px-3 py-1.5 text-sm font-medium text-zinc-600 hover:text-zinc-900 rounded-lg hover:bg-gray-100 transition-colors"}
       "Team"]
      (if signed-in?
        [:a {:href "/app"
             :class "px-4 py-1.5 text-sm font-medium text-white bg-zinc-900 rounded-lg hover:bg-zinc-700 transition-colors"}
         "Go to Dashboard"]
        [:<>
         [:a {:href "/signin"
              :class "px-4 py-1.5 text-sm font-medium text-zinc-700 border border-gray-300 rounded-lg hover:border-gray-500 hover:bg-gray-50 transition-colors"}
          "Sign In"]
         [:a {:href "/signup"
              :class "px-4 py-1.5 text-sm font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors"}
          "Sign Up"]])]]))

(defn- nav-link [href label icon path-expr]
  [:a {:href href
       :class "group flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150"
       :x-bind:class (str path-expr
                          " ? 'bg-zinc-800 text-emerald-400'"
                          " : 'text-zinc-400 hover:bg-zinc-800 hover:text-white'")}
   [:span {:class "w-5 flex-shrink-0"} icon]
   label])

(defn- sidebar-inner [firstname lastname]
  (let [initials (str (or (first firstname) \?) (or (first lastname) \?))]
    [:<>
     [:div {:class "flex items-center h-16 px-5 border-b border-zinc-800 flex-shrink-0"}
      [:a {:href "/"}
       [:span {:class "text-2xl font-giza font-bold text-white"} "greed."]
       [:span {:class "ml-1.5 text-xs font-medium text-emerald-500 align-top mt-1 inline-block"} "beta"]]]
     [:div {:class "flex flex-col flex-1 px-3 py-5 overflow-y-auto min-h-0"}
      [:nav {:class "space-y-0.5"
             :x-data "{ currentPath: window.location.pathname }"}
       (nav-link "/app"          "Dashboard" (svgs/dashboard)   "currentPath === '/app'")
       (nav-link "/app/finances/" "Finances"  (svgs/credit-card) "currentPath.startsWith('/app/finances')")
       (nav-link "/app/calendar" "Calendar"  (svgs/calendar)    "currentPath.startsWith('/app/calendar')")
       (nav-link "/app/tools"    "Tools"     (svgs/tools)       "currentPath.startsWith('/app/tools')")]
      [:div {:class "mt-auto pt-4 border-t border-zinc-800"}
       [:nav {:class "space-y-0.5 mb-4"}
        [:a {:href "/app/settings"
             :class "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-zinc-400 hover:bg-zinc-800 hover:text-white transition-all"}
         [:span {:class "w-5 flex-shrink-0"} (svgs/cog)]
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
        {:user/keys [firstname lastname]} (data/get-user ctx user-id)]
    [:aside {:class "hidden md:flex flex-col w-64 h-screen bg-black fixed top-0 left-0 z-30"}
     (sidebar-inner firstname lastname)]))

(defn mobile-sidebar [{:keys [session] :as ctx}]
  (let [user-id (:uid session)
        {:user/keys [firstname lastname]} (data/get-user ctx user-id)
        initials (str (or (first firstname) \?) (or (first lastname) \?))]
    [:div {:x-data "{ open: false }"}
     [:div {:class "md:hidden fixed top-0 left-0 right-0 h-14 bg-black z-20 flex items-center justify-between px-4"}
      [:a {:href "/"}
       [:span {:class "text-xl font-giza font-bold text-white"} "greed."]
       [:span {:class "ml-1.5 text-xs font-medium text-emerald-500 align-top mt-1 inline-block"} "beta"]]
      [:button {"@click" "open = true"
                :class "p-2 text-zinc-400 hover:text-white rounded-lg hover:bg-zinc-800 transition-colors"
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
      [:div {:class "flex items-center justify-between h-14 px-4 border-b border-zinc-800 flex-shrink-0"}
       [:a {:href "/"}
        [:span {:class "text-xl font-giza font-bold text-white"} "greed."]
        [:span {:class "ml-1.5 text-xs font-medium text-emerald-500 align-top mt-1 inline-block"} "beta"]]
       [:button {"@click" "open = false"
                 :class "p-2 text-zinc-400 hover:text-white rounded-lg hover:bg-zinc-800 transition-colors"}
        [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                 :d "M6 18L18 6M6 6l12 12"}]]]]
      [:div {:class "flex flex-col flex-1 px-3 py-5 overflow-y-auto"}
       [:nav {:class "space-y-0.5"
              :x-data "{ currentPath: window.location.pathname }"}
        (nav-link "/app"          "Dashboard" (svgs/dashboard)   "currentPath === '/app'")
        (nav-link "/app/finances/" "Finances"  (svgs/credit-card) "currentPath.startsWith('/app/finances')")
        (nav-link "/app/calendar" "Calendar"  (svgs/calendar)    "currentPath.startsWith('/app/calendar')")
        (nav-link "/app/tools"    "Tools"     (svgs/tools)       "currentPath.startsWith('/app/tools')")]
       [:div {:class "mt-auto pt-4 border-t border-zinc-800"}
        [:nav {:class "space-y-0.5 mb-4"}
         [:a {:href "/app/settings"
              :class "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-zinc-400 hover:bg-zinc-800 hover:text-white transition-all"}
          [:span {:class "w-5"} (svgs/cog)] "Settings"]]
        [:div {:class "flex items-center justify-between px-2 py-2"}
         [:a {:href "/app/profile" :class "flex items-center gap-2.5"}
          [:div {:class "w-8 h-8 rounded-full bg-emerald-600 flex items-center justify-center text-xs font-semibold text-white"}
           (str initials)]
          [:p {:class "text-xs font-medium text-zinc-300 truncate"} (str firstname " " lastname)]]
         [:a {:href "/logout" :class "p-1.5 text-zinc-500 hover:text-white hover:bg-zinc-800 rounded-md transition-colors"}
          (svgs/logout)]]]]]]))
