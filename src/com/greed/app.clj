(ns com.greed.app
  (:require [com.greed.middleware :as mid]
            [com.greed.data.core :as data]
            [com.greed.ui.app.admin :as admin]
            [com.greed.ui.app.tools :as tools]
            [com.greed.ui.app.goals :as goals]
            [com.greed.ui.app.account :as account]
            [com.greed.ui.app.settings :as settings]
            [com.greed.ui.app.finances :as finances]
            [com.greed.ui.app.statements :as spending]
            [com.greed.ui.app.banking :as banking]
            [com.greed.ui.app.profiles :as profiles]
            [com.greed.ui.app.plan :as plan]
            [com.greed.payments.billing :as billing]
            [com.greed.ui.app.vat :as vat]
            [com.greed.ui.app.company-tax :as company-tax]
            [com.greed.ui.app.calendar :as calendar]
            [com.greed.ui.app.insights :as insights]
            [com.greed.ui.app.dashboard :as dashboard]
            [com.greed.ui.components.forms :as forms]
            [com.greed.ui.tools.income-tax-calculator :as income-tax-calculator]
            [com.greed.ui.tools.bonus-tax-calculator :as bonus-tax-calculator]
            [com.greed.ui.tools.tax-returns :as tax-returns]))

(defn- redirect-to [location]
  (fn [_] {:status 301 :headers {"location" location}}))

(defn- for-profile
  "One address for a page every profile has: the business version while a
   business is chosen, else the personal one."
  [business-page personal-page]
  (fn [ctx]
    (if (data/session-business ctx) (business-page ctx) (personal-page ctx))))

(def module
  {:routes [["/app" {:middleware [mid/wrap-signed-in mid/wrap-plan]}
             ["" {:get dashboard/page}]
             ["/choose" {:get profiles/page :post profiles/choose}]
             ["/plan"
              ["" {:get plan/page}]
              ["/trial" {:post plan/start-trial}]
              ["/checkout" {:post billing/checkout}]
              ["/return" {:get billing/return}]
              ["/manage" {:get billing/manage}]]
             ["/vat" {:get vat/page}]
             ;; Tax and Settings: one address each, for whichever profile is chosen
             ["/tax" {:get (for-profile company-tax/page tools/page)}]
             ["/settings" {:get (for-profile profiles/settings-page settings/page)}]
             ["/company-tax" {:get (redirect-to "/app/tax")}]
             ["/invites/accept" {:post profiles/accept-invite}]
             ["/invites/decline" {:post profiles/decline-invite}]
             ["/businesses/create" {:post profiles/add-business}]
             ["/business"
              ["" {:get (redirect-to "/app/settings")}]
              ["/update" {:post profiles/update-details}]
              ["/vat" {:post profiles/update-vat}]
              ["/tax" {:post profiles/update-tax}]
              ["/shares" {:post profiles/update-shares}]
              ["/invite" {:post profiles/invite}]
              ["/invites/cancel" {:post profiles/cancel-invite}]
              ["/members/role" {:post profiles/change-role}]
              ["/members/remove" {:post profiles/remove-member}]
              ["/delete" {:post profiles/delete-business}]]
             ;; Budget and Spending show the chosen business's, or personal ones.
             ["/finances"
              ["/" {:get finances/page}]
              ["/account-type-options" {:get forms/account-type-field}]
              ["/create-budget-item" {:post mid/create-budget-item}]
              ["/update-budget-item" {:post mid/update-budget-item}]
              ["/delete-budget-item" {:post mid/delete-budget-item}]
              ["/make-monthly" {:post mid/make-monthly}]]
             ["/spending"
              ["" {:get spending/page}]
              ;; uploads moved to Banking; the old address still works
              ["/upload" {:post banking/upload}]
              ["/sync" {:post spending/sync-budget}]
              ["/add-income" {:post spending/add-income}]
              ["/add-expense" {:post spending/add-expense}]
              ["/delete" {:post spending/delete}]]
             ["/banking"
              ["" {:get banking/page}]
              ["/upload" {:post banking/upload}]
              ["/save-bank" {:post banking/save-bank}]]
             ;; Goals, Insights and Calendar: the current profile's, Personal or a business
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
             ["/calendar/toggle-event" {:post calendar/toggle-event}]
             ;; Personal pages: while a business is chosen they go to its home.
             ["" {:middleware [profiles/wrap-personal]}
             ["/tax"
              ["/income-tax-calculator" {:get income-tax-calculator/page-get :post income-tax-calculator/page}]
              ["/bonus-tax-calculator" {:get bonus-tax-calculator/page-get :post bonus-tax-calculator/page}]
              ;; the ITR12 simulator is a Premium tool
              ["/tax-returns" {:middleware [mid/wrap-premium]
                               :get tax-returns/page-get :post tax-returns/page}]]
             ;; Legacy redirects — Tools was renamed to Tax.
             ["/tools" {:get (redirect-to "/app/tax")}]
             ["/tools/income-tax-calculator" {:get (redirect-to "/app/tax/income-tax-calculator")}]
             ["/tools/tax-returns" {:get (redirect-to "/app/tax/tax-returns")}]]
             ["/admin" {:middleware [mid/wrap-admin]}
              ["" {:get (redirect-to "/app/admin/users")}]
              ["/users" {:get admin/page}]
              ["/users/update" {:post mid/admin-update-user}]
              ["/users/hash-password" {:post mid/admin-hash-user-password}]
              ["/users/delete" {:post mid/admin-delete-user}]
              ["/users/grant-trials" {:post admin/grant-trials}]
              ["/users/revoke-plan" {:post billing/admin-revoke}]]
             ;; Account: also on personal Settings; here for every profile
             ["/profile" {:get account/page}]
             ["/save-user" {:post mid/save-user}]
             ["/settings/delete-profile" {:post settings/delete-profile}]
             ["/settings/cancel-subscription" {:post billing/cancel}]
             ["/settings/resume-subscription" {:post billing/resume}]
             ["/save-finances" {:post mid/save-finances}]
             ["/save-tax-profile" {:post mid/save-tax-profile}]
             ["/dismiss-app-update-banner" {:post mid/dismiss-app-update-banner}]]]
   ;; Paystack's webhook: no session or CSRF token; checked by its signature
   :webhook-routes [["/paystack/webhook" {:post billing/webhook}]]})
