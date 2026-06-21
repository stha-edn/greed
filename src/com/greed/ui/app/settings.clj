(ns com.greed.ui.app.settings
  (:require [com.greed.ui :as ui]
            [com.greed.ui.components.forms :as forms]
            [com.greed.ui.components.headers :as headers]))


(defn page [ctx]
  (ui/app
   ctx
   [:div {:class "space-y-4"}
    (headers/pages-heading ["Settings"])
    [:div {:class "grid grid-cols-1 gap-4 lg:grid-cols-2 items-start"}
     (forms/finances ctx)
     (forms/tax-profile ctx)]]))
