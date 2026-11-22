(ns com.greed.ui.components.home
  "What every profile's Home is made of, so Personal and a business read
   the same way: the heading, and the From the bank statements section
   (money in and out by month, where this month's money went), and the
   quick actions. Each Home keeps its own hero and plan cards."
  (:require [clojure.string :as str]
            [com.greed.statements.categorise :as categorise]
            [com.greed.ui.app.statements :as spending]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.hero :as hero]
            [com.greed.utilities.core :as utilities])
  (:import [java.time LocalDate LocalTime YearMonth]
           [java.time.format DateTimeFormatter]
           [java.time.temporal ChronoUnit]))

(def personal
  "How Home words things on the personal side."
  {:possessive "your" :kept "kept" :savings "Savings"})

(def business
  "How Home words things for a business."
  {:possessive "the business's" :kept "kept in the business" :savings "Set aside"})

(def card "rounded-lg bg-white ring-1 ring-zinc-200/70")

(defn- whole [cents] (utilities/whole->rands (/ cents 100.0)))
(defn- rands [r] (utilities/whole->rands r))
(defn- pct [part total] (if (pos? total) (min 100 (Math/round (* 100.0 (/ (double part) total)))) 0))
(defn month-name [m pattern] (.format (YearMonth/parse m) (DateTimeFormatter/ofPattern pattern)))

(defn month-of [{:statement-summary/keys [month period-end]}]
  (or month (subs period-end 0 7)))

(defn whole-month? [{:statement-summary/keys [period-start period-end] :as s}]
  (let [ym (YearMonth/parse (month-of s))]
    (and (= (str (.atDay ym 1)) period-start) (= (str (.atEndOfMonth ym)) period-end))))

(defn- greeting []
  (let [h (.getHour (LocalTime/now))]
    (cond (< h 12) "Good morning" (< h 18) "Good afternoon" :else "Good evening")))

(defn heading
  "Home's heading, the same for every profile: Home, a greeting, which
   profile this is, and today's date."
  [user profile-label]
  [:div {:class "flex items-end justify-between gap-4"}
   [:div {:class "min-w-0"}
    [:h1 {:class "text-2xl font-semibold tracking-tight text-zinc-900"} "Home"]
    [:p {:class "mt-0.5 truncate text-sm text-zinc-500"}
     (str (greeting) ", " (str/capitalize (str (:user/firstname user))) " · " profile-label)]]
   [:p {:class "hidden whitespace-nowrap text-sm font-medium tabular-nums text-zinc-500 sm:block"}
    (.format (LocalDate/now) (DateTimeFormatter/ofPattern "EEEE, d MMMM"))]])

(defn latest-month
  "The month to lead with: the newest one a statement covers in full, all
   banks together; or, with only partial months, the newest of those.
   Returns {:month \"yyyy-MM\" :summaries [...] :whole? bool} or nil."
  [summaries]
  (let [full  (filter whole-month? summaries)
        pick  (fn [ss whole?]
                (when-let [m (some-> (first ss) month-of)]
                  {:month m :whole? whole? :summaries (filter #(= m (month-of %)) ss)}))]
    (or (pick full true) (pick summaries false))))

(defn monthly-flow
  "Money in and out per month, all banks together, oldest first, at most
   the last `n` months: [{:month :in :out}] in cents."
  [summaries n]
  (->> (group-by month-of summaries)
       (map (fn [[m ss]] {:month m
                          :in  (reduce + 0 (map :statement-summary/money-in-cents ss))
                          :out (reduce + 0 (map :statement-summary/money-out-cents ss))}))
       (sort-by :month)
       (take-last n)
       vec))

(defn flow-card
  "Money in against money out for the last six months."
  [flow]
  (let [top (max 1 (apply max 1 (mapcat (juxt :in :out) flow)))]
    [:section {:class (str card " flex h-full flex-col p-6")}
     [:div {:class "flex items-start justify-between gap-4"}
      [:div
       [:h3 {:class "text-sm font-semibold tracking-tight text-zinc-900"} "Money in and out"]
       [:p {:class "mt-0.5 text-xs text-zinc-500"} "Each month on the statements, all banks together."]]
      [:div {:class "flex flex-shrink-0 items-center gap-3 text-xs text-zinc-600"}
       [:span {:class "flex items-center gap-1.5"} [:span {:class "h-2 w-2 rounded-full bg-brand-600"}] "In"]
       [:span {:class "flex items-center gap-1.5"} [:span {:class "h-2 w-2 rounded-full bg-zinc-400"}] "Out"]]]
     (if (seq flow)
       [:div {:class "mt-6 flex h-44 items-end gap-4"}
        (for [{:keys [month in out]} flow]
          [:div {:class "flex h-full min-w-0 flex-1 flex-col justify-end"
                 :title (str (month-name month "MMMM yyyy") ": " (whole in) " in, " (whole out) " out")}
           [:div {:class "flex flex-1 items-end justify-center gap-1"}
            [:span {:class "block w-1/2 max-w-[18px] rounded-t-sm bg-brand-600 greed-col-grow" :style {:height (str (max 2 (pct in top)) "%")}}]
            [:span {:class "block w-1/2 max-w-[18px] rounded-t-sm bg-zinc-400 greed-col-grow" :style {:height (str (max 2 (pct out top)) "%")}}]]
           [:span {:class "mt-2 text-center text-xs text-zinc-500"} (month-name month "MMM")]])]
       [:p {:class "mt-5 text-sm text-zinc-600"} "Upload the business's statements to see its months side by side."])]))

(defn costs-card
  "Where this month's money went, by category."
  [summaries]
  (let [cats (->> (mapcat :statement-summary/categories summaries)
                  (reduce (fn [m [k v]] (update m k (fnil + 0) v)) {})
                  (sort-by (comp - val))
                  (take 5))
        top  (or (some-> cats first val) 1)]
    [:section {:class (str card " flex h-full flex-col p-6")}
     [:h3 {:class "text-sm font-semibold tracking-tight text-zinc-900"} "Where it went"]
     [:p {:class "mt-0.5 text-xs text-zinc-500"} "This month's largest categories."]
     (if (seq cats)
       [:ul {:class "mt-5 space-y-3"}
        (for [[k cents] cats]
          [:li {:class "text-sm"}
           [:div {:class "flex items-baseline justify-between gap-3"}
            [:span {:class "truncate text-zinc-700"} (categorise/categories k (name k))]
            [:span {:class "font-semibold tabular-nums text-zinc-900"} (whole cents)]]
           [:span {:class "mt-1.5 block h-1.5 overflow-hidden rounded-full bg-zinc-100"}
            [:span {:class "block h-full rounded-full bg-brand-600 greed-bar-grow" :style {:width (str (max 2 (pct cents top)) "%")}}]]])]
       [:p {:class "mt-5 text-sm text-zinc-600"} "Nothing spent on the statements this month."])]))

(defn- stat [label value & [sub cls]]
  [:div {:class "min-w-0"}
   [:p {:class "text-xs font-medium text-zinc-500"} label]
   [:p {:class (str "mt-1 text-lg font-semibold tabular-nums " (or cls "text-zinc-900"))} value]
   (when sub [:p {:class "mt-0.5 truncate text-xs text-zinc-500"} sub])])

(defn month-hero
  "This month from the bank statements, in the same header card as every
   tab: spent, read against the plan, planned and spent as bars, and money
   in, out and the net. The same for every profile; `who` words it."
  [{:keys [month whole? summaries]} items-by-id planned-expenses who]
  (let [{:keys [spent planned over linked? budgeted unplanned] :as cmp}
        (spending/compare-with-budget summaries items-by-id planned-expenses)
        in    (reduce + 0 (map :statement-summary/money-in-cents summaries))
        net   (- in spent)
        over? (and whole? (pos? planned) (pos? over))]
    (hero/card
     {:label (month-name month "MMMM yyyy")
      :value (whole spent)
      :unit "money out"
      :verdict (cond
                 (not (pos? planned)) {:text "No plan to compare" :tone :neutral}
                 (not whole?) {:text "Part of the month" :tone :neutral}
                 :else (spending/verdict-pill cmp))
      :reading (cond
                 (not (pos? planned)) (str "Add " (:possessive who) " costs on Budget to compare this month with a plan.")
                 (not whole?) "Only part of this month is on the statements, so it isn't compared with the plan yet."
                 :else (spending/verdict-text cmp))
      :tone (when over? :rose)
      :picture (hero/bars {:title "Planned against spent"
                           :rows [{:label "Planned" :value (whole planned) :amount planned :cls "bg-zinc-400"}
                                  {:label "Spent" :value (whole spent) :amount spent :cls (if over? "bg-rose-500" "bg-brand-600")}]
                           :note (when linked?
                                   (str (whole budgeted) " paid items already in the budget, " (whole unplanned) " went on everything else."))})
      :stats [["Money in" (whole in) nil :money-in]
              ["Money out" (whole spent) nil :money-out]
              ["Left over" (str (when (neg? net) "−") (whole (Math/abs (long net)))) (when (neg? net) "text-rose-700") :left-over]]
      :footer [:div {:class "mt-5 flex justify-end"}
               [:a {:href (str "/app/spending?month=" month)
                    :class "inline-flex items-center gap-1 rounded-md text-sm font-medium text-zinc-900 underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"}
                (str "See " (month-name month "MMMM") " in Spending") [:span {:aria-hidden "true"} "→"]]]})))

(defn empty-hero
  "No statements yet: the plan in brief and the way in."
  [{:keys [total-income total-expenses total-savings]} who]
  (let [left (- total-income total-expenses total-savings)]
    [:section {:class (str card " px-6 py-6 sm:px-8 sm:py-7")}
     [:h2 {:class "text-sm font-medium text-zinc-500"} "This month"]
     [:p {:class "mt-3 max-w-[36ch] text-2xl font-semibold tracking-[-0.02em] text-zinc-900"}
      (str "See where " (:possessive who) " money actually goes.")]
     [:p {:class "mt-2 max-w-[60ch] text-sm text-zinc-600"}
      (if (pos? total-income)
        (str "The budget plans " (rands total-income) " in and " (rands (+ total-expenses total-savings))
             " out each month, leaving " (rands left) ". Upload a bank statement to compare it with what happened.")
        "Plan the month in Budget, then upload a bank statement to compare it with what happened.")]
     [:div {:class "mt-5 flex flex-wrap gap-3"}
      (shared/btn :variant :dark :size :md :href "/app/banking" "Upload a statement")
      (shared/btn :variant :outline :size :md :href "/app/finances/" "Open Budget")]]))

(defn plan-card
  "The monthly plan from the budget: income, expenses and savings (a
   business's set-aside), and what is left."
  [{:keys [total-income total-expenses total-savings]} who]
  (let [left (- total-income total-expenses total-savings)
        rows [["Income" total-income "bg-brand-600"]
              ["Expenses" total-expenses "bg-zinc-700"]
              [(:savings who) total-savings "bg-zinc-400"]]]
    [:section {:class (str card " flex h-full flex-col p-6")}
     [:h3 {:class "text-sm font-semibold tracking-tight text-zinc-900"} "The monthly plan"]
     [:p {:class "mt-0.5 text-xs text-zinc-500"} (str "From " (:possessive who) " budget.")]
     (if (pos? (+ total-income total-expenses total-savings))
       [:<>
        [:div {:class "mt-5 space-y-3"}
         (for [[label r cls] rows]
           [:div {:class "grid grid-cols-[5rem_minmax(0,1fr)_auto] items-center gap-3 text-sm"}
            [:span {:class "text-zinc-600"} label]
            [:span {:class "h-1.5 overflow-hidden rounded-full bg-zinc-100"}
             [:span {:class (str "block h-full rounded-full greed-bar-grow " cls)
                     :style {:width (str (max 2 (pct r (max 1 total-income total-expenses))) "%")}}]]
            [:span {:class "w-24 text-right font-semibold tabular-nums text-zinc-900"} (rands r)]])]
        [:p {:class "mt-auto flex items-baseline justify-between border-t border-zinc-100 pt-4 text-sm"}
         [:span {:class "text-zinc-600"} (if (neg? left) "Planned beyond income" "Left each month")]
         [:span {:class (str "text-lg font-semibold tabular-nums " (if (neg? left) "text-rose-700" "text-zinc-900"))} (rands left)]]]
       [:p {:class "mt-5 text-sm text-zinc-600"} (str "No budget yet. Add " (:possessive who) " income and costs in Budget.")])]))

(defn due-card
  "Coming up: dated rows [{:what :due :note :amount}] (amount in cents,
   optional), soonest first; `empty` when there are none; `links` [[href
   label]] under the list."
  [rows {:keys [sub empty links]}]
  (let [today (LocalDate/now)]
    [:section {:class (str card " flex h-full flex-col p-6")}
     [:h3 {:class "text-sm font-semibold tracking-tight text-zinc-900"} "Coming up"]
     [:p {:class "mt-0.5 text-xs text-zinc-500"} sub]
     (if (seq rows)
       [:ul {:class "mt-4 divide-y divide-zinc-100"}
        (for [{:keys [what due amount note]} rows
              :let [days (.between ChronoUnit/DAYS today due)]]
          [:li {:class "flex items-start gap-4 py-3"}
           [:div {:class "flex w-12 flex-shrink-0 flex-col items-center rounded-md bg-zinc-100 py-1.5 text-center"}
            [:span {:class "text-[11px] font-medium leading-none text-zinc-500"} (.format ^LocalDate due (DateTimeFormatter/ofPattern "MMM"))]
            [:span {:class "mt-0.5 text-lg font-semibold leading-none tabular-nums text-zinc-900"} (.getDayOfMonth ^LocalDate due)]]
           [:div {:class "min-w-0 flex-1"}
            [:p {:class "truncate text-sm font-medium text-zinc-900"} what]
            [:p {:class "truncate text-xs text-zinc-500"} (str note " · " (if (zero? days) "today" (str "in " days (if (= 1 days) " day" " days"))))]]
           (when amount
             [:p {:class "flex-shrink-0 text-sm font-semibold tabular-nums text-zinc-900"}
              (cond (pos? amount) (whole amount) (neg? amount) (str (whole (- amount)) " back") :else "R0")])])]
       [:p {:class "mt-4 text-sm text-zinc-600"} empty])
     [:div {:class "mt-auto flex flex-wrap gap-x-5 gap-y-2 pt-4 text-sm"}
      (for [[href label] links]
        [:a {:href href :class "font-medium text-zinc-900 underline-offset-4 hover:underline"} label])]]))

(defn actions
  "Quick actions: [[href title sub] ...], one row of cards."
  [items]
  (let [action (fn [[href title sub]]
                 [:a {:href href :class (str card " group flex items-center justify-between gap-4 p-5 transition hover:bg-zinc-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400")}
                  [:span {:class "min-w-0"}
                   [:span {:class "block text-sm font-semibold text-zinc-900"} title]
                   [:span {:class "block truncate text-xs text-zinc-500"} sub]]
                  [:span {:aria-hidden "true" :class "text-zinc-400 transition-transform group-hover:translate-x-1"} "→"]])]
    [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-3"}
     (map action items)]))
