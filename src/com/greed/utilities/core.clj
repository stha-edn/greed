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

  (->string :standard-bank)

  )
