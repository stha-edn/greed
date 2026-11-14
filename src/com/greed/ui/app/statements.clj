(ns com.greed.ui.app.statements
  "The Spending tab: upload a bank statement, see where the money went.

   The PDF is read in memory, summarised and discarded. Only the totals are
   kept (com.greed.statements.summary), so the page shows patterns, never a
   list of transactions."
  (:require [clojure.string :as str]
            [com.biffweb :as biff]
            [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.statements.core :as statements]
            [com.greed.statements.summary :as summary]
            [com.greed.statements.categorise :as categorise]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.headers :as headers]
            [com.greed.utilities.core :as utilities])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]))

;; ---------------------------------------------------------------------------
;; Formatting

(defn- rands
  "Cents as rands with cents: R7,092.58."
  [cents]
  (utilities/format-currency (/ cents 100.0)))

(defn- whole
  "Cents as whole rands: R7,093."
  [cents]
  (utilities/whole->rands (/ cents 100.0)))

(defn- day-month [iso]
  (.format (LocalDate/parse iso) (DateTimeFormatter/ofPattern "d MMM")))

(defn- day-month-year [iso]
  (.format (LocalDate/parse iso) (DateTimeFormatter/ofPattern "d MMM yyyy")))

(defn- period-label [{:statement-summary/keys [period-start period-end]}]
  (str (day-month period-start) " to " (day-month-year period-end)))

(defn- month-of [{:statement-summary/keys [month period-end]}]
  (or month (subs period-end 0 7)))

(defn- month-href [m & [bank]]
  (str "/app/spending?month=" m (when bank (str "&bank=" (name bank)))))

(defn- month-label [s]
  (.format (java.time.YearMonth/parse (month-of s)) (DateTimeFormatter/ofPattern "MMMM yyyy")))

(defn- short-month [m]
  (.format (java.time.YearMonth/parse m) (DateTimeFormatter/ofPattern "MMM")))

(defn- whole-month?
  "Does the summary cover its whole calendar month?"
  [{:statement-summary/keys [period-start period-end] :as s}]
  (let [ym (java.time.YearMonth/parse (month-of s))]
    (and (= (str (.atDay ym 1)) period-start)
         (= (str (.atEndOfMonth ym)) period-end))))

(def ^:private bank-names {:fnb "FNB" :standard-bank "Standard Bank"})

(defn- pct [part whole]
  (if (pos? whole) (Math/round (* 100.0 (/ (double part) whole))) 0))

;; ---------------------------------------------------------------------------
;; Upload

(def ^:private error-messages
  {:not-pdf "That file is not a PDF. Download the statement as a PDF from online banking and try again."
   :too-large "That file is larger than 5 MB. Bank statements are much smaller; check that you picked the right file."
   :too-many-pages "That statement has more than 60 pages. Download a shorter period, a few months at a time."
   :password-protected "That PDF is password-protected. Download it again from online banking: emailed statements are often locked."
   :no-text "That PDF has no text in it, so it may be a scan or a photo. Download the statement from online banking instead."
   :unreadable "That PDF could not be read. Try downloading it again."
   :unsupported-bank "greed can read FNB and Standard Bank statements for now. Other banks are coming."
   :no-file "Choose a statement PDF first."})

(defn- upload-panel [& {:keys [error compact?]}]
  [:div {:class "rounded-lg border border-zinc-200 bg-white p-5"}
   (biff/form
    {:action "/app/spending/upload" :enctype "multipart/form-data"
     :class "flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between"}
    [:div {:class "min-w-0"}
     [:h2 {:class "text-sm font-semibold text-zinc-900"} (if compact? "Add a statement" "Upload a bank statement")]
     [:p {:class "mt-0.5 max-w-[60ch] text-sm text-zinc-600"}
      "The PDF from your online banking. greed reads it, keeps only totals per payee, and never stores the file. FNB and Standard Bank for now; a statement covering several months is split by month."]
     [:label {:for "statement" :class "sr-only"} "Statement PDF"]
     [:input {:id "statement" :name "statement" :type "file" :accept "application/pdf,.pdf" :required true
              :class "mt-3 block w-full max-w-md text-sm text-zinc-700 file:mr-4 file:rounded-md file:border-0 file:bg-zinc-100 file:px-4 file:py-2 file:text-sm file:font-medium file:text-zinc-900 hover:file:bg-zinc-200"}]]
    (shared/btn :variant :dark :size :md :type "submit" :class "flex-shrink-0" "Read statement"))
   (when error
     [:p {:role "alert" :class "mt-4 rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-700"}
      (get error-messages error (error-messages :unreadable))])])

;; ---------------------------------------------------------------------------
;; The latest statement

(defn- stat [label value & [sub]]
  [:div {:class "min-w-0"}
   [:p {:class "text-sm text-zinc-500"} label]
   [:p {:class "mt-1 text-lg font-semibold tabular-nums text-zinc-900"} value]
   (when sub [:p {:class "mt-0.5 text-xs text-zinc-500"} sub])])

(defn- linked-plan
  "Cents your budget plans for the expense items linked on this summary."
  [{:statement-summary/keys [budget-matches]} items-by-id]
  (reduce + 0 (for [{:keys [item-id]} budget-matches
                    :let [i (items-by-id item-id)]
                    :when (= :expenses (:budget-item/type i))]
                (* 100 (:budget-item/amount i)))))

(defn- headline [{:statement-summary/keys [bank money-out-cents money-in-cents transfers-in-cents transfers-out-cents
                                           line-count checks-ok salary-cents budget-matches budgeted-cents] :as s}
                 planned-expenses items-by-id]
  [:section {:class "rounded-lg border border-zinc-200 bg-white p-6 sm:p-8" :aria-labelledby "latest-h"}
   [:h2 {:id "latest-h" :class "text-sm font-medium text-zinc-500"}
    (str (bank-names bank (name bank)) " · " (month-label s))]
   [:p {:class "mt-3 flex flex-wrap items-baseline gap-x-3"}
    [:span {:class "text-4xl font-semibold tracking-[-0.03em] tabular-nums text-zinc-900"} (whole money-out-cents)]
    [:span {:class "text-sm text-zinc-500"} "spent"]]
   (when-not (whole-month? s)
     [:p {:class "mt-1 text-sm text-zinc-600"}
      (str "Only " (period-label s) " is on your statement, so this is part of the month.")])
   (when (and (pos? planned-expenses) (whole-month? s))
     (if (seq budget-matches)
       ;; Linked items are judged one by one below; the rest of the spending
       ;; is compared with what the budget leaves for everything else.
       (let [unplanned (- money-out-cents (or budgeted-cents 0))
             rest-plan (max 0 (- (* 100 planned-expenses) (linked-plan s items-by-id)))
             diff      (- unplanned rest-plan)]
         [:p {:class (str "mt-1 text-sm " (if (pos? diff) "text-rose-700" "text-zinc-600"))}
          (str (whole (or budgeted-cents 0)) " went to your linked budget items. The other " (whole unplanned)
               (if (pos? diff)
                 (str " is " (whole diff) " more than the " (whole rest-plan) " your budget leaves for everything else.")
                 (str " is " (whole (- diff)) " less than the " (whole rest-plan) " your budget leaves for everything else.")))])
       (let [planned-cents (* 100 planned-expenses)
             diff (- money-out-cents planned-cents)]
         [:p {:class (str "mt-1 text-sm " (if (pos? diff) "text-rose-700" "text-zinc-600"))}
          (if (pos? diff)
            (str (whole diff) " more than the " (whole planned-cents) " your budget plans for expenses.")
            (str (whole (- diff)) " less than the " (whole planned-cents) " your budget plans for expenses."))])))
   (when-not checks-ok
     [:p {:role "alert" :class "mt-4 rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-800"}
      "This statement's own totals did not match what greed read, so some lines may be missing. Treat these numbers as a guide."])
   [:div {:class "mt-6 grid grid-cols-2 gap-6 border-t border-zinc-100 pt-5 sm:grid-cols-4"}
    (stat "Money in" (whole money-in-cents)
          (if (pos? (or salary-cents 0)) (str "including " (whole salary-cents) " salary") "own transfers left out"))
    (if (seq budget-matches)
      (stat "Budget items paid" (whole (or budgeted-cents 0))
            (str (count (filter (comp pos? :count) budget-matches)) " of " (count budget-matches) " linked items"))
      (stat "Planned expenses" (if (pos? planned-expenses) (utilities/whole->rands planned-expenses) "–") "from your budget"))
    (stat "Between your accounts" (whole (+ transfers-in-cents transfers-out-cents)) "left out of these totals")
    (stat "Lines read" (str line-count) "none of them stored")]])

(defn- sync-button
  "Re-matches every saved month with the budget, then returns to this one."
  [{:statement-summary/keys [bank] :as s}]
  (biff/form {:action "/app/spending/sync" :class "flex-shrink-0"}
             [:input {:type "hidden" :name "month" :value (month-of s)}]
             [:input {:type "hidden" :name "bank" :value (name bank)}]
             (shared/btn :variant :outline :size :sm :type "submit" "Sync with budget")))

(defn- budget-items-card
  "Each budget item with a statement reference: planned against paid."
  [{:statement-summary/keys [budget-matches payees] :as s} items-by-id has-references?]
  (let [whole? (whole-month? s)]
    (cond
      (seq budget-matches)
      [:section {:class "rounded-lg border border-zinc-200 bg-white p-6" :aria-labelledby "bud-h"}
       [:div {:class "flex items-start justify-between gap-4"}
        [:div {:class "min-w-0"}
         [:h2 {:id "bud-h" :class "text-sm font-semibold text-zinc-900"} "Your budget items"]
         [:p {:class "mt-0.5 text-xs text-zinc-500"}
          "Payments matched by each item's statement reference. These are left out of the spending outside your budget. greed syncs when you change a budget item; use the button after changes made elsewhere."]]
        (sync-button s)]
       [:ul {:class "mt-4 divide-y divide-zinc-100"}
        (for [{:keys [item-id title cents count]} budget-matches
              :let [item    (items-by-id item-id)
                    planned (some-> item :budget-item/amount (* 100))
                    over    (when planned (- cents planned))]]
          [:li {:class "flex items-center justify-between gap-4 py-2.5"}
           [:span {:class "min-w-0"}
            [:span {:class "block truncate text-sm text-zinc-900"} (or (:budget-item/title item) title)]
            [:span {:class (str "block text-xs " (if (and whole? over (pos? over)) "text-rose-700" "text-zinc-500"))}
             (cond
               (nil? item) "No longer in your budget"
               (and whole? (zero? count)) (str "Nothing matched this month · planned " (whole planned))
               (and whole? (pos? over)) (str (whole over) " more than the " (whole planned) " planned")
               :else (str "Planned " (whole planned) " · " count (if (= 1 count) " payment" " payments")))]]
           [:span {:class "flex-shrink-0 text-sm font-semibold tabular-nums text-zinc-900"} (rands cents)]])]]

      :else
      [:section {:class "rounded-lg border border-dashed border-zinc-300 bg-white p-6" :aria-labelledby "bud-h"}
       [:h2 {:id "bud-h" :class "text-sm font-semibold text-zinc-900"} "Link your budget items"]
       [:p {:class "mt-1 max-w-[70ch] text-sm text-zinc-600"}
        (cond
          (and has-references? (nil? payees))
          "This month was read before greed kept totals per payee, so it cannot be matched with your budget. Upload the statement once more; after that it stays in step with your budget."
          has-references?
          "Your budget items have statement references, but this month has not been matched with them yet."
          :else
          "Give your expense items a statement reference in Finances, such as \"discovery life\" for medical aid. greed then leaves those payments out of the spending outside your budget, and shows each item's planned and paid amounts.")]
       (when (and has-references? payees)
         [:div {:class "mt-3"} (sync-button s)])
       [:a {:href "/app/finances" :class "mt-3 inline-block rounded-sm text-sm font-medium text-zinc-900 underline underline-offset-4 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"}
        "Go to Finances"]])))

(defn- category-chart [{:statement-summary/keys [categories money-out-cents]}]
  (let [rows (->> categories (sort-by (comp - val)))
        top  (or (some-> rows first val) 1)]
    [:section {:class "rounded-lg border border-zinc-200 bg-white p-6" :aria-labelledby "cat-h"}
     [:h2 {:id "cat-h" :class "text-sm font-semibold text-zinc-900"} "Where it went"]
     [:p {:class "mt-0.5 text-xs text-zinc-500"} "Spending by category, largest first."]
     (if (seq rows)
       [:ul {:class "mt-5 space-y-3"}
        (for [[cat cents] rows]
          [:li {:class "grid grid-cols-[minmax(0,9rem)_minmax(0,1fr)_auto] items-center gap-3 text-sm"}
           [:span {:class "truncate text-zinc-700"} (categorise/categories cat (name cat))]
           [:span {:class "h-2 overflow-hidden rounded-full bg-zinc-100"}
            [:span {:class "block h-full rounded-full bg-brand-600" :style {:width (str (max 2 (pct cents top)) "%")}}]]
           [:span {:class "w-36 text-right tabular-nums"}
            [:span {:class "font-semibold text-zinc-900"} (rands cents)]
            [:span {:class "ml-2 text-xs text-zinc-500"} (str (pct cents money-out-cents) "%")]]])]
       [:p {:class "mt-5 text-sm text-zinc-600"} "No spending on this statement."])]))

(defn- callouts [{:statement-summary/keys [fees-cents fee-count categories money-in-cents salary-cents refunds-cents]}]
  (let [bnpl (get categories :bnpl 0)
        debt (get categories :debt 0)
        others (- money-in-cents (or salary-cents 0))
        items (cond-> []
                (pos? fees-cents)
                (conj ["Bank fees" (str (rands fees-cents) " across " fee-count " charge" (when (not= 1 fee-count) "s") ". A cheaper account or fewer small payments could lower this.")])
                (pos? bnpl)
                (conj ["Buy now, pay later" (str (rands bnpl) " in instalments. These are debt repayments; keep them in your budget's expenses.")])
                (pos? debt)
                (conj ["Loans, credit and interest" (str (rands debt) " went to credit cards, loans and interest. Paying these down frees up money every month.")])
                (pos? (or refunds-cents 0))
                (conj ["Refunds and reversals" (str (rands refunds-cents) " came back to you and is taken off your spending. Payments the bank reversed are left out altogether.")])
                (pos? others)
                (conj ["Money in from others" (str (rands others) " came in besides your salary. If it repeats, add it to your budget's income.")]))]
    (when (seq items)
      [:section {:class "rounded-lg border border-zinc-200 bg-white p-6" :aria-labelledby "notes-h"}
       [:h2 {:id "notes-h" :class "text-sm font-semibold text-zinc-900"} "Worth knowing"]
       [:ul {:class "mt-4 space-y-4"}
        (for [[title body] items]
          [:li
           [:p {:class "text-sm font-medium text-zinc-900"} title]
           [:p {:class "mt-0.5 text-sm text-zinc-600"} body]])]])))

(defn- merchants [{:statement-summary/keys [top-merchants]}]
  (when (seq top-merchants)
    [:section {:class "rounded-lg border border-zinc-200 bg-white p-6" :aria-labelledby "top-h"}
     [:h2 {:id "top-h" :class "text-sm font-semibold text-zinc-900"} "Top places"]
     [:p {:class "mt-0.5 text-xs text-zinc-500"} "Where you spent the most this month."]
     [:ol {:class "mt-4 divide-y divide-zinc-100"}
      (for [{:keys [name category cents count]} top-merchants]
        [:li {:class "flex items-center justify-between gap-4 py-2.5"}
         [:span {:class "min-w-0"}
          [:span {:class "block truncate text-sm text-zinc-900"} name]
          [:span {:class "block text-xs text-zinc-500"}
           (str (categorise/categories category "Other") " · " count (if (= 1 count) " payment" " payments"))]]
         [:span {:class "flex-shrink-0 text-sm font-semibold tabular-nums text-zinc-900"} (rands cents)]])]]))

;; ---------------------------------------------------------------------------
;; History

(defn- month-on-month [summaries selected]
  (let [months (->> summaries
                    (group-by month-of)
                    (map (fn [[m ss]] [m (reduce + 0 (map :statement-summary/money-out-cents ss))]))
                    (sort-by first)
                    (take-last 6)
                    vec)]
    (when (>= (count months) 2)
      (let [top (max 1 (apply max (map second months)))]
      [:section {:class "rounded-lg border border-zinc-200 bg-white p-6" :aria-labelledby "mom-h"}
       [:h2 {:id "mom-h" :class "text-sm font-semibold text-zinc-900"} "Month to month"]
       [:p {:class "mt-0.5 text-xs text-zinc-500"} "Money spent each month, all your statements together. The month you are looking at is in green; pick a bar to see that month."]
       [:div {:class "mt-6 flex h-40 items-end gap-3"}
        (for [[m cents] months
              :let [latest? (= m (month-of selected))]]
          [:a {:href (month-href m) :aria-label (str (short-month m) ": " (whole cents) " spent")
               :class "flex h-full min-w-0 flex-1 flex-col justify-end rounded-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"}
           [:span {:class "mb-1 text-center text-xs tabular-nums text-zinc-700"} (whole cents)]
           [:span {:class (str "block rounded-t-md " (if latest? "bg-brand-600" "bg-zinc-300"))
                   :style {:height (str (max 2 (pct cents top)) "%")}}]
           [:span {:class "mt-2 truncate text-center text-xs text-zinc-500"} (short-month m)]])]]))))

(defn- history [summaries selected]
  [:section {:class "rounded-lg border border-zinc-200 bg-white" :aria-labelledby "hist-h"}
   [:div {:class "px-6 py-4"}
    [:h2 {:id "hist-h" :class "text-sm font-semibold text-zinc-900"} "Your months"]
    [:p {:class "text-xs text-zinc-500"} "Totals only, one row per bank and month. Pick one to see it, or remove it from your history."]]
   [:ul {:class "divide-y divide-zinc-100 border-t border-zinc-100"}
    (for [{:statement-summary/keys [bank money-out-cents money-in-cents] :xt/keys [id] :as s} summaries]
      [:li {:class (str "flex items-center gap-4 px-6 py-3" (when (= id (:xt/id selected)) " bg-zinc-50"))}
       [:a {:href (month-href (month-of s) bank) :class "min-w-0 flex-1 rounded-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"}
        [:span {:class "block text-sm text-zinc-900 underline-offset-4 hover:underline"} (month-label s)]
        [:span {:class "block text-xs text-zinc-500"} (str (bank-names bank (name bank)) " · " (period-label s))]]
       [:span {:class "text-right text-sm tabular-nums"}
        [:span {:class "block font-semibold text-zinc-900"} (str (whole money-out-cents) " out")]
        [:span {:class "block text-xs text-zinc-500"} (str (whole money-in-cents) " in")]]
       (biff/form {:action "/app/spending/delete" :class "flex"}
                  [:input {:type "hidden" :name "summary-id" :value (str id)}]
                  [:button {:type "submit" :aria-label (str "Remove " (month-label s)) :data-confirm "Remove this month from your history?"
                            :class "rounded-md p-1.5 text-zinc-500 transition hover:bg-zinc-100 hover:text-zinc-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"}
                   [:span {:aria-hidden "true"} "×"]])])]])

;; ---------------------------------------------------------------------------
;; Choosing a month

(defn- selected-summary
  "The summary to show: the month (and bank) in the address, else the newest."
  [summaries {:keys [month bank id]}]
  (or (when id (some #(when (= (str (:xt/id %)) id) %) summaries))
      (when month
        (let [in-month (filter #(= month (month-of %)) summaries)]
          (or (when bank (some #(when (= bank (name (:statement-summary/bank %))) %) in-month))
              (first in-month))))
      (first summaries)))

(defn- month-switcher
  "Previous and next month, and every month you have, newest last."
  [summaries selected]
  (let [months (vec (sort (distinct (map month-of summaries))))
        cur    (month-of selected)
        i      (.indexOf months cur)
        prev   (when (pos? i) (months (dec i)))
        next   (when (< i (dec (count months))) (months (inc i)))
        banks  (filter #(= cur (month-of %)) summaries)
        arrow  (fn [m label text]
                 (if m
                   [:a {:href (month-href m) :aria-label label
                        :class "rounded-md border border-zinc-200 bg-white px-3 py-1.5 text-sm text-zinc-900 transition hover:bg-zinc-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"} text]
                   [:span {:aria-hidden "true" :class "rounded-md border border-zinc-100 px-3 py-1.5 text-sm text-zinc-300"} text]))]
    (when (or (> (count months) 1) (> (count banks) 1))
      [:nav {:aria-label "Months" :class "space-y-3"}
       [:div {:class "flex items-center gap-2"}
        (arrow prev "Previous month" "←")
        [:ul {:class "flex min-w-0 flex-1 gap-2 overflow-x-auto"}
         (for [m months]
           [:li {:class "flex-shrink-0"}
            [:a (cond-> {:href (month-href m)
                         :class (str "block rounded-md px-3 py-1.5 text-sm transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 "
                                     (if (= m cur) "bg-zinc-900 font-medium text-white" "text-zinc-700 hover:bg-zinc-100"))}
                  (= m cur) (assoc :aria-current "page"))
             (.format (java.time.YearMonth/parse m) (DateTimeFormatter/ofPattern "MMM yyyy"))]])]
        (arrow next "Next month" "→")]
       (when (> (count banks) 1)
         [:ul {:class "flex flex-wrap gap-2" :aria-label "Bank"}
          (for [{:statement-summary/keys [bank] :xt/keys [id]} banks]
            [:li
             [:a {:href (month-href cur bank)
                  :class (str "block rounded-md border px-3 py-1 text-xs transition "
                              (if (= id (:xt/id selected)) "border-zinc-900 text-zinc-900" "border-zinc-200 text-zinc-600 hover:bg-zinc-100"))}
              (bank-names bank (name bank))]])])])))

;; ---------------------------------------------------------------------------
;; Page and actions

(defn page [{:keys [session params] :as ctx} & {:keys [error]}]
  (let [uid       (:uid session)
        summaries (data/get-statement-summaries ctx uid)
        latest    (selected-summary summaries params)
        items     (data/get-budget-items ctx uid)
        items-by-id (into {} (map (juxt :xt/id identity)) items)
        has-references? (boolean (some :budget-item/reference items))
        {:keys [total-expenses]} (data/get-budget-data items)]
    (ui/app
     ctx
     [:div {:class "space-y-6"}
      (when (:alert params) (alerts/info params))
      (headers/pages-heading ["Spending"] "Where your money actually went, from your bank statements.")
      (if latest
        [:<>
         (month-switcher summaries latest)
         (headline latest (or total-expenses 0) items-by-id)
         (budget-items-card latest items-by-id has-references?)
         [:div {:class "grid grid-cols-1 gap-6 lg:grid-cols-2"}
          (category-chart latest)
          [:div {:class "space-y-6"}
           (callouts latest)
           (merchants latest)]]
         (month-on-month summaries latest)
         (upload-panel :error error :compact? true)
         (history summaries latest)]
        [:<>
         [:section {:class "rounded-lg border border-zinc-200 bg-white p-6 sm:p-8"}
          [:h2 {:class "text-2xl font-semibold tracking-[-0.02em] text-zinc-900"} "See where your money went"]
          [:p {:class "mt-2 max-w-[60ch] text-[15px] leading-relaxed text-zinc-700"}
           "Your budget is the plan. A bank statement shows what happened. Upload one and greed shows your spending by category, what it cost in fees, where you spent the most, and how it compares with your plan."]]
         (upload-panel :error error)])])))

(defn upload
  "POST /app/spending/upload: read, summarise and save; never keep the file."
  [{:keys [params session] :as ctx}]
  (let [bs (get-in params [:statement :bytes])]
    (if (or (nil? bs) (zero? (alength ^bytes bs)))
      (page ctx :error :no-file)
      (let [{:keys [error] :as result} (statements/read-statement-pdf bs)]
        (if error
          (page ctx :error error)
          (do (data/save-statement-summaries
               ctx (:uid session)
               (summary/summarise-months
                result
                (data/budget-match-items ctx (:uid session))))
              {:status 303
               :headers {"location" "/app/spending?alert=statement-read"}}))))))

(defn sync-budget
  "POST /app/spending/sync: re-match saved months with the budget's statement
   references, then come back to the same month."
  [{:keys [session params] :as ctx}]
  (let [{:keys [needs-upload]} (data/sync-statement-summaries ctx (:uid session))]
    {:status 303
     :headers {"location" (str "/app/spending?alert=" (if (pos? needs-upload) "budget-synced-partly" "budget-synced")
                               (when-let [m (not-empty (:month params))] (str "&month=" m))
                               (when-let [b (not-empty (:bank params))] (str "&bank=" b)))}}))

(defn delete [ctx]
  (data/delete-statement-summary ctx)
  {:status 303
   :headers {"location" "/app/spending?alert=statement-removed"}})
