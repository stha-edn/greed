(ns com.greed.company-tax-test
  (:require [clojure.test :refer [deftest is testing are]]
            [com.greed.utilities.company-tax :as ct])
  (:import [java.time LocalDate]))

(defn- d [s] (LocalDate/parse s))

(deftest small-business-corporation-tables
  (testing "year of assessment ending 1 April 2026 to 31 March 2027"
    (are [taxable tax] (= tax (ct/tax taxable true (d "2027-02-28")))
      0       0
      99000   0
      100000  70
      365000  18620
      550000  57470
      600000  70970))
  (testing "the year before"
    (are [taxable tax] (= tax (ct/tax taxable true (d "2026-02-28")))
      95750   0
      365000  18848
      550000  57698))
  (testing "no table for the year: the newest is used, and said so"
    (is (:latest? (ct/sbc-table (d "2030-02-28"))))
    (is (not (:latest? (ct/sbc-table (d "2027-02-28")))))))

(deftest company-rate
  (is (= 270000 (ct/tax 1000000 false (d "2027-02-28"))))
  (is (= 0 (ct/tax -5000 false (d "2027-02-28"))) "a loss pays no tax"))

(deftest financial-years
  (is (= {:start (d "2026-03-01") :end (d "2027-02-28") :months-in 9} (ct/financial-year 2 (d "2026-11-14"))))
  (is (= {:start (d "2026-07-01") :end (d "2027-06-30") :months-in 5} (ct/financial-year 6 (d "2026-11-14"))))
  (is (= (d "2028-02-29") (:end (ct/financial-year 2 (d "2027-03-01")))) "a leap year's February")
  (is (= "2026-03" (first (ct/months (ct/financial-year 2 (d "2026-11-14")))))))

(deftest provisional-tax-dates
  (testing "February year end: the third payment is seven months after"
    (is (= {:first (d "2026-08-31") :second (d "2027-02-26") :third (d "2027-09-30")}
           (ct/provisional-dates (ct/financial-year 2 (d "2026-11-14"))))
        "28 Feb 2027 is a Sunday, so the second payment moves to Friday the 26th"))
  (testing "June year end: six months after"
    (is (= {:first (d "2026-12-31") :second (d "2027-06-30") :third (d "2027-12-31")}
           (ct/provisional-dates (ct/financial-year 6 (d "2026-11-14")))))))

(deftest taxable-income-from-the-budget
  (let [items [{:budget-item/type :income :budget-item/amount 11500}
               {:budget-item/type :expenses :budget-item/amount 2300 :budget-item/vat :standard}
               {:budget-item/type :expenses :budget-item/amount 5000 :budget-item/category :drawings}
               {:budget-item/type :savings :budget-item/amount 3000}]]
    (testing "a VAT vendor's amounts without VAT; drawings and set-aside are not costs"
      (is (= (* 12 (- 1000000 200000)) (ct/budget-taxable items true))))
    (testing "not a VAT vendor"
      (is (= (* 12 (- 1150000 230000)) (ct/budget-taxable items false))))))

(deftest taxable-income-from-statements
  (let [owner #uuid "00000000-0000-0000-0000-00000000cc01"
        items [{:xt/id owner :budget-item/title "Drawings" :budget-item/type :expenses :budget-item/amount 1
                :budget-item/reference "owner" :budget-item/category :drawings}]
        month {:statement-summary/month "2026-11"
               :statement-summary/money-in-cents 115000
               :statement-summary/payees
               [{:text "ENGEN" :category :fuel :spent-cents 11500 :refund-cents 0 :count 1}
                {:text "STAFF SALARIES" :category :salaries :spent-cents 50000 :refund-cents 0 :count 1}
                {:text "SARS EFILING" :category :sars :spent-cents 30000 :refund-cents 0 :count 1}
                {:text "OWNER PAYMENT" :category :other :spent-cents 40000 :refund-cents 0 :count 1}]}]
    (is (= {:so-far 40000 :months 1 :year 480000} (ct/statement-taxable [month] items true)))
    (is (nil? (ct/statement-taxable [] items true)))))
