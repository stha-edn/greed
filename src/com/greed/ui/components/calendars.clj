(ns com.greed.ui.components.calendars
  (:require [com.biffweb :as biff])
  (:import [java.time LocalDate YearMonth]))

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

(defn- event-row [{:event/keys [title date type] :xt/keys [id]}]
  (let [type    (or type :general)
        dot-cls (get type-dot type "bg-violet-400")]
    [:div {:class "relative flex items-center gap-3 px-5 py-3 border-b border-zinc-100 last:border-0 hover:bg-zinc-50 transition-colors"}
     [:div {:class (str "flex-shrink-0 w-2 h-2 rounded-full " dot-cls)}]
     [:div {:class "min-w-0 flex-1"}
      [:p {:class "text-sm font-medium text-zinc-800 truncate"} title]
      [:p {:class "text-xs text-zinc-400 mt-0.5"}
       (str (get type-label type "Event") " · " date)]]
     (biff/form {:hx-post    "/app/calendar/delete-event"
                 :hx-target  "#calendar-events"
                 :hx-swap    "outerHTML"
                 :hx-include "#cal-month, #cal-year"
                 :class      "flex flex-shrink-0"}
       [:input {:type "hidden" :name "event-id" :value (str id)}]
       [:button {:type    "submit"
                 :onclick "return confirm('Remove this event?')"
                 :class   "text-xs text-zinc-300 hover:text-red-500 transition-colors px-1 py-1"}
        "✕"])]))

(defn events-panel [_ctx events]
  [:div#calendar-events {:class "bg-white rounded-xl border border-zinc-200/70 shadow-card overflow-hidden"}
   [:div {:x-data "{ showForm: false }"}
    [:div {:class "flex items-center justify-between px-5 py-4 border-b border-zinc-100"}
     [:div
      [:p {:class "text-xs font-bold uppercase tracking-widest text-zinc-400"} "Events"]
      [:p {:class "text-xs text-zinc-400 mt-0.5"}
       (if (seq events) (str (count events) " this month") "Nothing scheduled")]]
     [:button {:class      "text-xs font-medium text-zinc-600 border border-zinc-200 rounded-lg px-3 py-1.5 hover:bg-zinc-50 hover:border-zinc-300 transition-all"
               :x-on:click "showForm = !showForm"}
      [:span {:x-show "!showForm"} "＋ Add"]
      [:span {:x-show "showForm" :x-cloak ""} "✕ Close"]]]
    [:div {:x-show "showForm" :x-cloak ""
           :class "px-5 py-4 border-b border-zinc-100 bg-zinc-50"}
     (biff/form {:hx-post    "/app/calendar/create-event"
                 :hx-target  "#calendar-events"
                 :hx-swap    "outerHTML"
                 :hx-include "#cal-month, #cal-year"
                 :class      "space-y-2.5"}
       [:input {:type        "text"
                :name        "title"
                :required    true
                :placeholder "Event title"
                :class       "w-full px-3 py-2 text-sm border border-zinc-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-zinc-900 bg-white placeholder:text-zinc-400"}]
       [:div {:class "grid grid-cols-2 gap-2"}
        [:select {:name  "type"
                  :class "px-3 py-2 text-sm border border-zinc-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-zinc-900 bg-white text-zinc-700"}
         [:option {:value "general"} "Event"]
         [:option {:value "bill"} "Bill"]
         [:option {:value "income"} "Payment in"]]
        [:input {:type     "date"
                 :name     "date"
                 :required true
                 :class    "px-3 py-2 text-sm border border-zinc-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-zinc-900 bg-white text-zinc-700"}]]
       [:button {:type  "submit"
                 :class "w-full py-2 text-sm font-semibold text-white bg-zinc-900 rounded-lg hover:bg-zinc-700 transition-colors"}
        "Save event"])]
    (if (seq events)
      [:div (map event-row events)]
      [:div {:class "flex flex-col items-center justify-center py-10 text-center px-5"}
       [:div {:class "w-10 h-10 rounded-full bg-zinc-50 flex items-center justify-center mb-3"}
        [:svg {:class "w-5 h-5 text-zinc-300" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "1.5"
                 :d "M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"}]]]
       [:p {:class "text-sm font-semibold text-zinc-500"} "No events this month"]
       [:p {:class "mt-1 text-xs text-zinc-400"} "Add bills, income drops, and deadlines"]])]])

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
      ;; ── Header ──────────────────────────────────────────────────────────────
      [:div {:class "flex items-center justify-between px-5 pt-5 pb-4 border-b border-zinc-100"}
       [:div {:class "flex items-baseline gap-2"}
        [:span {:class "text-2xl sm:text-3xl font-giza font-bold text-zinc-900"}
         (nth month-names (dec month))]
        [:span {:class "text-lg sm:text-xl font-giza text-zinc-400"} (str year)]]
       [:div {:class "flex items-center gap-3"}
        ;; Legend (hidden on mobile)
        [:div {:class "hidden sm:flex items-center gap-4 mr-1"}
         (for [[dot-color label] [["bg-emerald-400" "Payday"]
                                   ["bg-rose-400"    "Bill"]
                                   ["bg-violet-400"  "Event"]]]
           [:div {:class "flex items-center gap-1.5"}
            [:div {:class (str "w-2 h-2 rounded-full " dot-color)}]
            [:span {:class "text-[10px] font-semibold uppercase tracking-wider text-zinc-400"} label]])]
        ;; Navigation
        [:div {:class "flex items-center gap-1"}
         [:button {:class     "w-7 h-7 flex items-center justify-center rounded-lg text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700 transition-colors"
                   :hx-get    (str "/app/calendar/grid?month=" pm "&year=" py)
                   :hx-target "#calendar-grid"
                   :hx-swap   "outerHTML"}
          [:svg {:class "w-3.5 h-3.5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
           [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2.5" :d "M15 19l-7-7 7-7"}]]]
         [:button {:class     "w-7 h-7 flex items-center justify-center rounded-lg text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700 transition-colors"
                   :hx-get    (str "/app/calendar/grid?month=" nm "&year=" ny)
                   :hx-target "#calendar-grid"
                   :hx-swap   "outerHTML"}
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
                 [:div {:class (str "w-1.5 h-1.5 rounded-full " dot)}])])]))]]]))
