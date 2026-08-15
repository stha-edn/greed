(ns com.greed.utilities.time
  (:import [java.time LocalDate YearMonth]
           [java.time.format DateTimeFormatter]
           [java.time.temporal ChronoUnit]))


(defn days-until [today d]
  (long (.between ChronoUnit/DAYS today d)))

(defn relative-date
  "A human label for a day count: 0 → 'Today', 1 → 'Tomorrow', else 'in N days'."
  [n]
  (cond (zero? n) "Today"
        (= 1 n)   "Tomorrow"
        :else     (str "in " n " days")))

(defn format-date
  "A LocalDate as a short 'd MMM' label (e.g. 15 Aug)."
  [d]
  (.format d (DateTimeFormatter/ofPattern "d MMM")))

(defn format-event-date
  "A date string as a short 'd MMM' label, falling back to the raw string when
   it can't be parsed."
  [date]
  (try
    (format-date (LocalDate/parse date))
    (catch Exception _ date)))

(defn next-payday
  "Next payday as a LocalDate: today when today is payday, otherwise the
   coming occurrence of the configured day of month (clamped to the month's
   length, so a 31st payday lands on the 30th in a 30-day month)."
  [today payday]
  (when (and payday (pos? (long payday)))
    (let [year       (.getYear today)
          month      (.getMonthValue today)
          dom        (.getDayOfMonth today)
          this-month (YearMonth/of year month)
          this-pd    (min (long payday) (.lengthOfMonth this-month))]
      (if (<= dom this-pd)
        (LocalDate/of year month this-pd)
        (let [next (.plusMonths this-month 1)]
          (LocalDate/of (.getYear next) (.getMonthValue next)
                        (min (long payday) (.lengthOfMonth next))))))))

(defn prev-payday
  "Previous payday as a LocalDate: the payday before today (clamped to the
   month's length, mirroring next-payday)."
  [today payday]
  (when (and payday (pos? (long payday)))
    (let [year       (.getYear today)
          month      (.getMonthValue today)
          dom        (.getDayOfMonth today)
          this-month (YearMonth/of year month)
          this-pd    (min (long payday) (.lengthOfMonth this-month))]
      (if (> dom this-pd)
        (LocalDate/of year month this-pd)
        (let [prev (.minusMonths this-month 1)]
          (LocalDate/of (.getYear prev) (.getMonthValue prev)
                        (min (long payday) (.lengthOfMonth prev))))))))

(defn prev-month
  "The [month year] pair before the given month/year."
  [month year]
  (if (= month 1) [12 (dec year)] [(dec month) year]))

(defn next-month
  "The [month year] pair after the given month/year."
  [month year]
  (if (= month 12) [1 (inc year)] [(inc month) year]))

(defn first-day-of-week
  "The zero-indexed day-of-week of the 1st of a month (0 = Sunday), used for a
   calendar grid's leading blank cells."
  [year month]
  (mod (.getValue (.getDayOfWeek (LocalDate/of year month 1))) 7))

(comment

  (days-until (LocalDate/now) (LocalDate/now))

  (next-payday (LocalDate/now) 15)

  (first-day-of-week 2026 8)

  )
