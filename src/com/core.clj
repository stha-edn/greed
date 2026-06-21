(ns com.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))


(defn read-edn-resource
  "Read an EDN file from the resources directory.
   File should be in one of the paths specified in deps.edn:
   :paths [\"src\" \"resources\" \"target/resources\"]"
  [resource-path]
  (if-let [resource (io/resource resource-path)]
    (-> resource
        slurp
        edn/read-string)
    (throw (IllegalArgumentException.
            (str "Resource not found: " resource-path)))))

(defn get-tax-config
  "Reads config/tax.edn from resources. Called each time so config is always current."
  []
  (read-edn-resource "config/tax.edn"))

(defn get-tax-returns-config
  "Reads config/tax_returns.edn — 2026 year of assessment rates used by the tax returns simulator."
  []
  (read-edn-resource "config/tax_returns.edn"))

(def common-config
  (read-edn-resource "config/common.edn"))

(def alert-config
  (->> common-config
       (filter #(= "alert" (namespace (key %))))
       (into {})))

(comment

  (get-tax-config)
  common-config
  alert-config

  (get alert-config :alert/budget-item-saved)

  (:banking/banks common-config)

  (:finance/types common-config)

  (let [annual-income 1000000]
    (->> (get-tax-config)
         :tax-brackets
         (filter #(<= (:threshold %) annual-income))
         last))

  )
