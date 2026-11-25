(ns com.greed.categorise-test
  (:refer-clojure :exclude [cat])
  (:require [clojure.test :refer [deftest is are testing]]
            [com.greed.statements.categorise :as c]
            [com.greed.statements.fnb :as fnb]
            [com.greed.statements.standard-bank :as sb]))

(defn- k [d t] (fnb/kind {:description d :type t}))
(defn- cat [d t] (:category (c/categorise fnb/kind {:description d :type t})))
(defn- sb-line [d bank-type t] (c/categorise sb/kind {:description d :bank-type bank-type :type t}))

(deftest kinds-from-fnb-wording
  (are [d t expected] (= expected (k d t))
    "POS Purchase Engen Kyalami Garde"       :expenses :card-purchase
    "Internet Pmt To Netcash Ipay P2F My Vol" :expenses :payment-out
    "FNB App Payment To Sipho Dlamini"       :expenses :payment-out
    "FNB App Payment From Supplies"          :income   :payment-in
    "Rtc Credit B Mokoena"                   :income   :payment-in
    "Payshap Credit Online Resources"        :income   :payment-in
    "FNB App Transfer From Fund"             :income   :transfer
    "FNB App Transfer To Savings"            :expenses :transfer
    "Payshap Account Off-Us Sb Account"      :expenses :send-money
    "Send Money App Dr Send Thandi"          :expenses :send-money
    "Payshapid Off-Us The Rock"              :expenses :send-money
    "Bank charges"                           :expenses :bank-charges
    "#Monthly Account Fee"                   :expenses :bank-charges
    "ATM Cash Withdrawal Rosebank"           :expenses :cash-withdrawal
    "ADT Cash Deposit Sandton"               :income   :cash-deposit
    "Magtape Debit Discovery Life"           :expenses :debit-order
    "Prepaid Airtime Vodacom"                :expenses :prepaid
    "Credit Interest"                        :income   :interest
    "Something New"                          :expenses :other))

(deftest categories-from-merchants
  (are [d t expected] (= expected (cat d t))
    "POS Purchase PNP Exp Kyalami"        :expenses :groceries
    "POS Purchase Superspar Polofield"    :expenses :groceries
    "POS Purchase Woolworths"             :expenses :groceries
    "POS Purchase Sasol Waterfall"        :expenses :fuel
    "POS Purchase Chicken Licken Mall"    :expenses :eating-out
    "POS Purchase Admyt Parking"          :expenses :transport
    "POS Purchase Payflex"                :expenses :bnpl
    "POS Purchase Mall Of Africa"         :expenses :shopping
    "Prepaid Airtime Vodacom"             :expenses :airtime-data
    "Magtape Debit Discovery Life"        :expenses :insurance
    "Magtape Debit Virgin Active"         :expenses :debit-orders
    "POS Purchase Netflix.com"            :expenses :subscriptions
    "POS Purchase Vape King Crowthorn"    :expenses :other
    "FNB App Transfer From Fund"          :income   :transfers
    "Bank charges"                        :expenses :bank-charges
    "ATM Cash Withdrawal"                 :expenses :cash
    "Payshap Credit Online Resources"     :income   :income))

(deftest salary-for-every-bank
  (is (= :salary (:kind (c/categorise fnb/kind {:description "Acme Salaries Oct" :type :income}))))
  (is (= :salary (:kind (sb-line "ACME HOLDINGS" "BOLSA THIRD PARTY PAYMENT SALARIES" :income))))
  (is (= :income (:category (sb-line "ACME HOLDINGS" "BOLSA THIRD PARTY PAYMENT SALARIES" :income)))))

(deftest standard-bank-wording
  (are [d bank-type t kind category] (= [kind category] ((juxt :kind :category) (sb-line d bank-type t)))
    "WOOLWORTHS ROSEBANK"  "CHEQUE CARD PURCHASE"            :expenses :card-purchase   :groceries
    "SAVINGS"              "IB TRANSFER FROM"                :income   :transfer        :transfers
    "SB AUTOPAY"           "RTD-NOT PROVIDED FOR"            :income   :reversal        :income
    "IMMEDIATE PAYMENT"    "FEE IMMEDIATE PAYMENT"           :expenses :bank-charges    :bank-charges
    "INSUFFICIENT FUNDS"   "FEE- POS DECLINED INSUFF FUNDS"  :expenses :bank-charges    :bank-charges
    "OVERDRAFT"            "OVERDRAFT INTEREST"              :expenses :interest-charged :debt
    "CREDIT LIFE"          "CREDIT LIFE PROTECTION"          :expenses :insurance       :insurance
    "SB AUTOPAY"           "ELECTRONIC TRF - CREDIT CARD"    :expenses :debt-payment    :debt
    "GYM"                  "DEBICHECK DEBIT ORDER"           :expenses :debit-order     :debit-orders
    "ATM"                  "OTHER BANK ATM CASH WITHD. AT"   :expenses :cash-withdrawal :cash
    "GARAGE"               "REFUND/GARAGE CARD"              :income   :refund          :fuel))

(deftest business-categories
  (let [biz (fn [d bank-type t] (:category (c/categorise sb/kind {:description d :bank-type bank-type :type t} true)))
        own (fn [d bank-type t] (:category (c/categorise sb/kind {:description d :bank-type bank-type :type t})))]
    (testing "a business's payments out get business categories"
      (is (= :sars (biz "SARS-EFILING VAT201" "IB PAYMENT TO" :expenses)))
      (is (= :salaries (biz "STAFF" "BOLSA THIRD PARTY PAYMENT SALARIES" :expenses)))
      (is (= :rent (biz "OFFICE RENTAL" "IB PAYMENT TO" :expenses)))
      (is (= :software (biz "GOOGLE WORKSPACE" "CHEQUE CARD PURCHASE" :expenses)))
      (is (= :software (biz "NETFLIX.COM" "CHEQUE CARD PURCHASE" :expenses))))
    (testing "everything else is read as before"
      (is (= :fuel (biz "ENGEN" "CHEQUE CARD PURCHASE" :expenses)))
      (is (= :transfers (biz "SAVINGS" "IB TRANSFER TO" :expenses)))
      (is (= :income (biz "CLIENT" "PAYSHAP PAYMENT FROM" :income))))
    (testing "the personal side has no business categories"
      (is (= :subscriptions (own "NETFLIX.COM" "CHEQUE CARD PURCHASE" :expenses)))
      (is (= :other (own "OFFICE RENTAL" "IB PAYMENT TO" :expenses))))
    (testing "salary words mean money in on the personal side"
      (is (= :salary (:kind (c/categorise sb/kind {:description "ACME" :bank-type "SALARIES" :type :income})))))))
