(ns com.greed.ui.components.calendars
  (:require [com.biffweb :as biff]))


(defn- event-row [{:event/keys [title date id]}]
  [:div {:class "flex items-center justify-between py-2.5 border-b border-gray-50 last:border-0"}
   [:div {:class "flex items-center gap-3"}
    [:div {:class "w-2 h-2 rounded-full bg-violet-400 flex-shrink-0"}]
    [:div
     [:p {:class "text-sm font-medium text-zinc-800"} title]
     [:p {:class "text-xs text-zinc-400"} date]]]
   [:button {:class "text-xs text-zinc-400 hover:text-red-500 transition-colors px-2 py-1"
             :hx-post "/app/calendar/delete-event"
             :hx-vals (str "{\"event-id\": \"" id "\"}")
             :hx-target "#calendar-events"
             :hx-swap "outerHTML"}
    "Remove"]])

(defn events-panel [_ctx events]
  [:div#calendar-events {:class "bg-white rounded-xl border border-gray-100 shadow-card p-5"}
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
                 :class "flex flex-col sm:flex-row gap-2"}
       [:input {:type "text" :name "title" :required true
                :placeholder "Event title"
                :class "flex-1 px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-zinc-400"}]
       [:input {:type "date" :name "date" :required true
                :class "px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-zinc-400"}]
       [:button {:type "submit"
                 :class "px-4 py-2 text-sm font-medium text-white bg-zinc-900 rounded-lg hover:bg-zinc-700"}
        "Save"])]]
   (if (seq events)
     [:div {:class "mt-4 divide-y divide-gray-50"}
      (map event-row events)]
     [:p {:class "mt-4 text-sm text-zinc-400"} "No events yet."])])

(defn calendar [payday event-days]
  [:div
   {:x-data (str "app(" payday ", " event-days ")"),
    :x-init "initDate(); getNoOfDays();",
    :x-cloak ""}

   [:div {:class "bg-white rounded-xl border border-gray-100 shadow-card overflow-hidden"}
    [:div {:class "flex items-center justify-between py-3 px-4 sm:px-6"}
     [:div
      [:span {:x-text "MONTH_NAMES[month]"
              :class "text-2xl sm:text-3xl font-giza font-bold text-gray-800"}]
      [:span {:x-text "year"
              :class "ml-1.5 text-xl sm:text-2xl text-gray-400 font-giza"}]]]
    [:div
     [:div {:class "flex flex-wrap border-b border-gray-100"}
      [:template {:x-for "(dayName, index) in DAYS" :key "index"}
       [:div {:class "py-2 w-[14.28%] text-center text-xs text-gray-400 font-medium"}
        [:span {:x-text "dayName"}]]]]
     [:div {:class "flex flex-wrap border-l border-gray-100"}
      [:template {:x-for "blankday in blankdays"}
       [:div {:class "border-r border-b border-gray-100 h-10 sm:h-16 w-[14.28%]"}]]
      [:template {:x-for "(date, dateIndex) in no_of_days" :key "dateIndex"}
       [:div {:class "border-r border-b border-gray-100 h-10 sm:h-16 w-[14.28%] flex items-start justify-center pt-1.5"
              :x-bind:class "(date === payday ? 'bg-emerald-50 ' : '') + (isEventDay(date) ? 'bg-violet-50' : '')"}
        [:div {:class "w-7 h-7 flex items-center justify-center rounded-full text-sm font-medium transition-colors"
               :x-bind:class "date === day ? 'bg-zinc-900 text-white' : 'text-gray-700'"}
         [:span {:x-text "date"}]]]]]]]

   ;; Legend
   [:div {:class "flex items-center gap-4 mt-2 px-1"}
    [:div {:class "flex items-center gap-1.5"}
     [:div {:class "w-2.5 h-2.5 rounded bg-emerald-100"}]
     [:span {:class "text-xs text-zinc-400"} "Payday"]]
    [:div {:class "flex items-center gap-1.5"}
     [:div {:class "w-2.5 h-2.5 rounded bg-violet-100"}]
     [:span {:class "text-xs text-zinc-400"} "Event"]]
    [:div {:class "flex items-center gap-1.5"}
     [:div {:class "w-5 h-5 rounded-full bg-zinc-900 flex items-center justify-center"}
      [:span {:class "text-white text-xs"} "9"]]
     [:span {:class "text-xs text-zinc-400"} "Today"]]]])
