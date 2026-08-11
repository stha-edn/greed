(ns com.greed.ui.components.stats
  (:require [com.greed.ui.core :as c.ui]
            [com.greed.utilities.core :as utilities]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.forms :as forms])
  (:import [java.time LocalDate YearMonth]
           [java.time.temporal ChronoUnit]))

(defn- pct-share
  "Share of `amount` as a percentage of `total`, or nil when `total` isn't
   positive (nothing to divide against)."
  [amount total]
  (when (pos? (double (or total 0)))
    (double (* 100.0 (/ (double (or amount 0)) (double total))))))

(defn- legend-row [dot-cls label amount share value-cls]
  [:div {:class "flex items-center gap-3 min-w-0"}
   [:span {:class (str "flex-shrink-0 w-2 h-2 rounded-full " dot-cls)}]
   [:p {:class "truncate text-sm font-medium text-zinc-600"} label]
   [:div {:class "flex-1"}]
   (when share
     [:span {:class "flex-shrink-0 text-xs font-medium text-zinc-400 tabular-nums"}
      (format "%.0f%%" share)])
   [:p {:class (str "flex-shrink-0 text-sm font-semibold tabular-nums "
                    (or value-cls "text-zinc-900"))}
    (utilities/amount->rands (or amount 0))]])

(defn budget-summary
  "Finances-page hero: the month's plan read at a glance. One headline number
   — what's left after expenses and savings — with a stacked, proportional bar
   showing where the income goes (the storage-usage pattern: a budget that
   reads visually rather than as three competing cards). Overspend flips the
   headline and its tone instead of hiding the problem."
  [budget-items]
  (let [{:keys [total-income total-expenses total-savings]} (c.ui/get-budget-data budget-items)
        income     (or total-income 0)
        leftover   (- income (or total-expenses 0) (or total-savings 0))
        overspend? (neg? leftover)
        month      (.format (java.time.LocalDate/now)
                            (java.time.format.DateTimeFormatter/ofPattern "MMMM"))
        segments   (cond-> [{:cls "bg-rose-400"   :amount (or total-expenses 0)}
                            {:cls "bg-indigo-400" :amount (or total-savings 0)}]
                     (not overspend?) (conj {:cls "bg-emerald-400" :amount leftover}))]
    (if (zero? income)
      ;; Nothing to plan yet — keep the surface, explain, and point at the one
      ;; way in (the header's Add item). Same pattern as the dashboard's
      ;; budget-snapshot empty state.
      [:div {:class "reveal relative overflow-hidden rounded-2xl bg-white ring-1 ring-zinc-200/70 shadow-card-md"}
       [:div {:class "absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-emerald-500/40 to-transparent"}]
       [:div {:class "relative flex flex-col items-center px-6 py-12 text-center"}
        [:div {:class "mb-3 flex items-center justify-center w-12 h-12 rounded-full bg-emerald-50"}
         [:span {:class "text-emerald-500"} (svgs/wallet)]]
        [:p {:class "text-sm font-medium text-zinc-500"} "No budget yet"]
        [:p {:class "mt-1 text-xs text-zinc-400"} "Add your income, expenses and savings to plan this month."]
        (shared/btn :variant :primary :size :md :class "mt-5"
                    :attrs {"_" (shared/open-actions "budget-add-modal")}
                    (svgs/plus {:class "w-4 h-4"})
                    "Add your first item")]]
      [:div {:class "reveal relative overflow-hidden rounded-2xl bg-white ring-1 ring-zinc-200/70 shadow-card-md"}
       [:div {:class "absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-emerald-500/40 to-transparent"}]
       [:div {:class "absolute -top-24 -right-24 h-72 w-72 rounded-full bg-emerald-400/10 blur-3xl"}]
       [:div {:class "relative grid grid-cols-1 gap-8 p-6 sm:p-8 lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)] lg:items-center"}
        [:div
         [:div {:class "flex items-center justify-between gap-3"}
          [:div {:class "flex items-center gap-2.5"}
           [:span {:class (str "h-1.5 w-1.5 rounded-full "
                               (if overspend? "bg-rose-500" "bg-emerald-500"))}]
           [:p {:class (str "text-[11px] sm:text-xs font-semibold uppercase tracking-[0.18em] "
                            (if overspend? "text-rose-600" "text-emerald-600"))}
            (if overspend? "Over budget by" "Left to plan this month")]]
          [:span {:class "flex-shrink-0 rounded-full bg-zinc-100 px-2.5 py-1 text-[11px] font-semibold text-zinc-600"}
           month]]
         [:p {:class (str "mt-5 text-4xl font-bold leading-none tracking-[-0.04em] tabular-nums sm:text-5xl "
                          (if overspend? "text-rose-600" "text-zinc-900"))}
          (utilities/amount->rands (Math/abs (double leftover)))]
         [:p {:class "mt-3 text-sm text-zinc-400"}
          (if overspend?
            (str "That's " (utilities/amount->rands (Math/abs (double leftover)))
                 " more going out than coming in this month.")
            (str "Out of " (utilities/amount->rands income) " monthly income."))]]
        [:div
         [:p {:class "text-xs font-semibold text-zinc-400 uppercase tracking-wider"}
          "Where your income goes"]
         [:div {:class "mt-4 flex h-2 w-full overflow-hidden rounded-full bg-zinc-100"}
          (for [{:keys [cls amount]} segments]
            (when-let [share (pct-share amount income)]
              (when (>= share 0.5)
                [:div {:class (str "h-full flex-shrink-0 " cls)
                       :style {:width (str (format "%.1f" (min 100.0 share)) "%")}}])))]
         [:div {:class "mt-5 space-y-3"}
          (legend-row "bg-rose-400" "Expenses" total-expenses (pct-share total-expenses income) "text-zinc-900")
          (legend-row "bg-indigo-400" "Savings" total-savings (pct-share total-savings income) "text-zinc-900")
          (if overspend?
            (legend-row "bg-rose-300" "Overspend" (Math/abs (double leftover)) nil "text-rose-600")
            (legend-row "bg-emerald-400" "Unallocated" leftover (pct-share leftover income) "text-zinc-900"))]]]])))

(defn section-header
  "Consistent dashboard section title with an optional trailing link.
   Internal links get a chevron; external links get an underline on hover."
  [title & {:keys [href link-label external?]}]
  [:div {:class "flex items-center justify-between mb-3"}
   [:h2 {:class "text-xs font-semibold text-zinc-400 uppercase tracking-wider"} title]
   (when href
     (if external?
       [:a {:href href :target "_blank" :rel "noopener noreferrer"
            :class "-my-1.5 -mx-2 inline-flex items-center rounded-md px-2 py-2 text-xs font-medium text-emerald-600 transition hover:text-emerald-700 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/70 focus-visible:ring-offset-2 active:text-emerald-800 active:scale-[0.97]"}
        link-label]
       [:a {:href href
            :class "group -my-1.5 -mx-2 inline-flex items-center gap-1 rounded-md px-2 py-2 text-xs font-medium text-emerald-600 transition hover:text-emerald-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/70 focus-visible:ring-offset-2 active:text-emerald-800 active:scale-[0.97]"}
        link-label
        (svgs/->next {:class "size-3.5 -translate-x-0.5 transition-transform group-hover:translate-x-0"})]))])

(defn- tax-panel
  "Clean white dashboard surface used by the tax overview panels. An optional
   leading opts map (e.g. {:class \"flex-1\"}) lets sibling panels in an
   equal-height row (Upcoming | Goals) stretch to match each other."
  [& args]
  (let [opts?    (map? (first args))
        opts     (if opts? (first args) {})
        children (if opts? (rest args) args)]
    (into [:div {:class (str "bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card overflow-hidden "
                             (:class opts))}]
          children)))

(defn budget-snapshot
  "Dashboard budget brief: a single grouped panel with hairline cells for
   income, expenses, savings and unallocated. The finances page leads with the
   larger budget-summary hero instead."
  [budget-items]
  (if (empty? budget-items)
    (tax-panel
     [:div {:class "py-10 px-6 text-center"}
      [:div {:class "mx-auto mb-3 flex items-center justify-center w-10 h-10 rounded-full bg-zinc-50"}
       [:span {:class "text-zinc-400"} (svgs/wallet)]]
      [:p {:class "text-sm font-medium text-zinc-500"} "No budget yet"]
      [:p {:class "mt-0.5 text-xs text-zinc-400"} "Add your income and expenses to see your monthly breakdown."]
      (shared/btn :variant :primary :size :md :class "mt-4"
                  :attrs {"_" (shared/open-actions "dashboard-budget-add-modal")}
                  (svgs/plus {:class "w-4 h-4"})
                  "Add your first item")
      (shared/modal "dashboard-budget-add-modal" (forms/budget-item-form))])
    (let [{:keys [total-income total-expenses total-savings]} (c.ui/get-budget-data budget-items)
          leftover    (- total-income total-expenses total-savings)
          overspend?  (neg? leftover)
          rows        [{:label "Monthly income"
                        :value (utilities/amount->rands total-income)
                        :icon-bg "bg-emerald-50" :icon (svgs/trending-up) :tone "text-emerald-600"}
                       {:label "Monthly expenses"
                        :value (utilities/amount->rands total-expenses)
                        :icon-bg "bg-rose-50" :icon (svgs/trending-down) :tone "text-rose-600"}
                       {:label "Net savings"
                        :value (utilities/amount->rands total-savings)
                        :icon-bg "bg-indigo-50" :icon (svgs/wallet) :tone "text-indigo-600"}
                       {:label (if overspend? "Overspend" "Unallocated")
                        :value (utilities/amount->rands (Math/abs (double leftover)))
                        :icon-bg "bg-zinc-100" :icon (svgs/money) :tone "text-zinc-500"
                        :value-tone (if overspend? "text-rose-600" "text-zinc-900")}]]
      [:div {:class "bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card divide-y divide-zinc-100 lg:grid lg:grid-cols-4 lg:divide-y-0 lg:divide-x"}
       (for [{:keys [label value icon-bg icon tone value-tone]} rows]
         [:div {:class "flex items-center justify-between gap-4 px-5 py-4 sm:px-6 lg:flex-col lg:items-start lg:justify-center lg:gap-2.5 lg:py-5"}
          [:div {:class "flex items-center gap-3.5 min-w-0"}
           [:div {:class (str "flex flex-shrink-0 items-center justify-center w-9 h-9 rounded-xl " icon-bg)}
            [:span {:class tone} icon]]
           [:p {:class "text-sm font-medium text-zinc-600 truncate"} label]]
          [:p {:class (str "flex-shrink-0 text-lg font-semibold tracking-tight tabular-nums "
                           (or value-tone "text-zinc-900"))} value]])])))

(defn budget-section [budget-items]
  [:div
   (section-header "Budget this month" :href "/app/finances/" :link-label "View budget")
   (budget-snapshot budget-items)])

(defn- rand0
  "Compact Rand string with thousands separators, no decimals."
  [n]
  (format "R%,d" (long (Math/round (double (or n 0))))))

(defn- rate-label
  "Formats a rate as a fraction (e.g. 0.26) as '26%'."
  [rate]
  (str (int (Math/round (* 100 (double (or rate 0))))) "%"))

(defn- pct-label
  "Formats an already-percentage value (e.g. 31.0) as '31%'."
  [p]
  (str (int (Math/round (double (or p 0)))) "%"))

(defn- bracket-row [i b active-idx brackets]
  (let [active? (= i active-idx)
        from    (get b :threshold 0)
        nxt     (get-in brackets [(inc i) :threshold])
        range-s (if nxt (str (rand0 from) " – " (rand0 (dec nxt))) (str (rand0 from) "+"))]
    [:div {:class (str "flex items-center gap-3 px-5 py-3 sm:px-6 "
                       (when active? "bg-emerald-50/60"))}
     [:div {:class "flex items-center gap-2.5 min-w-0 flex-1"}
      [:span {:class (str "flex-shrink-0 w-1.5 h-1.5 rounded-full " (if active? "bg-emerald-500" "bg-zinc-300"))}]
      [:span {:class (str "text-sm truncate tabular-nums " (if active? "font-semibold text-zinc-900" "text-zinc-600"))} range-s]
      (when active?
        [:span {:class "flex-shrink-0 px-1.5 py-0.5 text-[10px] font-semibold text-emerald-700 bg-emerald-100 uppercase tracking-wide rounded"} "You"])]
     [:span {:class (str "flex-shrink-0 text-sm tabular-nums " (if active? "font-bold text-emerald-700" "text-zinc-500"))}
      (rate-label (get b :rate 0))]]))

(defn tax-bracket-breakdown
  [{:keys [brackets bracket-index marginal-rate annual-income next-threshold income-to-next-bracket]}]
  (when (and (seq brackets) (pos? (or annual-income 0)) bracket-index)
    (let [active   (nth brackets bracket-index)
          from     (get active :threshold 0)
          to       (or next-threshold from)
          span     (max 1 (- to from))
          progress (when next-threshold
                     (min 100.0 (max 0.0 (* 100.0 (/ (- (or annual-income 0) from) span)))))
          next-rate (when next-threshold (rate-label (get-in brackets [(inc bracket-index) :rate] 0)))]
      (tax-panel
       [:div {:class "flex items-start justify-between gap-3 px-5 pt-5 pb-4 sm:px-6"}
        [:div {:class "min-w-0"}
         [:h3 {:class "text-sm font-semibold text-zinc-900 tracking-tight"} "Your tax bracket"]
         [:p {:class "mt-0.5 text-xs text-zinc-400 leading-relaxed"}
          "Income is taxed in slices — your marginal rate applies only to the portion above the bracket threshold."]]
        [:div {:class "flex-shrink-0 text-right"}
         [:p {:class "text-2xl font-bold text-zinc-900 leading-none tracking-tight tabular-nums"} (pct-label marginal-rate)]
         [:p {:class "mt-1 text-[11px] text-zinc-400 uppercase tracking-wider"} "Marginal rate"]]]
       (when (and next-threshold progress)
         [:div {:class "px-5 pb-4 sm:px-6"}
          [:div {:class "overflow-hidden h-2 w-full bg-zinc-100 rounded-full"}
           [:div {:class "h-full overflow-hidden rounded-full"
                  :style {:width (str (format "%.0f" progress) "%")}}
            [:div {:class "h-full w-full bg-emerald-500 rounded-full greed-bar-grow"}]]]
          [:p {:class "mt-2 text-xs text-zinc-500"}
           [:span {:class "font-semibold text-zinc-700 tabular-nums"} (rand0 income-to-next-bracket)]
           (str " until the " next-rate " bracket")]])
       [:div {:class "border-t border-zinc-100 divide-y divide-zinc-100"}
        (map-indexed (fn [i b] (bracket-row i b bracket-index brackets)) brackets)]))))

(defn tax-stats [income-tax-data]
  (let [has-income? (pos? (or (:annual-income income-tax-data) 0))]
    [:div
     (section-header "Tax"
                     :href "https://www.sars.gov.za/tax-rates/income-tax/rates-of-tax-for-individuals/"
                     :link-label "SARS rates" :external? true)
     (if has-income?
       (tax-bracket-breakdown income-tax-data)
       (tax-panel
        [:div {:class "py-10 px-6 text-center"}
         [:div {:class "mx-auto mb-3 flex items-center justify-center w-10 h-10 rounded-full bg-zinc-50"}
          [:span {:class "text-zinc-400"} (svgs/chart-bar)]]
         [:p {:class "text-sm font-medium text-zinc-500"} "No tax data yet"]
         [:p {:class "mt-0.5 text-xs text-zinc-400"} "Add your salary to see your tax breakdown and bracket."]
         (shared/btn :variant :outline :size :md :class "mt-4" :href "/app/settings" "Add your salary")]))]))

(def ^:private upcoming-type-label
  {:bill "Bill" :income "Payment in" :general "Event"})

(def ^:private upcoming-type-dot
  {:bill "bg-rose-400" :income "bg-emerald-400" :general "bg-violet-400"})

(defn- days-until [today d]
  (long (.between ChronoUnit/DAYS today d)))

(defn- relative-date [n]
  (cond (zero? n) "Today"
        (= 1 n)   "Tomorrow"
        :else     (str "in " n " days")))

(defn- format-date [d]
  (.format d (java.time.format.DateTimeFormatter/ofPattern "d MMM")))

(defn- next-payday
  "Next payday as a LocalDate: today when today is payday, otherwise the
   coming occurrence of the configured day of month (clamped to the month's
   length, so a 31st payday lands on the 30th in a 30-day month)."
  [today payday]
  (when (and payday (pos? (long payday)))
    (let [year       (.getYear today)
          month      (.getMonthValue today)
          dom        (.getDayOfMonth today)
          this-month (YearMonth/of year month)
          this-pd    (min (long payday) (.lengthOfMonth this-month))]
      (if (<= dom this-pd)
        (LocalDate/of year month this-pd)
        (let [next (.plusMonths this-month 1)]
          (LocalDate/of (.getYear next) (.getMonthValue next)
                        (min (long payday) (.lengthOfMonth next))))))))

(defn upcoming-panel [payday events]
  (let [today   (LocalDate/now)
        pd      (next-payday today payday)
        pd-until (when pd (days-until today pd))
        upcoming (->> events
                      (filter (fn [{:event/keys [date]}]
                                (when date
                                  (let [d (LocalDate/parse date)]
                                    (not (neg? (days-until today d)))))))
                      (take 3))]
    (tax-panel
     {:class "flex-1"}
     (if (or pd (seq upcoming))
       [:div {:class "divide-y divide-zinc-100"}
        (when pd
          [:div {:class "flex items-center gap-3 px-5 py-3 sm:px-6"}
           [:div {:class "flex-shrink-0 w-2 h-2 rounded-full bg-emerald-400"}]
           [:div {:class "min-w-0 flex-1"}
            [:p {:class "text-sm font-medium text-zinc-800"} "Payday"]
            [:p {:class "mt-0.5 text-xs text-zinc-400"} (str "Salary · " (format-date pd))]]
           [:span {:class "flex-shrink-0 text-xs font-medium text-emerald-600 tabular-nums"}
            (relative-date pd-until)]])
        (for [{:event/keys [title date type]} upcoming]
          (let [d (LocalDate/parse date)]
            [:div {:class "flex items-center gap-3 px-5 py-3 sm:px-6"}
             [:div {:class (str "flex-shrink-0 w-2 h-2 rounded-full "
                                (get upcoming-type-dot (or type :general) "bg-violet-400"))}]
             [:div {:class "min-w-0 flex-1"}
              [:p {:class "text-sm font-medium text-zinc-800 truncate"} title]
              [:p {:class "mt-0.5 text-xs text-zinc-400"}
               (str (get upcoming-type-label (or type :general) "Event") " · " (format-date d))]]
             [:span {:class "flex-shrink-0 text-xs font-medium text-zinc-500 tabular-nums"}
              (relative-date (days-until today d))]]))]
       [:div {:class "h-full flex flex-col items-center justify-center py-10 px-6 text-center"}
        [:div {:class "mx-auto mb-3 flex items-center justify-center w-10 h-10 rounded-full bg-zinc-50"}
         [:span {:class "text-zinc-400"} (svgs/calendar)]]
        [:p {:class "text-sm font-medium text-zinc-500"} "Nothing scheduled"]
        [:p {:class "mt-0.5 text-xs text-zinc-400"} "Add bills and paydays to see what's next."]
        (shared/btn :variant :outline :size :md :class "mt-4" :href "/app/calendar" "Add an event")]))))

(defn upcoming-section [payday events]
  [:div {:class "flex flex-col h-full"}
   (section-header "Upcoming" :href "/app/calendar/" :link-label "View calendar")
   (upcoming-panel payday events)])

(defn- goal-pct [saved target]
  (if (and target (pos? target))
    (int (min 100 (Math/round (* 100.0 (/ (double (or saved 0)) target)))))
    0))

(defn goals-panel [goals]
  (let [sorted (sort-by (fn [{:goal/keys [saved target]}]
                          (if (and target (pos? target))
                            (double (/ (or saved 0) target))
                            0.0))
                        > goals)
        shown (take 2 sorted)
        rest-count (max 0 (- (count goals) (count shown)))]
    (tax-panel
     {:class "flex-1"}
     (if (seq goals)
       [:div {:class "divide-y divide-zinc-100"}
        (for [{:goal/keys [title target saved]} shown]
          (let [p (goal-pct saved target)
                complete? (>= (or saved 0) (or target 0))]
            [:div {:class "px-5 py-4 sm:px-6"}
             [:div {:class "flex items-center justify-between gap-3"}
              [:p {:class "text-sm font-medium text-zinc-800 truncate"} title]
              [:span {:class "flex-shrink-0 text-xs text-zinc-400 tabular-nums"}
               (str (utilities/amount->rands (or saved 0)) " of " (utilities/amount->rands (or target 0)))]]
             [:div {:class "mt-2 flex items-center gap-3"}
              [:div {:class "flex-1 overflow-hidden h-1.5 bg-zinc-100 rounded-full"}
               [:div {:class "h-full overflow-hidden rounded-full" :style {:width (str p "%")}}
                [:div {:class (str "h-full w-full rounded-full greed-bar-grow "
                                   (if complete? "bg-emerald-500" "bg-emerald-400"))}]]]
              [:span {:class "flex-shrink-0 text-xs font-medium text-emerald-600 tabular-nums"} (str p "%")]]]))
        (when (pos? rest-count)
          [:div {:class "px-5 py-3 text-center sm:px-6"}
           [:p {:class "text-xs text-zinc-400"} (str rest-count " more in Goals")]])]
       [:div {:class "h-full flex flex-col items-center justify-center py-10 px-6 text-center"}
        [:div {:class "mx-auto mb-3 flex items-center justify-center w-10 h-10 rounded-full bg-emerald-50"}
         [:span {:class "text-emerald-500"} (svgs/target)]]
        [:p {:class "text-sm font-medium text-zinc-500"} "No savings goals yet"]
        [:p {:class "mt-0.5 text-xs text-zinc-400"} "Set a target and watch your savings grow."]
        (shared/btn :variant :primary :size :md :class "mt-4" :href "/app/goals" "Add a goal")]))))

(defn goals-section [goals]
  [:div {:class "flex flex-col h-full"}
   (section-header "Goals" :href "/app/goals/" :link-label "View goals")
   (goals-panel goals)])
