(ns com.greed.ui.components.navs
  (:require [com.greed.data.core :as data]
            [com.greed.ui.components.svgs :as svgs]))

(defn nav [{:keys [session]}]
  (let [signed-in? (some? (:uid session))]
    [:nav {:class "flex items-center justify-between gap-3 py-4"}
     [:a {:href "/" :class "active:scale-[0.97] transition-transform"}
      [:span {:class "text-2xl sm:text-3xl font-giza font-bold text-zinc-900"} "greed."]]
     [:div {:class "flex items-center gap-1.5 sm:gap-2"}
      [:a {:href "/about"
           :class "whitespace-nowrap px-2 sm:px-3 py-1.5 text-xs sm:text-sm font-medium text-zinc-600 hover:text-zinc-900 rounded-lg hover:bg-zinc-100 transition-colors active:scale-[0.97]"}
       "About"]
      [:a {:href "/team"
           :class "whitespace-nowrap px-2 sm:px-3 py-1.5 text-xs sm:text-sm font-medium text-zinc-600 hover:text-zinc-900 rounded-lg hover:bg-zinc-100 transition-colors active:scale-[0.97]"}
       "Team"]
      (if signed-in?
        [:a {:href "/app"
             :class "whitespace-nowrap px-3 sm:px-4 py-1.5 text-xs sm:text-sm font-medium text-white bg-zinc-900 rounded-lg hover:bg-zinc-700 transition-colors active:scale-[0.97]"}
         "Dashboard"]
        [:<>
         [:a {:href "/signin"
              :class "whitespace-nowrap px-3 sm:px-4 py-1.5 text-xs sm:text-sm font-medium text-zinc-700 border border-zinc-300 rounded-lg hover:border-zinc-500 hover:bg-zinc-50 transition-colors active:scale-[0.97]"}
          "Sign In"]
         [:a {:href "/signup"
              :class "whitespace-nowrap px-3 sm:px-4 py-1.5 text-xs sm:text-sm font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors active:scale-[0.97]"}
          "Sign Up"]])]]))

(defn- link-active? [uri href]
  "True when the current request path is on the nav item's route.
   Dashboard matches exactly; the rest match by prefix."
  (when (and uri href)
    (if (= href "/app")
      (= uri "/app")
      (.startsWith ^String uri href))))

(defn- nav-link [href label icon active?]
  [:a {:href href
       :aria-current (when active? "page")
       :class (str "group relative flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors duration-150 active:opacity-70 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-500/70"
                   (if active?
                     " bg-zinc-800/80 text-emerald-400"
                     " text-zinc-400 hover:bg-zinc-800/60 hover:text-white"))}
   ;; Active indicator bar
   [:span {:class (str "absolute left-0 top-1/2 -translate-y-1/2 w-1 h-5 rounded-r-full bg-emerald-400 transition-opacity duration-150 "
                       (if active? "opacity-100" "opacity-0"))}]
   [:span {:class "w-5 h-5 flex-shrink-0 [&_svg]:w-full [&_svg]:h-full"} icon]
   label])

(def ^:private primary-links
  "Primary app destinations, shared by the desktop sidebar, mobile bottom bar,
   and mobile drawer. Ordered by importance: Dashboard first, then the app's
   core Tax surface, day-to-day planning (Finances), what's coming (Calendar),
   and progress (Goals) — Insights is the passive read. Items with :bottom? true
   live in the mobile bottom bar, so the drawer only lists the remainder."
  [{:href "/app"           :label "Dashboard" :icon (svgs/squares-2x2)   :bottom? true}
   {:href "/app/tax"       :label "Tax"       :icon (svgs/percent-badge) :bottom? true}
   {:href "/app/finances/" :label "Finances"  :icon (svgs/credit-card)   :bottom? true}
   {:href "/app/calendar"  :label "Calendar"  :icon (svgs/calendar)      :bottom? true}
   {:href "/app/goals"     :label "Goals"     :icon (svgs/target)        :bottom? true}
   {:href "/app/insights"  :label "Insights"  :icon (svgs/chart-bar)     :bottom? false}])

(defn- sidebar-inner [uri firstname lastname admin?]
  (let [initials (str (or (first firstname) \?) (or (first lastname) \?))]
    [:<>
     [:div {:class "flex items-center h-16 px-5 border-b border-zinc-800 flex-shrink-0"}
      [:a {:href "/"}
       [:span {:class "text-2xl font-giza font-bold text-white"} "greed."]
       [:span {:class "ml-1.5 text-xs font-medium text-emerald-500 align-top mt-1 inline-block"} "beta"]]]
     [:div {:class "flex flex-col flex-1 px-3 py-5 overflow-y-auto min-h-0"}
      [:nav {:class "space-y-0.5"}
       (for [item primary-links]
         (nav-link (:href item) (:label item) (:icon item) (link-active? uri (:href item))))
       (when admin?
         (nav-link "/app/admin/users" "Users" (svgs/users) (link-active? uri "/app/admin/users")))]
      [:div {:class "mt-auto pt-4 border-t border-zinc-800"}
       [:nav {:class "space-y-0.5 mb-4"}
        [:a {:href "/app/settings"
             :class "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-zinc-400 hover:bg-zinc-800 hover:text-white transition-colors duration-150 active:opacity-70 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-500/70"}
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

(defn sidebar [{:keys [session uri] :as ctx}]
  (let [user-id (:uid session)
        user (data/get-user ctx user-id)
        {:user/keys [firstname lastname]} user]
    [:aside {:class "hidden md:flex flex-col w-64 h-screen bg-black fixed top-0 left-0 z-30"}
     (sidebar-inner uri firstname lastname (data/admin? user))]))

(defn- bottom-tab [href label icon active?]
  [:a {:href href
       :aria-current (when active? "page")
       :class (str "flex flex-1 flex-col items-center justify-center gap-0.5 py-2 text-[10px] font-medium transition-colors active:opacity-60 active:scale-[0.96] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-emerald-400"
                   (if active?
                     " text-emerald-400"
                     " text-zinc-400 hover:text-white"))}
   [:span {:class "w-6 h-6 flex items-center justify-center [&_svg]:w-full [&_svg]:h-full"} icon]
   label])

(defn mobile-bottom-nav [{:keys [uri]}]
  [:nav {:class "md:hidden fixed bottom-0 left-0 right-0 z-20 bg-black border-t border-zinc-800 flex pb-[env(safe-area-inset-bottom)]"}
   (for [item (filter :bottom? primary-links)]
     (bottom-tab (:href item) (:label item) (:icon item) (link-active? uri (:href item))))])

(def ^:private drawer-toggle-actions
  "hyperscript for the hamburger: toggles the `open` attribute on the drawer
   and dim overlay. Tailwind `[&[open]]:` variants style the open state, so the
   slide/fade are pure CSS transitions."
  (str "on click\n"
       "  if the @open of #mobile-drawer is 'true'\n"
       "    remove @open from #mobile-drawer-overlay\n"
       "    remove @open from #mobile-drawer\n"
       "    set the @aria-expanded of #mobile-drawer-toggle to 'false'\n"
       "  else\n"
       "    add @open='true' to #mobile-drawer-overlay\n"
       "    add @open='true' to #mobile-drawer\n"
       "    set the @aria-expanded of #mobile-drawer-toggle to 'true'\n"
       "  end"))

(def ^:private drawer-close-actions
  "hyperscript for the overlay and close button: closes the drawer."
  (str "on click\n"
       "  remove @open from #mobile-drawer-overlay\n"
       "  remove @open from #mobile-drawer\n"
       "  set the @aria-expanded of #mobile-drawer-toggle to 'false'"))

(def ^:private drawer-escape-actions
  "hyperscript for the wrapper: Escape closes the drawer."
  (str "on keydown[key == 'Escape'] from window\n"
       "  if the @open of #mobile-drawer is 'true'\n"
       "    remove @open from #mobile-drawer-overlay\n"
       "    remove @open from #mobile-drawer\n"
       "    set the @aria-expanded of #mobile-drawer-toggle to 'false'\n"
       "  end"))

(defn mobile-sidebar [{:keys [session uri] :as ctx}]
  (let [user-id (:uid session)
        user (data/get-user ctx user-id)
        {:user/keys [firstname lastname]} user
        admin? (data/admin? user)
        initials (str (or (first firstname) \?) (or (first lastname) \?))]
    [:div {:_ drawer-escape-actions}
     [:div {:class "md:hidden fixed top-0 left-0 right-0 min-h-14 bg-black z-20 flex items-center justify-between px-4 pt-[env(safe-area-inset-top)]"}
      [:a {:href "/"}
       [:span {:class "text-xl font-giza font-bold text-white"} "greed."]
       [:span {:class "ml-1.5 text-xs font-medium text-emerald-500 align-top mt-1 inline-block"} "beta"]]
      [:button {:id "mobile-drawer-toggle"
                :type "button"
                :aria-label "Open menu"
                :aria-controls "mobile-drawer"
                :aria-expanded "false"
                :_ drawer-toggle-actions
                :class "p-2 text-zinc-400 hover:text-white rounded-lg hover:bg-zinc-800 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-emerald-400"}
       [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
        [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                :d "M4 6h16M4 12h16M4 18h16"}]]]]
     [:div {:id "mobile-drawer-overlay"
            :_ drawer-close-actions
            :class "fixed inset-0 bg-black/60 z-30 md:hidden opacity-0 pointer-events-none transition-opacity duration-200 [&[open]]:opacity-100 [&[open]]:pointer-events-auto"}]
     [:aside {:id "mobile-drawer"
              :class "fixed top-0 left-0 h-full w-64 bg-black z-40 md:hidden flex flex-col -translate-x-full invisible transition-transform duration-200 ease-out [&[open]]:translate-x-0 [&[open]]:visible"}
      [:div {:class "flex items-center justify-between min-h-14 px-4 border-b border-zinc-800 flex-shrink-0 pt-[env(safe-area-inset-top)]"}
       [:a {:href "/"}
        [:span {:class "text-xl font-giza font-bold text-white"} "greed."]
        [:span {:class "ml-1.5 text-xs font-medium text-emerald-500 align-top mt-1 inline-block"} "beta"]]
       [:button {:id "mobile-drawer-close"
                 :type "button"
                 :aria-label "Close menu"
                 :_ drawer-close-actions
                 :class "p-2 text-zinc-400 hover:text-white rounded-lg hover:bg-zinc-800 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-emerald-400"}
        [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                 :d "M6 18L18 6M6 6l12 12"}]]]]
      [:div {:class "flex flex-col flex-1 px-3 py-5 overflow-y-auto"}
       [:p {:class "px-3 mb-2 text-xs font-semibold text-zinc-500 uppercase tracking-wider"} "More"]
       [:nav {:class "space-y-0.5"}
        (for [item (remove :bottom? primary-links)]
          (nav-link (:href item) (:label item) (:icon item) (link-active? uri (:href item))))
        (when admin?
          (nav-link "/app/admin/users" "Users" (svgs/users) (link-active? uri "/app/admin/users")))]
       [:div {:class "mt-auto pt-4 border-t border-zinc-800"}
        [:nav {:class "space-y-0.5 mb-4"}
         [:a {:href "/app/settings"
              :class "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-zinc-400 hover:bg-zinc-800 hover:text-white transition-colors duration-150 active:opacity-70 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-500/70"}
          [:span {:class "w-5 h-5 flex-shrink-0 [&_svg]:w-full [&_svg]:h-full"} (svgs/cog)] "Settings"]]
        [:div {:class "flex items-center justify-between px-2 py-2"}
         [:a {:href "/app/profile" :class "flex items-center gap-2.5"}
          [:div {:class "w-8 h-8 rounded-full bg-emerald-600 flex items-center justify-center text-xs font-semibold text-white"}
           (str initials)]
          [:p {:class "text-xs font-medium text-zinc-300 truncate"} (str firstname " " lastname)]]
         [:a {:href "/logout" :class "p-1.5 text-zinc-500 hover:text-white hover:bg-zinc-800 rounded-md transition-colors"}
          (svgs/logout)]]]]]]))
