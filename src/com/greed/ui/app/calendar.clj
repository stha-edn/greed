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

(def ^:private hx-refresh {:status 200 :headers {"HX-Refresh" "true"} :body ""})

(defn create-event [ctx]
  (data/create-event ctx)
  hx-refresh)

(defn delete-event [ctx]
  (data/delete-event ctx)
  hx-refresh)
