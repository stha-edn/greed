(ns com.greed.ui.components.navs
  (:require [com.greed.data.core :as data]
            [com.greed.ui.components.svgs :as svgs]))

(def ^:private public-menu-toggle-actions
  "hyperscript for the public-nav hamburger (mobile only): toggles the `open`
   attribute on the compact link menu, mirroring the app drawer's pattern."
  (str "on click\n"
       "  if the @open of #public-nav-menu is 'true'\n"
       "    remove @open from #public-nav-menu\n"
       "    set the @aria-expanded of #public-nav-toggle to 'false'\n"
       "  else\n"
       "    add @open='true' to #public-nav-menu\n"
       "    set the @aria-expanded of #public-nav-toggle to 'true'\n"
       "  end"))

(def ^:private public-menu-escape-actions
  "hyperscript for the nav wrapper: Escape closes the public-nav menu."
  (str "on keydown[key == 'Escape'] from window\n"
       "  if the @open of #public-nav-menu is 'true'\n"
       "    remove @open from #public-nav-menu\n"
       "    set the @aria-expanded of #public-nav-toggle to 'false'\n"
       "  end"))

(def ^:private public-links
  [["/#features" "Features"] ["/#business" "Business"] ["/pricing" "Pricing"] ["/about" "About"] ["/contact" "Contact"]])

(defn nav [{:keys [session uri]}]
  (let [signed-in? (some? (:uid session))]
    [:nav {:class "relative flex items-center justify-between gap-3 py-3.5" :_ public-menu-escape-actions}
     [:a {:href "/" :aria-label "greed home"
          :class "inline-flex items-center rounded-xl transition-transform active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"}
      [:img {:src "/img/greed-symbol-green-tight.svg" :alt "greed" :width 192 :height 192 :class "h-9 w-auto"}]]
     [:ul {:class "absolute left-1/2 hidden -translate-x-1/2 items-center gap-1 rounded-full bg-zinc-100/80 p-1 ring-1 ring-zinc-200/70 md:flex"}
      (for [[href label] public-links]
        [:li
         [:a {:href href
              :class (str "block whitespace-nowrap rounded-full px-4 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 "
                          (if (= uri href) "bg-white text-zinc-900 shadow-card" "text-zinc-600 hover:text-zinc-900"))}
          label]])]
     [:div {:class "flex items-center gap-2"}
      (if signed-in?
        [:a {:href "/app"
             :class "whitespace-nowrap rounded-full bg-brand-600 px-4 py-2 text-sm font-semibold text-white shadow-[0_8px_20px_-10px_rgba(15,107,79,.8)] transition hover:bg-brand-700 active:scale-[0.97]"}
         "Your account"]
        [:<>
         [:a {:href "/signin"
              :class "hidden whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold text-zinc-700 transition hover:bg-zinc-100 hover:text-zinc-900 active:scale-[0.97] sm:inline-block"}
          "Sign in"]
         [:a {:href "/signup"
              :class "whitespace-nowrap rounded-full bg-brand-600 px-4 py-2 text-sm font-semibold text-white shadow-[0_8px_20px_-10px_rgba(15,107,79,.8)] transition hover:bg-brand-700 active:scale-[0.97]"}
          "Start free"]])
      [:button {:id "public-nav-toggle"
                :type "button"
                :aria-label "Open menu"
                :aria-haspopup "true"
                :aria-controls "public-nav-menu"
                :aria-expanded "false"
                :_ public-menu-toggle-actions
                :class "rounded-full p-2 text-zinc-600 transition-colors hover:bg-zinc-100 hover:text-zinc-900 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-500/70 md:hidden"}
       [:svg {:class "h-5 w-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
        [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "1.8"
                :d "M4 6h16M4 12h16M4 18h16"}]]]]
     [:div {:id "public-nav-menu"
            :class "absolute right-0 top-full z-50 mt-2 w-56 origin-top-right rounded-2xl bg-white p-2 opacity-0 shadow-card-hover ring-1 ring-zinc-200/70 scale-95 -translate-y-1 pointer-events-none transition duration-150 ease-out md:hidden [&[open]]:pointer-events-auto [&[open]]:translate-y-0 [&[open]]:scale-100 [&[open]]:opacity-100"}
      (for [[href label] public-links]
        [:a {:href href :class "block rounded-xl px-4 py-2.5 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50 hover:text-zinc-900"} label])
      (when-not signed-in?
        [:a {:href "/signin" :class "mt-1 block rounded-xl border-t border-zinc-100 px-4 py-2.5 text-sm font-semibold text-zinc-900 hover:bg-zinc-50"} "Sign in"])]]))

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
       :class (str "group relative flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors duration-150 active:opacity-70 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-zinc-400"
                   (if active?
                     " bg-zinc-200/70 text-zinc-900"
                     " text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900"))}
   [:span {:class "w-5 h-5 flex-shrink-0 [&_svg]:w-full [&_svg]:h-full"} icon]
   label])

(def ^:private shared-links
  "The pages every profile has, with the same names, in the same order.
   Each page shows the current profile's own money (Personal or a business)."
  [{:href "/app"           :label "Home"     :icon (svgs/squares-2x2)   :bottom? true}
   {:href "/app/finances/" :label "Budget"   :icon (svgs/credit-card)   :bottom? true}
   {:href "/app/spending"  :label "Spending" :icon (svgs/banknotes)     :bottom? true}
   {:href "/app/banking"   :label "Banking"  :icon (svgs/wallet)        :bottom? false}
   {:href "/app/tax"       :label "Tax"      :icon (svgs/percent-badge) :bottom? true}])

(def ^:private planning-links
  "Pages for planning and reading the month."
  [{:href "/app/calendar"  :label "Calendar" :icon (svgs/calendar)  :bottom? true}
   {:href "/app/goals"     :label "Goals"    :icon (svgs/target)    :bottom? false}
   {:href "/app/insights"  :label "Insights" :icon (svgs/chart-bar) :bottom? false}])

(defn- app-links
  "The app's primary links for the current profile: the shared pages first,
   then a VAT vendor's VAT page, then the planning pages. The mobile bottom
   bar takes the :bottom? ones (five at most); the drawer lists the rest."
  [business]
  (let [vat (when (:business/vat-registered business)
              [{:href "/app/vat" :label "VAT" :icon (svgs/percent-badge) :bottom? true}])
        links (concat shared-links vat planning-links)
        bottom (set (map :href (take 5 (filter :bottom? links))))]
    (map #(assoc % :bottom? (contains? bottom (:href %))) links)))

(defn- profile-switcher
  "Which profile the person is in (Personal or a business), shown to
   everyone however they signed up: with a business, a link to switch;
   without one yet, a link to add one."
  [ctx business]
  (let [has-business? (seq (data/get-user-businesses ctx (get-in ctx [:session :uid])))]
    [:div {:class "mb-4 flex items-center justify-between gap-2 rounded-lg bg-white ring-1 ring-zinc-200/70 px-3 py-2.5"}
     [:div {:class "min-w-0"}
      [:p {:class "truncate text-sm font-medium text-zinc-900"} (if business (:business/name business) "Personal")]
      [:p {:class "truncate text-xs text-zinc-500"}
       (if business (data/entity-types (:business/entity-type business)) "Your own money")]]
     [:a {:href (if has-business? "/app/choose" "/app/choose?add=1#add-business")
          :class "flex-shrink-0 rounded-md px-2 py-1 text-xs font-medium text-zinc-700 underline-offset-4 hover:bg-zinc-100 hover:text-zinc-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"}
      (if has-business? "Switch" "Add a business")]]))

(defn- mobile-profile-link
  "The current profile in the mobile top bar: a tap opens Continue as, where
   you switch or add a business."
  [business]
  [:a {:href "/app/choose"
       :aria-label (str "Profile: " (if business (:business/name business) "Personal") ". Switch or add a business")
       :class "ml-3 mr-auto inline-flex min-w-0 max-w-[45vw] items-center gap-1 rounded-md border border-zinc-200 bg-white px-2 py-1 text-xs font-medium text-zinc-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"}
   [:span {:class "truncate"} (if business (:business/name business) "Personal")]
   [:svg {:class "h-3 w-3 flex-shrink-0 text-zinc-500" :viewBox "0 0 20 20" :fill "currentColor" :aria-hidden "true"}
    [:path {:fill-rule "evenodd" :clip-rule "evenodd"
            :d "M5.23 7.21a.75.75 0 011.06.02L10 11.17l3.71-3.94a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z"}]]])

(defn- sidebar-inner [uri firstname lastname admin? ctx business]
  (let [initials (str (or (first firstname) \?) (or (first lastname) \?))]
    [:<>
     [:div {:class "flex items-center h-16 px-5 border-b border-zinc-200 flex-shrink-0"}
      [:a {:href "/" :class "inline-flex items-start"}
       [:img {:src "/img/greed-symbol-green-tight.svg"
              :alt "greed"
              :width 192 :height 192
              :class "h-8 w-auto"}]
       [:span {:class "ml-1.5 text-xs font-medium leading-none text-zinc-500"} "beta"]]]
     [:div {:class "flex flex-col flex-1 px-3 py-5 overflow-y-auto min-h-0"}
      (profile-switcher ctx business)
      [:nav {:class "space-y-0.5"}
       (for [item (app-links business)]
         (nav-link (:href item) (:label item) (:icon item) (link-active? uri (:href item))))
       (when admin?
         (nav-link "/app/admin/users" "Users" (svgs/users) (link-active? uri "/app/admin/users")))]
      [:div {:class "mt-auto pt-4 border-t border-zinc-200"}
       [:nav {:class "space-y-0.5 mb-4"}
        (nav-link "/app/plan" "Plan" (svgs/gift) (link-active? uri "/app/plan"))
        [:a {:href "/app/settings"
             :class "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900 transition-colors duration-150 active:opacity-70 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-zinc-400"}
         [:span {:class "w-5 h-5 flex-shrink-0 [&_svg]:w-full [&_svg]:h-full"} (svgs/cog)]
         "Settings"]]
       [:div {:class "flex items-center justify-between px-2 py-2"}
        [:a {:href "/app/profile" :class "flex items-center gap-2.5 min-w-0 group"}
         [:div {:class "flex-shrink-0 w-8 h-8 rounded-full bg-zinc-200 flex items-center justify-center text-xs font-semibold text-zinc-700"}
          (str initials)]
         [:div {:class "min-w-0"}
          [:p {:class "text-xs font-medium text-zinc-700 truncate group-hover:text-zinc-900 transition-colors"}
           (str firstname " " lastname)]
          [:p {:class "text-[11px] text-zinc-500"} "Account"]]]
        [:a {:href "/logout"
             :class "flex-shrink-0 p-1.5 text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100 rounded-md transition-colors"
             :title "Sign out"}
         (svgs/logout)]]]]]))

(defn sidebar [{:keys [session uri] :as ctx}]
  (let [user-id (:uid session)
        user (data/get-user ctx user-id)
        {:user/keys [firstname lastname]} user]
    [:aside {:class "hidden md:flex flex-col w-64 h-dvh bg-zinc-50 border-r border-zinc-200 fixed top-0 left-0 z-30"}
     (sidebar-inner uri firstname lastname (data/admin? user) ctx (data/session-business ctx))]))

(defn- bottom-tab [href label icon active?]
  [:a {:href href
       :aria-current (when active? "page")
       :class (str "flex flex-1 flex-col items-center justify-center gap-0.5 py-2 text-[11px] font-medium transition-colors active:opacity-60 active:scale-[0.96] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-zinc-400"
                   (if active?
                     " text-zinc-900"
                     " text-zinc-500 hover:text-zinc-900"))}
   [:span {:class "w-6 h-6 flex items-center justify-center [&_svg]:w-full [&_svg]:h-full"} icon]
   label])

(defn mobile-bottom-nav [{:keys [uri] :as ctx}]
  [:nav {:class "md:hidden fixed bottom-0 left-0 right-0 z-20 bg-white border-t border-zinc-200 flex pb-[env(safe-area-inset-bottom)]"}
   (for [item (filter :bottom? (app-links (data/session-business ctx)))]
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
     [:div {:class "md:hidden fixed top-0 left-0 right-0 min-h-14 bg-zinc-50 border-b border-zinc-200 z-20 flex items-center justify-between px-4 pt-[env(safe-area-inset-top)]"}
      [:a {:href "/" :class "inline-flex items-start"}
       [:img {:src "/img/greed-symbol-green-tight.svg"
              :alt "greed"
              :width 192 :height 192
              :class "h-7 w-auto"}]
       [:span {:class "ml-1.5 text-xs font-medium leading-none text-zinc-500"} "beta"]]
      (mobile-profile-link (data/session-business ctx))
      [:button {:id "mobile-drawer-toggle"
                :type "button"
                :aria-label "Open menu"
                :aria-controls "mobile-drawer"
                :aria-expanded "false"
                :_ drawer-toggle-actions
                :class "p-2 text-zinc-600 hover:text-zinc-900 rounded-lg hover:bg-zinc-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-zinc-400"}
       [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
        [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "1.8"
                :d "M4 6h16M4 12h16M4 18h16"}]]]]
     [:div {:id "mobile-drawer-overlay"
            :_ drawer-close-actions
            :class "fixed inset-0 bg-zinc-900/40 z-30 md:hidden opacity-0 pointer-events-none transition-opacity duration-200 [&[open]]:opacity-100 [&[open]]:pointer-events-auto"}]
     [:aside {:id "mobile-drawer"
              :class "fixed top-0 left-0 h-full w-64 bg-zinc-50 shadow-xl z-40 md:hidden flex flex-col -translate-x-full invisible transition-transform duration-200 ease-out [&[open]]:translate-x-0 [&[open]]:visible"}
      [:div {:class "flex items-center justify-between min-h-14 px-4 border-b border-zinc-200 flex-shrink-0 pt-[env(safe-area-inset-top)]"}
       [:a {:href "/" :class "inline-flex items-start"}
        [:img {:src "/img/greed-symbol-green-tight.svg"
               :alt "greed"
               :width 192 :height 192
               :class "h-7 w-auto"}]
        [:span {:class "ml-1.5 text-xs font-medium leading-none text-zinc-500"} "beta"]]
       [:button {:id "mobile-drawer-close"
                 :type "button"
                 :aria-label "Close menu"
                 :_ drawer-close-actions
                 :class "p-2 text-zinc-600 hover:text-zinc-900 rounded-lg hover:bg-zinc-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-zinc-400"}
        [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "1.8"
                 :d "M6 18L18 6M6 6l12 12"}]]]]
      [:div {:class "flex flex-col flex-1 px-3 py-5 overflow-y-auto"}
       (profile-switcher ctx (data/session-business ctx))
       [:p {:class "px-3 mb-2 text-xs font-medium text-zinc-500"} "More"]
       [:nav {:class "space-y-0.5"}
        (for [item (remove :bottom? (app-links (data/session-business ctx)))]
          (nav-link (:href item) (:label item) (:icon item) (link-active? uri (:href item))))
        (when admin?
          (nav-link "/app/admin/users" "Users" (svgs/users) (link-active? uri "/app/admin/users")))]
       [:div {:class "mt-auto pt-4 border-t border-zinc-200"}
        [:nav {:class "space-y-0.5 mb-4"}
         (nav-link "/app/plan" "Plan" (svgs/gift) (link-active? uri "/app/plan"))
         [:a {:href "/app/settings"
              :class "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900 transition-colors duration-150 active:opacity-70 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-zinc-400"}
          [:span {:class "w-5 h-5 flex-shrink-0 [&_svg]:w-full [&_svg]:h-full"} (svgs/cog)] "Settings"]]
        [:div {:class "flex items-center justify-between px-2 py-2"}
         [:a {:href "/app/profile" :class "flex items-center gap-2.5"}
          [:div {:class "w-8 h-8 rounded-full bg-zinc-200 flex items-center justify-center text-xs font-semibold text-zinc-700"}
           (str initials)]
          [:span {:class "min-w-0"}
           [:span {:class "block text-xs font-medium text-zinc-700 truncate"} (str firstname " " lastname)]
           [:span {:class "block text-[11px] text-zinc-500"} "Account"]]]
         [:a {:href "/logout" :class "p-1.5 text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100 rounded-md transition-colors"}
          (svgs/logout)]]]]]]))
