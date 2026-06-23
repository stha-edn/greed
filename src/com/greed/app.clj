(ns com.greed.app
  (:require [com.greed.middleware :as mid]
            [com.greed.ui.app.tools :as tools]
            [com.greed.ui.app.goals :as goals]
            [com.greed.ui.app.account :as account]
            [com.greed.ui.app.settings :as settings]
            [com.greed.ui.app.finances :as finances]
            [com.greed.ui.app.calendar :as calendar]
            [com.greed.ui.app.insights :as insights]
            [com.greed.ui.app.dashboard :as dashboard]
            [com.greed.ui.components.tables :as tables]
            [com.greed.ui.tools.income-tax-calculator :as income-tax-calculator]
            [com.greed.ui.tools.bonus-tax-calculator :as bonus-tax-calculator]
            [com.greed.ui.tools.tax-returns :as tax-returns]))

(defn- redirect-to [location]
  (fn [_] {:status 301 :headers {"location" location}}))



(def module
  {:routes [["/app" {:middleware [mid/wrap-signed-in]}
             ["" {:get dashboard/page}]
             ["/finances"
              ["/" {:get finances/page}]
              ["/add-modal" {:get tables/add-modal}]
              ["/create-budget-item" {:post mid/create-budget-item}]
              ["/update-budget-item" {:post mid/update-budget-item}]
              ["/delete-budget-item" {:get mid/delete-budget-item}]]
             ["/goals"
              ["" {:get goals/page}]
              ["/create-goal" {:post mid/create-goal}]
              ["/update-goal" {:post mid/update-goal}]
              ["/delete-goal" {:post mid/delete-goal}]]
             ["/insights" {:get insights/page}]
             ["/calendar" {:get calendar/page}]
             ["/calendar/grid" {:get calendar/calendar-grid}]
             ["/calendar/create-event" {:post calendar/create-event}]
             ["/calendar/delete-event" {:post calendar/delete-event}]
             ["/tax"
              ["" {:get tools/page}]
              ["/income-tax-calculator" {:get income-tax-calculator/page-get :post income-tax-calculator/page}]
              ["/bonus-tax-calculator" {:get bonus-tax-calculator/page-get :post bonus-tax-calculator/page}]
              ["/tax-returns" {:get tax-returns/page :post tax-returns/result-page}]]
             ;; Legacy redirects — Tools was renamed to Tax.
             ["/tools" {:get (redirect-to "/app/tax")}]
             ["/tools/income-tax-calculator" {:get (redirect-to "/app/tax/income-tax-calculator")}]
             ["/tools/tax-returns" {:get (redirect-to "/app/tax/tax-returns")}]
             ["/settings" {:get settings/page}]
             ["/profile" {:get account/page}]
             ["/save-user" {:post mid/save-user}]
             ["/save-finances" {:post mid/save-finances}]
             ["/save-tax-profile" {:post mid/save-tax-profile}]]]})
