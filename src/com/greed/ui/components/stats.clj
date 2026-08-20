(ns com.greed.ui.components.stats
  (:require [com.greed.data.core :as c.data]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.time :as u.time]
            [com.greed.utilities.tax :as tax]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.ui.components.shared :as shared])
  (:import [java.time LocalDate]
           [java.time.temporal ChronoUnit]))

(declare ^:private hero-panel
         ^:private hero-eyebrow
         ^:private hero-headline
         ^:private hero-status
         ^:private hero-stats-row
         ^:private hero-substat
         ^:private legend-row)

(defn finance-hero
  "Finances-page hero: the month's plan read at a glance, in the same language
   as the tax hero. One headline number — what's left to plan after expenses
   and savings — with a stacked bar of where every rand of income goes. The pay
   period anchors the pace below it: how far through the month we are, and what
   that leaves to spend per day. Overspend flips the headline and its tone
   instead of hiding the problem."
  [budget-items payday]
  (let [{:keys [total-income total-expenses total-savings]} (c.data/get-budget-data budget-items)
        income     (or total-income 0)
        leftover   (- income (or total-expenses 0) (or total-savings 0))
        overspend? (neg? leftover)
        today      (LocalDate/now)
        pd         (u.time/next-payday today payday)
        prev-pd    (u.time/prev-payday today payday)
        period-len (when (and pd prev-pd)
                     (inc (long (.between ChronoUnit/DAYS prev-pd pd))))
        elapsed    (when (and pd prev-pd)
                     (inc (long (.between ChronoUnit/DAYS prev-pd today))))
        remaining  (when pd (long (.between ChronoUnit/DAYS today pd)))
        spend-per-day (when (and remaining (pos? remaining) (pos? income))
                        (/ (double (Math/abs (double leftover))) remaining))
        expenses-share (if (pos? income) (* 100.0 (/ (double (or total-expenses 0)) income)) 0.0)
        savings-share  (if (pos? income) (* 100.0 (/ (double (or total-savings 0)) income)) 0.0)
        leftover-share (- 100.0 expenses-share savings-share)]
    (if (zero? income)
      ;; Nothing to plan yet — keep the surface, explain, and point at the one
      ;; way in (the income list's Add item). Same pattern as the dashboard's
      ;; summary-card empty state.
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
                                 (str "Payday · " (u.time/format-date pd))]))
         (hero-headline (utilities/whole->rands (Math/abs (double leftover))) "this month")
         (hero-status (if overspend?
                        (str "That's " (utilities/amount->rands (Math/abs (double leftover)))
                             " more going out than coming in this month.")
                        (str "Out of " (utilities/amount->rands income) " in income."))
                      :tone (when overspend? "text-rose-600"))]
        [:div {:class "rounded-xl bg-zinc-50 p-5 ring-1 ring-zinc-200/50"}
         [:p {:class "text-xs font-semibold text-zinc-500 uppercase tracking-wider"} "Where your money goes"]
         [:div {:class "mt-3 flex h-2.5 w-full overflow-hidden rounded-full bg-zinc-200/70"}
          [:div {:class "h-full rounded-full bg-rose-400 greed-bar-grow"
                 :style {:width (str (min 100.0 (max 0.0 expenses-share)) "%")}}]
          [:div {:class "h-full rounded-full bg-indigo-500 greed-bar-grow"
                 :style {:width (str (min 100.0 (max 0.0 savings-share)) "%")}}]
          [:div {:class "h-full rounded-full bg-emerald-300 greed-bar-grow"
                 :style {:width (str (min 100.0 (max 0.0 leftover-share)) "%")}}]]
         (when (and pd prev-pd)
           [:div {:class "mt-4 border-t border-zinc-100 pt-4"}
            [:div {:class "flex items-center justify-between gap-3"}
             [:p {:class "text-xs font-semibold text-zinc-500 uppercase tracking-wider"} "Pay period"]
             [:p {:class "text-xs font-medium text-zinc-400 tabular-nums"}
              (str "Day " elapsed " of " period-len)]]
            [:div {:class "mt-2 h-1.5 w-full overflow-hidden rounded-full bg-zinc-200/70"}
             [:div {:class (str "h-full rounded-full " (if overspend? "bg-rose-400" "bg-emerald-400"))
                    :style {:width (str (min 100.0 (double (* 100.0 (/ (double elapsed) (double period-len))))) "%")}}]]
            (when spend-per-day
              [:p {:class (str "mt-3 text-sm font-semibold tabular-nums "
                               (if overspend? "text-rose-600" "text-emerald-600"))}
               (utilities/amount->rands (Math/round (double spend-per-day)))
               (if overspend? " per day over until payday" " per day to spend until payday")])])]]
       [:div {:class "flex-1"}]
       (hero-stats-row
        (hero-substat "Income" (utilities/amount->rands income) "text-emerald-600")
        (hero-substat "Expenses" (utilities/amount->rands total-expenses) "text-rose-600")
        (hero-substat "Savings rate" (utilities/pct-label (utilities/pct-share total-savings income))))))))

(defn savings-pace
  "Insights pacing read: the month's savings rate against the 20% benchmark,
   framed by the current pay period — how far through it we are and how soon
   payday arrives."
  [budget-items payday]
  (let [{:keys [total-income total-savings]} (c.data/get-budget-data budget-items)
        income     (or total-income 0)
        savings    (or total-savings 0)
        rate       (utilities/pct-share savings income)
        rate-num   (int (Math/round rate))
        on-target? (>= rate-num 20)
        today      (LocalDate/now)
        pd         (u.time/next-payday today payday)
        prev-pd    (u.time/prev-payday today payday)
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
          (utilities/pct-label rate)]]
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

(defn- summary-card
  "Dashboard widget card: a coloured icon chip and label up top with an
   explicit View link, one glanceable figure, a one-line reading, and
   optional supporting rows. The whole card links through to the feature
   page — hover lifts the ring and nudges the chevron, press gives instant
   1:1 feedback (scale on pointer-down)."
  [& {:keys [label href icon icon-cls value value-cls reading reveal children]}]
  [:a
   {:href href
    :class (str "group relative flex flex-col overflow-hidden rounded-2xl bg-white ring-1 ring-zinc-200/70 shadow-card p-5 transition-all duration-150 ease-out hover:ring-zinc-300 hover:shadow-card-hover active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/70 focus-visible:ring-offset-2 "
                (or reveal ""))}
   [:div {:class "pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-emerald-500/40 to-transparent"}]
   [:div {:class "flex items-center justify-between"}
    [:div {:class "flex items-center gap-2.5"}
     (when icon
       [:span {:class (str "flex h-9 w-9 items-center justify-center rounded-xl " (or icon-cls "bg-zinc-50 text-zinc-500"))}
        icon])
     [:p {:class "text-[11px] font-semibold text-zinc-400 uppercase tracking-wider"} label]]
    [:span {:class "flex items-center gap-1 text-xs font-medium text-zinc-400 transition-colors duration-150 ease-out group-hover:text-emerald-600"}
     "View"
     (svgs/->next {:class "-translate-x-0.5 size-3.5 transition-transform duration-150 ease-out group-hover:translate-x-0 group-hover:text-emerald-600"})]]
   [:p {:class (str "mt-4 text-3xl font-bold leading-none tracking-tight tabular-nums " (or value-cls "text-zinc-900"))}
    value]
   (when reading
     [:p {:class "mt-1.5 text-xs text-zinc-400 text-wrap"} reading])
   (when (seq children)
     [:div {:class "mt-4 border-t border-zinc-100"} children])])

(defn- bracket-row [i b active-idx brackets]
  (let [active? (= i active-idx)
        from    (get b :threshold 0)
        nxt     (get-in brackets [(inc i) :threshold])
        range-s (if nxt (str (utilities/whole->rands from) " – " (utilities/whole->rands (dec nxt))) (str (utilities/whole->rands from) "+"))]
    [:div {:class (str "flex items-center gap-3 px-5 py-3 sm:px-6 "
                       (when active? "bg-emerald-50/60"))}
     [:div {:class "flex items-center gap-2.5 min-w-0 flex-1"}
      [:span {:class (str "flex-shrink-0 w-1.5 h-1.5 rounded-full " (if active? "bg-emerald-500" "bg-zinc-300"))}]
      [:span {:class (str "text-sm truncate tabular-nums " (if active? "font-semibold text-zinc-900" "text-zinc-600"))} range-s]
      (when active?
        [:span {:class "flex-shrink-0 px-1.5 py-0.5 text-[10px] font-semibold text-emerald-700 bg-emerald-100 uppercase tracking-wide rounded"} "You"])]
     [:span {:class (str "flex-shrink-0 text-sm tabular-nums " (if active? "font-bold text-emerald-700" "text-zinc-500"))}
      (utilities/rate-label (get b :rate 0))]]))

(defn tax-bracket-breakdown
  [{:keys [brackets bracket-index marginal-rate annual-income next-threshold income-to-next-bracket]}]
  (when (and (seq brackets) (pos? (or annual-income 0)) bracket-index)
    (let [active   (nth brackets bracket-index)
          from     (get active :threshold 0)
          to       (or next-threshold from)
          span     (max 1 (- to from))
          progress (when next-threshold
                     (min 100.0 (max 0.0 (* 100.0 (/ (- (or annual-income 0) from) span)))))
          next-rate (when next-threshold (utilities/rate-label (get-in brackets [(inc bracket-index) :rate] 0)))]
      (tax-panel
       {:class "flex-1"}
       [:div {:class "flex items-start justify-between gap-3 px-5 pt-5 pb-4 sm:px-6"}
        [:div {:class "min-w-0"}
         [:h3 {:class "text-sm font-semibold text-zinc-900 tracking-tight"} "Your tax bracket"]
         [:p {:class "mt-0.5 text-xs text-zinc-400 leading-relaxed"}
          "Income is taxed in slices — your marginal rate applies only to the portion above the bracket threshold."]]
        [:div {:class "flex-shrink-0 text-right"}
         [:p {:class "text-2xl font-bold text-zinc-900 leading-none tracking-tight tabular-nums"} (utilities/pct-label marginal-rate)]
         [:p {:class "mt-1 text-[11px] text-zinc-400 uppercase tracking-wider"} "Marginal rate"]]]
       (when (and next-threshold progress)
         [:div {:class "px-5 pb-4 sm:px-6"}
          [:div {:class "overflow-hidden h-2 w-full bg-zinc-100 rounded-full"}
           [:div {:class "h-full overflow-hidden rounded-full"
                  :style {:width (str (utilities/fmt-d "%.0f" progress) "%")}}
            [:div {:class "h-full w-full bg-emerald-500 rounded-full greed-bar-grow"}]]]
          [:p {:class "mt-2 text-xs text-zinc-500"}
           [:span {:class "font-semibold text-zinc-700 tabular-nums"} (utilities/whole->rands income-to-next-bracket)]
           (str " until the " next-rate " bracket")]])
       [:div {:class "border-t border-zinc-100 divide-y divide-zinc-100"}
        (map-indexed (fn [i b] (bracket-row i b bracket-index brackets)) brackets)]
       [:div {:class "flex justify-end border-t border-zinc-100 px-5 py-3 sm:px-6"}
        [:a {:href "https://www.sars.gov.za/tax-rates/income-tax/rates-of-tax-for-individuals/"
             :target "_blank" :rel "noopener noreferrer"
             :class "text-xs font-medium text-emerald-600 hover:underline"}
         "Rates from SARS"]]))))

(defn tax-summary
  "Dashboard widget for the tax surface: what SARS keeps, split in colour."
  [income-tax-data]
  (let [annual-income  (or (:annual-income income-tax-data) 0)
        net-tax        (or (:net-tax income-tax-data) 0)
        net-income     (or (:net-income income-tax-data) 0)
        effective-rate (double (or (:effective-rate income-tax-data) 0))
        marginal-rate  (or (:marginal-rate income-tax-data) 0)
        keep-share     (max 0.0 (- 100.0 effective-rate))]
    (if (pos? annual-income)
      (summary-card
       :label "Tax"
       :icon (svgs/percent-badge)
       :icon-cls "bg-violet-50 text-violet-600"
       :href "/app/tax"
       :value (utilities/pct-label effective-rate)
       :reading (str "You keep " (utilities/pct-label keep-share) " of every rand · marginal " (utilities/pct-label marginal-rate) ".")
       :reveal "reveal reveal-2"
       :children
       [:div
        [:div {:class "mt-3 flex h-2 w-full overflow-hidden rounded-full bg-zinc-100"}
         [:div {:class "h-full bg-emerald-500" :style {:width (str (utilities/fmt-d "%.1f" keep-share) "%")}}]
         [:div {:class "h-full bg-rose-500" :style {:width (str (utilities/fmt-d "%.1f" effective-rate) "%")}}]]
        [:div {:class "mt-4 space-y-3"}
         (legend-row "bg-emerald-500" "You keep" net-income (utilities/pct-label keep-share) "text-emerald-600")
         (legend-row "bg-rose-500" "SARS" net-tax (utilities/pct-label effective-rate) "text-rose-600")]])
      (summary-card
       :label "Tax"
       :icon (svgs/percent-badge)
       :icon-cls "bg-violet-50 text-violet-600"
       :href "/app/tax"
       :value "—"
       :reveal "reveal reveal-2"
       :reading "Add your salary to see your tax breakdown and bracket."))))

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
         (hero-headline (utilities/whole->rands net-income) "take-home this year")
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
        (hero-substat "Effective rate" (utilities/pct-label effective-rate) "text-rose-600")
        (hero-substat "Marginal rate" (utilities/pct-label marginal-rate)))))))

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

(def ^:private upcoming-type-dot
  {:bill "bg-rose-400" :income "bg-emerald-400" :todo "bg-violet-400" :general "bg-violet-400"})

(defn upcoming-summary
  "Dashboard widget for the upcoming surface: the next payday and the next
   few scheduled events, colour-coded by type."
  [payday events]
  (let [today     (LocalDate/now)
        pd        (u.time/next-payday today payday)
        pd-until  (when pd (u.time/days-until today pd))
        ahead     (->> events
                       (filter (fn [{:event/keys [date done]}]
                                 (when (and date (not done))
                                   (let [d (LocalDate/parse date)]
                                     (not (neg? (u.time/days-until today d)))))))
                       (take 3))
        n-ahead   (count ahead)]
    (if (or pd (seq ahead))
      (summary-card
       :label "Upcoming"
       :icon (svgs/calendar)
       :icon-cls "bg-sky-50 text-sky-600"
       :href "/app/calendar"
       :value (if pd (u.time/relative-date pd-until) "—")
       :reading (str (if pd (str "Salary · " (u.time/format-date pd)) "No payday set")
                     (when (pos? n-ahead)
                       (str " · " n-ahead " event" (when (not= 1 n-ahead) "s") " ahead")))
       :reveal "reveal"
       :children
       [:div {:class "divide-y divide-zinc-100"}
        (when pd
          [:div {:class "flex items-center gap-2.5 py-2.5"}
           [:span {:class "h-2 w-2 flex-shrink-0 rounded-full bg-emerald-400"}]
           [:div {:class "min-w-0 flex-1"}
            [:p {:class "text-xs text-zinc-500"} "Payday"]]
           [:span {:class "text-xs font-semibold text-emerald-600 tabular-nums"} (u.time/format-date pd)]])
        (for [{:event/keys [title date type]} ahead]
          (let [d (LocalDate/parse date)]
            [:div {:class "flex items-center gap-2.5 py-2.5"}
             [:span {:class (str "h-2 w-2 flex-shrink-0 rounded-full "
                                 (get upcoming-type-dot (or type :general) "bg-violet-400"))}]
             [:div {:class "min-w-0 flex-1"}
              [:p {:class "truncate text-xs font-medium text-zinc-700"} title]]
             [:span {:class "flex-shrink-0 text-xs text-zinc-400 tabular-nums"}
              (u.time/relative-date (u.time/days-until today d))]]))])
      (summary-card
       :label "Upcoming"
       :icon (svgs/calendar)
       :icon-cls "bg-sky-50 text-sky-600"
       :href "/app/calendar"
       :value "—"
       :reveal "reveal"
       :reading "Add bills and paydays to see what's next."))))

(defn goals-summary
  "Dashboard widget for the goals surface: total saved toward the overall
   target, with progress on the nearest goals."
  [goals]
  (let [total-saved  (reduce + (map (fn [{:goal/keys [saved]}] (or saved 0)) goals))
        total-target (reduce + (map (fn [{:goal/keys [target]}] (or target 0)) goals))
        banked       (when (pos? total-target)
                       (int (Math/round (* 100.0 (/ (double total-saved) (double total-target))))))]
    (if (seq goals)
      (summary-card
       :label "Goals"
       :icon (svgs/target)
       :icon-cls "bg-amber-50 text-amber-600"
       :href "/app/goals"
       :value (utilities/whole->rands total-saved)
       :reading (if banked
                  (str "of " (utilities/whole->rands total-target) " target · " (utilities/pct-label banked) " banked")
                  (str "across " (count goals) " goal" (when (not= 1 (count goals)) "s")))
       :reveal "reveal reveal-3"
       :children
       [:div {:class "divide-y divide-zinc-100"}
        (for [{:goal/keys [title saved target]} (take 2 goals)]
          (let [p         (utilities/goal-pct saved target)
                complete? (>= (or saved 0) (or target 0))]
            [:div {:class "py-2.5"}
             [:div {:class "flex items-center justify-between gap-3"}
              [:p {:class "min-w-0 truncate text-xs font-medium text-zinc-700"} title],
              [:span {:class "flex-shrink-0 text-xs text-zinc-400 tabular-nums"}
               (str (utilities/whole->rands (or saved 0)) " / " (utilities/whole->rands (or target 0)))]],
             [:div {:class "mt-1.5 flex h-1.5 w-full overflow-hidden rounded-full bg-zinc-100"}
              [:div {:class (str "h-full rounded-full " (if complete? "bg-emerald-500" "bg-emerald-400"))
                     :style {:width (str p "%")}}]]]))])
      (summary-card
       :label "Goals"
       :icon (svgs/target)
       :icon-cls "bg-amber-50 text-amber-600"
       :href "/app/goals"
       :value "—"
       :reveal "reveal reveal-3"
       :reading "Set a target and watch your savings grow."))))

;; ---------------------------------------------------------------------------
;; Dashboard charts
;;
;; The dashboard's visual spine: server-rendered SVG, no charting library —
;; just Clojure computing arcs and Tailwind colouring them, so the page renders
;; in one round-trip. Bar fills already animate via .greed-bar-grow; the rings
;; and donut present statically (calm, Apple-style) and inherit the card
;; reveal fade-up. Fractions are relative to income and never re-normalised,
;; so an overspend visibly overflows the ring instead of hiding behind a
;; clipped bar.
;; ---------------------------------------------------------------------------

(defn- ring-progress
  "SVG progress ring: a hairline track with a rounded-cap progress arc that
   starts at twelve o'clock and sweeps clockwise. pct is 0–100."
  [pct & {:keys [size stroke track-cls arc-cls]}]
  (let [size   (or size 132)
        stroke (or stroke 12)
        r      (/ (- size stroke) 2)
        c      (* 2 Math/PI r)
        p      (max 0.0 (min 100.0 (double (or pct 0))))
        dash   (* c p 0.01)
        cx     (/ size 2)
        cy     (/ size 2)]
    [:svg {:viewBox (str "0 0 " size " " size)
           :aria-hidden "true"
           :class "block h-auto w-full"}
     [:g {:transform (str "rotate(-90 " cx " " cy ")")}
      [:circle {:cx cx :cy cy :r r
                :fill "none"
                :class (or track-cls "text-zinc-100")
                :stroke "currentColor"
                :stroke-width stroke}]
      (when (pos? p)
        [:circle {:cx cx :cy cy :r r
                  :fill "none"
                  :class (or arc-cls "text-emerald-500")
                  :stroke "currentColor"
                  :stroke-width stroke
                  :stroke-linecap "round"
                  :stroke-dasharray (str (utilities/fmt-d "%.2f" dash) " " c)}])]]))

(defn- donut-chart
  "Segmented donut. `segments` is a seq of {:frac <percent, 0–100+> :cls <tailwind
   colour>}. Segments stack clockwise from twelve o'clock with rounded caps and
   a small breathing gap. Fractions are relative to income and never
   re-normalised, so an overspend (expenses + savings > 100) visibly overflows
   the ring. Renders only the track when there are no segments."
  [segments & {:keys [size stroke track-cls]}]
  (let [size   (or size 184)
        stroke (or stroke 16)
        r      (/ (- size stroke) 2)
        c      (* 2 Math/PI r)
        cx     (/ size 2)
        cy     (/ size 2)
        segs   (filterv #(pos? (double (or (:frac %) 0))) segments)
        k      (count segs)
        gap    (+ stroke 4)
        avail  (max 0.0 (- c (* k gap)))
        arcs   (loop [i 0, start 0.0, acc []]
                 (if (>= i k)
                   acc
                   (let [{:keys [cls frac]} (nth segs i)
                         ;; Divide by a fixed 100, not the segments' own sum —
                         ;; that would re-normalise an overspend back down to
                         ;; a full circle. Dividing by 100 lets the segments'
                         ;; combined length exceed `avail`, so the dasharray
                         ;; wraps past twelve o'clock and visibly overlaps
                         ;; itself, which is the overflow cue the ring is for.
                         d (* avail (/ (double frac) 100.0))]
                     (recur (inc i)
                            (+ start d gap)
                            (conj acc
                                  [:circle {:cx cx :cy cy :r r
                                            :fill "none"
                                            :class cls
                                            :stroke "currentColor"
                                            :stroke-width stroke
                                            :stroke-linecap "round"
                                            :stroke-dasharray (str (utilities/fmt-d "%.2f" d) " " c)
                                            :stroke-dashoffset (utilities/fmt-d "%.2f" (- start))}])))))]
    [:svg {:viewBox (str "0 0 " size " " size)
           :aria-hidden "true"
           :class "block h-auto w-full"}
     (into [:g {:transform (str "rotate(-90 " cx " " cy ")")}]
           (cond-> [(when track-cls
                      [:circle {:cx cx :cy cy :r r
                                :fill "none"
                                :class track-cls
                                :stroke "currentColor"
                                :stroke-width stroke}])]
             true (into arcs)))]))

(defn- chart-card
  "Chart panel chrome: title, one-line subtitle, optional trailing link, and the
   chart body. Same white card language as the summary cards, but taller so the
   chart breathes. Stretches to fill its grid cell (h-full)."
  [title & {:keys [subtitle href link-label reveal children]}]
  [:div {:class (str "relative flex h-full flex-col overflow-hidden rounded-2xl bg-white ring-1 ring-zinc-200/70 shadow-card p-6 "
                     (or reveal "reveal"))}
   [:div {:class "pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-emerald-500/40 to-transparent"}]
   [:div {:class "flex items-start justify-between gap-3"}
    [:div {:class "min-w-0"}
     [:h3 {:class "text-sm font-semibold text-zinc-900 tracking-tight"} title]
     (when subtitle
       [:p {:class "mt-0.5 text-xs text-zinc-400 leading-relaxed"} subtitle])]
    (when href
      [:a {:href href
           :class "group -my-1.5 -mx-2 inline-flex items-center gap-1 rounded-md px-2 py-2 text-xs font-medium text-emerald-600 transition hover:text-emerald-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/70 focus-visible:ring-offset-2 active:text-emerald-800 active:scale-[0.97]"}
       link-label
       (svgs/->next {:class "size-3.5 -translate-x-0.5 transition-transform group-hover:translate-x-0"})])]
   [:div {:class "mt-5 flex flex-1 flex-col"} children]])

(defn- chart-empty
  "Compact empty state inside a chart card: icon chip, one line, and a primary
   action pointing at the feature that fills the chart."
  [& {:keys [icon title body href btn-label]}]
  [:div {:class "flex h-full flex-col items-center justify-center px-4 py-10 text-center"}
   [:div {:class "mb-3 flex h-11 w-11 items-center justify-center rounded-full bg-emerald-50"}
    [:span {:class "text-emerald-500"} icon]]
   [:p {:class "text-sm font-medium text-zinc-500"} title]
   [:p {:class "mt-1 max-w-xs text-xs text-zinc-400 leading-relaxed"} body]
   (shared/btn :variant :outline :size :md :class "mt-5" :href href btn-label)])

(defn- legend-row
  "One dot/label/amount/pct row beneath a chart."
  [dot-cls label amount pct & [amount-cls]]
  [:div {:class "flex items-center gap-3"}
   [:span {:class (str "flex-shrink-0 h-2.5 w-2.5 rounded-full " dot-cls)}]
   [:span {:class "min-w-0 flex-1 text-sm text-zinc-600 truncate"} label]
   [:span {:class (str "flex-shrink-0 text-sm font-semibold tabular-nums " (or amount-cls "text-zinc-900"))}
    (utilities/whole->rands amount)]
   [:span {:class "flex-shrink-0 w-10 text-right text-xs text-zinc-400 tabular-nums"} pct]])

(defn cashflow-donut
  "Dashboard chart: this month's income split as a donut — expenses, savings
   and what's left to plan — with the leftover amount at the centre. An
   overspend overflows the ring (expenses and savings exceed a full circle)
   and flips the centre to a warning, echoing the insights page's honesty."
  [budget-items]
  (let [{:keys [total-income total-expenses total-savings]} (c.data/get-budget-data budget-items)
        income     (or total-income 0)
        expenses   (or total-expenses 0)
        savings    (or total-savings 0)
        leftover   (- income expenses savings)
        overspend? (neg? leftover)
        exp-pct    (utilities/pct-share expenses income)
        sav-pct    (utilities/pct-share savings income)
        rem-pct    (utilities/pct-share (max 0 leftover) income)
        segs       (cond-> [{:frac exp-pct :cls "text-rose-400"}
                            {:frac sav-pct :cls "text-indigo-400"}]
                     (not overspend?) (conj {:frac rem-pct :cls "text-zinc-300"}))]
    (chart-card
     "Where your money goes"
     :subtitle "This month's income, split by plan."
     :href "/app/insights" :link-label "Insights"
     :children
     (if (zero? income)
       (chart-empty
        :icon (svgs/wallet)
        :title "No income yet"
        :body "Add your income in Finances to see this month's split at a glance."
        :href "/app/finances"
        :btn-label "Add your income")
       [:<>
        [:div {:class "grid grid-cols-1 items-center gap-6 sm:grid-cols-2"}
         [:div {:class "relative mx-auto w-full max-w-[190px]"}
          (donut-chart segs :track-cls "text-zinc-100")
          [:div {:class "absolute inset-0 flex flex-col items-center justify-center text-center"}
           [:p {:class (str "text-2xl font-bold leading-none tracking-tight tabular-nums "
                            (if overspend? "text-rose-600" "text-zinc-900"))}
            (utilities/whole->rands (Math/abs (double leftover)))]
           [:p {:class "mt-1 text-[11px] font-semibold uppercase tracking-wider text-zinc-400"}
            (if overspend? "over budget" "left to plan")]]]
         [:div {:class "w-full"}
          (when overspend?
            [:div {:class "mb-4 rounded-lg bg-rose-50 px-3 py-2 text-xs leading-relaxed text-rose-700"}
             (str "Expenses and savings are " (utilities/whole->rands (Math/round (double (Math/abs leftover))))
                  " over your income this month.")])
          [:div {:class "space-y-3"}
           (legend-row "bg-rose-400" "Expenses" expenses (utilities/pct-label exp-pct))
           (legend-row "bg-indigo-400" "Savings" savings (utilities/pct-label sav-pct))
           (if overspend?
             (legend-row "bg-zinc-300" "Overspend" (Math/abs (long leftover)) "—" "text-rose-600")
             (legend-row "bg-zinc-300" "Left to plan" (max 0 leftover) (utilities/pct-label rem-pct)))]]]]))))

(defn savings-ring
  "Dashboard chart: the month's savings rate on a ring against the 20%
   benchmark, with how much more to save this month to reach it."
  [budget-items]
  (let [{:keys [total-income total-savings]} (c.data/get-budget-data budget-items)
        income     (or total-income 0)
        savings    (or total-savings 0)
        rate       (double (or (utilities/pct-share savings income) 0))
        rate-num   (int (Math/round rate))
        on-target? (>= rate-num 20)
        shortfall  (* 0.01 (- 20.0 rate))]
    (chart-card
     "Savings rate"
     :subtitle "Against a healthy 20% benchmark."
     :reveal "reveal reveal-2"
     :children
     (if (zero? income)
       (chart-empty
        :icon (svgs/trending-up)
        :title "No income yet"
        :body "Add your income in Finances to see your savings rate on the ring."
        :href "/app/finances"
        :btn-label "Add your income")
       [:<>
        [:div {:class "flex flex-col items-center"}
         [:div {:class "relative w-full max-w-[160px]"}
          (ring-progress rate
                         :size 160 :stroke 16
                         :track-cls "text-zinc-100"
                         :arc-cls (cond on-target? "text-emerald-500"
                                        (pos? rate-num) "text-amber-500"
                                        :else "text-zinc-300"))
          [:div {:class "absolute inset-0 flex flex-col items-center justify-center text-center"}
           [:p {:class (str "text-3xl font-bold leading-none tracking-tight tabular-nums "
                            (cond on-target? "text-emerald-600"
                                  (pos? rate-num) "text-amber-600"
                                  :else "text-zinc-900"))}
            (utilities/pct-label rate)]
           [:p {:class "mt-1 text-[11px] font-semibold uppercase tracking-wider text-zinc-400"}
            "of income saved"]]]
         [:div {:class "mt-5 w-full border-t border-zinc-100 pt-4"}
          (cond
            on-target?
            [:p {:class "text-xs leading-relaxed text-emerald-600"}
             "Past the 20% benchmark — savings are compounding nicely."]
            (pos? rate-num)
            [:p {:class "text-xs leading-relaxed text-zinc-500"}
             (str "Save " (utilities/whole->rands (Math/round (* shortfall income)))
                  " more this month to reach the 20% benchmark.")]
            :else
            [:p {:class "text-xs leading-relaxed text-zinc-500"}
             "Nothing set aside yet — every contribution compounds."])
          [:div {:class "mt-2.5 flex items-center justify-between"}
           [:p {:class "text-xs text-zinc-400"} "Saved this month"]
           [:p {:class "text-xs font-semibold text-zinc-900 tabular-nums"}
            (utilities/whole->rands savings)]]]]]))))

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
   payday pill) sits right-aligned on the same row."
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
  [:div {:class "mt-4 flex items-baseline gap-2.5 sm:mt-5"}
   [:p {:class "text-4xl sm:text-5xl font-bold text-zinc-900 leading-none tracking-[-0.04em] tabular-nums"}
    value]
   [:span {:class "text-sm font-medium text-zinc-400"} suffix]])

(defn- hero-status
  "One-line reading of the headline, below it. Tone defaults to zinc; pass an
   explicit tone for warnings (e.g. an overspend)."
  [content & {:keys [tone]}]
  [:p {:class (str "mt-2 text-sm " (or tone "text-zinc-500"))} content])

(defn- hero-stats-row
  "Three supporting figures under the headline, split by a hairline rule."
  [& substats]
  (into [:div {:class "grid grid-cols-3 gap-4 pt-4 mt-4 border-t border-zinc-100"}]
        substats))

(defn- hero-substat
  "One supporting figure: tiny uppercase label over a medium value."
  [label value & [value-cls]]
  [:div {:class "min-w-0"}
   [:p {:class "text-[11px] font-medium text-zinc-500 uppercase tracking-wider whitespace-nowrap"} label]
   [:p {:class (str "mt-1 text-sm font-semibold whitespace-nowrap tabular-nums sm:text-lg "
                    (or value-cls "text-zinc-900"))} value]])

(defn dashboard-hero
  "Dashboard feature card leading with the person's month, not their tax
   bracket — what's left to plan, framed by savings rate, income and payday.
   A pay-period ring reads how far through the month you are at a glance. The
   tax breakdown has its own section further down the page."
  [budget-items payday]
  (let [{:keys [total-income total-expenses total-savings]} (c.data/get-budget-data budget-items)
        income        (or total-income 0)
        leftover      (- income (or total-expenses 0) (or total-savings 0))
        overspend?    (neg? leftover)
        savings-rate  (utilities/pct-share total-savings income)
        today         (LocalDate/now)
        pd            (u.time/next-payday today payday)
        prev-pd       (u.time/prev-payday today payday)
        pd-until      (when pd (u.time/days-until today pd))
        period-len    (when (and pd prev-pd)
                        (inc (long (.between ChronoUnit/DAYS prev-pd pd))))
        elapsed       (when (and pd prev-pd)
                        (inc (long (.between ChronoUnit/DAYS prev-pd today))))
        period-pct    (when (and pd prev-pd)
                        (min 100.0 (max 0.0 (* 100.0 (/ (double elapsed) (double period-len))))))]
    (if (zero? income)
      (hero-panel
       {:class "h-full" :inner-class "flex flex-col items-center justify-center h-full px-6 py-12 text-center"}
       [:div {:class "mb-3 flex items-center justify-center w-12 h-12 rounded-full bg-emerald-50"}
        [:span {:class "text-emerald-500"} (svgs/wallet)]]
       [:p {:class "text-sm font-medium text-zinc-500"} "No income yet"]
       [:p {:class "mt-1 text-xs text-zinc-400"} "Add your income in Finances to see your month at a glance."]
       (shared/btn :variant :primary :size :md :class "mt-5" :href "/app/finances" "Add your income"))
      (hero-panel
       {:class "h-full" :inner-class "flex flex-col h-full"}
       [:div {:class "grid grid-cols-1 items-center gap-8 lg:grid-cols-[minmax(0,1fr)_auto]"}
        [:div
         (hero-eyebrow "Your month")
         (hero-headline (utilities/whole->rands (Math/abs (double leftover))) (if overspend? "over budget" "left to plan"))
         (hero-status (if overspend?
                        "Spending is ahead of income this month — worth a look."
                        "On track — spending is under income so far.")
                      :tone (when overspend? "text-rose-600"))]
        (when (and pd prev-pd)
          [:div {:class "relative w-28"}
           (ring-progress period-pct
                          :size 112 :stroke 10
                          :track-cls "text-zinc-100"
                          :arc-cls (if overspend? "text-rose-400" "text-emerald-500"))
           [:div {:class "absolute inset-0 flex flex-col items-center justify-center text-center"}
            [:p {:class (str "text-xl font-bold leading-none tracking-tight tabular-nums "
                             (if overspend? "text-rose-600" "text-zinc-900"))}
             (str "Day " elapsed)]
            [:p {:class "mt-0.5 text-[10px] font-semibold uppercase tracking-wider text-zinc-400"}
             (str "of " period-len)]]])]
       [:div {:class "flex-1"}]
       (hero-stats-row
        (hero-substat "Savings rate" (utilities/pct-label savings-rate))
        (hero-substat "Income" (utilities/whole->rands income) "text-emerald-600")
        (hero-substat "Payday" (cond (nil? pd) "—"
                                     (zero? pd-until) "Today"
                                     :else (str "in " pd-until "d"))))))))

(defn goals-hero
  "Goals feature card leading with how much of the overall goal total is funded."
  [goals]
  (let [saved-total  (reduce + (map #(or (:goal/saved %) 0) goals))
        target-total (reduce + (map #(or (:goal/target %) 0) goals))
        remaining    (max 0 (- target-total saved-total))
        p            (utilities/goal-pct saved-total target-total)
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
     (hero-headline (if (pos? total-income) (utilities/pct-label savings-rate) "—") "of income saved")
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
        pd            (u.time/next-payday today payday)
        pd-until      (when pd (u.time/days-until today pd))
        upcoming      (->> events
                           (filter (fn [{:event/keys [date]}]
                                     (when date
                                       (not (neg? (u.time/days-until today (LocalDate/parse date))))))))
        upcoming-count (count upcoming)
        bills         (count (filter #(= (or (:event/type %) :general) :bill) upcoming))
        income        (count (filter #(= (or (:event/type %) :general) :income) upcoming))
        overdue-todos (count (filter (fn [{:event/keys [date done] :as event}]
                                       (and (= (or (:event/type event) :general) :todo)
                                            (not done)
                                            date
                                            (neg? (u.time/days-until today (LocalDate/parse date)))))
                                     events))
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
                        (str "Salary lands " (u.time/format-date pd) " — "
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
      (hero-substat "Overdue" (str overdue-todos) (when (pos? overdue-todos) "text-rose-600"))))))
