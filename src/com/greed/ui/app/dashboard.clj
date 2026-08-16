(ns com.greed.ui.app.dashboard
  (:require [com.core :as c]
            [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.core :as c.greed]
            [com.greed.ui.components.stats :as stats]
            [com.greed.ui.components.cards :as cards]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.headers :as headers]))

(def finance-tax-prompt-interval-ms
  "How often the finance & tax prompt may appear, in milliseconds."
  (* 24 60 60 1000))

(defn finance-tax-prompt-due?
  "True when the prompt should be shown for this session — i.e. it hasn't
   been dismissed within the last 24 hours."
  [{:keys [session]}]
  (let [dismissed-at (:finance-tax-prompt-dismissed-at session)]
    (or (nil? dismissed-at)
        (>= (- (System/currentTimeMillis) (long dismissed-at))
            finance-tax-prompt-interval-ms))))

(defn app-update-banner-due?
  "True when the signed-in user hasn't acknowledged the current :app/version
   (common.edn) yet — see middleware/dismiss-app-update-banner. Bumping
   :app/version re-shows the banner to everyone, including users who'd
   already dismissed an earlier version."
  [{:keys [session]}]
  (not= (:app-update-seen-version session) (:app/version c/common-config)))

(defn- today-str []
  (.format (java.time.LocalDate/now)
           (java.time.format.DateTimeFormatter/ofPattern "EEE, d MMM yyyy")))

(defn salary-set? [finances]
  (let [salary (get finances :finances/salary)]
    (and (some? salary) (pos? (long (or salary 0))))))

(defn page [{:keys [session params] :as ctx}]
  (let [user-id            (:uid session)
        user               (data/get-user ctx user-id)
        finances           (data/get-finances ctx user-id)
        income-tax-data    (c.greed/get-income-tax-data user finances)
        budget-items       (data/get-budget-items ctx user-id)
        payday             (:finances/payday finances)
        events             (data/get-events ctx user-id)
        goals              (data/get-goals ctx user-id)
        show-finance-tax-prompt (and (not (data/admin? user))
                                     (not (salary-set? finances))
                                     (finance-tax-prompt-due? ctx))
        show-app-update-banner (app-update-banner-due? ctx)]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (when show-finance-tax-prompt (alerts/finance-tax-prompt-modal))
      (when (:success params) (alerts/success :type (keyword (:success params))))
      (when (:alert params) (alerts/info params))
      (when show-app-update-banner
        (alerts/info {:alert "app-updated"} :dismiss-href "/app/dismiss-app-update-banner"))
      (headers/home-heading :user user :date (today-str))

      ;; Hero: net take-home feature card + bank card
      [:div {:class "grid grid-cols-1 items-stretch gap-4 lg:grid-cols-3"}
       [:div {:class "lg:col-span-2"}
        (stats/dashboard-hero budget-items payday)]
       [:div {:class "lg:col-span-1"}
        [:div {:class "mb-3 lg:hidden"}
         (stats/section-header "Wallet")]
        [:div {:class "flex items-center justify-center lg:h-full lg:justify-end"}
         (cards/bank-card
          :finances finances
          :budget-items budget-items
          :net-monthly-income (when-let [net (:net-income income-tax-data)]
                                (/ net 12)))]]]

      ;; Money at a glance — charts carry the budget/savings story already
      ;; told by the hero above; the summary row below moves on to the
      ;; surfaces that don't have a hero-sized treatment of their own.
      (stats/section-header "Money at a glance")
      [:div {:class "grid grid-cols-1 items-stretch gap-4 lg:grid-cols-5"}
       [:div {:class "lg:col-span-3"}
        (stats/cashflow-donut budget-items)]
       [:div {:class "lg:col-span-2"}
        (stats/savings-ring budget-items)]]

      ;; More to explore — ordered by what's actionable first (what's next),
      ;; then the two background reads (Tax, Goals). Budget is deliberately
      ;; not repeated here: the hero and the donut above already cover it
      ;; twice. Each card shares one uniform grid.
      (stats/section-header "More to explore")
      [:div {:class "grid grid-cols-1 gap-4 lg:grid-cols-3 lg:items-stretch"}
       (stats/upcoming-summary payday events)
       (stats/tax-summary income-tax-data)
       (stats/goals-summary goals)]])))
