(ns com.greed.ui.app.calendar
  "The Calendar of the current profile: Personal's events and payday, or a
   business's events with its SARS due dates added automatically."
  (:require [com.biffweb :as biff]
            [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.calendars :as calendars]
            [com.greed.ui.components.stats :as stats]
            [com.greed.utilities.due-dates :as due-dates]
            [com.greed.ui.components.hero :as hero])
  (:import [java.time LocalDate]))

(defn- current-month-year []
  (let [now (LocalDate/now)]
    [(.getMonthValue now) (.getYear now)]))

(defn- payday
  "Personal's payday; a business has none."
  [{:keys [session] :as ctx}]
  (when-not (data/session-business ctx)
    (:finances/payday (data/get-finances ctx (:uid session)))))

(defn events
  "The profile's events, by date. A business's include its SARS due dates
   from the start of this month to a year ahead."
  [ctx]
  (let [business (data/session-business ctx)
        stored   (data/get-events ctx (data/owner ctx))
        today    (LocalDate/now)]
    (if business
      (sort-by #(or (:event/date %) "")
               (concat stored (due-dates/sars-events business (.withDayOfMonth today 1) (.plusYears today 1))))
      stored)))

(defn- business-hero
  "A business has no payday: the same header card, leading with the next
   SARS date, and how far through the month it is."
  [events]
  (let [today  (LocalDate/now)
        ahead  (filter #(and (:event/date %) (not (.isBefore (LocalDate/parse (:event/date %)) today))) events)
        sars   (first (filter :event/auto ahead))
        d      (some-> sars :event/date LocalDate/parse)
        days   (when d (.between java.time.temporal.ChronoUnit/DAYS today d))
        length (.lengthOfMonth today)
        day    (.getDayOfMonth today)]
    (hero/card
     {:label "Next SARS date"
      :badge (when d (hero/badge (.format d (java.time.format.DateTimeFormatter/ofPattern "d MMM")) :tone (when (< days 14) :rose)))
      :value (cond (nil? d) "-" (zero? days) "Today" :else (str days))
      :unit (cond (nil? d) "no SARS dates yet" (zero? days) "is due" :else (str "day" (when (not= 1 days) "s") " to go"))
      :reading (if sars
                 (:event/title sars)
                 "Register for VAT, or set a company's financial year, in Settings.")
      :picture (hero/bars {:title (.format today (java.time.format.DateTimeFormatter/ofPattern "MMMM yyyy"))
                           :of length
                           :rows [{:label "Days gone" :value (str day " of " length) :amount day :cls "bg-zinc-400"}
                                  {:label "Days left" :value (str (- length day)) :amount (- length day) :cls "bg-brand-600"}]})
      :stats [["SARS dates" (str (count (filter :event/auto ahead)))]
              ["Bills" (str (count (filter #(and (= :bill (:event/type %)) (not (:event/auto %))) ahead)))]
              ["Payments in" (str (count (filter #(= :income (:event/type %)) ahead)))]]})))

(defn page [ctx]
  (let [pd     (payday ctx)
        events (events ctx)
        [month year] (current-month-year)]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (headers/pages-heading ["Calendar"]
                             (if-let [b (data/session-business ctx)]
                               (str (:business/name b) "'s dates. SARS due dates are added for you.")
                               "Your paydays, bills and to-dos."))
      (if (data/session-business ctx)
        (business-hero events)
        (stats/calendar-hero pd events))
      [:div {:class "grid grid-cols-1 gap-4 lg:grid-cols-3 lg:items-start"
             ;; Lives here rather than on the nav buttons: those are inside
             ;; #calendar-grid and die with each swap, so they could never
             ;; hear their own afterRequest to un-dim. Bubbled events from
             ;; whichever button fired keep this wrapper alive.
             :_ "on htmx:beforeRequest from #calendar-grid add .opacity-50 to #calendar-grid
on htmx:afterRequest remove .opacity-50 from #calendar-grid"}
       [:div {:class "lg:col-span-2"} (calendars/calendar year month pd events)]
       [:div {:class "lg:col-span-1 space-y-4"}
        (calendars/todos-panel ctx events)
        (calendars/scheduled-panel ctx events)]]])))

(defn calendar-grid [{:keys [params] :as ctx}]
  (let [month (Integer/parseInt (:month params))
        year  (Integer/parseInt (:year params))]
    (calendars/calendar year month (payday ctx) (events ctx))))

(def ^:private hx-refresh {:status 200 :headers {"HX-Refresh" "true"} :body ""})

(defn create-event [{:keys [params] :as ctx}]
  (data/create-event ctx)
  (let [events (events (biff/assoc-db ctx))]
    (if (empty? (:type params))
      (calendars/todos-panel ctx events)
      (calendars/scheduled-panel ctx events))))

(defn toggle-event [ctx]
  (data/toggle-event ctx)
  (calendars/todos-panel ctx (events (biff/assoc-db ctx))))

(defn delete-event [ctx]
  (data/delete-event ctx)
  hx-refresh)
