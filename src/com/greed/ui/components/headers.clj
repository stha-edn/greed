(ns com.greed.ui.components.headers
  (:require [com.greed.ui.components.navs :as navs]))

(defn pages [ctx & content]
  [:header {:class "bg-white border-b border-zinc-200"}
   [:div {:class "container px-6 mx-auto"}
    (navs/nav ctx)
    content]])

(defn app [ctx]
  [:<>
   (navs/sidebar ctx)
   (navs/mobile-sidebar ctx)])

(defn pages-heading [breadcrumbs]
  [:div {:class "mb-6"}
   [:div {:class "flex items-center gap-1.5 text-sm text-zinc-400 mb-1"}
    [:a {:href "/app" :class "hover:text-zinc-600 transition-colors"} "Home"]
    (for [crumb breadcrumbs]
      [:<>
       [:span "/"]
       [:span {:class "text-zinc-600"} crumb]])]
   [:h1 {:class "text-xl font-semibold text-zinc-900"}
    (last breadcrumbs)]])

(defn home-heading [& {:keys [user date]}]
  [:div {:class "flex items-end justify-between gap-4"}
   [:div
    [:p {:class "text-sm font-medium text-emerald-600 mb-0.5"} "Welcome back"]
    [:h1 {:class "text-2xl sm:text-3xl font-bold tracking-tight text-zinc-900"}
     (str (:user/firstname user) " " (:user/lastname user))]]
   (when date
     [:p {:class "hidden sm:block text-sm font-medium text-zinc-400 tabular-nums whitespace-nowrap"} date])])
