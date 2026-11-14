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
            [com.greed.utilities.vat :as vat]
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

(defn compare-with-budget
  "Planned against spent for one month: `summaries` are that month's
   summaries (one per bank), `planned-expenses` the budget's expenses in
   rands. When budget items are linked by statement reference, their payments
   are judged item by item, and only the rest of the spending is compared with
   what the budget leaves for everything else. All amounts in cents; :over is
   negative when under plan."
  [summaries items-by-id planned-expenses]
  (let [spent      (reduce + 0 (map :statement-summary/money-out-cents summaries))
        budgeted   (reduce + 0 (keep :statement-summary/budgeted-cents summaries))
        linked-ids (distinct (map :item-id (mapcat :statement-summary/budget-matches summaries)))
        linked?    (boolean (seq linked-ids))
        planned    (* 100 planned-expenses)
        linked-plan (reduce + 0 (for [id linked-ids
                                      :let [i (items-by-id id)]
                                      :when (= :expenses (:budget-item/type i))]
                                  (* 100 (:budget-item/amount i))))
        unplanned  (- spent budgeted)
        rest-plan  (max 0 (- planned linked-plan))]
    {:spent spent :planned planned :linked? linked? :budgeted budgeted
     :unplanned unplanned :rest-plan rest-plan
     :over (if linked? (- unplanned rest-plan) (- spent planned))}))

(defn- verdict
  "One sentence on planned against spent (see compare-with-budget)."
  [{:keys [linked? budgeted unplanned rest-plan planned over]}]
  [:p {:class (str "mt-1 text-sm " (if (pos? over) "text-rose-700" "text-zinc-600"))}
   (if linked?
     (str (whole budgeted) " went to your linked budget items. The other " (whole unplanned) " is "
          (whole (Math/abs (long over))) (if (pos? over) " more" " less")
          " than the " (whole rest-plan) " your budget leaves for everything else.")
     (str (whole (Math/abs (long over))) (if (pos? over) " more" " less")
          " than the " (whole planned) " your budget plans for expenses."))])

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
     (verdict (compare-with-budget [s] items-by-id planned-expenses)))
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
          "Give your expense items a statement reference in your budget, such as \"discovery life\" for medical aid. greed then leaves those payments out of the spending outside your budget, and shows each item's planned and paid amounts.")]
       (when (and has-references? payees)
         [:div {:class "mt-3"} (sync-button s)])
       [:a {:href "/app/finances" :class "mt-3 inline-block rounded-sm text-sm font-medium text-zinc-900 underline underline-offset-4 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"}
        "Go to your budget"]])))

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

(defn- income-title-set
  "Titles of the budget's income items, lower case, to tell what is already in it."
  [items]
  (set (for [{:budget-item/keys [type title]} items :when (= :income type)]
         (str/lower-case (str/trim (str title))))))

(defn- money-in-card
  "A business's payers this month, each with a button that adds it to the
   budget's income at this month's amount."
  [{:statement-summary/keys [income-payees bank] :as s} items]
  (let [in-budget (income-title-set items)]
    [:section {:class "rounded-lg border border-zinc-200 bg-white p-6" :aria-labelledby "in-h"}
     [:h2 {:id "in-h" :class "text-sm font-semibold text-zinc-900"} "Money in"]
     [:p {:class "mt-0.5 text-xs text-zinc-500"} "Who paid the business this month. Add a regular payer to the budget's income at this month's amount; you can change it in Budget."]
     (cond
       (nil? income-payees)
       [:p {:class "mt-4 text-sm text-zinc-600"} "This month was read before greed kept payers. Upload the statement once more to see them here."]

       (empty? income-payees)
       [:p {:class "mt-4 text-sm text-zinc-600"} "No money came in this month, apart from transfers between the business's own accounts."]

       :else
       [:ul {:class "mt-4 divide-y divide-zinc-100"}
        (for [{:keys [name cents count]} income-payees]
          [:li {:class "flex items-center justify-between gap-4 py-2.5"}
           [:span {:class "min-w-0"}
            [:span {:class "block truncate text-sm text-zinc-900"} name]
            [:span {:class "block text-xs text-zinc-500"} (str (rands cents) " · " count (if (= 1 count) " payment" " payments"))]]
           (cond
             (in-budget (str/lower-case (str/trim name)))
             [:span {:class "flex-shrink-0 text-xs font-medium text-zinc-500"} "In your income"]

             (str/blank? name) nil

             :else
             (biff/form {:action "/app/spending/add-income" :class "flex-shrink-0"}
                        [:input {:type "hidden" :name "month" :value (month-of s)}]
                        [:input {:type "hidden" :name "bank" :value (clojure.core/name bank)}]
                        [:input {:type "hidden" :name "payer" :value name}]
                        (shared/btn :variant :outline :size :sm :type "submit"
                                    :attrs {:aria-label (str "Add " name " to income")}
                                    "Add to income")))])])]))

(defn- payee-name
  "A payee's readable name; months read before names were kept use its text."
  [{:keys [name text]}]
  (or (not-empty name) (summary/merchant-name text)))

(defn- money-out-card
  "A business's largest payees this month, each with a button that adds it
   to the budget's expenses, with its name as the statement reference so its
   payments are matched from then on. A payee already matched by an item's
   reference shows that item instead."
  [{:statement-summary/keys [payees bank] :as s} items]
  (let [match-items (for [{:budget-item/keys [title type reference category] :xt/keys [id]} items
                          :when (and reference (#{:expenses :savings} type))]
                      (cond-> {:id id :title title :reference reference} category (assoc :category category)))
        linked      (summary/payee-items match-items (take 15 payees))
        titles      (set (for [{:budget-item/keys [type title]} items :when (#{:expenses :savings} type)]
                           (str/lower-case (str/trim (str title)))))]
    [:section {:class "rounded-lg border border-zinc-200 bg-white p-6" :aria-labelledby "out-h"}
     [:h2 {:id "out-h" :class "text-sm font-semibold text-zinc-900"} "Money out"]
     [:p {:class "mt-0.5 text-xs text-zinc-500"}
      "Who the business paid this month, largest first. Add a regular cost to the budget's expenses at this month's amount; its name becomes the statement reference, so its payments are matched from then on."]
     (if (nil? payees)
       [:p {:class "mt-4 text-sm text-zinc-600"} "This month was read before greed kept payees. Upload the statement once more to see them here."]
       [:ul {:class "mt-4 divide-y divide-zinc-100"}
        (for [[{:keys [spent-cents refund-cents count category] :as p} item] linked
              :let [nm (payee-name p)]]
          [:li {:class "flex items-center justify-between gap-4 py-2.5"}
           [:span {:class "min-w-0"}
            [:span {:class "block truncate text-sm text-zinc-900"} nm]
            [:span {:class "block text-xs text-zinc-500"}
             (str (rands (- spent-cents refund-cents)) " · " (categorise/categories category "Other") " · "
                  count (if (= 1 count) " payment" " payments"))]]
           (cond
             item [:span {:class "flex-shrink-0 truncate text-xs font-medium text-zinc-500"} (str "In budget: " (:title item))]
             (titles (str/lower-case (str/trim nm))) [:span {:class "flex-shrink-0 text-xs font-medium text-zinc-500"} "In your budget"]
             (str/blank? nm) nil
             :else
             (biff/form {:action "/app/spending/add-expense" :class "flex-shrink-0"}
                        [:input {:type "hidden" :name "month" :value (month-of s)}]
                        [:input {:type "hidden" :name "bank" :value (name bank)}]
                        [:input {:type "hidden" :name "payee" :value (:text p)}]
                        (shared/btn :variant :outline :size :sm :type "submit"
                                    :attrs {:aria-label (str "Add " nm " to expenses")}
                                    "Add to expenses")))])])]))

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
;; Insights card

(defn- bar-row [label cents top bar-class]
  [:div {:class "grid grid-cols-[minmax(0,8rem)_minmax(0,1fr)_auto] items-center gap-3 text-sm"}
   [:span {:class "truncate text-zinc-600"} label]
   [:span {:class "h-2.5 overflow-hidden rounded-full bg-zinc-100"}
    [:span {:class (str "block h-full rounded-full " bar-class) :style {:width (str (max 2 (pct cents top)) "%")}}]]
   [:span {:class "w-24 text-right font-semibold tabular-nums text-zinc-900"} (whole cents)]])

(defn planned-vs-spent-card
  "For Insights: the newest full month from the user's bank statements
   against the budget's planned expenses, all banks together. Partial months
   (the edges of a statement) are not compared."
  [summaries budget-items planned-expenses]
  (let [items-by-id (into {} (map (juxt :xt/id identity)) budget-items)
        full        (filter whole-month? summaries)
        month       (some-> (first full) month-of)
        in-month    (filter #(= month (month-of %)) full)
        {:keys [spent planned linked? budgeted unplanned over] :as cmp}
        (when month (compare-with-budget in-month items-by-id planned-expenses))
        banks       (str/join " and " (distinct (map #(bank-names (:statement-summary/bank %) "bank") in-month)))
        link        (fn [href label]
                      [:a {:href href :class "mt-5 inline-flex items-center gap-1 rounded-sm text-sm font-medium text-zinc-900 underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"}
                       label [:span {:aria-hidden "true"} "→"]])]
    [:div {:class "p-6 bg-white ring-1 ring-zinc-200/70 rounded-lg"}
     [:h3 {:class "text-sm font-semibold text-zinc-900 tracking-tight"} "Planned vs spent"]
     [:p {:class "mt-0.5 text-xs text-zinc-500 leading-relaxed"}
      (if month
        (str (month-label (first in-month)) ", from your " banks " statement" (when (> (count in-month) 1) "s") ".")
        "What your bank statements show against this plan.")]
     (cond
       (and month (pos? planned-expenses))
       [:div {:class "mt-5 grid grid-cols-1 gap-6 lg:grid-cols-2 lg:gap-10"}
        [:div {:class "space-y-3"}
         (let [top (max 1 planned spent)]
           [:<>
            (bar-row "Planned" planned top "bg-zinc-400")
            (bar-row "Spent" spent top (if (pos? over) "bg-rose-500" "bg-brand-600"))
            (when linked?
              [:<>
               (bar-row "Budget items" budgeted top "bg-brand-600/60")
               (bar-row "Everything else" unplanned top "bg-brand-600/60")])])]
        [:div
         [:p {:class (str "text-2xl font-semibold tracking-[-0.02em] tabular-nums "
                          (if (pos? over) "text-rose-700" "text-zinc-900"))}
          (str (whole (Math/abs (long over))) (if (pos? over) " over plan" " under plan"))]
         (verdict cmp)
         (link (month-href month) (str "See " (month-label (first in-month)) " in Spending"))]]

       month
       [:div
        [:p {:class "mt-5 text-sm text-zinc-600"}
         (str (whole spent) " spent. Add your expenses in Finances to compare it with a plan.")]
        (link "/app/finances" "Go to Finances")]

       (seq summaries)
       [:div
        [:p {:class "mt-5 text-sm text-zinc-600"}
         "Your statements only cover part of a month so far. Upload one that covers a full month to compare it with your plan."]
        (link "/app/spending" "Go to Spending")]

       :else
       [:div
        [:p {:class "mt-5 text-sm text-zinc-600"}
         "Upload a bank statement to see what you actually spent against this plan, by month and by category."]
        (link "/app/spending" "Upload a statement")])]))

;; ---------------------------------------------------------------------------
;; Page and actions

(defn page
  "GET /app/spending: the chosen business's months, or the personal ones."
  [{:keys [params] :as ctx} & {:keys [error]}]
  (let [owner     (data/owner ctx)
        business  (data/session-business ctx)
        summaries (data/get-statement-summaries ctx owner)
        latest    (selected-summary summaries params)
        items     (data/get-owner-budget-items ctx owner)
        items-by-id (into {} (map (juxt :xt/id identity)) items)
        has-references? (boolean (some :budget-item/reference items))
        {:keys [total-expenses]} (data/get-budget-data items)]
    (ui/app
     ctx
     [:div {:class "space-y-6"}
      (when (:alert params) (alerts/info params))
      (headers/pages-heading ["Spending"]
                             (if business
                               (str "Where " (:business/name business) "'s money actually went, from its bank statements.")
                               "Where your money actually went, from your bank statements."))
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
         ;; a business can add its payers and payees to its budget from here
         (when business
           [:div {:class "grid grid-cols-1 gap-6 lg:grid-cols-2"}
            (money-in-card latest items)
            (money-out-card latest items)])
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
  [{:keys [params] :as ctx}]
  (let [bs (get-in params [:statement :bytes])]
    (if (or (nil? bs) (zero? (alength ^bytes bs)))
      (page ctx :error :no-file)
      (let [owner (data/owner ctx)
            {:keys [error] :as result} (statements/read-statement-pdf bs :business? (some? (:business-id owner)))]
        (if error
          (page ctx :error error)
          (do (data/save-statement-summaries
               ctx owner
               (summary/summarise-months
                result
                (data/budget-match-items ctx owner)))
              {:status 303
               :headers {"location" "/app/spending?alert=statement-read"}}))))))

(defn add-income
  "POST /app/spending/add-income: a business adds one of the month's payers
   to its budget's income, at that month's amount. The amount is read from
   the saved month, not from the form."
  [{:keys [params] :as ctx}]
  (let [owner    (data/owner ctx)
        s        (when (:business-id owner)
                   (some #(when (and (= (:month params) (month-of %))
                                     (= (:bank params) (name (:statement-summary/bank %))))
                            %)
                         (data/get-statement-summaries ctx owner)))
        payer    (some #(when (= (:payer params) (:name %)) %) (:statement-summary/income-payees s))
        back     (str "/app/spending?month=" (:month params) "&bank=" (:bank params))]
    (if (and payer (not (str/blank? (:name payer))))
      (do (data/upsert-budget-item (assoc ctx :params {:title (subs (:name payer) 0 (min 80 (count (:name payer))))
                                                       :type "income"
                                                       :amount (str (Math/round (/ (:cents payer) 100.0)))}))
          {:status 303 :headers {"location" (str back "&alert=income-added")}})
      {:status 303 :headers {"location" back}})))

(defn add-expense
  "POST /app/spending/add-expense: a business adds one of the month's payees
   to its budget's expenses at that month's amount, with the payee's name as
   the statement reference, then re-matches its months. A VAT vendor's item
   is standard-rated when the payee's category usually carries VAT. The
   amount is read from the saved month, not from the form."
  [{:keys [params] :as ctx}]
  (let [owner (data/owner ctx)
        s     (when (:business-id owner)
                (some #(when (and (= (:month params) (month-of %))
                                  (= (:bank params) (name (:statement-summary/bank %))))
                         %)
                      (data/get-statement-summaries ctx owner)))
        p     (some #(when (= (:payee params) (:text %)) %) (:statement-summary/payees s))
        nm    (some-> p payee-name)
        back  (str "/app/spending?month=" (:month params) "&bank=" (:bank params))]
    (if (and p (not (str/blank? nm)))
      (do (data/upsert-budget-item
           (assoc ctx :params {:title (subs nm 0 (min 80 (count nm)))
                               :type "expenses"
                               :amount (str (max 0 (Math/round (/ (- (:spent-cents p) (:refund-cents p)) 100.0))))
                               :reference nm
                               :vat (if (vat/vat-categories (:category p)) "standard" "none")}))
          (data/sync-statement-summaries ctx owner)
          {:status 303 :headers {"location" (str back "&alert=expense-added")}})
      {:status 303 :headers {"location" back}})))

(defn sync-budget
  "POST /app/spending/sync: re-match saved months with the budget's statement
   references, then come back to the same month."
  [{:keys [params] :as ctx}]
  (let [{:keys [needs-upload]} (data/sync-statement-summaries ctx (data/owner ctx))]
    {:status 303
     :headers {"location" (str "/app/spending?alert=" (if (pos? needs-upload) "budget-synced-partly" "budget-synced")
                               (when-let [m (not-empty (:month params))] (str "&month=" m))
                               (when-let [b (not-empty (:bank params))] (str "&bank=" b)))}}))

(defn delete [ctx]
  (data/delete-statement-summary ctx)
  {:status 303
   :headers {"location" "/app/spending?alert=statement-removed"}})
