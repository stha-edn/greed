(ns com.greed.utilities.company-tax
  "Income tax for a (Pty) Ltd: taxable income estimated from the budget and
   from bank statements, tax at the company rate or the small business
   corporation (SBC) table, and the provisional tax payments of the
   financial year. Estimates only. Rates live in resources/config/common.edn."
  (:require [com.core :as c]
            [com.greed.statements.summary :as summary]
            [com.greed.utilities.vat :as vat])
  (:import [java.time DayOfWeek LocalDate Month YearMonth]))

(defn company-rate
  "The company income tax rate in percent (27)."
  []
  (:company-tax/rate c/common-config 27))

(defn sbc-table
  "The SBC table for the year of assessment ending on `year-end`:
   {:from :to :brackets [{:above :base :rate}]}, and :latest? true when no
   table covers that date and the newest one is used instead."
  [^LocalDate year-end]
  (let [tables (:company-tax/sbc-tables c/common-config)
        covers (fn [{:keys [from to]}]
                 (and (not (.isBefore year-end (LocalDate/parse from)))
                      (not (.isAfter year-end (LocalDate/parse to)))))]
    (or (first (filter covers tables))
        (assoc (last (sort-by :to tables)) :latest? true))))

(defn sbc-tax
  "Tax on `taxable` rands under an SBC table's brackets: the base of the
   highest bracket it is above, plus its rate on the amount above."
  [brackets taxable]
  (if (pos? taxable)
    (let [{:keys [above base rate]} (last (filter #(> taxable (:above %)) (sort-by :above brackets)))]
      (+ base (* (/ rate 100.0) (- taxable above))))
    0.0))

(defn tax
  "Income tax in rands on `taxable` rands for the year ending `year-end`."
  [taxable sbc? ^LocalDate year-end]
  (Math/round (double (if sbc?
                        (sbc-tax (:brackets (sbc-table year-end)) taxable)
                        (* (/ (company-rate) 100.0) (max 0 taxable))))))

;; ---------------------------------------------------------------------------
;; The financial year and its provisional tax payments

(defn- last-business-day
  "`d`, or the Friday before it when it falls on a weekend."
  [^LocalDate d]
  (if (#{DayOfWeek/SATURDAY DayOfWeek/SUNDAY} (.getDayOfWeek d))
    (recur (.minusDays d 1))
    d))

(defn financial-year
  "The financial year, ending on the last day of `year-end-month` (1-12), that
   `date` falls in: {:start :end :months-in}, where :months-in counts the
   year's months up to and including `date`'s month."
  [year-end-month ^LocalDate date]
  (let [end-this (.atEndOfMonth (YearMonth/of (.getYear date) (int year-end-month)))
        end      (if (.isAfter date end-this)
                   (.atEndOfMonth (YearMonth/of (inc (.getYear date)) (int year-end-month)))
                   end-this)
        start    (.atDay (.minusYears (.plusMonths (YearMonth/from end) 1) 1) 1)]
    {:start start :end end
     :months-in (inc (- (+ (* 12 (.getYear date)) (.getMonthValue date))
                        (+ (* 12 (.getYear start)) (.getMonthValue start))))}))

(defn months
  "The months of a financial year as \"yyyy-MM\" strings."
  [{:keys [^LocalDate start]}]
  (map #(str (YearMonth/from (.plusMonths start %))) (range 12)))

(defn provisional-dates
  "Provisional tax due dates for a financial year: the first six months in,
   the second at year end, and the optional third six months after year end
   (seven when the year ends in February). Weekends move a date to the
   Friday before; public holidays are not known here."
  [{:keys [^LocalDate start ^LocalDate end]}]
  (let [third-months (if (= Month/FEBRUARY (.getMonth end)) 7 6)]
    {:first  (last-business-day (.atEndOfMonth (YearMonth/from (.plusMonths start 5))))
     :second (last-business-day end)
     :third  (last-business-day (.atEndOfMonth (YearMonth/from (.plusMonths end third-months))))}))

;; ---------------------------------------------------------------------------
;; Taxable income (cents)

(def not-deductible
  "Spending categories left out of a statement's deductible costs: SARS
   payments, owner's drawings, transfers, cash (unknown use) and loans and
   credit (repayments are not a cost, and interest cannot be told apart)."
  #{:sars :drawings :transfers :cash :debt :income})

(defn- ex-vat
  "Cents without the VAT inside, when the business is a VAT vendor and VAT applies."
  [cents vendor? standard?]
  (if (and vendor? standard?) (- cents (vat/vat-in cents)) cents))

(defn budget-taxable
  "A year's taxable income from the budget, in cents: income less expenses,
   times 12. Set-aside items are not costs; expense items in the SARS or
   Owner's drawings category are left out. A VAT vendor's standard-rated
   amounts are taken without VAT."
  [items vendor?]
  (let [items  (remove :budget-item/from-statement items) ; once-offs are not monthly
        amount (fn [{:budget-item/keys [type amount vat]}]
                 (ex-vat (* 100 amount) vendor? (= :standard (or vat (vat/default-vat-type type)))))
        income (reduce + 0 (map amount (filter #(= :income (:budget-item/type %)) items)))
        costs  (reduce + 0 (map amount (filter #(and (= :expenses (:budget-item/type %))
                                                     (not (#{:sars :drawings} (:budget-item/category %))))
                                               items)))]
    (* 12 (- income costs))))

(defn statement-taxable
  "Taxable income from the bank statement months of the financial year read
   so far, scaled to a full year: {:year :months :so-far} in cents, or nil
   with no months. Money in less deductible spending (see not-deductible); a
   payee matched to a budget item uses the item's category and VAT type."
  [summaries budget-items vendor?]
  (when (seq summaries)
    (let [items (for [{:budget-item/keys [title type reference category vat] :xt/keys [id]} budget-items
                      :when (and reference (#{:expenses :savings} type))]
                  (cond-> {:id id :title title :reference reference :vat (or vat (vat/default-vat-type type))}
                    category (assoc :category category)))
          net   #(- (:spent-cents %) (:refund-cents %))
          costs (fn [{:statement-summary/keys [payees categories]}]
                  (if payees
                    (reduce + 0 (for [[p item] (summary/payee-items items payees)
                                      :let [cat (or (:category item) (:category p))]
                                      :when (not (not-deductible cat))]
                                  (ex-vat (max 0 (net p)) vendor?
                                          (if item (= :standard (:vat item)) (contains? vat/vat-categories cat)))))
                    (reduce + 0 (for [[cat cents] categories :when (not (not-deductible cat))]
                                  (ex-vat cents vendor? (contains? vat/vat-categories cat))))))
          income (reduce + 0 (map #(ex-vat (:statement-summary/money-in-cents %) vendor? true) summaries))
          so-far (- income (reduce + 0 (map costs summaries)))
          n      (count (distinct (map :statement-summary/month summaries)))]
      {:so-far so-far :months n :year (Math/round (/ (* 12.0 so-far) n))})))
