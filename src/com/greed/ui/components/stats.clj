(ns com.greed.ui.components.stats
  (:require [com.greed.ui.core :as c.ui]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.tax :as tax]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.ui.components.shared :as shared])
  (:import [java.time LocalDate YearMonth]
           [java.time.temporal ChronoUnit]))

(declare ^:private pct-label
         ^:private days-until
         ^:private relative-date
         ^:private format-date
         ^:private next-payday
         ^:private prev-payday
         ^:private hero-panel
         ^:private hero-eyebrow
         ^:private hero-headline
         ^:private hero-status
         ^:private hero-stats-row
         ^:private hero-substat)

(defn- pct-share
  "Share of `amount` as a percentage of `total`, or nil when `total` isn't
   positive (nothing to divide against)."
  [amount total]
  (when (pos? (double (or total 0)))
    (double (* 100.0 (/ (double (or amount 0)) (double total))))))

(defn- whole->rands
  "Rands at display scale — whole, no trailing decimals — for large hero
   figures where a '.00' suffix would read as noise."
  [amount]
  (format "R%,d" (long (Math/round (double amount)))))

(defn finance-hero
  "Finances-page hero: the month's plan read at a glance. One headline number
   — what's left to plan after expenses and savings — framed by the pay period:
   how far through it we are, what that leaves to spend per day, and the bills
   still to clear before the next payday. Overspend flips the headline and its
   tone instead of hiding the problem."
  [budget-items payday events]
  (let [{:keys [total-income total-expenses total-savings]} (c.ui/get-budget-data budget-items)
        income     (or total-income 0)
        leftover   (- income (or total-expenses 0) (or total-savings 0))
        overspend? (neg? leftover)
        today      (LocalDate/now)
        pd         (next-payday today payday)
        prev-pd    (prev-payday today payday)
        period-len (when (and pd prev-pd)
                     (inc (long (.between ChronoUnit/DAYS prev-pd pd))))
        elapsed    (when (and pd prev-pd)
                     (inc (long (.between ChronoUnit/DAYS prev-pd today))))
        remaining  (when pd (long (.between ChronoUnit/DAYS today pd)))
        spend-per-day (when (and remaining (pos? remaining) (pos? income))
                        (/ (double (Math/abs (double leftover))) remaining))
        bills-ahead (->> events
                         (filter (fn [{:event/keys [type date]}]
                                   (when (and (= (or type :general) :bill) date)
                                     (let [until (days-until today (LocalDate/parse date))]
                                       (and (not (neg? until))
                                            (or (nil? pd) (<= until remaining))))))))
        bills       (take 3 bills-ahead)
        bills-more  (max 0 (- (count bills-ahead) (count bills)))]
    (if (zero? income)
      ;; Nothing to plan yet — keep the surface, explain, and point at the one
      ;; way in (the income list's Add item). Same pattern as the dashboard's
      ;; budget-glance empty state.
      [:div {:class "reveal relative overflow-hidden rounded-2xl bg-white ring-1 ring-zinc-200/70 shadow-card-md"}
       [:div {:class "absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-emerald-500/40 to-transparent"}]
       [:div {:class "relative flex flex-col items-center px-6 py-12 text-center"}
        [:div {:class "mb-3 flex items-center justify-center w-12 h-12 rounded-full bg-emerald-50"}
         [:span {:class "text-emerald-500"} (svgs/wallet)]]
        [:p {:class "text-sm font-medium text-zinc-500"} "No budget yet"]
        [:p {:class "mt-1 text-xs text-zinc-400"} "Add your income, expenses and savings to plan this month."]
        (shared/btn :variant :primary :size :md :class "mt-5"
                    :attrs {"_" (shared/open-actions "budget-add-income-modal")}
                    (svgs/plus {:class "w-4 h-4"})
                    "Add your first item")]]
      (hero-panel
       {:inner-class "flex flex-col"}
       [:div {:class "grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)] lg:items-center"}
        [:div
         (hero-eyebrow (if overspend? "Over budget by" "Left to plan")
                       :badge (when pd
                                [:span {:class "flex-shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700 ring-1 ring-emerald-600/15"}
                                 (str "Payday · " (format-date pd))]))
         (hero-headline (whole->rands (Math/abs (double leftover))) "this month")
         (hero-status (if overspend?
                        (str "That's " (utilities/amount->rands (Math/abs (double leftover)))
                             " more going out than coming in this month.")
                        (str "Out of " (utilities/amount->rands income) " in income."))
                      :tone (when overspend? "text-rose-600"))
         (when (and pd prev-pd)
           [:div {:class "mt-6"}
            [:div {:class "flex items-center justify-between gap-3"}
             [:p {:class "text-[11px] font-medium text-zinc-500 uppercase tracking-wider"} "Pay period"]
             [:p {:class "text-xs font-medium text-zinc-400 tabular-nums"}
              (str "Day " elapsed " of " period-len)]]
            [:div {:class "mt-2 h-1.5 w-full overflow-hidden rounded-full bg-zinc-100"}
             [:div {:class (str "h-full rounded-full " (if overspend? "bg-rose-400" "bg-emerald-400"))
                    :style {:width (str (min 100.0 (double (* 100.0 (/ (double elapsed) (double period-len))))) "%")}}]]])
         (when spend-per-day
           [:p {:class (str "mt-3 text-sm font-semibold tabular-nums "
                            (if overspend? "text-rose-600" "text-emerald-600"))}
            (utilities/amount->rands (Math/round (double spend-per-day)))
            (if overspend? " per day over until payday" " per day to spend until payday")])]
        [:div
         [:div {:class "flex items-center justify-between gap-3"}
          [:p {:class "text-xs font-semibold text-zinc-400 uppercase tracking-wider"} "Bills before payday"]
          [:a {:href "/app/calendar"
               :class "group inline-flex items-center gap-0.5 rounded-md text-xs font-medium text-emerald-600 transition hover:text-emerald-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/70 focus-visible:ring-offset-2 active:text-emerald-800 active:scale-[0.97]"}
           "View calendar"
           (svgs/->next {:class "size-3.5 -translate-x-0.5 transition-transform group-hover:translate-x-0"})]]
         (if (seq bills)
           [:div {:class "mt-4 divide-y divide-zinc-100"}
            (for [{:event/keys [title date]} bills]
              (let [until (days-until today (LocalDate/parse date))]
                [:div {:class "flex items-center gap-3 py-2.5"}
                 [:span {:class "flex-shrink-0 w-2 h-2 rounded-full bg-rose-400"}]
                 [:div {:class "min-w-0 flex-1"}
                  [:p {:class "truncate text-sm font-medium text-zinc-800"} title]
                  [:p {:class "mt-0.5 text-xs text-zinc-400"} (format-date (LocalDate/parse date))]]
                 [:span {:class "flex-shrink-0 text-xs font-medium text-zinc-500 tabular-nums"}
                  (relative-date until)]]))
            (when (pos? bills-more)
              [:p {:class "pt-2.5 text-xs text-zinc-400"}
               (str bills-more " more before payday")])]
           [:p {:class "mt-4 text-sm text-zinc-400"} "No bills scheduled before payday."])]]
       [:div {:class "flex-1"}]
       (hero-stats-row
         (hero-substat "Income" (utilities/amount->rands income) "text-emerald-600")
         (hero-substat "Expenses" (utilities/amount->rands total-expenses) "text-rose-600")
         (hero-substat "Savings rate" (pct-label (pct-share total-savings income))))))))

(defn savings-pace
  "Insights pacing read: the month's savings rate against the 20% benchmark,
   framed by the current pay period — how far through it we are and how soon
   payday arrives."
  [budget-items payday]
  (let [{:keys [total-income total-savings]} (c.ui/get-budget-data budget-items)
        income     (or total-income 0)
        savings    (or total-savings 0)
        rate       (pct-share savings income)
        rate-num   (int (Math/round rate))
        on-target? (>= rate-num 20)
        today      (LocalDate/now)
        pd         (next-payday today payday)
        prev-pd    (prev-payday today payday)
        period-len (when (and pd prev-pd)
                     (inc (long (.between ChronoUnit/DAYS prev-pd pd))))
        elapsed    (when (and pd prev-pd)
                     (inc (long (.between ChronoUnit/DAYS prev-pd today))))
        remaining  (when pd (long (.between ChronoUnit/DAYS today pd)))]
    [:div {:class "p-6 bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card"}
     [:h3 {:class "text-sm font-semibold text-zinc-900 tracking-tight"} "Savings pace"]
     [:p {:class "mt-0.5 mb-5 text-xs text-zinc-400 leading-relaxed"}
      "How your savings rate stacks up against a healthy benchmark, and where payday sits."]
     (cond
       (not (pos? income))
       [:p {:class "text-center py-6 text-sm text-zinc-400"}
        "Add your "
        [:a {:href "/app/finances" :class "font-medium text-emerald-600 hover:underline"} "income"]
        " in Finances to see your pace."]
       (nil? pd)
       [:div {:class "text-center py-6"}
        [:p {:class "text-sm text-zinc-400"} "Set your payday in Finances to anchor your pace."]
        (shared/btn :variant :outline :size :md :class "mt-4" :href "/app/finances" "Set payday")]
       :else
       [:<>
        (when prev-pd
          [:div
           [:div {:class "flex items-center justify-between gap-3"}
            [:p {:class "text-[11px] font-medium text-zinc-500 uppercase tracking-wider"} "Pay period"]
            [:p {:class "text-xs font-medium text-zinc-400 tabular-nums"}
             (str "Day " elapsed " of " period-len)]]
           [:div {:class "mt-2 h-1.5 w-full overflow-hidden rounded-full bg-zinc-100"}
            [:div {:class "h-full rounded-full bg-emerald-400"
                   :style {:width (str (min 100.0 (double (* 100.0 (/ (double elapsed) (double period-len))))) "%")}}]]])
        [:div {:class "mt-5 flex items-center justify-between gap-3"}
         [:p {:class "text-sm text-zinc-600"} "Savings rate"]
         [:p {:class (str "text-lg font-semibold tabular-nums "
                          (cond on-target? "text-emerald-600"
                                (pos? rate-num) "text-amber-600"
                                :else "text-zinc-900"))}
          (pct-label rate)]]
        (if (pos? savings)
          [:div {:class "mt-3"}
           [:div {:class "overflow-hidden h-1.5 w-full bg-zinc-100 rounded-full"}
            [:div {:class "h-full rounded-full bg-emerald-400"
                   :style {:width (str (min 100.0 (double (* 100.0 (/ (max rate 0.0) 20.0)))) "%")}}]]
           [:p {:class "mt-1.5 text-xs text-zinc-400 leading-relaxed"}
            (if on-target?
              "Past the 20% benchmark — savings are compounding nicely."
              (str (Math/abs (- 20 rate-num)) "% from the 20% savings benchmark."))]]
          [:p {:class "mt-3 text-xs text-zinc-400 leading-relaxed"} "Nothing set aside yet this month."])
         [:p {:class "mt-3 text-xs text-zinc-400 leading-relaxed"}
          (if (pos? remaining)
            (str "Payday in " remaining " day" (when (not= 1 remaining) "s") ".")
            "Payday is today.")]])]))

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

(defn- glance-row
  "One label/value row used by the budget glance."
  [label value value-cls]
  [:div {:class "flex items-center justify-between gap-3 px-5 py-3 sm:px-6"}
   [:p {:class "text-sm text-zinc-600"} label]
   [:p {:class (str "text-sm font-semibold tabular-nums " value-cls)} value]])

(defn budget-glance
  "Dashboard teaser for the planning surface: what's left to plan this month,
   with income, expenses and savings underneath. The Finances page leads with
   the fuller picture."
  [budget-items]
  (let [{:keys [total-income total-expenses total-savings]} (c.ui/get-budget-data budget-items)
        income     (or total-income 0)
        leftover   (- income (or total-expenses 0) (or total-savings 0))
        overspend? (neg? leftover)]
    (tax-panel
     {:class "flex-1"}
     (if (zero? income)
       [:div {:class "flex h-full flex-col items-center justify-center px-6 py-10 text-center"}
        [:div {:class "mx-auto mb-3 flex items-center justify-center w-10 h-10 rounded-full bg-zinc-50"}
         [:span {:class "text-zinc-400"} (svgs/wallet)]]
        [:p {:class "text-sm font-medium text-zinc-500"} "No budget yet"]
        [:p {:class "mt-0.5 text-xs text-zinc-400"} "Add your income and expenses in Finances to plan this month."]
        (shared/btn :variant :outline :size :md :class "mt-4" :href "/app/finances" "Plan this month")]
       [:div {:class "flex h-full flex-col"}
        [:div {:class "px-5 pt-5 sm:px-6"}
         [:p {:class "text-[11px] font-medium text-zinc-500 uppercase tracking-wider"}
          (if overspend? "Over budget by" "Left to plan")]
         [:p {:class (str "mt-1 text-2xl font-bold tracking-tight tabular-nums "
                          (if overspend? "text-rose-600" "text-zinc-900"))}
          (whole->rands (Math/abs (double leftover)))]
         [:p {:class "mt-0.5 text-xs text-zinc-400"}
          (if overspend?
            "more going out than coming in this month."
            (str "of " (whole->rands income) " income this month."))]]
        [:div {:class "mt-auto border-t border-zinc-100 divide-y divide-zinc-100"}
         (glance-row "Income" (utilities/amount->rands income) "text-emerald-600")
         (glance-row "Expenses" (utilities/amount->rands total-expenses) "text-rose-600")
         (glance-row "Savings" (utilities/amount->rands total-savings) "text-zinc-900")]]))))

(defn budget-section [budget-items]
  [:div {:class "flex flex-col h-full"}
   (section-header "Budget" :href "/app/finances/" :link-label "View budget")
   (budget-glance budget-items)])

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
       {:class "flex-1"}
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
        (map-indexed (fn [i b] (bracket-row i b bracket-index brackets)) brackets)]
       [:div {:class "flex justify-end border-t border-zinc-100 px-5 py-3 sm:px-6"}
        [:a {:href "https://www.sars.gov.za/tax-rates/income-tax/rates-of-tax-for-individuals/"
             :target "_blank" :rel "noopener noreferrer"
             :class "text-xs font-medium text-emerald-600 hover:underline"}
         "Rates from SARS"]]))))

(defn tax-stats [income-tax-data]
  (let [has-income? (pos? (or (:annual-income income-tax-data) 0))]
    [:div {:class "flex flex-col h-full"}
     (section-header "Tax" :href "/app/tax" :link-label "View tax")
     (if has-income?
       (tax-bracket-breakdown income-tax-data)
       (tax-panel
        {:class "flex-1"}
        [:div {:class "flex h-full flex-col items-center justify-center py-10 px-6 text-center"}
         [:div {:class "mx-auto mb-3 flex items-center justify-center w-10 h-10 rounded-full bg-zinc-50"}
          [:span {:class "text-zinc-400"} (svgs/percent-badge)]]
         [:p {:class "text-sm font-medium text-zinc-500"} "No tax data yet"]
         [:p {:class "mt-0.5 text-xs text-zinc-400"} "Add your salary to see your tax breakdown and bracket."]
         (shared/btn :variant :outline :size :md :class "mt-4" :href "/app/settings" "Add your salary")]))]))

(defn tax-hero
  "Tax-page hero: the annual picture at a glance. One headline number — what
   you take home for the year — with the split of every rand as a stacked bar,
   so what SARS keeps is impossible to miss."
  [income-tax-data]
  (let [annual-income  (or (:annual-income income-tax-data) 0)
        net-tax        (or (:net-tax income-tax-data) 0)
        net-income     (or (:net-income income-tax-data) 0)
        effective-rate (double (or (:effective-rate income-tax-data) 0))
        marginal-rate  (or (:marginal-rate income-tax-data) 0)
        tax-share      (min 100.0 (max 0.0 effective-rate))
        take-share     (max 0.0 (- 100.0 tax-share))
        status         (if (pos? net-tax)
                         (str "Of every R1 you earn, " (int (Math/round effective-rate)) "c goes to SARS.")
                         "You're below the rebate threshold — no income tax this year.")]
    (if (not (pos? annual-income))
      (hero-panel
       {:inner-class "flex flex-col items-center px-6 py-12 text-center"}
       [:div {:class "mb-3 flex items-center justify-center w-12 h-12 rounded-full bg-emerald-50"}
        [:span {:class "text-emerald-500"} (svgs/percent-badge)]]
       [:p {:class "text-sm font-medium text-zinc-500"} "No salary yet"]
       [:p {:class "mt-1 text-xs text-zinc-400"} "Add your salary in Settings to see your annual tax picture."]
       (shared/btn :variant :primary :size :md :class "mt-5" :href "/app/settings" "Add your salary"))
      (hero-panel
       {:inner-class "flex flex-col"}
       [:div {:class "grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)] lg:items-center"}
        [:div
         (hero-eyebrow "Your tax year"
                       :badge [:span {:class "flex-shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700 ring-1 ring-emerald-600/15"}
                               "2026/27"])
         (hero-headline (whole->rands net-income) "take-home this year")
         (hero-status status)]
        [:div {:class "rounded-xl bg-zinc-50 p-5 ring-1 ring-zinc-200/50"}
         [:p {:class "text-xs font-semibold text-zinc-500 uppercase tracking-wider"} "Where your income goes"]
         [:div {:class "mt-3 flex h-2.5 w-full overflow-hidden rounded-full bg-zinc-200/70"}
          [:div {:class "h-full rounded-l-full bg-emerald-500 greed-bar-grow"
                 :style {:width (str take-share "%")}}]
          [:div {:class "h-full rounded-r-full bg-rose-400"
                 :style {:width (str tax-share "%")}}]]
         [:div {:class "mt-3 flex items-center justify-between gap-3 text-sm"}
          [:span {:class "flex items-center gap-2 text-zinc-500"}
           [:span {:class "h-2 w-2 rounded-full bg-emerald-500"}] "Take-home"]
          [:span {:class "font-semibold text-zinc-900 tabular-nums"} (utilities/amount->rands net-income)]]
         [:div {:class "mt-1.5 flex items-center justify-between gap-3 text-sm"}
          [:span {:class "flex items-center gap-2 text-zinc-500"}
           [:span {:class "h-2 w-2 rounded-full bg-rose-400"}] "Tax"]
          [:span {:class "font-semibold text-rose-600 tabular-nums"} (utilities/amount->rands net-tax)]]]]
       [:div {:class "flex-1"}]
       (hero-stats-row
        (hero-substat "Annual gross" (utilities/amount->rands annual-income))
        (hero-substat "Effective rate" (pct-label effective-rate) "text-rose-600")
        (hero-substat "Marginal rate" (pct-label marginal-rate)))))))

(defn tax-readiness
  "Tax-page companion to the bracket breakdown: the deductions Greed applies
   from the tax profile (Settings) — retirement annuity, medical aid credits.
   Prompts to add them when none are set, so the estimate stays honest."
  [income-tax-data tax-profile]
  (let [annual-income (or (:annual-income income-tax-data) 0)
        age           (or (:age income-tax-data) 21)
        med-monthly   (or (:tax-profile/medical-monthly tax-profile) 0)
        dependants    (or (:tax-profile/medical-dependants tax-profile) 0)
        ra-annual     (or (:tax-profile/ra-annual tax-profile) 0)
        ra-ded        (tax/ra-deduction annual-income ra-annual)
        mtc           (if (pos? med-monthly) (tax/medical-tax-credit dependants) 0)
        add-med       (tax/additional-medical-credit age med-monthly 0 mtc)
        has-any?      (or (pos? ra-ded) (pos? mtc) (pos? add-med))]
    (tax-panel
     {:class "flex-1"}
     (if (not has-any?)
       [:div {:class "flex h-full flex-col items-center justify-center px-6 py-10 text-center"}
        [:div {:class "mx-auto mb-3 flex items-center justify-center w-10 h-10 rounded-full bg-emerald-50"}
         [:span {:class "text-emerald-500"} (svgs/percent-badge)]]
        [:p {:class "text-sm font-medium text-zinc-500"} "Sharpen your estimate"]
        [:p {:class "mt-0.5 max-w-xs text-xs text-zinc-400"}
         "Add your medical aid and retirement annuity details — Greed applies them to your annual tax picture automatically."]
        (shared/btn :variant :outline :size :md :class "mt-4" :href "/app/settings" "Add details")]
       [:div
        [:div {:class "flex items-center justify-between gap-3 px-5 pt-5 sm:px-6"}
         [:h3 {:class "text-sm font-semibold text-zinc-900 tracking-tight"} "Your deductions"]
         [:span {:class "flex-shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700 ring-1 ring-emerald-600/15"} "Applied"]]
        [:p {:class "mt-0.5 px-5 text-xs text-zinc-400 leading-relaxed sm:px-6"}
         "These lower your taxable income — and what SARS keeps."]
        [:div {:class "mt-4 border-t border-zinc-100 divide-y divide-zinc-100"}
         (when (pos? ra-ded)
           [:div {:class "flex items-center justify-between gap-3 px-5 py-3 sm:px-6"}
            [:p {:class "text-sm text-zinc-600"} "Retirement annuity deduction"]
            [:p {:class "text-sm font-semibold text-zinc-900 tabular-nums"} (utilities/amount->rands ra-ded)]])
         (when (pos? mtc)
           [:div {:class "flex items-center justify-between gap-3 px-5 py-3 sm:px-6"}
            [:p {:class "text-sm text-zinc-600"} "Medical aid tax credit"]
            [:p {:class "text-sm font-semibold text-emerald-600 tabular-nums"} (utilities/amount->rands mtc)]])
         (when (pos? add-med)
           [:div {:class "flex items-center justify-between gap-3 px-5 py-3 sm:px-6"}
            [:p {:class "text-sm text-zinc-600"} "Additional medical credit (s6B)"]
            [:p {:class "text-sm font-semibold text-emerald-600 tabular-nums"} (utilities/amount->rands add-med)]])]
        [:div {:class "flex justify-end px-5 py-4 sm:px-6"}
         [:a {:href "/app/settings" :class "text-xs font-medium text-emerald-600 hover:underline"} "Edit in Settings"]]]))))

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

(defn- prev-payday
  "Previous payday as a LocalDate: the payday before today (clamped to the
   month's length, mirroring next-payday)."
  [today payday]
  (when (and payday (pos? (long payday)))
    (let [year       (.getYear today)
          month      (.getMonthValue today)
          dom        (.getDayOfMonth today)
          this-month (YearMonth/of year month)
          this-pd    (min (long payday) (.lengthOfMonth this-month))]
      (if (> dom this-pd)
        (LocalDate/of year month this-pd)
        (let [prev (.minusMonths this-month 1)]
          (LocalDate/of (.getYear prev) (.getMonthValue prev)
                        (min (long payday) (.lengthOfMonth prev))))))))

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

;; Every app page leads with a hero in the same card chrome so the hierarchy
;; reads consistently across the product: eyebrow label, one headline number
;; with a muted unit, an optional one-line reading, and three supporting
;; figures split by a hairline rule.

(defn- hero-panel
  "Shared hero surface: white card with a hairline ring, emerald top glow and a
   soft corner bloom. An optional leading opts map ({:class ...} for the card,
   :inner-class for the padded content) lets a hero stretch inside an
   equal-height row."
  [& args]
  (let [opts?    (map? (first args))
        opts     (if opts? (first args) {})
        children (if opts? (rest args) args)]
    [:div {:class (str "relative overflow-hidden rounded-2xl bg-white ring-1 ring-zinc-200/70 shadow-card-md "
                       (:class opts))}
     [:div {:class "absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-emerald-500/40 to-transparent"}]
     [:div {:class "absolute -top-24 -right-24 h-72 w-72 rounded-full bg-emerald-400/10 blur-3xl"}]
     (into [:div {:class (str "relative px-6 py-6 sm:px-8 sm:py-7 " (:inner-class opts))}]
           children)]))

(defn- hero-eyebrow
  "Uppercase emerald label above the headline. An optional badge (e.g. the
   dashboard payday pill) sits right-aligned on the same row."
  [label & {:keys [badge]}]
  [:div {:class "flex items-center gap-2.5"}
   [:span {:class "h-1.5 w-1.5 rounded-full bg-emerald-500"}]
   [:p {:class "text-[11px] sm:text-xs font-semibold text-emerald-600 uppercase tracking-[0.18em]"} label]
   [:div {:class "flex-1"}]
   (when badge badge)])

(defn- hero-headline
  "The hero's single headline number, with a muted unit beside it. One shared
   display scale (same as the finance hero) so figures read consistently."
  [value suffix]
  [:div {:class "mt-5 flex items-baseline gap-2.5 sm:mt-6"}
   [:p {:class "text-4xl sm:text-5xl font-bold text-zinc-900 leading-none tracking-[-0.04em] tabular-nums"}
    value]
   [:span {:class "text-sm font-medium text-zinc-400"} suffix]])

(defn- hero-status
  "One-line reading of the headline, below it. Tone defaults to zinc; pass an
   explicit tone for warnings (e.g. an overspend)."
  [content & {:keys [tone]}]
  [:p {:class (str "mt-3 text-sm " (or tone "text-zinc-500"))} content])

(defn- hero-stats-row
  "Three supporting figures under the headline, split by a hairline rule."
  [& substats]
  (into [:div {:class "grid grid-cols-3 gap-4 pt-5 mt-6 border-t border-zinc-100"}]
        substats))

(defn- hero-substat
  "One supporting figure: tiny uppercase label over a medium value."
  [label value & [value-cls]]
  [:div {:class "min-w-0"}
   [:p {:class "text-[11px] font-medium text-zinc-500 uppercase tracking-wider whitespace-nowrap"} label]
   [:p {:class (str "mt-1 text-sm font-semibold whitespace-nowrap tabular-nums sm:text-lg "
                    (or value-cls "text-zinc-900"))} value]])

(defn dashboard-hero
  "Dashboard feature card leading with monthly net take-home."
  [finances income-tax-data]
  (let [{:finances/keys [salary payday]} finances
        {:keys [net-tax net-income effective-rate]} income-tax-data
        monthly-net (when net-income (/ net-income 12))
        monthly-tax (when net-tax (/ net-tax 12))]
    (hero-panel
     {:class "h-full" :inner-class "flex flex-col h-full"}
     (hero-eyebrow "Net take-home"
                   :badge (when payday
                            [:span {:class "flex-shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700 ring-1 ring-emerald-600/15"}
                             (str "Payday · " (utilities/ordinal payday))]))
     (hero-headline (if monthly-net (utilities/amount->rands monthly-net) "—") "p/m")
     [:div {:class "flex-1"}]
     (hero-stats-row
      (hero-substat "Gross salary"   (utilities/amount->rands (or salary 0)))
      (hero-substat "Est. tax / mo"  (if monthly-tax (utilities/amount->rands monthly-tax) "—"))
      (hero-substat "Effective rate" (utilities/->percentage (or effective-rate 0)))))))

(defn goals-hero
  "Goals feature card leading with how much of the overall goal total is funded."
  [goals]
  (let [saved-total  (reduce + (map #(or (:goal/saved %) 0) goals))
        target-total (reduce + (map #(or (:goal/target %) 0) goals))
        remaining    (max 0 (- target-total saved-total))
        p            (goal-pct saved-total target-total)
        status       (cond
                       (and (pos? target-total) (>= saved-total target-total))
                       [:span {:class "text-emerald-600"} "Every goal fully funded — nice work."]
                       (>= p 50) "More than halfway to fully funding your goals."
                       (pos? saved-total) "Keep going — every contribution gets you closer."
                       :else
                       [:span "Nothing saved yet. "
                        [:a {:href "/app/finances"
                             :class "font-semibold text-emerald-600 underline underline-offset-2"}
                         "Add savings"]
                        " in Finances to start funding your goals."])]
    (hero-panel
     (hero-eyebrow "Overall goal funding")
     (hero-headline (if (pos? target-total) (str p "%") "—") "funded across all goals")
     (hero-status status)
     (hero-stats-row
      (hero-substat "Active goals" (str (count goals)))
      (hero-substat "Saved" (utilities/amount->rands saved-total) "text-emerald-600")
      (hero-substat "Still to save" (utilities/amount->rands remaining))))))

(defn insights-hero
  "Insights feature card leading with the monthly savings rate."
  [total-income total-expenses leftover overspend? savings-rate]
  (let [rate-num (int (Math/round savings-rate))
        status   (cond
                   (not (pos? total-income))
                   [:span "Add your "
                    [:a {:href "/app/finances"
                         :class "font-semibold text-emerald-600 underline underline-offset-2"}
                     "income"]
                    " in Finances to get a read on your savings."]
                   overspend?
                   [:span {:class "text-rose-600"} "You're spending more than you earn — review your budget in Finances."]
                   (>= rate-num 20) "A strong savings foundation — keep it up."
                   (>= rate-num 10) "On a healthy track. A 20% savings rate is a great goal."
                   (pos? rate-num) "A solid start — every little bit compounds."
                   :else
                   [:span "Nothing set aside yet. "
                    [:a {:href "/app/finances"
                         :class "font-semibold text-emerald-600 underline underline-offset-2"}
                     "Add savings"]
                    " in Finances."])]
    (hero-panel
     (hero-eyebrow "Monthly savings rate")
     (hero-headline (if (pos? total-income) (pct-label savings-rate) "—") "of income saved")
     (hero-status status :tone (when overspend? "text-rose-600"))
     (hero-stats-row
      (hero-substat "Income" (utilities/amount->rands total-income) "text-emerald-600")
      (hero-substat "Expenses" (utilities/amount->rands total-expenses) "text-rose-600")
      (hero-substat (if overspend? "Overspend" "Unallocated")
                    (utilities/amount->rands (Math/abs (long leftover)))
                    (if overspend? "text-rose-600" "text-zinc-900"))))))

(defn calendar-hero
  "Calendar feature card leading with the next payday — the month's anchor —
   and what's scheduled ahead of it."
  [payday events]
  (let [today         (LocalDate/now)
        pd            (next-payday today payday)
        pd-until      (when pd (days-until today pd))
        upcoming      (->> events
                           (filter (fn [{:event/keys [date]}]
                                     (when date
                                       (not (neg? (days-until today (LocalDate/parse date))))))))
        upcoming-count (count upcoming)
        bills         (count (filter #(= (or (:event/type %) :general) :bill) upcoming))
        income        (count (filter #(= (or (:event/type %) :general) :income) upcoming))
        events-ahead  (count (filter #(= (or (:event/type %) :general) :general) upcoming))
        status        (cond
                        (nil? pd)
                        [:span "Add a "
                         [:a {:href "/app/finances"
                              :class "font-semibold text-emerald-600 underline underline-offset-2"}
                          "payday"]
                         " in Finances to anchor your month."]
                        (zero? pd-until)
                        "Salary lands today."
                        :else
                        (str "Salary lands " (format-date pd) " — "
                             (if (zero? upcoming-count)
                               "nothing else scheduled."
                               (str upcoming-count
                                    (if (= 1 upcoming-count) " thing" " things")
                                    " scheduled ahead."))))]
    (hero-panel
     (hero-eyebrow "Next payday")
     (hero-headline (cond
                      (nil? pd) "—"
                      (zero? pd-until) "Today"
                      :else (str pd-until))
                    (cond
                      (nil? pd) "payday not set"
                      (zero? pd-until) "is payday"
                      :else (str "day" (when (not= 1 pd-until) "s") " until payday")))
     (hero-status status)
     (hero-stats-row
      (hero-substat "Bills" (str bills) "text-rose-600")
      (hero-substat "Payments in" (str income) "text-emerald-600")
      (hero-substat "Events" (str events-ahead))))))
