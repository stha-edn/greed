(ns com.greed.statements.summary
  "Turns a read statement (lines with :kind and :category) into the totals the
   Spending tab keeps, one summary per calendar month. Pure; the lines
   themselves are not kept."
  (:require [clojure.string :as str])
  (:import [java.time LocalDate YearMonth]))

(def ^:private prefixes
  "FNB's wording before the merchant or person, longest first."
  [#"(?i)^pos purchase\s+"
   #"(?i)^internet pmt to\s+"
   #"(?i)^fnb app payment (to|from)\s+"
   #"(?i)^send money app dr send\s+"
   #"(?i)^payshap(id)? (account )?off-us\s+"
   #"(?i)^payshap credit\s+"
   #"(?i)^rtc credit\s+"
   #"(?i)^magtape debit\s+"
   #"(?i)^prepaid\s+"])

(defn- stable-word
  "The part of a statement word that stays the same from month to month, or
   nil. Account numbers, dates and reference codes change, so a word with a
   digit in it keeps only the letters it starts with, and only when there are
   at least 3 of them:
     SBSAVAFNO20007921542 -> SBSAVAFNO
     FI85346911121002260825 -> nil (2 letters is too little to go on)
     P2F5AED0 -> nil, 20260825 -> nil"
  [w]
  (cond
    (str/blank? w)          nil
    (not (re-find #"\d" w)) w
    :else (let [[_ letters] (re-find #"^(\p{L}{3,})\d" w)] letters)))

(defn- words
  "The stable words of a statement text: split on spaces and punctuation
   (WESBANK_FI8534...0825 -> WESBANK FI8534...0825), then each word cut to
   its stable part (see stable-word)."
  [s]
  (keep stable-word (str/split (str s) #"[^\p{L}\p{N}&']+")))

(defn merchant-name
  "A readable name for the merchant or person on a line: FNB's wording and
   reference codes (tokens mixing letters and digits) removed.
   \"Internet Pmt To Netcash Ipay P2F5AED0 My Vol\" -> \"Netcash Ipay My Vol\"."
  [description]
  (let [d (reduce (fn [s re] (str/replace s re "")) (str description) prefixes)
        n (str/join " " (words d))]
    (if (str/blank? n) (str/trim (str description)) n)))

(defn cancel-reversals
  "Takes out each payment the bank reversed, together with its reversal: the
   latest earlier payment of the same amount. A reversal with no such payment
   (it reverses something before the statement) counts as a refund."
  [lines]
  (let [out-of (fn [kept {:keys [amount-cents]}]
                 (some (fn [i] (let [l (kept i)]
                                 (when (and (= :expenses (:type l)) (not= :transfer (:kind l))
                                            (= amount-cents (:amount-cents l)) (not (::gone l)))
                                   i)))
                       (range (dec (count kept)) -1 -1)))]
    (->> lines
         (reduce (fn [kept l]
                   (if (and (= :reversal (:kind l)) (= :income (:type l)))
                     (if-let [i (out-of kept l)]
                       (assoc-in kept [i ::gone] true)
                       (conj kept (assoc l :kind :refund :category :other)))
                     (conj kept l)))
                 [])
         (remove ::gone)
         vec)))

(defn normalise
  "Statement text or a reference in the form they are matched in: the stable
   words, lower case, single spaces."
  [s]
  (str/lower-case (str/join " " (words s))))

(defn references
  "The match phrases in a budget item's statement reference: comma-separated
   and normalised, so a reference pasted from a statement with its changing
   numbers still matches (\"WESBANK_FI8534...0825\" -> \"wesbank\"). Phrases
   under 3 characters are dropped so a stray letter matches nothing."
  [s]
  (->> (str/split (str s) #",")
       (map normalise)
       (filter #(>= (count %) 3))
       distinct
       vec))

(defn- budget-items
  "Budget items ({:id :title :reference}) that have match words."
  [items]
  (->> items
       (map #(assoc % :words (references (:reference %))))
       (filter (comp seq :words))
       vec))

(defn payee-text
  "The statement text kept for a payee: the stable words of the line's
   description and the bank's transaction type (see `words`)."
  [{:keys [description bank-type]}]
  (str/join " " (words (str description " " bank-type))))

(defn payees
  "Totals per payee for a set of lines: what was paid to them and refunded by
   them. Transfers between own accounts are left out. This is what the
   budget sync matches references against, so no line is kept."
  [lines]
  (->> lines
       (filter #(and (not= :transfer (:kind %))
                     (or (= :expenses (:type %)) (= :refund (:kind %)))))
       (group-by #(normalise (payee-text %)))
       (map (fn [[_ ls]]
              (let [paid (filter #(= :expenses (:type %)) ls)]
                {:text (payee-text (first ls))
                 :category (:category (or (first paid) (first ls)))
                 :spent-cents (reduce + 0 (map :amount-cents paid))
                 :refund-cents (reduce + 0 (map :amount-cents (remove #(= :expenses (:type %)) ls)))
                 :count (count paid)})))
       (sort-by (juxt (comp - :spent-cents) :text))
       vec))

(defn budget-totals
  "What was paid towards each budget item with a reference, refunds taken off.
   A payee counts towards the first item whose reference words appear in its
   text. `items` are {:id :title :reference}; `payees` come from `payees`."
  [items payees]
  (let [items   (budget-items items)
        item-of (fn [{:keys [text]}]
                  ;; whole words only: "bp" must not match inside "bpay"
                  (let [t (str " " (normalise text) " ")]
                    (some (fn [{:keys [words] :as i}] (when (some #(str/includes? t (str " " % " ")) words) i)) items)))
        by-item (group-by (comp :id item-of) payees)
        matches (vec (for [{:keys [id title]} items
                           :let [ps (by-item id)]]
                       {:item-id id :title title
                        :cents (max 0 (- (reduce + 0 (map :spent-cents ps)) (reduce + 0 (map :refund-cents ps))))
                        :count (reduce + 0 (map :count ps))}))]
    {:budget-matches matches
     :budgeted-cents (reduce + 0 (map :cents matches))}))

(defn- totals
  "Totals for a set of lines (reversals already cancelled)."
  [lines]
  (let [sum        #(reduce + 0 (map :amount-cents %))
        transfer?  #(= :transfer (:kind %))
        refund?    #(= :refund (:kind %))
        in         (filter #(= :income (:type %)) lines)
        out        (filter #(= :expenses (:type %)) lines)
        spend      (remove transfer? out)
        refunds    (filter refund? in)
        fees       (filter #(= :bank-charges (:kind %)) out)
        by-cat     #(update-vals (group-by :category %) sum)
        ;; refunds count against their category; a category with only
        ;; refunds this month has no spending to show, so it drops out
        categories (->> (merge-with + (by-cat spend) (update-vals (by-cat refunds) -))
                        (filter (comp pos? val))
                        (into {}))
        merchants  (->> spend
                        (remove #(= :bank-charges (:kind %)))
                        (group-by #(str/lower-case (merchant-name (:description %))))
                        (map (fn [[_ ls]]
                               {:name (merchant-name (:description (first ls)))
                                :category (:category (first ls))
                                :cents (sum ls)
                                :count (count ls)}))
                        (sort-by (juxt (comp - :cents) :name))
                        (take 5)
                        vec)]
    {:line-count (count lines)
     :money-in-cents (sum (remove #(or (transfer? %) (refund? %)) in))
     :salary-cents (sum (filter #(= :salary (:kind %)) in))
     :refunds-cents (sum refunds)
     :money-out-cents (max 0 (- (sum spend) (sum refunds)))
     :transfers-in-cents (sum (filter transfer? in))
     :transfers-out-cents (sum (filter transfer? out))
     :categories categories
     :fees-cents (sum fees)
     :fee-count (count fees)
     :top-merchants merchants}))

(defn summarise
  "Totals for a whole statement. `statement` is the reader's result with
   categorised lines ({:bank :period :transactions :problems}). Returns a map
   whose keys match :statement-summary/* (unqualified), without :month."
  [{:keys [bank period transactions problems]} & [items]]
  (let [lines (cancel-reversals transactions)
        ps    (payees lines)]
    (merge {:bank bank
            :period-start (first period)
            :period-end (second period)
            :checks-ok (empty? problems)
            :payees ps}
           (totals lines)
           (budget-totals items ps))))

(defn summarise-months
  "One summary per calendar month the statement covers, each with :month
   (\"2026-10\") and the part of the statement period inside that month.
   Reversals are cancelled across the whole statement first, so a payment
   reversed early the next month still drops out. `items` are the user's
   budget items as {:id :title :reference}; payments matching a reference
   are totalled per item (see budget-totals)."
  [{:keys [bank period transactions problems]} & [items]]
  (let [lines  (cancel-reversals transactions)
        dates  (map :date lines)
        [start end] (if (seq period)
                      (map #(LocalDate/parse %) period)
                      (map #(LocalDate/parse %) [(first (sort dates)) (last (sort dates))]))
        months (when (and start end)
                 (take-while #(not (.isAfter (.atDay ^YearMonth % 1) end))
                             (iterate #(.plusMonths ^YearMonth % 1) (YearMonth/from start))))
        by-month (group-by #(subs (:date %) 0 7) lines)]
    (vec (for [^YearMonth ym months
               :let [m (str ym)
                     from (if (.isAfter (.atDay ym 1) start) (.atDay ym 1) start)
                     to   (if (.isBefore (.atEndOfMonth ym) end) (.atEndOfMonth ym) end)]]
           (merge {:bank bank
                   :month m
                   :period-start (str from)
                   :period-end (str to)
                   :checks-ok (empty? problems)}
                  (let [ps (payees (get by-month m []))]
                    (merge {:payees ps}
                           (totals (get by-month m []))
                           (budget-totals items ps))))))))
