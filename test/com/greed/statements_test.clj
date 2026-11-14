(ns com.greed.statements-test
  "Statement readers, tested on made-up statements in each bank's layout.
   Real statements never go in the repo."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.greed.statements.core :as statements]
            [com.greed.statements.fnb :as fnb]
            [com.greed.statements.standard-bank :as sb]))

(def fnb-text
  ;; Shape of the text PDFBox extracts from an FNB statement; names and
  ;; numbers are invented.
  (str/join "\n"
            ["First National Bank - a division of FirstRand Bank Limited."
             "fnb.co.za"
             "Statement Period : 20 December 2026 to 10 January 2027"
             "Opening Balance 1,000.00 Cr"
             "Closing Balance 7,615.50 Cr"
             "Transactions in RAND (ZAR)"
             "Date Description Amount Balance"
             "20 Dec Payshap Credit Acme Freelance 7,050.00Cr 8,050.00Cr"
             "22 Dec POS Purchase Woolworths Rosebank 400568*4888 21 Dec 249.99 7,800.01Cr 4.00"
             "24 Dec 8.00 7,792.01Cr"
             "02 Jan Internet Pmt To Netcash Ipay P2F5AED0 My Vol 100.00 7,692.01Cr"
             "05 Jan POS Purchase Engen Kyalami 400568*4888 04 Jan 9,000.00 1,307.99 4.00"
             "08 Jan FNB App Transfer From Savings 8,923.49Cr 7,615.50Cr"
             "Closing Balance 7,615.50Cr"
             "Turnover for Statement Period"
             "No. Credit Transactions 2 15,973.49Cr"
             "No. Debit Transactions 4 9,357.99Dr"]))

(deftest fnb-detects-its-statement
  (is (fnb/fnb-statement? fnb-text))
  (is (not (fnb/fnb-statement? "Standard Bank statement"))))

(deftest fnb-reads-lines
  (let [{:keys [bank period transactions problems]} (fnb/parse fnb-text)]
    (testing "every check the statement prints agrees"
      (is (= :fnb bank))
      (is (= [] problems)))
    (testing "the period, and years across New Year"
      (is (= ["2026-12-20" "2027-01-10"] period))
      (is (= ["2026-12-20" "2026-12-22" "2026-12-24" "2027-01-02" "2027-01-05" "2027-01-08"]
             (map :date transactions))))
    (testing "money in and out, in cents"
      (is (= {:date "2026-12-20" :description "Payshap Credit Acme Freelance"
              :amount-cents 705000 :type :income}
             (first transactions)))
      (is (= [:income :expenses :expenses :expenses :expenses :income] (map :type transactions)))
      (is (= 900000 (:amount-cents (nth transactions 4)))))
    (testing "card details dropped, fee lines named"
      (is (= "POS Purchase Woolworths Rosebank" (:description (nth transactions 1))))
      (is (= "Bank charges" (:description (nth transactions 2))))
      (is (= "Internet Pmt To Netcash Ipay P2F5AED0 My Vol" (:description (nth transactions 3)))))))

(deftest fnb-reports-mismatches
  (testing "a missing line breaks the totals and the running balance"
    (let [broken (str/replace fnb-text "02 Jan Internet Pmt To Netcash Ipay P2F5AED0 My Vol 100.00 7,692.01Cr\n" "")
          {:keys [problems]} (fnb/parse broken)]
      (is (some #(str/starts-with? % "Money out does not match") problems))
      (is (some #(str/starts-with? % "The running balance does not add up") problems))))
  (testing "no statement period"
    (is (= ["The statement period could not be read."]
           (:problems (fnb/parse (str/replace fnb-text #"Statement Period.*\n" "")))))))

(def ^:private sb-text
  "Made-up Standard Bank text in the layout the PDF reader gives."
  (str/join "\n"
            ["Standard Bank of South Africa  standardbank.co.za"
             "From: 25 Dec 26   To: 05 Jan 27"
             "Payments -R1,250.50   Deposits R10,000.00"
             "Transaction details"
             "STATEMENT OPENING BALANCE 1,000.00"
             "27 Dec 26 WOOLWORTHS ROSEBANK 1234*5678 26 DEC -250.50 749.50"
             "CHEQUE CARD PURCHASE"
             "31 Dec 26 ACME HOLDINGS 10,000.00 10,749.50"
             "BOLSA THIRD PARTY PAYMENT SALARIES"
             "02 Jan 27 SB AUTOPAY -1,000.00 9,749.50"
             "ELECTRONIC TRF - CREDIT CARD"]))

(deftest standard-bank-reads-lines
  (is (sb/standard-bank-statement? sb-text))
  (let [{:keys [bank period transactions problems]} (sb/parse sb-text)]
    (is (= :standard-bank bank))
    (is (= ["2026-12-25" "2027-01-05"] period))
    (is (= [] problems))
    (is (= [["2026-12-27" 25050 :expenses] ["2026-12-31" 1000000 :income] ["2027-01-02" 100000 :expenses]]
           (map (juxt :date :amount-cents :type) transactions)))
    (is (= ["WOOLWORTHS ROSEBANK" "CHEQUE CARD PURCHASE"]
           ((juxt :description :bank-type) (first transactions))))))

(deftest standard-bank-reports-mismatches
  (is (seq (:problems (sb/parse (str/replace sb-text "Payments -R1,250.50" "Payments -R9.99")))))
  (is (seq (:problems (sb/parse (str/replace sb-text "10,749.50" "10,700.00"))))))

(deftest read-statement-text-dispatch
  (is (= :fnb (:bank (statements/read-statement-text fnb-text))))
  (is (= {:error :no-text} (statements/read-statement-text "  ")))
  (is (= :standard-bank (:bank (statements/read-statement-text sb-text))))
  (is (= {:error :unsupported-bank} (statements/read-statement-text "Some other bank\n01 Jan x 1.00 1.00"))))
