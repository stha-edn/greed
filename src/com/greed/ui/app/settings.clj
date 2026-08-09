(ns com.greed.ui.app.settings
  (:require [com.greed.ui :as ui]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.forms :as forms]
            [com.greed.ui.components.headers :as headers]))


(defn page [ctx]
  (ui/app
   ctx
   [:div {:class "space-y-4"}
    (when (:alert (:params ctx)) (alerts/info (:params ctx)))
    (headers/pages-heading ["Settings"])
    [:div {:class "grid grid-cols-1 items-start gap-4 lg:grid-cols-2"}
     (forms/finances ctx)
     (forms/tax-profile ctx)]]))
