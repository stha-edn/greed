(ns com.greed.ui.components.breadcrumbs
  (:require [com.greed.ui.components.svgs :as svgs]))

(defn breadcrumbs
  "App breadcrumb trail. Each crumb is a plain label; the final crumb is the
   current page (aria-current) and the rest are non-clickable context.
   A Home link leads back to the app root."
  [crumbs]
  [:nav {:class "flex items-center gap-1.5 text-sm text-zinc-400"
         :aria-label "Breadcrumb"}
   [:a {:href "/app"
        :class "flex items-center hover:text-zinc-600 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 active:text-zinc-700"
        :aria-label "Home"}
    (svgs/home {:class "size-4"})]
   (map-indexed
    (fn [i crumb]
      [:<>
       (svgs/->next {:class "size-3.5 text-zinc-300"})
       (if (= i (dec (count crumbs)))
         [:span {:class "text-zinc-600" :aria-current "page"} crumb]
         [:span crumb])])
    crumbs)])
