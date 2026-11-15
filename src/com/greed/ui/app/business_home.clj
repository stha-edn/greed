(ns com.greed.ui.app.business-home
  "A business's home (/app while a business is chosen): this month at a
   glance from its bank statements against its budget, the plan, what is
   due to SARS next, money in and out over recent months, where the money
   went, and quick actions."
  (:require [clojure.string :as str]
            [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.home :as home]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.stats :as stats]
            [com.greed.utilities.company-tax :as ct]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.vat :as vat])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]
           [java.time.temporal ChronoUnit]))

;; ---------------------------------------------------------------------------
;; Helpers

(defn- whole [cents] (utilities/whole->rands (/ cents 100.0)))
(defn- rands [r] (utilities/whole->rands r))
(defn- pct [part total] (if (pos? total) (min 100 (Math/round (* 100.0 (/ (double part) total)))) 0))

(defn coming-up
  "What is due to SARS next, from the budget: the current VAT period's
   VAT201 for a VAT vendor, and a (Pty) Ltd's next provisional tax payment.
   [{:what :due :amount :note}], soonest first; amounts in cents."
  [{:business/keys [entity-type vat-registered vat-category year-end-month sbc]} items ^LocalDate today]
  (sort-by :due
           (concat
            (when (and vat-registered vat-category)
              (let [p (vat/period vat-category today)
                    {:keys [net]} (vat/budget-estimate items (:months p))]
                [{:what "VAT201" :due (:due p) :amount net
                  :note (str "VAT for " (.format ^LocalDate (:start p) (DateTimeFormatter/ofPattern "MMM"))
                             (when (> (:months p) 1) (str " to " (.format ^LocalDate (:end p) (DateTimeFormatter/ofPattern "MMM")))))}]))
            (when (= :pty-ltd entity-type)
              (let [fy    (ct/financial-year (or year-end-month 2) today)
                    tax   (ct/tax (/ (ct/budget-taxable items vat-registered) 100.0) sbc (:end fy))
                    half  (Math/round (/ tax 2.0))
                    {first-due :first second-due :second} (ct/provisional-dates fy)
                    upcoming (cond (not (.isBefore ^LocalDate first-due today)) {:due first-due :amount (* 100 half) :n "First"}
                                   (not (.isBefore ^LocalDate second-due today)) {:due second-due :amount (* 100 (- tax half)) :n "Second"})]
                (when upcoming
                  [{:what (str (:n upcoming) " provisional tax payment") :due (:due upcoming) :amount (:amount upcoming)
                    :note "Company income tax for the year, from the budget"}]))))))

;; ---------------------------------------------------------------------------
;; Sections

;; ---------------------------------------------------------------------------
;; Page

(defn- upcoming-events
  "The profile's own dated events from today, as Coming up rows."
  [events today]
  (for [{:event/keys [title date type done]} events
        :when (and date (not done))
        :let [d (LocalDate/parse date)]
        :when (not (.isBefore d today))]
    {:what title :due d :note (case type :bill "Bill" :income "Payment in" :todo "To do" "Event")}))

(defn- tax-card
  "The business's tax at a glance, in the same card as Personal's Tax."
  [{:business/keys [entity-type vat-registered year-end-month sbc]} items]
  (if (= :pty-ltd entity-type)
    (let [fy  (ct/financial-year (or year-end-month 2) (LocalDate/now))
          tax (ct/tax (/ (ct/budget-taxable items vat-registered) 100.0) sbc (:end fy))]
      (stats/summary-card :label "Tax" :href "/app/tax" :value (rands tax)
                          :reading (str "Company income tax for the year to "
                                        (.format ^LocalDate (:end fy) (DateTimeFormatter/ofPattern "MMMM yyyy"))
                                        ", from the budget.")))
    (stats/summary-card :label "Tax" :href "/app/tax" :value "Personal"
                        :reading "The profit is taxed in the owners' personal returns.")))

(defn page
  "GET /app while a business is chosen: the same Home as Personal, with
   the business's money."
  [{:keys [session params] :as ctx} {:xt/keys [id] :as business}]
  (let [user        (data/get-user ctx (:uid session))
        today       (LocalDate/now)
        items       (data/get-business-budget-items ctx id)
        plan        (data/get-budget-data (data/items-for-month items (data/current-month)))
        summaries   (data/get-statement-summaries ctx {:business-id id})
        month       (home/latest-month summaries)
        month-items (when month (data/items-for-month items (:month month)))
        items-by-id (into {} (map (juxt :xt/id identity)) month-items)
        goals       (data/get-goals ctx {:business-id id})
        rows        (take 4 (sort-by :due (concat (coming-up business items today)
                                                  (upcoming-events (data/get-events ctx {:business-id id}) today))))
        owner?      (= :owner (:business-member/role business))]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (home/heading user (str (:business/name business) " · " (data/entity-types (:business/entity-type business))))
      (if month
        (home/month-hero month items-by-id (:total-expenses (data/get-budget-data month-items)) home/business)
        (home/empty-hero plan home/business))
      [:div
       (stats/section-header "The plan and what's next" :href "/app/finances/" :link-label "Open Budget")
       [:div {:class "grid grid-cols-1 items-stretch gap-4 lg:grid-cols-2"}
        (home/plan-card plan home/business)
        (home/due-card rows {:sub "SARS dates estimated from the budget, and the business's own dates."
                             :empty (if (= :pty-ltd (:business/entity-type business))
                                      "Set the financial year in Settings to see provisional tax dates."
                                      "Nothing coming up. Add dates on the Calendar.")
                             :links (cond-> [["/app/calendar" "Calendar"]]
                                      (:business/vat-registered business) (conj ["/app/vat" "VAT"])
                                      true (conj ["/app/tax" "Tax"]))})]]
      (when month
        [:div
         (stats/section-header "From the bank statements" :href "/app/spending" :link-label "Open Spending")
         [:div {:class "grid grid-cols-1 items-stretch gap-4 lg:grid-cols-5"}
          [:div {:class "lg:col-span-3"} (home/flow-card (home/monthly-flow summaries 6))]
          [:div {:class "lg:col-span-2"} (home/costs-card (:summaries month))]]])
      [:div
       (stats/section-header "Progress")
       [:div {:class "grid grid-cols-1 gap-4 lg:grid-cols-2 lg:items-stretch"}
        (tax-card business items)
        (stats/goals-summary goals)]]
      [:div
       (stats/section-header "Quick actions")
       (home/actions [["/app/banking" "Upload a statement" "Read a bank statement PDF"]
                      ["/app/finances/" "Update the budget" "Income, costs and set-aside"]
                      (if owner?
                        ["/app/settings" "Invite someone" "A co-founder or bookkeeper"]
                        ["/app/goals" "Add a goal" "A tax reserve or an equipment fund"])])]])))
