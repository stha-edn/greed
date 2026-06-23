(ns com.greed.ui.app.calendar
  (:require [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.calendars :as calendars])
  (:import [java.time LocalDate]))

(defn- current-month-year []
  (let [now (LocalDate/now)]
    [(.getMonthValue now) (.getYear now)]))

(defn page [{:keys [session] :as ctx}]
  (let [user-id  (:uid session)
        finances (data/get-finances ctx user-id)
        payday   (:finances/payday finances)
        events   (data/get-events ctx user-id)
        [month year] (current-month-year)]
    (ui/app
     ctx
     [:div {:class "space-y-4"}
      (headers/pages-heading ["Calendar"])
      (calendars/calendar year month payday events)
      (calendars/events-panel ctx events)])))

(defn calendar-grid [{:keys [params session] :as ctx}]
  (let [user-id  (:uid session)
        month    (Integer/parseInt (:month params))
        year     (Integer/parseInt (:year params))
        finances (data/get-finances ctx user-id)
        payday   (:finances/payday finances)
        events   (data/get-events ctx user-id)]
    (calendars/calendar year month payday events)))

(defn- parse-cal-params [params]
  (let [now (LocalDate/now)]
    [(or (some-> (:cal-month params) Integer/parseInt) (.getMonthValue now))
     (or (some-> (:cal-year params)  Integer/parseInt) (.getYear now))]))

(defn- with-oob-calendar [ctx user-id month year events-panel]
  (let [finances (data/get-finances ctx user-id)
        payday   (:finances/payday finances)
        events   (data/get-events ctx user-id)
        cal      (calendars/calendar year month payday events)
        cal-oob  (into [:div {:id "calendar-grid" :hx-swap-oob "outerHTML"}] (rest cal))]
    [:<> events-panel cal-oob]))

(defn create-event [{:keys [params session] :as ctx}]
  (data/create-event ctx)
  (let [user-id      (:uid session)
        [month year] (parse-cal-params params)
        events       (data/get-events ctx user-id)]
    (with-oob-calendar ctx user-id month year (calendars/events-panel ctx events))))

(defn delete-event [{:keys [params session] :as ctx}]
  (data/delete-event ctx)
  (let [user-id      (:uid session)
        [month year] (parse-cal-params params)
        events       (data/get-events ctx user-id)]
    (with-oob-calendar ctx user-id month year (calendars/events-panel ctx events))))
