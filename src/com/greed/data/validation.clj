(ns com.greed.data.validation
  (:require [com.greed.utilities.core :as utilities]))

(defn valid-payday? [payday]
  (and (>= payday 1)
       (<= payday 31)))

(defn ->valid-payday [payday]
  (let [day (utilities/->int payday)]
    (if (valid-payday? day)
      day
      (do (throw (ex-info "Invalid payday, defaulting to 1"
                          {:payday payday}))
          1))))

(defn valid-amount? [amount]
  (and (number? amount)
       (>= amount 0)))

(defn ->valid-amount [amount]
  (let [amt (utilities/->int amount)]
    (if (valid-amount? amt)
      amt
      (do (throw (ex-info "Invalid amount, defaulting to 0"
                          {:amount amount}))
          0))))
