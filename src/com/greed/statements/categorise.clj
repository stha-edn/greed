(ns com.greed.statements.categorise
  "Gives each statement line a kind (how the money moved) and a category (what
   it was for).

   Each bank's adapter decides the kind from its own wording (fnb/kind,
   standard-bank/kind). Salary is recognised here for every bank. The category
   comes from the kind, or else from a keyword list of well-known South African
   merchants; it is greed's own list, not the bank's, and anything it does not
   know is :other."
  (:require [clojure.string :as str]))

(def categories
  "Category keyword -> label, in a stable display order."
  (array-map
   :income        "Money in"
   :groceries     "Groceries"
   :fuel          "Fuel"
   :eating-out    "Eating out"
   :transport     "Transport and parking"
   :shopping      "Shopping"
   :subscriptions "Subscriptions"
   :bnpl          "Buy now, pay later"
   :debt          "Loans, credit and interest"
   :insurance     "Insurance"
   :airtime-data  "Airtime and data"
   :electricity   "Electricity"
   :debit-orders  "Debit orders"
   :cash          "Cash"
   :transfers     "Transfers"
   :bank-charges  "Bank charges"
   :other         "Other"))

(def kinds
  {:card-purchase    "Card purchase"
   :payment-out      "Payment out"
   :send-money       "PayShap, eWallet or Instant Money"
   :payment-in       "Payment in"
   :salary           "Salary"
   :refund           "Refund"
   :reversal         "Reversed by the bank"
   :transfer         "Own-account transfer"
   :bank-charges     "Bank charges"
   :cash-withdrawal  "Cash withdrawal"
   :cash-deposit     "Cash deposit"
   :debit-order      "Debit order"
   :debt-payment     "Loan or credit card payment"
   :interest         "Interest earned"
   :interest-charged "Interest charged"
   :insurance        "Insurance"
   :prepaid          "Prepaid"
   :other            "Other"})

(def ^:private merchants
  "First match wins. Keywords are matched as whole words, ignoring case."
  [[:groceries     ["checkers" "pnp" "pick n pay" "woolworths" "spar" "superspar" "shoprite" "makro" "food lover" "boxer" "usave"]]
   [:fuel          ["engen" "sasol" "shell" "bp" "caltex" "astron" "totalenergies" "total garage" "garage"]]
   [:eating-out    ["chicken licken" "kfc" "nando" "nandos" "mcdonald" "mcdonalds" "steers" "wimpy" "debonairs" "romans" "spur" "uber eats" "mr d" "starbucks" "vida"]]
   [:transport     ["admyt" "parking" "park" "uber" "bolt" "gautrain" "taxi" "toll"]]
   [:subscriptions ["apple.com" "google" "netflix" "spotify" "showmax" "dstv" "youtube" "canva" "microsoft" "openai" "adobe"]]
   [:bnpl          ["payflex" "payjustnow" "mobicred"]]
   [:debt          ["wesbank" "mfc" "credit card" "personal loan"]]
   [:insurance     ["discovery life" "old mutual" "sanlam" "outsurance" "miway" "hollard" "momentum" "credit life"]]
   [:shopping      ["takealot" "amazon" "mr price" "mrp" "pep" "ackermans" "edgars" "clicks" "dis-chem" "dischem" "game" "incredible connection" "mall of"]]
   [:airtime-data  ["airtime" "data bundle" "vodacom" "mtn" "cell c" "telkom"]]
   [:electricity   ["electricity" "eskom" "city power" "prepaid elec"]]])

(defn merchant-category [d]
  (let [d (str/lower-case (str d))]
    (some (fn [[cat words]]
            (when (some #(re-find (re-pattern (str "(?<![a-z])" (java.util.regex.Pattern/quote %) "(?![a-z])")) d) words)
              cat))
          merchants)))

(defn salary?
  "A payment in that is a salary or wage, whatever the bank calls it."
  [{:keys [type description bank-type]}]
  (and (= :income type)
       (boolean (re-find #"(?i)\b(salar(y|ies)|wages?|payroll)\b" (str description " " bank-type)))))

(defn category
  "What the line was for."
  [{:keys [description type]} k]
  (case k
    :bank-charges :bank-charges
    :transfer     :transfers
    (:cash-withdrawal :cash-deposit) :cash
    (:interest-charged :debt-payment) :debt
    :insurance    :insurance
    (if (and (= :income type) (not= :refund k))
      :income
      (or (merchant-category description)
          (when (= :debit-order k) :debit-orders)
          (when (= :prepaid k) :airtime-data)
          :other))))

(defn categorise
  "Adds :kind and :category to a statement line, using the bank adapter's
   kind function."
  [kind-fn line]
  (let [k (if (salary? line) :salary (kind-fn line))]
    (assoc line :kind k :category (category line k))))
