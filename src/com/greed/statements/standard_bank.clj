(ns com.greed.statements.standard-bank
  "Reads the transaction lines of a Standard Bank statement, from the text of
   the PDF downloaded from online banking.

   Each transaction is a short block: a first line with the date
   (\"21 Aug 26\"), the details (merchant, account or reference), the amount
   (payments negative, deposits positive) and the running balance; then a line
   with Standard Bank's own transaction type (\"CHEQUE CARD PURCHASE\",
   \"IB PAYMENT TO\"). Depending on how the PDF is read, the amounts can come
   on a line of their own, so the reader takes every amount in the block and
   uses the last two: amount, then balance.

   The statement prints its own checks: the opening balance and, at the end,
   the total payments and deposits. The reader checks itself against both and
   against the running balance."
  (:require [clojure.string :as str])
  (:import [java.time LocalDate]))

(def ^:private months
  {"jan" 1 "feb" 2 "mar" 3 "apr" 4 "may" 5 "jun" 6
   "jul" 7 "aug" 8 "sep" 9 "oct" 10 "nov" 11 "dec" 12})

(def ^:private date-line #"^(\d{1,2}) (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) (\d{2})\b\s*(.*)$")

(def ^:private money-re #"-?[\d,]+\.\d{2}")

(def ^:private page-furniture
  "Lines that are part of the page, not a transaction: headers and footers
   repeated on every page, and the summary at the end."
  #"(?i)^(please verify|the standard bank of south africa|we subscribe|customer care|website:|standard bank$|\d+ month statement|from:|to:|account number|account holder|product name|transaction details|date description|statement summary|payments\b|deposits\b|today.s debits|pg \d+ of|\d{1,2} [a-z]{3} \d{4}$)")

(defn standard-bank-statement?
  [text]
  (boolean (and (re-find #"(?i)standardbank\.co\.za|Standard Bank of South Africa" text)
                (re-find #"(?i)Transaction details" text))))

(defn ->cents
  "\"-1,234.56\" -> -123456."
  [s]
  (Long/parseLong (str/replace s #"[,.]" "")))

(defn- short-date [d m yy]
  (LocalDate/of (+ 2000 (Integer/parseInt yy)) (int (months (str/lower-case m))) (Integer/parseInt d)))

(defn statement-period
  "[start end] from \"From: 12 Aug 26\" and \"To: 10 Nov 26\"."
  [text]
  (let [f (re-find #"(?i)From:\s*(\d{1,2}) ([A-Za-z]{3}) (\d{2})\b" text)
        t (re-find #"(?i)To:\s*(\d{1,2}) ([A-Za-z]{3}) (\d{2})\b" text)]
    (when (and f t (months (str/lower-case (nth f 2))) (months (str/lower-case (nth t 2))))
      [(apply short-date (rest f)) (apply short-date (rest t))])))

(defn clean
  "Drops what identifies a payment rather than the payee: amounts, times,
   card numbers, dates and long reference numbers."
  [s]
  (-> (str s)
      (str/replace money-re " ")
      (str/replace #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}" " ")
      (str/replace #"\b\d{2}-\d{2}-\d{4}\b" " ")
      (str/replace #"\b\d{2}H\d{2}(:\d{2})?\b" " ")
      (str/replace #"\b\d{4}\*\d{4}(\s+\d{1,2} [A-Z]{3})?" " ")
      (str/replace #"\*{2,}\d*" " ")
      (str/replace #"\+?\b\d{6,}\b" " ")
      (str/replace #"\s+" " ")
      str/trim))

(defn- blocks
  "Groups the text into one block per transaction."
  [lines]
  (loop [[line & more] lines, cur nil, out []]
    (if (nil? line)
      (cond-> out cur (conj cur))
      (let [l (str/trim line)
            m (re-matches date-line l)]
        (cond
          m (recur more {:date (vec (rest (butlast m))) :first (last m) :rest []} (cond-> out cur (conj cur)))
          (or (nil? cur) (str/blank? l)) (recur more cur out)
          (re-find page-furniture l) (recur more nil (conj out cur))
          (< (count (:rest cur)) 3) (recur more (update cur :rest conj l) out)
          :else (recur more nil (conj out cur)))))))

(defn- block->line [{[d m yy] :date first-line :first rest-lines :rest}]
  (let [text  (str/join " " (cons first-line rest-lines))
        money (re-seq money-re text)]
    (when (>= (count money) 2)
      (let [amount  (->cents (nth money (- (count money) 2)))
            balance (->cents (last money))]
        {:date (str (short-date d m yy))
         :description (let [c (clean first-line)] (if (str/blank? c) (clean (str/join " " rest-lines)) c))
         :bank-type (clean (str/join " " rest-lines))
         :signed-cents amount
         :balance-cents balance}))))

(defn- found [re text] (some-> (re-find re text) second ->cents))

(defn parse
  "Reads a Standard Bank statement's text. Returns
   {:bank :standard-bank :period [...] :transactions [{:date :description
   :bank-type :amount-cents :type}] :problems [...]}. A line with no amount
   (a waived fee shows only a balance) is not a transaction and is skipped."
  [text]
  (let [period   (statement-period text)
        all      (keep block->line (blocks (str/split-lines text)))
        ;; a block with one amount is a balance-only line: skipped by block->line
        rows     (vec (remove #(zero? (:signed-cents %)) all))
        opening  (found #"(?i)STATEMENT OPENING BALANCE\s+(-?[\d,]+\.\d{2})" text)
        breaks   (when opening
                   (->> (map vector (cons {:balance-cents opening} all) all)
                        (keep (fn [[prev row]]
                                (when (not= (+ (:balance-cents prev) (:signed-cents row)) (:balance-cents row))
                                  (:date row))))))
        payments (found #"(?i)Payments\s+-R([\d,]+\.\d{2})" text)
        deposits (found #"(?i)Deposits\s+R?([\d,]+\.\d{2})" text)
        sum      (fn [pred] (reduce + 0 (map :signed-cents (filter pred rows))))
        problems (cond-> []
                   (nil? period) (conj "The statement period could not be read.")
                   (empty? rows) (conj "No transaction lines were found.")
                   (and payments (not= payments (- (sum (comp neg? :signed-cents)))))
                   (conj "Payments do not match the statement's total.")
                   (and deposits (not= deposits (sum (comp pos? :signed-cents))))
                   (conj "Deposits do not match the statement's total.")
                   (seq breaks) (conj (str "The running balance does not add up on " (first breaks) ".")))]
    {:bank :standard-bank
     :period (when period (mapv str period))
     :transactions (mapv (fn [{:keys [signed-cents] :as r}]
                           (-> r
                               (assoc :amount-cents (Math/abs (long signed-cents))
                                      :type (if (pos? signed-cents) :income :expenses))
                               (dissoc :signed-cents :balance-cents)))
                         rows)
     :problems problems}))

(defn kind
  "How the money moved, from Standard Bank's transaction type line."
  [{:keys [bank-type type]}]
  (let [t (str/upper-case (str bank-type))
        in? (= :income type)
        has? #(re-find % t)]
    (cond
      (has? #"^RTD")                                                    :reversal
      (has? #"\bFEE\b|FEE-|FEE:|BANK CHARGES|SERVICE FEE|MONTHLY FEE")  :bank-charges
      (has? #"INTEREST")                                                (if in? :interest :interest-charged)
      (has? #"CREDIT LIFE|INSURANCE|ASSURANCE")                         :insurance
      (has? #"\bIB TRANSFER\b|TRANSFER FROM|TRANSFER TO")               :transfer
      (has? #"CREDIT CARD|ACCOUNT PAYMENT")                             :debt-payment
      (has? #"DEBIT ORDER|DC DEBIT|DEBIT TRANSFER|MND COLLECT|DEBICHECK") :debit-order
      (has? #"REFUND")                                                  (if in? :refund :card-purchase)
      (has? #"CARD PURCHASE|POS PURCHASE")                              :card-purchase
      (has? #"ATM|CASH WITHD")                                          :cash-withdrawal
      (has? #"CASH DEPOSIT")                                            :cash-deposit
      (has? #"PRE-?PAID")                                               :prepaid
      (has? #"INSTANT ?MON|CASH TO")                                    :send-money
      in?                                                               :payment-in
      (has? #"PAYMENT TO|IMMEDIATE PAYMENT|PAYSHAP")                    :payment-out
      :else                                                             :other)))
