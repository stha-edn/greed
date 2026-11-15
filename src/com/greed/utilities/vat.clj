(ns com.greed.utilities.vat
  "VAT for a VAT-registered business: the VAT inside an amount, the tax
   period a date falls in and when its VAT201 is due, and estimates of what
   the period will owe SARS from the budget and from bank statements.

   These are estimates: greed does not see tax invoices, which a VAT201
   needs. The rate and thresholds live in resources/config/common.edn."
  (:require [com.core :as c]
            [com.greed.statements.summary :as summary])
  (:import [java.time DayOfWeek LocalDate YearMonth]))

(defn rate
  "The VAT rate in percent (15)."
  []
  (:vat/rate c/common-config 15))

(defn compulsory-threshold
  "Taxable sales in 12 months above which registering is compulsory, in rands."
  []
  (:vat/compulsory-threshold c/common-config 2300000))

(defn voluntary-threshold
  "Taxable sales in 12 months from which a business may register, in rands."
  []
  (:vat/voluntary-threshold c/common-config 120000))

(def categories
  "Tax period categories greed supports, with how SARS describes them."
  (array-map :a "Category A: every two months, ending Feb, Apr, Jun, Aug, Oct and Dec"
             :b "Category B: every two months, ending Jan, Mar, May, Jul, Sep and Nov"
             :c "Category C: every month"))

(def vat-types
  "How VAT applies to a budget item's amount."
  (array-map :standard "Standard (15%)" :zero "Zero-rated (0%)" :none "No VAT"))

(defn default-vat-type
  "Income is usually standard-rated. Costs default to no VAT so that input
   VAT is not claimed on salaries, SARS payments or unregistered suppliers by
   accident."
  [budget-type]
  (if (= :income budget-type) :standard :none))

(defn vat-number?
  "A South African VAT number: 10 digits starting with 4."
  [s]
  (boolean (re-matches #"4\d{9}" (str s))))

(defn vat-in
  "The VAT inside a VAT-inclusive amount, in the same unit: amount × 15/115."
  ([amount] (vat-in amount (rate)))
  ([amount r] (Math/round (/ (* (double amount) r) (+ 100.0 r)))))

;; ---------------------------------------------------------------------------
;; Tax periods

(defn due-date
  "When a period's VAT201 is due on eFiling: the last business day of the
   month after the period ends. Weekends are skipped; public holidays are
   not known here, so a due date on one is a day late."
  [^LocalDate period-end]
  (loop [d (.atEndOfMonth (YearMonth/from (.plusMonths period-end 1)))]
    (if (#{DayOfWeek/SATURDAY DayOfWeek/SUNDAY} (.getDayOfWeek d))
      (recur (.minusDays d 1))
      d)))

(defn period
  "The tax period of `category` that `date` falls in:
   {:start :end :due :months}."
  [category ^LocalDate date]
  (let [ym     (YearMonth/from date)
        m      (.getMonthValue ym)
        start  (case category
                 :c ym
                 ;; A starts in odd months (Jan-Feb ... Nov-Dec)
                 :a (if (odd? m) ym (.minusMonths ym 1))
                 ;; B starts in even months (Dec-Jan, Feb-Mar ... Oct-Nov)
                 :b (if (even? m) ym (.minusMonths ym 1)))
        months (if (= :c category) 1 2)
        end    (.atEndOfMonth (.plusMonths ^YearMonth start (dec months)))]
    {:start (.atDay ^YearMonth start 1) :end end :due (due-date end) :months months}))

(defn periods
  "The period `date` is in and the `n - 1` before it, newest first."
  [category ^LocalDate date n]
  (take n (iterate #(period category (.minusDays ^LocalDate (:start %) 1)) (period category date))))

(defn period-months
  "The months of a period as \"yyyy-MM\" strings."
  [{:keys [^LocalDate start months]}]
  (map #(str (YearMonth/from (.plusMonths start %))) (range months)))

;; ---------------------------------------------------------------------------
;; Estimates (all in cents)

(defn budget-estimate
  "VAT for one period from the budget: output VAT on standard-rated income,
   input VAT on standard-rated expenses, times the months in the period.
   Amounts include VAT; budget amounts are in rands, results in cents."
  [items months]
  (let [items  (remove :budget-item/from-statement items) ; once-offs are not monthly
        vat-of (fn [t] (reduce + 0 (for [{:budget-item/keys [type amount vat]} items
                                         :when (= t type)
                                         :when (= :standard (or vat (default-vat-type type)))]
                                     (vat-in (* 100 amount months)))))
        output (vat-of :income)
        input  (vat-of :expenses)]
    {:output output :input input :net (- output input)}))

(def vat-categories
  "Spending categories whose payments usually carry VAT from a registered
   supplier. Salaries, SARS, drawings, interest and loans, insurance, cash,
   transfers, debit orders and anything greed could not place are left out,
   so the estimate leans towards less input VAT, not more."
  #{:groceries :fuel :eating-out :transport :shopping :subscriptions :airtime-data
    :electricity :bank-charges :software :stock :rent})

(defn statement-estimate
  "VAT for the months of a period from bank statements. Output VAT on money
   in (own transfers, refunds and reversals are already left out of it).
   Input VAT per payee: a payee matched to a budget item uses that item's VAT
   type; others count when their category usually carries VAT. Months read
   before payee totals were kept fall back to their categories.
   `budget-items` are the business's items (for references and VAT types)."
  [summaries budget-items]
  (let [items  (for [{:budget-item/keys [title type reference category vat] :xt/keys [id]} budget-items
                     :when (and reference (#{:expenses :savings} type))]
                 (cond-> {:id id :title title :reference reference
                          :vat (or vat (default-vat-type type))}
                   category (assoc :category category)))
        net    #(- (:spent-cents %) (:refund-cents %))
        input-of (fn [{:statement-summary/keys [payees categories]}]
                   (if payees
                     (reduce + 0 (for [[p item] (summary/payee-items items payees)
                                       :let [cat (or (:category item) (:category p))]
                                       :when (if item
                                               (= :standard (:vat item))
                                               (vat-categories cat))]
                                   (vat-in (max 0 (net p)))))
                     (reduce + 0 (for [[cat cents] categories :when (vat-categories cat)]
                                   (vat-in cents)))))
        output (reduce + 0 (map #(vat-in (:statement-summary/money-in-cents %)) summaries))
        input  (reduce + 0 (map input-of summaries))]
    {:output output :input input :net (- output input)
     :months (count (distinct (map :statement-summary/month summaries)))}))
