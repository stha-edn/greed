(ns com.greed.ui.components.calendars
  (:require [com.biffweb :as biff])
  (:import [java.time LocalDate YearMonth]))


(def ^:private type-dot
  {:bill "bg-rose-400" :income "bg-emerald-400" :general "bg-violet-400"})

(def ^:private type-label
  {:bill "Bill" :income "Payment in" :general "Event"})

(defn- event-row [{:event/keys [title date type] :xt/keys [id]}]
  (let [type (or type :general)]
    [:div {:class "flex items-center justify-between py-2.5 border-b border-zinc-50 last:border-0"}
     [:div {:class "flex items-center gap-3"}
      [:div {:class (str "w-2 h-2 rounded-full flex-shrink-0 " (get type-dot type "bg-violet-400"))}]
      [:div
       [:p {:class "text-sm font-medium text-zinc-800"} title]
       [:p {:class "text-xs text-zinc-400"} (str (get type-label type "Event") " · " date)]]]
     (biff/form {:hx-post "/app/calendar/delete-event"
                 :hx-target "#calendar-events"
                 :hx-swap "outerHTML"
                 :hx-include "#cal-month, #cal-year"
                 :class "flex"}
       [:input {:type "hidden" :name "event-id" :value (str id)}]
       [:button {:type "submit"
                 :onclick "return confirm('Remove this event?')"
                 :class "text-xs text-zinc-400 hover:text-red-500 transition-colors px-2 py-1"}
        "Remove"])]))

(defn events-panel [_ctx events]
  [:div#calendar-events {:class "bg-white rounded-xl border border-zinc-200/70 shadow-card p-5"}
   [:div {:x-data "{ showForm: false }"}
    [:div {:class "flex items-center justify-between mb-4"}
     [:p {:class "text-xs font-medium text-zinc-400 uppercase tracking-wider"} "Events"]
     [:button {:class "text-xs font-medium text-emerald-600 hover:text-emerald-700"
               :x-on:click "showForm = !showForm"}
      "＋ Add event"]]
    [:div {:x-show "showForm" :x-cloak "" :class "mt-3"}
     (biff/form {:hx-post "/app/calendar/create-event"
                 :hx-target "#calendar-events"
                 :hx-swap "outerHTML"
                 :hx-include "#cal-month, #cal-year"
                 :class "flex flex-col sm:flex-row gap-2"}
       [:input {:type "text" :name "title" :required true
                :placeholder "Event title"
                :class "flex-1 px-3 py-2 text-sm border border-zinc-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-zinc-400"}]
       [:select {:name "type"
                 :class "px-3 py-2 text-sm border border-zinc-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-zinc-400 text-zinc-700"}
        [:option {:value "general"} "Event"]
        [:option {:value "bill"} "Bill"]
        [:option {:value "income"} "Payment in"]]
       [:input {:type "date" :name "date" :required true
                :class "px-3 py-2 text-sm border border-zinc-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-zinc-400"}]
       [:button {:type "submit"
                 :class "px-4 py-2 text-sm font-medium text-white bg-zinc-900 rounded-lg hover:bg-zinc-700"}
        "Save"])]]
   (if (seq events)
     [:div {:class "mt-4 divide-y divide-zinc-100"}
      (map event-row events)]
     [:div {:class "mt-2 flex flex-col items-center justify-center py-8 text-center"}
      [:div {:class "w-10 h-10 rounded-full bg-zinc-50 flex items-center justify-center mb-3 text-zinc-300"}
       [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
        [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                :d "M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"}]]]
      [:p {:class "text-sm font-medium text-zinc-500"} "No events yet"]
      [:p {:class "mt-0.5 text-xs text-zinc-400"} "Add one to track paydays and bills"]])])

(def ^:private month-names
  ["January" "February" "March" "April" "May" "June"
   "July" "August" "September" "October" "November" "December"])

(def ^:private day-names ["Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"])

(defn- prev-month [month year]
  (if (= month 1) [12 (dec year)] [(dec month) year]))

(defn- next-month [month year]
  (if (= month 12) [1 (inc year)] [(inc month) year]))

(defn- first-day-of-week
  "Returns 0=Sun..6=Sat for the first day of the given month/year."
  [year month]
  (mod (.getValue (.getDayOfWeek (LocalDate/of year month 1))) 7))

(def ^:private type-priority {:bill 0 :income 1 :general 2})

(defn- event-day-types
  "Map of day-of-month -> dominant event type for the given month.
   When a day has multiple events, bills take precedence, then income."
  [events year month]
  (->> events
       (keep (fn [{:event/keys [date type]}]
               (when date
                 (let [d (LocalDate/parse date)]
                   (when (and (= (.getYear d) year) (= (.getMonthValue d) month))
                     [(.getDayOfMonth d) (or type :general)])))))
       (reduce (fn [m [day type]]
                 (if-let [cur (get m day)]
                   (if (< (get type-priority type 2) (get type-priority cur 2))
                     (assoc m day type) m)
                   (assoc m day type)))
               {})))

(def ^:private type-cell-bg
  {:bill "bg-rose-50 " :income "bg-emerald-50 " :general "bg-violet-50 "})

(defn calendar [year month payday events]
  (let [today       (LocalDate/now)
        today-day   (.getDayOfMonth today)
        today-month (.getMonthValue today)
        today-year  (.getYear today)
        days-count  (.lengthOfMonth (YearMonth/of year month))
        blank-count (first-day-of-week year month)
        event-days  (event-day-types events year month)
        [pm py]     (prev-month month year)
        [nm ny]     (next-month month year)]
    [:div#calendar-grid
     [:input {:type "hidden" :id "cal-month" :name "cal-month" :value (str month)}]
     [:input {:type "hidden" :id "cal-year"  :name "cal-year"  :value (str year)}]
     [:div {:class "bg-white rounded-xl border border-zinc-200/70 shadow-card overflow-hidden"}
      [:div {:class "flex items-center justify-between py-3 px-4 sm:px-6"}
       [:div
        [:span {:class "text-2xl sm:text-3xl font-giza font-bold text-zinc-800"}
         (nth month-names (dec month))]
        [:span {:class "ml-1.5 text-xl sm:text-2xl text-zinc-400 font-giza"} (str year)]]
       [:div {:class "flex items-center gap-1"}
        [:button {:class "p-1.5 rounded-lg hover:bg-zinc-100 transition-colors text-zinc-500 hover:text-zinc-800"
                  :hx-get (str "/app/calendar/grid?month=" pm "&year=" py)
                  :hx-target "#calendar-grid"
                  :hx-swap "outerHTML"}
         [:svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
          [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M15 19l-7-7 7-7"}]]]
        [:button {:class "p-1.5 rounded-lg hover:bg-zinc-100 transition-colors text-zinc-500 hover:text-zinc-800"
                  :hx-get (str "/app/calendar/grid?month=" nm "&year=" ny)
                  :hx-target "#calendar-grid"
                  :hx-swap "outerHTML"}
         [:svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
          [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M9 5l7 7-7 7"}]]]]]
      [:div {:class "flex flex-wrap border-b border-zinc-100"}
       (for [d day-names]
         [:div {:class "py-2 w-[14.28%] text-center text-xs text-zinc-400 font-medium"} d])]
      [:div {:class "flex flex-wrap border-l border-zinc-100"}
       (for [_ (range blank-count)]
         [:div {:class "border-r border-b border-zinc-100 h-10 sm:h-16 w-[14.28%]"}])
       (for [d (range 1 (inc days-count))]
         (let [is-today    (and (= d today-day) (= month today-month) (= year today-year))
               is-payday   (= d payday)
               event-type  (get event-days d)
               cell-bg     (cond is-payday  "bg-emerald-50 "
                                 event-type (get type-cell-bg event-type "bg-violet-50 ")
                                 :else "")
               num-cls     (if is-today "bg-zinc-900 text-white shadow-card-md" "text-zinc-700")]
           [:div {:class (str "border-r border-b border-zinc-100 h-10 sm:h-16 w-[14.28%] "
                              "flex items-start justify-center pt-1.5 transition-colors hover:bg-zinc-50 "
                              cell-bg)}
            [:div {:class (str "w-7 h-7 flex items-center justify-center rounded-full "
                               "text-sm font-medium transition-colors " num-cls)}
             d]]))]]
     [:div {:class "flex flex-wrap items-center gap-4 mt-2 px-1"}
      [:div {:class "flex items-center gap-1.5"}
       [:div {:class "w-2.5 h-2.5 rounded bg-emerald-100"}]
       [:span {:class "text-xs text-zinc-400"} "Payday"]]
      [:div {:class "flex items-center gap-1.5"}
       [:div {:class "w-2.5 h-2.5 rounded bg-rose-100"}]
       [:span {:class "text-xs text-zinc-400"} "Bill"]]
      [:div {:class "flex items-center gap-1.5"}
       [:div {:class "w-2.5 h-2.5 rounded bg-violet-100"}]
       [:span {:class "text-xs text-zinc-400"} "Event"]]
      [:div {:class "flex items-center gap-1.5"}
       [:div {:class "w-5 h-5 rounded-full bg-zinc-900 flex items-center justify-center"}
        [:span {:class "text-white text-xs"} today-day]]
       [:span {:class "text-xs text-zinc-400"} "Today"]]]]))
