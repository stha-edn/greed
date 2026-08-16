(ns com.greed.ui.app.tools
  (:require [com.greed.core :as c.greed]
            [com.greed.data.core :as data]
            [com.greed.ui :as ui]
            [com.greed.ui.tools.core :as tools]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.stats :as stats]))

(defn page [ctx]
  (let [user-id         (:uid (:session ctx))
        user            (data/get-user ctx user-id)
        finances        (data/get-finances ctx user-id)
        income-tax-data (c.greed/get-income-tax-data user finances)
        tax-profile     (data/get-tax-profile ctx user-id)
        has-income?     (pos? (or (:annual-income income-tax-data) 0))]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (headers/pages-heading ["Tax"] "What you earn, what SARS takes, what you keep.")
      (stats/tax-hero income-tax-data)
      (when has-income?
        [:div {:class "grid grid-cols-1 gap-4 lg:grid-cols-2 lg:items-stretch"}
         (stats/tax-bracket-breakdown income-tax-data)
         (stats/tax-readiness income-tax-data tax-profile)])
      (tools/tools)])))
