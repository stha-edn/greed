(ns com.greed.ui.components.calendars
  (:require [com.biffweb :as biff]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.svgs :as svgs])
  (:import [java.time LocalDate YearMonth]
           [java.time.format DateTimeFormatter]))

(def ^:private month-names
  ["January" "February" "March" "April" "May" "June"
   "July" "August" "September" "October" "November" "December"])

(def ^:private type-label
  {:bill "Bill" :income "Payment in" :general "Event"})

(def ^:private type-dot
  {:bill "bg-rose-400" :income "bg-emerald-400" :general "bg-violet-400"})

(defn- prev-month [month year]
  (if (= month 1) [12 (dec year)] [(dec month) year]))

(defn- next-month [month year]
  (if (= month 12) [1 (inc year)] [(inc month) year]))

(defn- first-day-of-week [year month]
  (mod (.getValue (.getDayOfWeek (LocalDate/of year month 1))) 7))

(defn- event-day-types [events year month]
  ;; Returns map of day-of-month -> set of all event types on that day
  (->> events
       (keep (fn [{:event/keys [date type]}]
               (when date
                 (let [d (LocalDate/parse date)]
                   (when (and (= (.getYear d) year) (= (.getMonthValue d) month))
                     [(.getDayOfMonth d) (or type :general)])))))
       (reduce (fn [m [day type]]
                 (update m day (fnil conj #{}) type))
               {})))

(defn- format-event-date [date]
  (try
    (.format (LocalDate/parse date) (DateTimeFormatter/ofPattern "d MMM"))
    (catch Exception _ date)))

(defn- event-row [{:event/keys [title date type] :xt/keys [id]}]
  (let [type    (or type :general)
        dot-cls (get type-dot type "bg-violet-400")]
    [:div {:class "relative flex items-center gap-3 px-5 py-3 border-b border-zinc-100 last:border-0 hover:bg-zinc-50 transition-colors"}
     [:div {:class (str "flex-shrink-0 w-2 h-2 rounded-full " dot-cls)}]
     [:div {:class "min-w-0 flex-1"}
      [:p {:class "text-sm font-medium text-zinc-800 truncate"} title]
      [:p {:class "text-xs text-zinc-400 mt-0.5"}
       (str (get type-label type "Event") " · " (format-event-date date))]]
     (biff/form {:hx-post    "/app/calendar/delete-event"
                 :hx-target  "#calendar-events"
                 :hx-swap    "outerHTML"
                 :hx-include "#cal-month, #cal-year"
                 :class      "flex flex-shrink-0"}
       [:input {:type "hidden" :name "event-id" :value (str id)}]
        [:button {:type    "submit"
                  :title   "Remove event"
                  :aria-label "Remove event"
                  :data-confirm "Remove this event?"
                  :class   "flex items-center justify-center w-6 h-6 text-zinc-300 hover:text-red-500 hover:bg-red-50 rounded-md transition-all active:scale-95 active:text-red-600 active:bg-red-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-400 focus-visible:ring-offset-2"}
         (svgs/close {:class "w-4 h-4"})])]))

(defn events-panel [_ctx events]
  [:div#calendar-events {:class "bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card overflow-hidden"}
   [:div {:class "flex items-center justify-between px-5 py-4 border-b border-zinc-100"}
    [:div
     [:p {:class "text-xs font-semibold text-zinc-400 uppercase tracking-wider"} "Events"]
     [:p {:class "text-xs text-zinc-400 mt-0.5"}
      (if (seq events) (str (count events) " this month") "Nothing scheduled")]]
    [:button {:class "inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-zinc-600 border border-zinc-300 rounded-lg hover:bg-zinc-50 hover:border-zinc-400 transition-all active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2"
              :type "button"
              :_ "on click\n  toggle .hidden on #calendar-add-form\n  toggle .hidden on #cal-add-label\n  toggle .hidden on #cal-close-label"}
     [:span {:id "cal-add-label" :class "flex items-center gap-1.5"}
      (svgs/plus {:class "w-3.5 h-3.5"})
      "Add"]
     [:span {:id "cal-close-label" :class "hidden flex items-center gap-1.5"}
      (svgs/close {:class "w-3.5 h-3.5"})
      "Close"]]]
   [:div {:id "calendar-add-form" :class "hidden px-5 py-4 border-b border-zinc-100 bg-zinc-50"}
    (biff/form {:hx-post    "/app/calendar/create-event"
                :hx-target  "#calendar-events"
                :hx-swap    "outerHTML"
                :hx-include "#cal-month, #cal-year"
                :class      "space-y-2.5"}
      [:input {:type        "text"
               :name        "title"
               :required    true
               :placeholder "Event title"
               :class       (shared/base-input-class)}]
      [:div {:class "grid grid-cols-2 gap-2"}
       [:select {:name  "type"
                 :class (shared/base-input-class)}
        [:option {:value "general"} "Event"]
        [:option {:value "bill"} "Bill"]
        [:option {:value "income"} "Payment in"]]
       [:input {:type     "date"
                :name     "date"
                :required true
                :class    (shared/base-input-class)}]]
      (shared/btn :variant :primary :size :md :type "submit" :class "w-full"
                  "Save event"))]
   (if (seq events)
     [:div {:class "divide-y divide-zinc-100"}
      (map event-row events)]
     [:div {:class "flex flex-col items-center justify-center py-10 text-center px-5"}
      [:div {:class "w-10 h-10 rounded-full bg-zinc-100 flex items-center justify-center mb-3"}
       [:span {:class "text-zinc-400"} (svgs/calendar)]]
      [:p {:class "text-sm font-semibold text-zinc-500"} "No events this month"]
      [:p {:class "mt-1 text-xs text-zinc-400"} "Add bills, income drops, and deadlines"]])])

(defn calendar [year month payday events]
  (let [today       (LocalDate/now)
        today-day   (.getDayOfMonth today)
        today-month (.getMonthValue today)
        today-year  (.getYear today)
        days-count  (.lengthOfMonth (YearMonth/of year month))
        blank-count (first-day-of-week year month)
        event-days  (event-day-types events year month)
        [pm py]     (prev-month month year)
        [nm ny]     (next-month month year)
        current?    (and (= month today-month) (= year today-year))]
    [:div#calendar-grid {:aria-label (str (nth month-names (dec month)) " " year)}
     [:input {:type "hidden" :id "cal-month" :name "cal-month" :value (str month)}]
     [:input {:type "hidden" :id "cal-year"  :name "cal-year"  :value (str year)}]
     [:div {:class "bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card overflow-hidden"}
      ;; ── Header ──────────────────────────────────────────────────────────────
      [:div {:class "flex items-center justify-between px-5 pt-5 pb-4 border-b border-zinc-100 gap-3"}
       [:div {:class "flex items-baseline gap-2 min-w-0"}
        [:span {:class "text-2xl sm:text-3xl font-bold text-zinc-900 tracking-tight truncate"}
         (nth month-names (dec month))]
        [:span {:class "text-lg sm:text-xl font-medium text-zinc-400"} (str year)]]
       [:div {:class "flex items-center gap-3"}
        ;; Legend (hidden on mobile)
        [:div {:class "hidden sm:flex items-center gap-4 mr-1"}
         (for [[dot-color label] [["bg-emerald-400" "Payday"]
                                  ["bg-rose-400"    "Bill"]
                                  ["bg-violet-400"  "Event"]]]
           [:div {:class "flex items-center gap-1.5"}
            [:div {:class (str "w-2 h-2 rounded-full " dot-color)}]
            [:span {:class "text-[10px] font-semibold uppercase tracking-wider text-zinc-400"} label]])]
        ;; Jump back to the current month when browsing another one
        (when-not current?
          [:button {:type     "button"
                    :hx-get   (str "/app/calendar/grid?month=" today-month "&year=" today-year)
                    :hx-target "#calendar-grid"
                    :hx-swap  "outerHTML"
                    :class    "px-2.5 h-7 text-xs font-medium text-emerald-600 border border-emerald-200 rounded-lg hover:bg-emerald-50 transition-all active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2"}
           "Today"])
        ;; Navigation
        [:div {:class "flex items-center gap-1"}
         [:button {:type     "button"
                   :aria-label "Previous month"
                   :class    "w-8 h-8 flex items-center justify-center rounded-lg text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700 transition-all active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2"
                   :hx-get   (str "/app/calendar/grid?month=" pm "&year=" py)
                   :hx-target "#calendar-grid"
                   :hx-swap  "outerHTML"}
          [:svg {:class "w-3.5 h-3.5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
           [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2.5" :d "M15 19l-7-7 7-7"}]]]
         [:button {:type     "button"
                   :aria-label "Next month"
                   :class    "w-8 h-8 flex items-center justify-center rounded-lg text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700 transition-all active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2"
                   :hx-get   (str "/app/calendar/grid?month=" nm "&year=" ny)
                   :hx-target "#calendar-grid"
                   :hx-swap  "outerHTML"}
          [:svg {:class "w-3.5 h-3.5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
           [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2.5" :d "M9 5l7 7-7 7"}]]]]]]
      ;; ── Day-of-week headers ─────────────────────────────────────────────────
      [:div {:class "flex flex-wrap border-b border-zinc-100"}
       (for [d ["Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"]]
         [:div {:class "py-2.5 w-[14.28%] text-center text-[10px] font-bold uppercase tracking-wider text-zinc-400"} d])]
      ;; ── Day cells ────────────────────────────────────────────────────────────
      [:div {:class "flex flex-wrap"}
       ;; Blank leading cells
       (for [_ (range blank-count)]
         [:div {:class "border-r border-b border-zinc-100 h-14 sm:h-20 w-[14.28%]"}])
       ;; Active day cells
       (for [d (range 1 (inc days-count))]
         (let [is-today  (and (= d today-day) (= month today-month) (= year today-year))
               is-payday (= d payday)
               ;; Combine payday (emerald) with any event types on this day
               all-types (cond-> (get event-days d #{})
                           is-payday (conj :income))
               ;; One dot per distinct type, in stable order: bill → income → general
               dots      (filterv some? (map type-dot (filter all-types [:bill :income :general])))
               num-cls   (if is-today
                           "bg-zinc-900 text-white shadow-card-md"
                           "text-zinc-700")]
           [:div {:class "border-r border-b border-zinc-100 h-14 sm:h-20 w-[14.28%] flex flex-col items-center pt-2 transition-colors hover:bg-zinc-50"}
            [:div {:class (str "w-7 h-7 flex items-center justify-center rounded-full "
                               "text-sm font-medium transition-colors " num-cls)}
             d]
              (when (seq dots)
                [:div {:class "flex items-center gap-0.5 mt-1"}
                 (for [dot dots]
                   [:div {:class (str "w-1.5 h-1.5 rounded-full " dot)}])])
              ]
             )
            )
           ]
          ]
         ]
         )
         )
