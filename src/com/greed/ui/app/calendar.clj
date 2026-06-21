(ns com.greed.ui.app.calendar
  (:require [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [clojure.string :as str]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.calendars :as calendars])
  (:import [java.time LocalDate]))


(defn- current-month-event-days [events]
  (let [now (LocalDate/now)
        month (.getMonthValue now)
        year (.getYear now)]
    (->> events
         (filter (fn [{:event/keys [date]}]
                   (when date
                     (let [d (LocalDate/parse date)]
                       (and (= (.getMonthValue d) month)
                            (= (.getYear d) year))))))
         (map (fn [{:event/keys [date]}]
                (.getDayOfMonth (LocalDate/parse date))))
         vec)))

(defn- event-days-js [days]
  (str "[" (str/join "," days) "]"))

(defn page [{:keys [session] :as ctx}]
  (let [user-id  (:uid session)
        finances (data/get-finances ctx user-id)
        payday   (:finances/payday finances)
        events   (data/get-events ctx user-id)
        days     (event-days-js (current-month-event-days events))]
    (ui/app
     ctx
     [:div {:class "space-y-4"}
      (headers/pages-heading ["Calendar"])
      (calendars/calendar payday days)
      (calendars/events-panel ctx events)])))

(defn create-event [{:keys [session] :as ctx}]
  (data/create-event ctx)
  (let [user-id (:uid session)
        events  (data/get-events ctx user-id)]
    (calendars/events-panel ctx events)))

(defn delete-event [{:keys [session] :as ctx}]
  (data/delete-event ctx)
  (let [user-id (:uid session)
        events  (data/get-events ctx user-id)]
    (calendars/events-panel ctx events)))
