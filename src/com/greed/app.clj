(ns com.greed.app
  (:require [com.greed.middleware :as mid]
            [com.greed.ui.app.tools :as tools]
            [com.greed.ui.app.account :as account]
            [com.greed.ui.app.settings :as settings]
            [com.greed.ui.app.finances :as finances]
            [com.greed.ui.app.calendar :as calendar]
            [com.greed.ui.app.dashboard :as dashboard]
            [com.greed.ui.components.tables :as tables]
            [com.greed.ui.tools.income-tax-calculator :as income-tax-calculator]
            [com.greed.ui.tools.tax-returns :as tax-returns]))



(def module
  {:routes [["/app" {:middleware [mid/wrap-signed-in]}
             ["" {:get dashboard/page}]
             ["/finances"
              ["/" {:get finances/page}]
              ["/add-modal" {:get tables/add-modal}]
              ["/create-budget-item" {:post mid/create-budget-item}]
              ["/update-budget-item" {:post mid/update-budget-item}]
              ["/delete-budget-item" {:get mid/delete-budget-item}]]
             ["/calendar" {:get calendar/page}]
             ["/tools"
              ["" {:get tools/page}]
              ["/income-tax-calculator" {:get income-tax-calculator/page-get :post income-tax-calculator/page}]
              ["/tax-returns" {:get tax-returns/page :post tax-returns/result-page}]]
             ["/settings" {:get settings/page}]
             ["/profile" {:get account/page}]
             ["/save-user" {:post mid/save-user}]
             ["/save-finances" {:post mid/save-finances}]]]})
