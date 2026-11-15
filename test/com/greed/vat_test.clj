(ns com.greed.vat-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.greed.utilities.vat :as vat])
  (:import [java.time LocalDate]))

(defn- d [s] (LocalDate/parse s))

(deftest vat-inside-an-amount
  (is (= 1500 (vat/vat-in 11500 15)))
  (is (= 0 (vat/vat-in 0 15))))

(deftest vat-numbers
  (is (vat/vat-number? "4123456789"))
  (is (not (vat/vat-number? "5123456789")))
  (is (not (vat/vat-number? "412345678")))
  (is (not (vat/vat-number? "41234567890"))))

(deftest tax-periods
  (testing "category A: two months ending in even months"
    (is (= {:start (d "2026-11-01") :end (d "2026-12-31") :due (d "2027-01-29") :months 2}
           (vat/period :a (d "2026-11-14")))
        "31 Jan 2027 is a Sunday, so the return is due on Friday the 29th")
    (is (= (d "2026-11-01") (:start (vat/period :a (d "2026-12-31"))))))
  (testing "category B: two months ending in odd months, across the new year"
    (is (= {:start (d "2026-10-01") :end (d "2026-11-30") :due (d "2026-12-31") :months 2}
           (vat/period :b (d "2026-11-14"))))
    (is (= {:start (d "2026-12-01") :end (d "2027-01-31") :due (d "2027-02-26") :months 2}
           (vat/period :b (d "2027-01-10")))))
  (testing "category C: every month"
    (is (= {:start (d "2026-11-01") :end (d "2026-11-30") :due (d "2026-12-31") :months 1}
           (vat/period :c (d "2026-11-14")))))
  (testing "earlier periods, newest first"
    (is (= ["2026-11-01" "2026-09-01" "2026-07-01"]
           (map (comp str :start) (vat/periods :a (d "2026-11-14") 3))))
    (is (= ["2026-11" "2026-12"] (vat/period-months (vat/period :a (d "2026-11-14")))))))

(deftest from-the-budget
  (let [items [{:budget-item/type :income :budget-item/amount 11500}
               {:budget-item/type :income :budget-item/amount 5000 :budget-item/vat :zero}
               {:budget-item/type :expenses :budget-item/amount 2300 :budget-item/vat :standard}
               {:budget-item/type :expenses :budget-item/amount 1000}
               {:budget-item/type :savings :budget-item/amount 9999 :budget-item/vat :standard}]]
    (testing "income is standard unless set, costs carry no VAT unless set, set-aside never does"
      (is (= {:output 300000 :input 60000 :net 240000} (vat/budget-estimate items 2))))))

(deftest from-bank-statements
  (let [makro  #uuid "00000000-0000-0000-0000-00000000aa01"
        office #uuid "00000000-0000-0000-0000-00000000aa02"
        items  [{:xt/id makro :budget-item/title "Stock" :budget-item/type :expenses :budget-item/amount 1
                 :budget-item/reference "makro" :budget-item/vat :none}
                {:xt/id office :budget-item/title "Office" :budget-item/type :expenses :budget-item/amount 1
                 :budget-item/reference "office supplies" :budget-item/vat :standard}]
        month  {:statement-summary/month "2026-11"
                :statement-summary/money-in-cents 115000
                :statement-summary/payees
                [{:text "ENGEN" :category :fuel :spent-cents 11500 :refund-cents 0 :count 1}
                 {:text "STAFF SALARIES" :category :salaries :spent-cents 50000 :refund-cents 0 :count 1}
                 {:text "MAKRO MIDRAND" :category :other :spent-cents 23000 :refund-cents 0 :count 1}
                 {:text "OFFICE SUPPLIES" :category :other :spent-cents 23000 :refund-cents 0 :count 1}]}]
    (testing "money in is output VAT; costs by category, or by their budget item's VAT type"
      (is (= {:output 15000 :input 4500 :net 10500 :months 1}
             (vat/statement-estimate [month] items))))
    (testing "months read before payee totals use their categories"
      (is (= {:output 0 :input 300 :net -300 :months 1}
             (vat/statement-estimate [{:statement-summary/month "2026-12"
                                       :statement-summary/money-in-cents 0
                                       :statement-summary/categories {:fuel 2300 :salaries 5000}}]
                                     items))))))

(deftest sars-due-dates-on-the-calendar
  (let [due-dates (requiring-resolve 'com.greed.utilities.due-dates/sars-events)
        biz {:business/entity-type :pty-ltd :business/vat-registered true :business/vat-category :a
             :business/year-end-month 2}
        evs (due-dates biz (d "2026-11-01") (d "2027-03-31"))]
    (testing "each VAT201 and provisional payment in range, by date"
      (is (= [["2026-11-30" "VAT201 due (Sep–Oct)"]
              ["2027-01-29" "VAT201 due (Nov–Dec)"]
              ["2027-02-26" "Second provisional tax payment"]
              ["2027-03-31" "VAT201 due (Jan–Feb)"]]
             (map (juxt :event/date :event/title) evs))))
    (testing "added by greed: bills that cannot be removed"
      (is (every? #(and (:event/auto %) (= :bill (:event/type %))) evs)))
    (testing "nothing for a business that is neither a VAT vendor nor a company"
      (is (empty? (due-dates {:business/entity-type :sole-proprietor} (d "2026-11-01") (d "2027-03-31")))))))
