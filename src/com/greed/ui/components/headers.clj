(ns com.greed.ui.components.headers
  (:require [com.greed.ui.components.navs :as navs]))

(defn pages [ctx & content]
  [:header {:class "bg-white border-b border-slate-200"}
   [:div {:class "container px-6 mx-auto"}
    (navs/nav ctx)
    content]])

(defn app [ctx]
  [:<>
   (navs/sidebar ctx)
   (navs/mobile-sidebar ctx)])

(defn pages-heading [breadcrumbs]
  [:div {:class "mb-6"}
   [:div {:class "flex items-center gap-1.5 text-sm text-slate-400 mb-1"}
    [:a {:href "/app" :class "hover:text-slate-600 transition-colors"} "Home"]
    (for [crumb breadcrumbs]
      [:<>
       [:span "/"]
       [:span {:class "text-slate-600"} crumb]])]
   [:h1 {:class "text-xl font-semibold text-slate-900"}
    (last breadcrumbs)]])

(defn home-heading [& {:keys [user]}]
  [:div {:class "mb-6"}
   [:p {:class "text-sm text-slate-400 mb-0.5"} "Welcome back"]
   [:h1 {:class "text-xl font-semibold text-slate-900"}
    (str (:user/firstname user) " " (:user/lastname user))]])
