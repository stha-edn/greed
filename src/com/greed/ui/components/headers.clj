(ns com.greed.ui.components.headers
  (:require [com.greed.ui.components.breadcrumbs :as breadcrumbs]
   [com.greed.ui.components.navs :as navs]
   [clojure.string :as string]))

(defn pages [ctx & content]
  [:header {:class "site-header sticky top-0 z-40 shadow-sm shadow-zinc-900/5"}
   [:div {:class "container px-6 mx-auto"}
    (navs/nav ctx)
    content]])

(defn app [ctx]
  [:<>
   (navs/sidebar ctx)
   (navs/mobile-sidebar ctx)
   (navs/mobile-bottom-nav ctx)])

(defn pages-heading [crumbs & [subtitle]]
  (let [current (last crumbs)
        label   (if (string? current) current (first current))]
    [:div {:class "mb-6"}
     (breadcrumbs/breadcrumbs crumbs)
     [:h1 {:class "mt-1 text-xl font-semibold tracking-tight text-zinc-900"}
      label]
     (when subtitle
       [:p {:class "mt-0.5 text-sm text-zinc-500"} subtitle])]))

(defn- greeting []
  (let [hour (.getHour (java.time.LocalTime/now))]
    (cond
      (< hour 12) "Good morning"
      (< hour 17) "Good afternoon"
      :else "Good evening")))

(defn home-heading [& {:keys [user date]}]
  [:div {:class "flex items-end justify-between gap-4"}
   [:div
    [:p {:class "text-sm font-medium text-emerald-600 mb-1"}
     (str (greeting) ", " (string/capitalize (:user/firstname user)))]]
   (when date
     [:p {:class "hidden sm:block text-sm font-medium text-zinc-400 tabular-nums whitespace-nowrap"} date])])
