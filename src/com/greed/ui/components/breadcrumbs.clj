(ns com.greed.ui.components.breadcrumbs
  (:require [com.greed.ui.components.svgs :as svgs]))

(defn- crumb
  "One crumb that isn't the current page. A `[label href]` pair renders as a
   link back to a parent section; a bare label renders as muted context."
  [item]
  (let [[label href] (if (string? item) [item nil] item)]
    (if href
      [:a {:href href
           :class "flex items-center text-zinc-500 hover:text-zinc-900 hover:underline underline-offset-2 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 active:text-zinc-700 active:scale-[0.97]"}
       label]
      [:span label])))

(defn breadcrumbs
  "App breadcrumb trail. Each crumb is a plain label or a `[label href]` link
   to a parent section. The final crumb names the current page (aria-current)
   and never links. A Home icon leads back to the app root."
  [crumbs]
  (let [last? (dec (count crumbs))]
    [:nav {:class "flex items-center gap-1.5 text-sm text-zinc-400"
           :aria-label "Breadcrumb"}
     [:a {:href "/app"
          :class "flex items-center transition hover:text-zinc-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 active:text-zinc-700 active:scale-[0.97]"
          :aria-label "Home"}
      (svgs/home {:class "size-4"})]
     (map-indexed
      (fn [i item]
        (let [label (if (string? item) item (first item))]
          [:<>
           (svgs/->next {:class "size-3.5 text-zinc-300"})
           (if (= i last?)
             [:span {:class "text-zinc-700 font-medium" :aria-current "page"} label]
             (crumb item))]))
      crumbs)]))
