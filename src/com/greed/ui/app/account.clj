(ns com.greed.ui.app.account
  (:require [com.greed.ui :as ui]
   [com.greed.ui.components.forms :as forms]
   [com.greed.ui.components.headers :as headers]))


(defn page [ctx]
  (ui/app
    ctx
    [:div {:class "space-y-4"}
     (headers/pages-heading ["Account" "Settings"])
     (forms/user ctx)]))
