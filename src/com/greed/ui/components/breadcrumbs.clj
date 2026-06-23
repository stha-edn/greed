(ns com.greed.ui.components.breadcrumbs
  (:require [com.greed.ui.components.svgs :as svgs]))


(defn breadcumb [breadcrumbs]
  [:div
   {:class "flex items-center overflow-x-auto whitespace-nowrap"}
   (svgs/home)
   (for [breadcrumb breadcrumbs]
     [:a
      {:href "#", :class "text-zinc-600 dark:text-zinc-200"}
      [:span
       {:class "mx-5 text-zinc-500"}
       (svgs/->next)]
      [:a
       {:href "#",
        :class "text-zinc-600 hover:underline"}
       breadcrumb]])])
