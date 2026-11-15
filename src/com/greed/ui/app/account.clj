(ns com.greed.ui.app.account
  (:require [com.greed.ui :as ui]
            [com.greed.ui.components.forms :as forms]
            [com.greed.ui.components.headers :as headers]))

(defn page [ctx]
  (ui/app
   ctx
   [:div {:class "space-y-7"}
    (headers/pages-heading ["Account"] "You, the person: your name, email, password and age. The same for every profile.")
    (forms/user ctx)]))
