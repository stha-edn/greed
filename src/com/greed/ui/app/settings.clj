(ns com.greed.ui.app.settings
  (:require [com.greed.ui :as ui]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.forms :as forms]
            [com.greed.ui.components.headers :as headers]))

(defn page [ctx]
  (ui/app
   ctx
   [:div {:class "space-y-7"}
    (headers/pages-heading ["Settings"] "Personal: your salary, payday and tax details.")
    [:div {:class "grid grid-cols-1 items-start gap-4 lg:grid-cols-2"}
     (forms/finances ctx)
     (forms/tax-profile ctx)]]))
