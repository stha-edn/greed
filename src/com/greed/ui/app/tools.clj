(ns com.greed.ui.app.tools
  (:require [com.greed.ui :as ui]
            [com.greed.ui.tools.core :as tools]
            [com.greed.ui.components.headers :as headers]))


(defn page [ctx]
  (ui/app
   ctx
   [:div.container.mx-auto
    (headers/pages-heading ["Tools"])
    (tools/tools)]))
