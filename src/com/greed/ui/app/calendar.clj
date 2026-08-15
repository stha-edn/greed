(ns com.greed.ui.app.calendar
  (:require [com.biffweb :as biff]
            [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.calendars :as calendars]
            [com.greed.ui.components.stats :as stats])
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
     [:div {:class "space-y-7"}
      (headers/pages-heading ["Calendar"])
      (stats/calendar-hero payday events)
      [:div {:class "grid grid-cols-1 gap-4 lg:grid-cols-3 lg:items-start"}
       [:div {:class "lg:col-span-2"} (calendars/calendar year month payday events)]
       [:div {:class "lg:col-span-1 space-y-4"}
        (calendars/todos-panel ctx events)
        (calendars/scheduled-panel ctx events)]]])))

(defn calendar-grid [{:keys [params session] :as ctx}]
  (let [user-id  (:uid session)
        month    (Integer/parseInt (:month params))
        year     (Integer/parseInt (:year params))
        finances (data/get-finances ctx user-id)
        payday   (:finances/payday finances)
        events   (data/get-events ctx user-id)]
    (calendars/calendar year month payday events)))

(def ^:private hx-refresh {:status 200 :headers {"HX-Refresh" "true"} :body ""})

(defn- fresh-events [ctx user-id]
  (data/get-events (biff/assoc-db ctx) user-id))

(defn create-event [{:keys [params session] :as ctx}]
  (data/create-event ctx)
  (let [user-id (:uid session)
        events  (fresh-events ctx user-id)]
    (if (empty? (:type params))
      (calendars/todos-panel ctx events)
      (calendars/scheduled-panel ctx events))))

(defn toggle-event [{:keys [session] :as ctx}]
  (data/toggle-event ctx)
  (let [user-id (:uid session)
        events  (fresh-events ctx user-id)]
    (calendars/todos-panel ctx events)))

(defn delete-event [ctx]
  (data/delete-event ctx)
  hx-refresh)
