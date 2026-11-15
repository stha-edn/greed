(ns com.greed.utilities.due-dates
  "A business's SARS due dates over a stretch of time, for its Calendar:
   each VAT period's VAT201 for a VAT vendor, and a (Pty) Ltd's provisional
   tax payments. Shown as calendar events greed adds itself; they are not
   stored and cannot be removed."
  (:require [com.greed.utilities.company-tax :as ct]
            [com.greed.utilities.vat :as vat])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]))

(defn- mon [^LocalDate d] (.format d (DateTimeFormatter/ofPattern "MMM")))

(defn sars-events
  "Due dates between `from` and `to` (inclusive) as event-shaped maps:
   {:event/title :event/date :event/type :bill :event/auto true}."
  [{:business/keys [entity-type vat-registered vat-category year-end-month]} ^LocalDate from ^LocalDate to]
  (let [in-range? (fn [^LocalDate d] (and (not (.isBefore d from)) (not (.isAfter d to))))
        vat-dues  (when (and vat-registered vat-category)
                    (->> (iterate #(vat/period vat-category (.plusDays ^LocalDate (:end %) 1))
                                  ;; start a period early so its due date can fall in range
                                  (vat/period vat-category (.minusMonths from 2)))
                         (take-while #(not (.isAfter ^LocalDate (:due %) to)))
                         (filter #(in-range? (:due %)))
                         (map (fn [{:keys [start end due months]}]
                                {:event/title (str "VAT201 due (" (mon start) (when (> months 1) (str "–" (mon end))) ")")
                                 :event/date (str due)}))))
        prov-dues (when (= :pty-ltd entity-type)
                    (for [fy (->> (iterate #(ct/financial-year (or year-end-month 2) (.plusDays ^LocalDate (:end %) 1))
                                           (ct/financial-year (or year-end-month 2) (.minusYears from 1)))
                                  (take-while #(not (.isAfter ^LocalDate (:start %) to))))
                          [label d] (let [{:keys [first second third]} (ct/provisional-dates fy)]
                                      [["First provisional tax payment" first]
                                       ["Second provisional tax payment" second]
                                       ["Optional third provisional tax payment" third]])
                          :when (in-range? d)]
                      {:event/title label :event/date (str d)}))]
    (->> (concat vat-dues prov-dues)
         (map #(assoc % :event/type :bill :event/auto true :xt/id (str "sars-" (:event/date %) "-" (hash (:event/title %)))))
         (sort-by :event/date)
         distinct
         vec)))
