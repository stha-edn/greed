(ns com.greed.utilities.core
  (:require [clojure.string :as string]))

(defn ->int
  "Converts a string to an integer.
   Parameters:
   - s: String to convert
   Returns integer value"
  [s]
  (try
    (Integer/parseInt s)
    (catch NumberFormatException _
      nil)))

(defn income->annual-income
  "Converts monthly income to annual income.
   Parameters:
   - monthly-income: Monthly income in Rand
   Returns annual income in Rand"
  [monthly-income]

  (* monthly-income 12))

(defn annual-income->monthly-income
  "Converts annual income to monthly income.
   Parameters:
   - annual-income: Annual income in Rand
   Returns monthly income in Rand"
  [annual-income]
  (/ annual-income 12))

(defn format-currency
  "Formats a number as currency.
   Parameters:
   - amount: Amount in cents
   Returns formatted currency string"
  [amount]
  (let [formatter (doto (java.text.NumberFormat/getInstance)
                    (.setMinimumFractionDigits 2)
                    (.setMaximumFractionDigits 2))]
    (str "R" (.format formatter amount))))

(defn amount->rands
  "Converts amount in cents to Rand.
   Parameters:
   - amount: Amount in cents
   Returns amount in Rand"
  [amount]
  (format-currency amount))

(defn ->percentage
  "Converts a double to a percentage.
   Parameters:
   - d: double to convert
   Returns percentage value"
  [d]
  (format "%.2f%%" (double d)))

(defn fmt-d
  "Formats a number for HTML attributes (SVG dasharray/dashoffset, CSS widths)
   with a dot decimal separator regardless of the JVM locale — en-ZA would
   otherwise emit commas, which break attribute parsing."
  [f n]
  (String/format (java.util.Locale/ROOT) f (object-array [(double n)])))

(defn pct-share
  "Share of `amount` as a percentage of `total`, or nil when `total` isn't
   positive (nothing to divide against)."
  [amount total]
  (when (pos? (double (or total 0)))
    (double (* 100.0 (/ (double (or amount 0)) (double total))))))

(defn whole->rands
  "Rands at display scale — whole, no trailing decimals — for large hero
   figures where a '.00' suffix would read as noise."
  [n]
  (format "R%,d" (long (Math/round (double (or n 0))))))

(defn rate-label
  "Formats a rate as a fraction (e.g. 0.26) as '26%'."
  [rate]
  (str (int (Math/round (* 100 (double (or rate 0))))) "%"))

(defn pct-label
  "Formats an already-percentage value (e.g. 31.0) as '31%'."
  [p]
  (str (int (Math/round (double (or p 0)))) "%"))

(defn goal-pct
  "Progress through a goal: saved/target as a 0–100 integer (0 when target
   isn't positive)."
  [saved target]
  (if (and target (pos? target))
    (int (min 100 (Math/round (* 100.0 (/ (double (or saved 0)) target)))))
    0))

(defn mock-last-four
  "Deterministic decorative last-4 digits for the card mockup, derived from a
   stable seed so it doesn't shuffle on every render. Purely cosmetic — never
   sourced from or resembling a real card number."
  [seed]
  (format "%04d" (mod (Math/abs (hash seed)) 10000)))

(defn ->keyword
  "Converts a string to a keyword.
   Parameters:
   - s: String to convert
   Returns keyword value"
  [s]
  (-> s
      (string/lower-case)
      (string/trim)
      (string/replace #" " "-")
      (string/replace #"\." "-")
      (string/replace #"/" "-")
      (string/replace #"_" "-")
      keyword))

(defn ->string
  "Converts a keyword to a string.
   Parameters:
   - k: Keyword to convert
   Returns string value"
  [k]
  (-> k
      name
      (string/replace #"-" " ")
      (string/capitalize)))

(defn ->uuid
  "Converts a string to a UUID.
   Parameters:
   - s: String to convert
   Returns UUID value"
  [s]
  (try
    (java.util.UUID/fromString s)
    (catch IllegalArgumentException _
      nil)))

(defn ordinal [n]
  (let [n (int n)
        suffix (cond
                 (#{11 12 13} (mod n 100)) "th"
                 (= 1 (mod n 10)) "st"
                 (= 2 (mod n 10)) "nd"
                 (= 3 (mod n 10)) "rd"
                 :else "th")]
    (str n suffix)))

(comment

  (->int "1234")

  (->keyword "Mastercard")

  (->string :standard-bank))
