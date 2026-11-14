(ns com.greed.statements.fnb
  "Reads the transaction lines of an FNB account statement, from the text of
   the PDF downloaded from online banking.

   An FNB line reads: day, short month, description, amount (\"Cr\" when money
   came in, nothing when it went out), the running balance (\"Cr\" when in
   credit) and, sometimes, the accrued bank charge. Lines have no year, so the
   year comes from the statement period at the top.

   The statement prints its own checks: the opening balance, and the number
   and total of credit and debit lines. The reader checks its result against
   them, so a layout change cannot pass silently."
  (:require [clojure.string :as str])
  (:import [java.time LocalDate]))

(def ^:private months
  {"jan" 1 "feb" 2 "mar" 3 "apr" 4 "may" 5 "jun" 6
   "jul" 7 "aug" 8 "sep" 9 "oct" 10 "nov" 11 "dec" 12
   "january" 1 "february" 2 "march" 3 "april" 4 "june" 6 "july" 7
   "august" 8 "september" 9 "october" 10 "november" 11 "december" 12})

(def ^:private money #"[\d,]+\.\d{2}")

(def ^:private line-re
  ;; 12 Oct  <description>  1,000.00 Cr  1,600.20 Cr  [8.00]
  #"^(\d{1,2}) (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\b\s*(.*?)\s*([\d,]+\.\d{2}) ?(Cr|Dr)?\s+([\d,]+\.\d{2}) ?(Cr|Dr)?(?:\s+[\d,]+\.\d{2})?\s*$")

(defn fnb-statement?
  "True when the text looks like an FNB statement."
  [text]
  (boolean (and (re-find #"(?i)First National Bank|fnb\.co\.za" text)
                (re-find #"(?i)Transactions in RAND" text))))

(defn ->cents
  "\"1,234.56\" -> 123456."
  [s]
  (Long/parseLong (str/replace s #"[,.]" "")))

(defn- signed [cents marker]
  (if (= "Cr" marker) cents (- cents)))

(defn- parse-date
  "LocalDate from a day and month name as strings, and a year as a string
   (from the statement period) or a number (from a LocalDate)."
  [d m y]
  (LocalDate/of (int (if (string? y) (Long/parseLong y) y))
                (int (months (str/lower-case m)))
                (int (Long/parseLong d))))

(defn statement-period
  "[start end] LocalDates from \"Statement Period : 12 October 2026 to 31 October 2026\"."
  [text]
  (when-let [[_ d1 m1 y1 d2 m2 y2]
             (re-find #"(?i)Statement Period\s*:?\s*(\d{1,2}) ([A-Za-z]+) (\d{4})\s+to\s+(\d{1,2}) ([A-Za-z]+) (\d{4})" text)]
    (when (and (months (str/lower-case m1)) (months (str/lower-case m2)))
      [(parse-date d1 m1 y1) (parse-date d2 m2 y2)])))

(defn- line-date
  "A line's date. The year is the statement end's year, or the one before when
   that would put the line after the statement ends (a December-to-January
   statement)."
  [d m [_ end]]
  (let [dt (parse-date d m (.getYear end))]
    (if (.isAfter dt end) (.minusYears dt 1) dt)))

(defn clean-description
  "Drops the masked card number and the card-swipe date FNB adds after POS
   lines, and names the bank's own fee lines, which have no description."
  [s]
  (let [s (-> s
              (str/replace #"\s*\d{6}\*\d{4}(\s+\d{1,2} [A-Z][a-z]{2})?\s*$" "")
              (str/replace #"\s+" " ")
              str/trim)]
    (if (str/blank? s) "Bank charges" s)))

(defn- opening-balance [text]
  (when-let [[_ amt marker] (re-find #"(?i)Opening Balance\s+([\d,]+\.\d{2}) ?(Cr|Dr)?" text)]
    ;; FNB prints a zero opening balance without a marker.
    (signed (->cents amt) (or marker (if (zero? (->cents amt)) "Cr" "Dr")))))

(defn- turnover [text kind]
  (when-let [[_ n amt] (re-find (re-pattern (str "(?i)No\\. " kind " Transactions\\s+(\\d+)\\s+([\\d,]+\\.\\d{2})")) text)]
    {:count (Long/parseLong n) :cents (->cents amt)}))

(defn parse
  "Reads an FNB statement's text. Returns
   {:bank :fnb
    :period [\"yyyy-MM-dd\" \"yyyy-MM-dd\"]
    :transactions [{:date :description :amount-cents (positive) :type (:income or :expenses)}]
    :problems [...]}  ; empty when every check the statement prints agrees"
  [text]
  (let [period (statement-period text)
        lines  (str/split-lines text)
        rows   (when period
                 (vec (for [line lines
                            :let [m (re-matches line-re (str/trim line))]
                            :when m
                            :let [[_ d mon desc amt amt-mark bal bal-mark] m
                                  cents (->cents amt)
                                  credit? (= "Cr" amt-mark)]]
                        {:date (str (line-date d mon period))
                         :description (clean-description desc)
                         :amount-cents cents
                         :type (if credit? :income :expenses)
                         :balance-cents (signed (->cents bal) (or bal-mark "Dr"))})))
        credits (filter #(= :income (:type %)) rows)
        debits  (filter #(= :expenses (:type %)) rows)
        sum     #(reduce + 0 (map :amount-cents %))
        opening (opening-balance text)
        breaks  (when opening
                  (->> (map vector (cons {:balance-cents opening} rows) rows)
                       (keep (fn [[prev row]]
                               (let [delta (if (= :income (:type row)) (:amount-cents row) (- (:amount-cents row)))]
                                 (when (not= (+ (:balance-cents prev) delta) (:balance-cents row))
                                   (:date row)))))))
        tc (turnover text "Credit")
        td (turnover text "Debit")
        problems (cond-> []
                   (nil? period) (conj "The statement period could not be read.")
                   (and period (empty? rows)) (conj "No transaction lines were found.")
                   (and period tc (not= tc {:count (count credits) :cents (sum credits)}))
                   (conj (str "Money in does not match the statement's total ("
                              (count credits) " lines read, " (:count tc) " expected)."))
                   (and period td (not= td {:count (count debits) :cents (sum debits)}))
                   (conj (str "Money out does not match the statement's total ("
                              (count debits) " lines read, " (:count td) " expected)."))
                   (seq breaks) (conj (str "The running balance does not add up on " (first breaks) ".")))]
    {:bank :fnb
     :period (when period (mapv str period))
     :transactions (mapv #(dissoc % :balance-cents) rows)
     :problems problems}))

(defn kind
  "How the money moved, from the fixed phrase FNB starts each line with."
  [{:keys [description type]}]
  (let [d (str description)
        in? (= :income type)
        has? #(boolean (re-find % d))]
    (cond
      (= "Bank charges" d)                                                     :bank-charges
      (has? #"(?i)\b(service fee|monthly (account )?fee|admin fee|bank charges?)\b") :bank-charges
      (has? #"(?i)\b(transfer (from|to)|inter-?account|internal transfer)\b")  :transfer
      (has? #"(?i)^pos purchase\b")                                            :card-purchase
      (has? #"(?i)\b(atm cash|cash withdrawal)\b")                             :cash-withdrawal
      (has? #"(?i)\b(cash deposit|adt cash)\b")                                :cash-deposit
      (has? #"(?i)\b(debit order|magtape debit)\b")                            :debit-order
      (has? #"(?i)\bprepaid\b")                                                :prepaid
      (has? #"(?i)\binterest\b")                                               (if in? :interest :interest-charged)
      (has? #"(?i)\b(reversal|returned)\b")                                    :reversal
      in?                                                                      :payment-in
      (has? #"(?i)\b(payshap(id)?|send money)\b")                              :send-money
      (has? #"(?i)\b(pmt to|payment to)\b")                                    :payment-out
      :else                                                                    :other)))
