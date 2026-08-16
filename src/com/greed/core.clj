(ns com.greed.core
  (:require [com.greed.utilities.tax :as tax]
            [com.greed.utilities.core :as utilities]))

(defn get-income-tax-data [user finances]
  (let [{:user/keys [age]
         :or {age 21}} (or user {})
        {:finances/keys [salary]
         :or {salary 0}} (or finances {})
        salary-num (when (number? salary) salary)
        annual-income (utilities/income->annual-income (or salary-num 0))]
    (tax/calculate-income-tax annual-income (or age 21))))
