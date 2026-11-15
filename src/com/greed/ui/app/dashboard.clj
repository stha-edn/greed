(ns com.greed.ui.app.dashboard
  (:require [com.core :as c]
            [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.app.business-home :as business-home]
            [com.greed.core :as c.greed]
            [com.greed.ui.components.stats :as stats]
            [com.greed.ui.components.cards :as cards]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.home :as home]
            [com.greed.utilities.time :as u.time]))

(defn- personal-page [{:keys [session params] :as ctx}]
  (let [user-id            (:uid session)
        user               (data/get-user ctx user-id)
        finances           (data/get-finances ctx user-id)
        income-tax-data    (c.greed/get-income-tax-data user finances)
        budget-items       (data/get-budget-items ctx user-id)
        payday             (:finances/payday finances)
        events             (data/get-events ctx user-id)
        goals              (data/get-goals ctx user-id)
        summaries          (data/get-statement-summaries ctx user-id)
        month              (home/latest-month summaries)
        ;; the plan for this month: monthly items and this month's statement items
        plan               (data/get-budget-data (data/items-for-month budget-items (data/current-month)))
        month-items        (when month (data/items-for-month budget-items (:month month)))
        items-by-id        (into {} (map (juxt :xt/id identity)) month-items)
        today              (java.time.LocalDate/now)
        next-pd            (u.time/next-payday today payday)
        rows               (take 4 (sort-by :due
                                            (concat
                                             (when next-pd
                                               [{:what "Payday" :due next-pd :note "Salary"
                                                 :amount (some-> (:net-income income-tax-data) double (/ 12) (* 100) Math/round)}])
                                             (for [{:event/keys [title date type done]} events
                                                   :when (and date (not done))
                                                   :let [d (java.time.LocalDate/parse date)]
                                                   :when (not (.isBefore d today))]
                                               {:what title :due d
                                                :note (case type :bill "Bill" :income "Payment in" :todo "To do" "Event")}))))]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (home/heading user "Personal")

      ;; The same Home as a business's: hero, plan and what's next, the
      ;; bank statements, progress and quick actions.
      (if month
        (home/month-hero month items-by-id (or (:total-expenses (data/get-budget-data month-items)) 0) home/personal)
        (home/empty-hero plan home/personal))

      [:div
       (stats/section-header "The plan and what's next" :href "/app/finances/" :link-label "Open Budget")
       [:div {:class "grid grid-cols-1 items-stretch gap-4 lg:grid-cols-2"}
        (home/plan-card plan home/personal)
        (home/due-card rows {:sub "Your next payday, and the dates on your calendar."
                             :empty "Nothing coming up. Set your payday in Settings, or add dates on the Calendar."
                             :links [["/app/calendar" "Calendar"] ["/app/tax" "Tax"]]})]]

      (when month
        [:div
         (stats/section-header "From the bank statements" :href "/app/spending" :link-label "Open Spending")
         [:div {:class "grid grid-cols-1 items-stretch gap-4 lg:grid-cols-5"}
          [:div {:class "lg:col-span-3"} (home/flow-card (home/monthly-flow summaries 6))]
          [:div {:class "lg:col-span-2"} (home/costs-card (:summaries month))]]])

      [:div
       (stats/section-header "Progress")
       [:div {:class "grid grid-cols-1 gap-4 lg:grid-cols-2 lg:items-stretch"}
        (stats/tax-summary income-tax-data)
        (stats/goals-summary goals)]]

      [:div
       (stats/section-header "Quick actions")
       (home/actions [["/app/banking" "Upload a statement" "Read a bank statement PDF"]
                      ["/app/finances/" "Update the budget" "Income, expenses and savings"]
                      ["/app/goals" "Add a goal" "Something you are saving towards"]])]])))

(defn page
  "GET /app: the business's home while a business is chosen, else the
   personal dashboard."
  [ctx]
  (if-let [business (data/session-business ctx)]
    (business-home/page ctx business)
    (personal-page ctx)))
