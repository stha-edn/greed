(ns com.greed.utilities.tax
  (:require [com.core :as c]))

(def ^:private default-age-secondary-rebate 65)
(def ^:private default-age-tertiary-rebate 75)

(defn calculate-income-tax
  "Calculates South African income tax for individuals based on SARS tax rates.

   Parameters:
   - annual-income: Annual taxable income in Rand
   - age: Age of the taxpayer in years
   - config: (optional) tax config map — defaults to config/tax.edn

   Returns a map with tax calculation details"
  ([annual-income age] (calculate-income-tax annual-income age (c/get-tax-config)))
  ([annual-income age config]

  (when (or (not (number? annual-income)) (neg? annual-income))
    (throw (IllegalArgumentException. "Annual income must be a positive number")))

  (when (or (not (number? age)) (neg? age))
    (throw (IllegalArgumentException. "Age must be a positive number")))


  (let [tax config
        rebates-config (or (:rebates tax) {})
        thresholds-config (or (:thresholds tax) {})
        age-bands-config (or (:age-bands tax) {})
        age-tertiary (get age-bands-config :tertiary-rebate-from default-age-tertiary-rebate)
        age-secondary (get age-bands-config :secondary-rebate-from default-age-secondary-rebate)
        primary (get rebates-config :primary 0)
        secondary (get rebates-config :secondary 0)
        tertiary (get rebates-config :tertiary 0)
        tax-threshold (cond
                        (>= age age-tertiary) (get thresholds-config :age-75-plus 0)
                        (>= age age-secondary) (get thresholds-config :age-65-plus 0)
                        :else (get thresholds-config :under-65 0))
        ;; Determine which tax bracket applies (from config)
        brackets (or (:tax-brackets tax) [])
        bracket-index (->> brackets
                           (keep-indexed (fn [i b] (when (<= (get b :threshold 0) (or annual-income 0)) i)))
                           last)
        applicable-bracket (when bracket-index (nth brackets bracket-index))
        next-bracket (when (and bracket-index (< (inc bracket-index) (count brackets)))
                       (nth brackets (inc bracket-index)))
        ;; Calculate gross tax (guard when no bracket or missing config)
        excess (if applicable-bracket
                 (- (or annual-income 0) (get applicable-bracket :threshold 0))
                 0)
        gross-tax (if applicable-bracket
                    (+ (get applicable-bracket :base-tax 0)
                       (* excess (get applicable-bracket :rate 0)))
                    0)
        ;; Apply rebates based on age (from config age-bands)
        rebates (cond
                  (>= age age-tertiary) (+ primary secondary tertiary)
                  (>= age age-secondary) (+ primary secondary)
                  :else primary)

        ;; Final tax calculation
        net-tax (max 0 (- gross-tax (or rebates 0)))]

    ;; Return detailed tax calculation
    {:annual-income annual-income
     :age age
     :tax-threshold tax-threshold
     :gross-tax gross-tax
     :rebates rebates
     :net-tax net-tax
     :net-income (- annual-income net-tax)
     :effective-rate (if (pos? annual-income)
                       (* 100 (/ net-tax annual-income))
                       0)
     ;; Bracket context for the UI breakdown
     :brackets brackets
     :bracket-index bracket-index
     :bracket-threshold (when applicable-bracket (get applicable-bracket :threshold 0))
     :marginal-rate (if applicable-bracket (* 100 (get applicable-bracket :rate 0)) 0)
     :next-threshold (when next-bracket (get next-bracket :threshold 0))
     :income-to-next-bracket (when next-bracket
                               (max 0 (- (get next-bracket :threshold 0) (or annual-income 0))))})))


(comment

  (calculate-income-tax 600000 28)

  )
