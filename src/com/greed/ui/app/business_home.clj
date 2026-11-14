(ns com.greed.ui.app.business-home
  "A business's home (/app while a business is chosen): this month at a
   glance from its bank statements against its budget, the plan, what is
   due to SARS next, money in and out over recent months, where the money
   went, and quick actions."
  (:require [clojure.string :as str]
            [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.statements.categorise :as categorise]
            [com.greed.ui.app.statements :as spending]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.stats :as stats]
            [com.greed.utilities.company-tax :as ct]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.vat :as vat])
  (:import [java.time LocalDate LocalTime YearMonth]
           [java.time.format DateTimeFormatter]
           [java.time.temporal ChronoUnit]))

;; ---------------------------------------------------------------------------
;; Helpers

(defn- whole [cents] (utilities/whole->rands (/ cents 100.0)))
(defn- rands [r] (utilities/whole->rands r))
(defn- pct [part total] (if (pos? total) (min 100 (Math/round (* 100.0 (/ (double part) total)))) 0))
(defn- month-name [m pattern] (.format (YearMonth/parse m) (DateTimeFormatter/ofPattern pattern)))
(defn- fmt [^LocalDate d] (.format d (DateTimeFormatter/ofPattern "d MMM yyyy")))

(def ^:private card "rounded-lg bg-white ring-1 ring-zinc-200/70")

(defn- month-of [{:statement-summary/keys [month period-end]}]
  (or month (subs period-end 0 7)))

(defn- whole-month? [{:statement-summary/keys [period-start period-end] :as s}]
  (let [ym (YearMonth/parse (month-of s))]
    (and (= (str (.atDay ym 1)) period-start) (= (str (.atEndOfMonth ym)) period-end))))

(defn- greeting []
  (let [h (.getHour (LocalTime/now))]
    (cond (< h 12) "Good morning" (< h 18) "Good afternoon" :else "Good evening")))

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

(defn coming-up
  "What is due to SARS next, from the budget: the current VAT period's
   VAT201 for a VAT vendor, and a (Pty) Ltd's next provisional tax payment.
   [{:what :due :amount :note}], soonest first; amounts in cents."
  [{:business/keys [entity-type vat-registered vat-category year-end-month sbc]} items ^LocalDate today]
  (sort-by :due
           (concat
            (when (and vat-registered vat-category)
              (let [p (vat/period vat-category today)
                    {:keys [net]} (vat/budget-estimate items (:months p))]
                [{:what "VAT201" :due (:due p) :amount net
                  :note (str "VAT for " (.format ^LocalDate (:start p) (DateTimeFormatter/ofPattern "MMM"))
                             (when (> (:months p) 1) (str " to " (.format ^LocalDate (:end p) (DateTimeFormatter/ofPattern "MMM")))))}]))
            (when (= :pty-ltd entity-type)
              (let [fy    (ct/financial-year (or year-end-month 2) today)
                    tax   (ct/tax (/ (ct/budget-taxable items vat-registered) 100.0) sbc (:end fy))
                    half  (Math/round (/ tax 2.0))
                    {first-due :first second-due :second} (ct/provisional-dates fy)
                    upcoming (cond (not (.isBefore ^LocalDate first-due today)) {:due first-due :amount (* 100 half) :n "First"}
                                   (not (.isBefore ^LocalDate second-due today)) {:due second-due :amount (* 100 (- tax half)) :n "Second"})]
                (when upcoming
                  [{:what (str (:n upcoming) " provisional tax payment") :due (:due upcoming) :amount (:amount upcoming)
                    :note "Company income tax for the year, from the budget"}]))))))

;; ---------------------------------------------------------------------------
;; Sections

(defn- heading [user {:business/keys [name entity-type]}]
  [:div {:class "flex items-end justify-between gap-4"}
   [:div {:class "min-w-0"}
    [:h1 {:class "truncate text-2xl font-semibold tracking-tight text-zinc-900"} name]
    [:p {:class "mt-0.5 text-sm text-zinc-500"}
     (str (greeting) ", " (str/capitalize (str (:user/firstname user))) " · " (data/entity-types entity-type))]]
   [:p {:class "hidden whitespace-nowrap text-sm font-medium tabular-nums text-zinc-500 sm:block"}
    (.format (LocalDate/now) (DateTimeFormatter/ofPattern "EEEE, d MMMM"))]])

(defn- stat [label value & [sub cls]]
  [:div {:class "min-w-0"}
   [:p {:class "text-xs font-medium text-zinc-500"} label]
   [:p {:class (str "mt-1 text-lg font-semibold tabular-nums " (or cls "text-zinc-900"))} value]
   (when sub [:p {:class "mt-0.5 truncate text-xs text-zinc-500"} sub])])

(defn- hero
  "This month: spent against the plan, with money in, out and the net."
  [{:keys [month whole? summaries]} items-by-id planned-expenses]
  (let [{:keys [spent planned over linked? budgeted unplanned]}
        (spending/compare-with-budget summaries items-by-id planned-expenses)
        in   (reduce + 0 (map :statement-summary/money-in-cents summaries))
        net  (- in spent)
        top  (max 1 planned spent)
        over? (and whole? (pos? planned) (pos? over))]
    [:section {:class (str card " px-6 py-6 sm:px-8 sm:py-7") :aria-labelledby "month-h"}
     [:div {:class "flex flex-wrap items-center gap-2"}
      [:h2 {:id "month-h" :class "text-sm font-medium text-zinc-500"} (month-name month "MMMM yyyy")]
      (when-not whole?
        [:span {:class "rounded-full bg-zinc-100 px-2 py-0.5 text-xs font-medium text-zinc-600"} "Part of the month"])]
     [:div {:class "mt-3 grid grid-cols-1 gap-8 lg:grid-cols-5"}
      [:div {:class "lg:col-span-3"}
       [:p {:class "flex flex-wrap items-baseline gap-x-3"}
        [:span {:class "text-4xl font-semibold tracking-[-0.03em] tabular-nums text-zinc-900"} (whole spent)]
        [:span {:class "text-sm text-zinc-500"} "spent"]]
       [:p {:class (str "mt-1 text-sm " (if over? "text-rose-700" "text-zinc-600"))}
        (cond
          (not (pos? planned)) "Add the business's costs to its budget to compare this month with a plan."
          (not whole?) "Only part of this month is on the statements, so it is not compared with the plan yet."
          linked? (str (whole (Math/abs (long over))) (if over? " more" " less")
                       " than the budget leaves for costs outside its linked items.")
          :else (str (whole (Math/abs (long over))) (if over? " more" " less") " than the "
                     (whole planned) " the budget plans for expenses."))]
       ;; planned against spent
       [:div {:class "mt-6 space-y-3"}
        (for [[label cents cls] [["Planned" planned "bg-zinc-300"]
                                 ["Spent" spent (if over? "bg-rose-500" "bg-brand-600")]]]
          [:div {:class "grid grid-cols-[4.5rem_minmax(0,1fr)_auto] items-center gap-3 text-sm"}
           [:span {:class "text-zinc-600"} label]
           [:span {:class "h-2.5 overflow-hidden rounded-full bg-zinc-100"}
            [:span {:class (str "block h-full rounded-full " cls) :style {:width (str (max 2 (pct cents top)) "%")}}]]
           [:span {:class "w-24 text-right font-semibold tabular-nums text-zinc-900"} (whole cents)]])
        (when linked?
          [:p {:class "text-xs text-zinc-500"}
           (str (whole budgeted) " went to linked budget items, " (whole unplanned) " to everything else.")])]]
      [:div {:class "grid grid-cols-2 gap-x-6 gap-y-5 border-t border-zinc-100 pt-5 lg:col-span-2 lg:border-l lg:border-t-0 lg:pl-8 lg:pt-0"}
       (stat "Money in" (whole in) "own transfers left out")
       (stat "Money out" (whole spent) "own transfers left out")
       (stat "In minus out" (str (when (neg? net) "−") (whole (Math/abs (long net))))
             (if (neg? net) "more went out than came in" "kept in the business")
             (when (neg? net) "text-rose-700"))
       (stat "Statements" (str (count summaries)) (str/join ", " (distinct (map #(case (:statement-summary/bank %) :fnb "FNB" :standard-bank "Standard Bank" "bank") summaries))))]]
     [:div {:class "mt-6 flex justify-end"}
      [:a {:href (str "/app/spending?month=" month)
           :class "inline-flex items-center gap-1 rounded-md text-sm font-medium text-zinc-900 underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"}
       (str "See " (month-name month "MMMM") " in Spending") [:span {:aria-hidden "true"} "→"]]]]))

(defn- hero-empty
  "No statements yet: the plan in brief and the way in."
  [{:keys [total-income total-expenses total-savings]}]
  (let [left (- total-income total-expenses total-savings)]
    [:section {:class (str card " px-6 py-8 sm:px-8")}
     [:h2 {:class "text-sm font-medium text-zinc-500"} "This month"]
     [:p {:class "mt-3 max-w-[36ch] text-2xl font-semibold tracking-[-0.02em] text-zinc-900"}
      "See where the business's money actually goes."]
     [:p {:class "mt-2 max-w-[60ch] text-sm text-zinc-600"}
      (if (pos? total-income)
        (str "The budget plans " (rands total-income) " in and " (rands (+ total-expenses total-savings))
             " out each month, leaving " (rands left) ". Upload a bank statement to compare it with what happened.")
        "Plan the month in Budget, then upload a bank statement to compare it with what happened.")]
     [:div {:class "mt-5 flex flex-wrap gap-3"}
      (shared/btn :variant :dark :size :md :href "/app/spending" "Upload a statement")
      (shared/btn :variant :outline :size :md :href "/app/finances/" "Open Budget")]]))

(defn- plan-card [{:keys [total-income total-expenses total-savings]}]
  (let [left (- total-income total-expenses total-savings)
        rows [["Income" total-income "bg-brand-600"]
              ["Expenses" total-expenses "bg-zinc-700"]
              ["Set aside" total-savings "bg-zinc-400"]]]
    [:section {:class (str card " flex h-full flex-col p-6")}
     [:h3 {:class "text-sm font-semibold tracking-tight text-zinc-900"} "The monthly plan"]
     [:p {:class "mt-0.5 text-xs text-zinc-500"} "From the business's budget."]
     (if (pos? (+ total-income total-expenses total-savings))
       [:<>
        [:div {:class "mt-5 space-y-3"}
         (for [[label r cls] rows]
           [:div {:class "grid grid-cols-[5rem_minmax(0,1fr)_auto] items-center gap-3 text-sm"}
            [:span {:class "text-zinc-600"} label]
            [:span {:class "h-1.5 overflow-hidden rounded-full bg-zinc-100"}
             [:span {:class (str "block h-full rounded-full " cls)
                     :style {:width (str (max 2 (pct r (max 1 total-income total-expenses))) "%")}}]]
            [:span {:class "w-24 text-right font-semibold tabular-nums text-zinc-900"} (rands r)]])]
        [:p {:class "mt-auto flex items-baseline justify-between border-t border-zinc-100 pt-4 text-sm"}
         [:span {:class "text-zinc-600"} (if (neg? left) "Planned beyond income" "Left each month")]
         [:span {:class (str "text-lg font-semibold tabular-nums " (if (neg? left) "text-rose-700" "text-zinc-900"))} (rands left)]]]
       [:p {:class "mt-5 text-sm text-zinc-600"} "No budget yet. Add the business's income and costs in Budget."])]))

(defn- due-card [rows {:business/keys [entity-type vat-registered]}]
  (let [today (LocalDate/now)]
    [:section {:class (str card " flex h-full flex-col p-6")}
     [:h3 {:class "text-sm font-semibold tracking-tight text-zinc-900"} "Coming up"]
     [:p {:class "mt-0.5 text-xs text-zinc-500"} "What is due to SARS next, estimated from the budget."]
     (if (seq rows)
       [:ul {:class "mt-4 divide-y divide-zinc-100"}
        (for [{:keys [what due amount note]} rows
              :let [days (.between ChronoUnit/DAYS today due)]]
          [:li {:class "flex items-start gap-4 py-3"}
           [:div {:class "flex w-12 flex-shrink-0 flex-col items-center rounded-md bg-zinc-100 py-1.5 text-center"}
            [:span {:class "text-[11px] font-medium leading-none text-zinc-500"} (.format ^LocalDate due (DateTimeFormatter/ofPattern "MMM"))]
            [:span {:class "mt-0.5 text-lg font-semibold leading-none tabular-nums text-zinc-900"} (.getDayOfMonth ^LocalDate due)]]
           [:div {:class "min-w-0 flex-1"}
            [:p {:class "text-sm font-medium text-zinc-900"} what]
            [:p {:class "truncate text-xs text-zinc-500"} (str note " · " (if (zero? days) "due today" (str "in " days " days")))]]
           [:p {:class "flex-shrink-0 text-sm font-semibold tabular-nums text-zinc-900"}
            (cond (pos? amount) (whole amount) (neg? amount) (str (whole (- amount)) " back") :else "R0")]])]
       [:p {:class "mt-4 text-sm text-zinc-600"}
        (case entity-type
          (:sole-proprietor :partnership) "The profit is taxed in the owners' personal returns; see your personal Tax page."
          :pty-ltd "Set the financial year in Business settings to see provisional tax dates."
          "Nothing to show yet.")])
     [:div {:class "mt-auto flex flex-wrap gap-x-5 gap-y-2 pt-4 text-sm"}
      (when vat-registered [:a {:href "/app/vat" :class "font-medium text-zinc-900 underline-offset-4 hover:underline"} "VAT"])
      [:a {:href "/app/company-tax" :class "font-medium text-zinc-900 underline-offset-4 hover:underline"} "Tax"]]]))

(defn- flow-card
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
            [:span {:class "block w-1/2 max-w-[18px] rounded-t-sm bg-brand-600" :style {:height (str (max 2 (pct in top)) "%")}}]
            [:span {:class "block w-1/2 max-w-[18px] rounded-t-sm bg-zinc-400" :style {:height (str (max 2 (pct out top)) "%")}}]]
           [:span {:class "mt-2 text-center text-xs text-zinc-500"} (month-name month "MMM")]])]
       [:p {:class "mt-5 text-sm text-zinc-600"} "Upload the business's statements to see its months side by side."])]))

(defn- costs-card
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
            [:span {:class "block h-full rounded-full bg-brand-600" :style {:width (str (max 2 (pct cents top)) "%")}}]]])]
       [:p {:class "mt-5 text-sm text-zinc-600"} "Nothing spent on the statements this month."])]))

(defn- actions [owner?]
  (let [action (fn [href title sub]
                 [:a {:href href :class (str card " group flex items-center justify-between gap-4 p-5 transition hover:bg-zinc-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400")}
                  [:span {:class "min-w-0"}
                   [:span {:class "block text-sm font-semibold text-zinc-900"} title]
                   [:span {:class "block truncate text-xs text-zinc-500"} sub]]
                  [:span {:aria-hidden "true" :class "text-zinc-400 transition-transform group-hover:translate-x-1"} "→"]])]
    [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-3"}
     (action "/app/spending" "Upload a statement" "Read a bank statement PDF")
     (action "/app/finances/" "Update the budget" "Income, costs and set-aside")
     (if owner?
       (action "/app/business" "Invite someone" "A co-founder or bookkeeper")
       (action "/app/choose" "Switch profile" "Personal or another business"))]))

;; ---------------------------------------------------------------------------
;; Page

(defn page
  "GET /app while a business is chosen."
  [{:keys [session params] :as ctx} {:xt/keys [id] :as business}]
  (let [user        (data/get-user ctx (:uid session))
        items       (data/get-business-budget-items ctx id)
        items-by-id (into {} (map (juxt :xt/id identity)) items)
        plan        (data/get-budget-data items)
        summaries   (data/get-statement-summaries ctx {:business-id id})
        month       (latest-month summaries)
        owner?      (= :owner (:business-member/role business))]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (when (:alert params) (alerts/info params))
      (heading user business)
      (if month
        (hero month items-by-id (:total-expenses plan))
        (hero-empty plan))
      [:div
       (stats/section-header "The plan and what's next" :href "/app/finances/" :link-label "Open Budget")
       [:div {:class "grid grid-cols-1 items-stretch gap-4 lg:grid-cols-2"}
        (plan-card plan)
        (due-card (coming-up business items (LocalDate/now)) business)]]
      (when month
        [:div
         (stats/section-header "From the bank statements" :href "/app/spending" :link-label "Open Spending")
         [:div {:class "grid grid-cols-1 items-stretch gap-4 lg:grid-cols-5"}
          [:div {:class "lg:col-span-3"} (flow-card (monthly-flow summaries 6))]
          [:div {:class "lg:col-span-2"} (costs-card (:summaries month))]]])
      [:div
       (stats/section-header "Quick actions")
       (actions owner?)]])))
