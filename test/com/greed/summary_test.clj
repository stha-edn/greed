(ns com.greed.summary-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.greed.statements.categorise :as categorise]
            [com.greed.statements.fnb :as fnb]
            [com.greed.statements.summary :as summary]))

(def ^:private lines
  (mapv (partial categorise/categorise fnb/kind)
        [{:date "2026-10-12" :description "FNB App Transfer From Fund" :amount-cents 20000 :type :income}
         {:date "2026-10-13" :description "POS Purchase Engen Kyalami Garde" :amount-cents 17380 :type :expenses}
         {:date "2026-10-15" :description "FNB App Payment From Supplies" :amount-cents 100000 :type :income}
         {:date "2026-10-17" :description "POS Purchase Payflex" :amount-cents 56634 :type :expenses}
         {:date "2026-10-20" :description "POS Purchase Engen Kyalami Garde" :amount-cents 40000 :type :expenses}
         {:date "2026-10-23" :description "Bank charges" :amount-cents 800 :type :expenses}
         {:date "2026-10-29" :description "Internet Pmt To Netcash Ipay P2Ff76A2 My Vol" :amount-cents 10000 :type :expenses}
         {:date "2026-10-30" :description "FNB App Transfer To Savings" :amount-cents 5000 :type :expenses}]))

(deftest merchant-names
  (is (= "Engen Kyalami Garde" (summary/merchant-name "POS Purchase Engen Kyalami Garde")))
  (is (= "Netcash Ipay My Vol" (summary/merchant-name "Internet Pmt To Netcash Ipay P2Ff76A2 My Vol")))
  (is (= "Sb Account" (summary/merchant-name "Payshap Account Off-Us Sb Account")))
  (is (= "Bank charges" (summary/merchant-name "Bank charges"))))

(deftest summarise-statement
  (let [s (summary/summarise {:bank :fnb :period ["2026-10-12" "2026-10-31"] :transactions lines :problems []})]
    (testing "money in and out leave out own-account transfers"
      (is (= 100000 (:money-in-cents s)))
      (is (= (+ 17380 56634 40000 800 10000) (:money-out-cents s)))
      (is (= 20000 (:transfers-in-cents s)))
      (is (= 5000 (:transfers-out-cents s))))
    (testing "spending by category"
      (is (= {:fuel 57380 :bnpl 56634 :bank-charges 800 :other 10000} (:categories s))))
    (testing "fees"
      (is (= 800 (:fees-cents s)))
      (is (= 1 (:fee-count s))))
    (testing "top merchants: grouped, largest first, no bank charges"
      (is (= [{:name "Engen Kyalami Garde" :category :fuel :cents 57380 :count 2}
              {:name "Payflex" :category :bnpl :cents 56634 :count 1}
              {:name "Netcash Ipay My Vol" :category :other :cents 10000 :count 1}]
             (:top-merchants s))))
    (testing "period, bank and checks"
      (is (= ["2026-10-12" "2026-10-31" :fnb 8 true]
             ((juxt :period-start :period-end :bank :line-count :checks-ok) s))))))

(deftest summarise-flags-failed-checks
  (is (false? (:checks-ok (summary/summarise {:bank :fnb :period ["2026-10-12" "2026-10-31"]
                                               :transactions lines :problems ["Money out does not match"]})))))

(deftest reversals-and-refunds
  (let [ls [{:date "2026-09-01" :description "SB AUTOPAY" :kind :debt-payment :category :debt :type :expenses :amount-cents 50000}
            {:date "2026-09-02" :description "WOOLWORTHS" :kind :card-purchase :category :groceries :type :expenses :amount-cents 30000}
            {:date "2026-09-03" :description "SB AUTOPAY" :kind :reversal :category :income :type :income :amount-cents 50000}
            {:date "2026-09-04" :description "WOOLWORTHS" :kind :refund :category :groceries :type :income :amount-cents 10000}
            {:date "2026-09-05" :description "OLD THING" :kind :reversal :category :income :type :income :amount-cents 999}
            {:date "2026-09-25" :description "ACME" :kind :salary :category :income :type :income :amount-cents 2500000}]
        s (summary/summarise {:bank :standard-bank :period ["2026-09-01" "2026-09-30"] :transactions ls :problems []})]
    (testing "a reversed payment drops out with its reversal"
      (is (= 4 (:line-count s)))
      (is (nil? (get-in s [:categories :debt]))))
    (testing "refunds, and reversals of earlier payments, reduce spending"
      (is (= (+ 10000 999) (:refunds-cents s)))
      (is (= (- 30000 10000 999) (:money-out-cents s)))
      (is (= {:groceries 20000} (:categories s))))
    (testing "salary is money in, refunds are not"
      (is (= 2500000 (:salary-cents s)))
      (is (= 2500000 (:money-in-cents s))))))

(deftest split-by-month
  (let [ms (summary/summarise-months
            {:bank :standard-bank :period ["2026-08-12" "2026-10-10"] :problems []
             :transactions [{:date "2026-08-20" :description "ENGEN" :kind :card-purchase :category :fuel :type :expenses :amount-cents 1000}
                            {:date "2026-08-31" :description "SB AUTOPAY" :kind :debt-payment :category :debt :type :expenses :amount-cents 7000}
                            {:date "2026-09-02" :description "SB AUTOPAY" :kind :reversal :category :income :type :income :amount-cents 7000}
                            {:date "2026-10-01" :description "ENGEN" :kind :card-purchase :category :fuel :type :expenses :amount-cents 3000}]})]
    (is (= [["2026-08" "2026-08-12" "2026-08-31"]
            ["2026-09" "2026-09-01" "2026-09-30"]
            ["2026-10" "2026-10-01" "2026-10-10"]]
           (map (juxt :month :period-start :period-end) ms)))
    (testing "a reversal in the next month still cancels its payment"
      (is (= [1000 0 3000] (map :money-out-cents ms)))
      (is (= [1 0 1] (map :line-count ms))))))

(deftest budget-references
  (is (= ["discovery life" "vitality"] (summary/references " Discovery Life, vitality ,, a ")))
  (let [med  #uuid "00000000-0000-0000-0000-0000000000a1"
        food #uuid "00000000-0000-0000-0000-0000000000a2"
        items [{:id med :title "Medical Aid" :reference "discovery life"}
               {:id food :title "Groceries" :reference "woolworths, checkers"}
               {:id (random-uuid) :title "No reference" :reference "  "}]
        ls [{:date "2026-09-01" :description "Magtape Debit Discovery Life" :kind :debit-order :category :insurance :type :expenses :amount-cents 300000}
            {:date "2026-09-02" :description "WOOLWORTHS ROSEBANK" :bank-type "CHEQUE CARD PURCHASE" :kind :card-purchase :category :groceries :type :expenses :amount-cents 50000}
            {:date "2026-09-03" :description "CHECKERS" :kind :card-purchase :category :groceries :type :expenses :amount-cents 20000}
            {:date "2026-09-04" :description "WOOLWORTHS" :kind :refund :category :groceries :type :income :amount-cents 5000}
            {:date "2026-09-05" :description "ENGEN" :kind :card-purchase :category :fuel :type :expenses :amount-cents 40000}
            {:date "2026-09-06" :description "Transfer to Discovery Life savings" :kind :transfer :category :transfers :type :expenses :amount-cents 99900}]
        s (summary/summarise {:bank :fnb :period ["2026-09-01" "2026-09-30"] :transactions ls :problems []} items)]
    (testing "each item's payments, refunds taken off; transfers never count"
      (is (= [{:item-id med :title "Medical Aid" :cents 300000 :count 1}
              {:item-id food :title "Groceries" :cents 65000 :count 2}]
             (:budget-matches s)))
      (is (= 365000 (:budgeted-cents s))))
    (testing "what is left is the spending outside the budget"
      (is (= 40000 (- (:money-out-cents s) (:budgeted-cents s)))))
    (testing "no items, no matches"
      (is (= [] (:budget-matches (summary/summarise {:bank :fnb :period ["2026-09-01" "2026-09-30"] :transactions ls :problems []})))))))

(deftest payee-totals
  (is (= "Internet Pmt To Netcash Ipay My Vol"
         (summary/payee-text {:description "Internet Pmt To Netcash Ipay P2F5AED0 My Vol"})))
  (is (= "WOOLWORTHS CHEQUE CARD PURCHASE"
         (summary/payee-text {:description "WOOLWORTHS 1234" :bank-type "CHEQUE CARD PURCHASE"})))
  (let [ps (summary/payees [{:description "ENGEN" :type :expenses :kind :card-purchase :category :fuel :amount-cents 1000}
                            {:description "Engen" :type :expenses :kind :card-purchase :category :fuel :amount-cents 500}
                            {:description "ENGEN" :type :income :kind :refund :category :fuel :amount-cents 200}
                            {:description "Savings" :type :expenses :kind :transfer :category :transfers :amount-cents 9000}
                            {:description "Salary" :type :income :kind :salary :category :income :amount-cents 9000}])]
    (testing "grouped by text, refunds kept apart, transfers and income left out"
      (is (= [{:name "ENGEN" :text "ENGEN" :category :fuel :spent-cents 1500 :refund-cents 200 :count 2}] ps)))
    (testing "the sync matches against payees alone"
      (is (= {:budget-matches [{:item-id 1 :title "Fuel" :cents 1300 :count 2}] :budgeted-cents 1300 :categories {:fuel 1300}}
             (summary/budget-totals [{:id 1 :title "Fuel" :reference "engen, sasol"}] ps))))
    (testing "an item with a category puts what it matches in that category"
      (is (= {:stock 1300}
             (:categories (summary/budget-totals [{:id 1 :title "Fuel" :reference "engen" :category :stock}] ps)))))))

(deftest references-that-change-each-month
  (testing "numbers stuck to a name are dropped, the name is kept"
    (is (= "WESBANK" (summary/payee-text {:description "WESBANK_FI85346911121002260825"})))
    (is (= ["wesbank"] (summary/references "WESBANK_FI85346911121002261025"))))
  (let [ps (summary/payees [{:description "WESBANK_FI85346911121002260825" :type :expenses :kind :debit-order :category :debt :amount-cents 450000}
                            {:description "WESBANK_FI85346911121002261025" :type :expenses :kind :debit-order :category :debt :amount-cents 450000}])]
    (testing "the same payee every month, whatever the numbers"
      (is (= 1 (count ps))))
    (testing "matches a typed name or a pasted statement reference"
      (is (= 900000 (:budgeted-cents (summary/budget-totals [{:id 1 :title "Car" :reference "wesbank"}] ps))))
      (is (= 900000 (:budgeted-cents (summary/budget-totals [{:id 1 :title "Car" :reference "WESBANK_FI85346911121002260825"}] ps)))))
    (testing "whole words only"
      (is (= 0 (:budgeted-cents (summary/budget-totals [{:id 1 :title "Bank" :reference "wes"}] ps)))))))

(deftest references-with-numbers-attached
  (testing "letters in front of a number are kept, the number is not"
    (is (= "SBSAVAFNO" (summary/payee-text {:description "SBSAVAFNO20007921542"})))
    (is (= ["sbsavafno"] (summary/references "SBSAVAFNO20007921542")))
    (is (= "Netcash Ipay My Vol" (summary/payee-text {:description "Netcash Ipay P2F5AED0 My Vol"})))
    (is (= "" (summary/payee-text {:description "FI85346911121002260825 20260825 07H54"}))))
  (let [ps (summary/payees [{:description "SBSAVAFNO20007921542" :type :expenses :kind :payment-out :category :other :amount-cents 100000}
                            {:description "SBSAVAFNO20007921999" :type :expenses :kind :payment-out :category :other :amount-cents 100000}])]
    (is (= 1 (count ps)))
    (is (= 200000 (:budgeted-cents (summary/budget-totals [{:id 1 :title "Savings" :reference "SBSAVAFNO20007921542"}] ps))))))

(deftest money-in-by-payer
  (let [ps (summary/income-payees
            [{:description "FNB App Payment From Acme Holdings" :type :income :kind :payment-in :amount-cents 500000}
             {:description "FNB App Payment From Acme Holdings Ref INV2026" :type :income :kind :payment-in :amount-cents 250000}
             {:description "Payshap Credit Small Client" :type :income :kind :payment-in :amount-cents 10000}
             {:description "FNB App Transfer From Savings" :type :income :kind :transfer :amount-cents 999999}
             {:description "WOOLWORTHS" :type :income :kind :refund :amount-cents 5000}
             {:description "ENGEN" :type :expenses :kind :card-purchase :amount-cents 1000}])]
    (testing "payers largest first, with the bank's wording and reference codes dropped"
      (is (= [["Acme Holdings" 500000 1] ["Acme Holdings Ref INV" 250000 1] ["Small Client" 10000 1]]
             (map (juxt :name :cents :count) ps))))
    (testing "transfers, refunds and payments out are not money in"
      (is (not-any? #(re-find #"(?i)savings|woolworths|engen" (:name %)) ps)))))
