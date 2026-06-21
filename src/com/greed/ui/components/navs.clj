(ns com.greed.ui.components.navs
  (:require [com.greed.data.core :as data]
            [com.greed.ui.components.svgs :as svgs]))


(defn nav []
  [:nav
   {:class "flex flex-col py-6 sm:flex-row sm:justify-between sm:items-center"}
   [:a
    {:href "/"}
    [:span
     {:class "text-6xl font-giza font-semibold text-black md:text-8xl"}
     "greed."]]
   [:div
    {:class "flex items-center space-x-2 -mx-2 mt-2 sm:mt-0"}
    [:a
     {:href "/team",
      :class "px-3 py-1 text-sm font-semibold text-black transition-colors duration-300 transform rounded-md hover:bg-gray-100"}
     "Team"]
    [:a
     {:href "/signin",
      :class "px-3 py-1 text-sm font-semibold text-black transition-colors duration-300 transform border-2 border-black rounded-md hover:bg-gray-500"}
     "Sign In"]
    [:a
     {:href "/signup",
      :class "px-3 py-1 text-sm font-semibold text-white transition-colors duration-300 transform border-2 border-black bg-black rounded-md hover:bg-gray-500 hover:text-black"}
     "Sign Up"]]])

(defn sidebar [{:keys [session] :as ctx}]
  (let [user-id (:uid session)
        {:user/keys [firstname
                     lastname]} (data/get-user ctx user-id)]
    [:aside
     {:class "hidden md:flex flex-col w-72 h-screen px-4 py-8 overflow-y-auto bg-white border-r"
      #_#_:x-show "open"
      #_#_:x-transition:enter "transition transform duration-300"
      #_#_:x-transition:enter-start "-translate-x-full"
      #_#_:x-transition:enter-end "translate-x-0"
      #_#_:x-transition:leave "transition transform duration-300"
      #_#_:x-transition:leave-start "translate-x-0"
      #_#_:x-transition:leave-end "-translate-x-full"}
     [:.
      [:span
       {:class "text-6xl font-giza font-semibold text-black md:text-8xl"}
       "greed."]]
     [:div
      {:class "relative mt-6"}
      [:span
       {:class "absolute inset-y-0 left-0 flex items-center pl-3"}
       (svgs/search)]
      [:input
       {:type "text",
        :class "w-full py-2 pl-10 pr-4 text-gray-700 bg-white border rounded-md focus:border-blue-400 focus:ring-blue-300 focus:ring-opacity-40 focus:outline-none focus:ring",
        :placeholder "Search"}]]
     [:div
      {:class "flex flex-col justify-between flex-1 mt-6"}
      [:nav
       {:x-data "{ currentPath: window.location.pathname }"}
       [:a
        {:class "flex items-center px-4 py-2 text-gray-600 transition-colors duration-300 transform rounded-md hover:bg-gray-100 hover:text-gray-700",
         :x-bind:class "currentPath === '/app' ? 'bg-gray-100 text-gray-700' : ''",
         :href "/"}
        (svgs/dashboard)
        [:span {:class "mx-4 font-medium"} "Dashboard"]]
       [:a
        {:class "flex items-center px-4 py-2 mt-5 text-gray-600 transition-colors duration-300 transform rounded-md hover:bg-gray-100 hover:text-gray-700",
         :x-bind:class "currentPath === '/app/finances' ? 'bg-gray-100 text-gray-700' : ''",
         :href "/app/finances"}
        (svgs/credit-card)
        [:span {:class "mx-4 font-medium"} "Finances"]]
       [:a
        {:class "flex items-center px-4 py-2 mt-5 text-gray-600 transition-colors duration-300 transform rounded-md hover:bg-gray-100 hover:text-gray-700",
         :x-bind:class "currentPath === '/app/calendar' ? 'bg-gray-100 text-gray-700' : ''",
         :href "/app/calendar"}
        (svgs/calendar)
        [:span {:class "mx-4 font-medium"} "Calendar"]]
       [:a
        {:class "flex items-center px-4 py-2 mt-5 text-gray-600 transition-colors duration-300 transform rounded-md hover:bg-gray-100 hover:text-gray-700",
         :x-bind:class "currentPath === '/app/tools' ? 'bg-gray-100 text-gray-700' : ''",
         :href "/app/tools"}
        (svgs/tools)
        [:span {:class "mx-4 font-medium"} "Tools"]]
       [:hr {:class "my-6 border-gray-200"}]
       [:a
        {:class "flex items-center px-4 py-2 mt-5 text-gray-600 transition-colors duration-300 transform rounded-md hover:bg-gray-100 hover:text-gray-700",
         :x-bind:class "currentPath === '/app/settings' ? 'bg-gray-100 text-gray-700' : ''"
         :href "/app/settings"}
        (svgs/cog)
        [:span {:class "mx-4 font-medium"} "Settings"]]]
      [:div
       {:class "flex items-center justify-between mt-6"}
       [:a
        {:href "/app/profile",
         :class "flex item-center justify-between px-2"}
        [:span
         {:class "font-medium text-sm text-gray-800 px-2 py-1 border-b-2 border-black hover:border-2 hover:rounded-md transition duration-300"}
         (str firstname " " lastname)]]
       [:a
        {:href "/logout"
         :class "flex items-center px-2 py-1 border-2 border-black rounded-md hover:bg-black hover:text-white"}
        (svgs/logout)]]]]))

(defn mobile-sidebar [{:keys [session] :as ctx}]
  (let [user-id (:uid session)
        {:user/keys [firstname
                     lastname]} (data/get-user ctx user-id)]
    [:div
     {:x-data "{ open: false}"}
     ;; Hamburger button (mobile only)
     [:button
      {:class "md:hidden fixed top-4 left-4 z-20"
       "@click" "open = true"
       :ariaLabel "Open sidebar"}
      [:span
       {:class "text-6xl font-giza font-semibold text-black md:text-8xl"}
       "greed."]]
     ;; Overlay (mobile only, when sidebar is open)
     [:div
      {:x-show "open"
       :class "fixed inset-0 bg-black bg-opacity-40 z-10 md:hidden"
       "@click" "open = false"}]
     ;; Sidebar
     [:aside
      {:class (str "flex flex-col w-72 h-screen px-4 py-8 overflow-y-auto bg-white border-r z-20 fixed top-0 left-0 transition-transform duration-300"
                   " "
                   "md:static md:translate-x-0")
       :x-show "open"
       :x-transition:enter "transition transform duration-300"
       :x-transition:enter-start "-translate-x-full"
       :x-transition:enter-end "translate-x-0"
       :x-transition:leave "transition transform duration-300"
       :x-transition:leave-start "translate-x-0"
       :x-transition:leave-end "-translate-x-full"}
      [:.
       [:button {"@click" "open = false"
                 :class "p-2"}
        [:span
         {:class "text-6xl font-giza font-semibold text-black md:text-8xl"}
         "greed."]]]
      [:div
       {:class "relative mt-6"}
       [:span
        {:class "absolute inset-y-0 left-0 flex items-center pl-3"}
        (svgs/search)]
       [:input
        {:type "text",
         :class "w-full py-2 pl-10 pr-4 text-gray-700 bg-white border rounded-md focus:border-blue-400 focus:ring-blue-300 focus:ring-opacity-40 focus:outline-none focus:ring",
         :placeholder "Search"}]]
      [:div
       {:class "flex flex-col justify-between flex-1 mt-6"}
       [:nav
        {:x-data "{ currentPath: window.location.pathname }"}
        [:a
         {:class "flex items-center px-4 py-2 text-gray-600 transition-colors duration-300 transform rounded-md hover:bg-gray-100 hover:text-gray-700",
          :x-bind:class "currentPath === '/app' ? 'bg-gray-100 text-gray-700' : ''",
          :href "/"}
         (svgs/dashboard)
         [:span {:class "mx-4 font-medium"} "Dashboard"]]
        [:a
        {:class "flex items-center px-4 py-2 mt-5 text-gray-600 transition-colors duration-300 transform rounded-md hover:bg-gray-100 hover:text-gray-700",
         :x-bind:class "currentPath === '/app/finances' ? 'bg-gray-100 text-gray-700' : ''",
         :href "/app/finances"}
        (svgs/credit-card)
        [:span {:class "mx-4 font-medium"} "Finances"]]
        [:a
         {:class "flex items-center px-4 py-2 mt-5 text-gray-600 transition-colors duration-300 transform rounded-md hover:bg-gray-100 hover:text-gray-700",
          :x-bind:class "currentPath === '/app/calendar' ? 'bg-gray-100 text-gray-700' : ''",
          :href "/app/calendar"}
         (svgs/calendar)
         [:span {:class "mx-4 font-medium"} "Calendar"]]
        [:a
         {:class "flex items-center px-4 py-2 mt-5 text-gray-600 transition-colors duration-300 transform rounded-md hover:bg-gray-100 hover:text-gray-700",
          :x-bind:class "currentPath === '/app/tools' ? 'bg-gray-100 text-gray-700' : ''",
          :href "/app/tools"}
         (svgs/tools)
         [:span {:class "mx-4 font-medium"} "Tools"]]
        [:hr {:class "my-6 border-gray-200"}]
        [:a
         {:class "flex items-center px-4 py-2 mt-5 text-gray-600 transition-colors duration-300 transform rounded-md hover:bg-gray-100 hover:text-gray-700",
          :x-bind:class "currentPath === '/app/settings' ? 'bg-gray-100 text-gray-700' : ''"
          :href "/app/settings"}
         (svgs/cog)
         [:span {:class "mx-4 font-medium"} "Settings"]]]
       [:div
        {:class "flex items-center justify-between mt-6"}
        [:a
         {:href "/app/profile",
          :class "flex item-center justify-between px-2"}
         [:span
          {:class "font-medium text-sm text-gray-800 px-2 py-1 border-b-2 border-black hover:border-2 hover:rounded-md transition duration-300"}
          (str firstname " " lastname)]]
        [:a
         {:href "/logout"
          :class "flex items-center px-2 py-1 border-2 border-black rounded-md hover:bg-black hover:text-white"}
         (svgs/logout)]]]]]))
