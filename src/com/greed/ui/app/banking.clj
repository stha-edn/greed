(ns com.greed.ui.app.banking
  "The Banking page of the current profile (Personal or a business): the
   bank card and bank details, statement uploads, which months each bank's
   statements cover, bank fees, and the statement history. Spending reads
   the statements; Banking is where they come in."
  (:require [com.core :as c]
            [com.greed.ui :as ui]
            [com.greed.core :as c.greed]
            [com.greed.data.core :as data]
            [com.greed.data.plans :as plans]
            [com.greed.statements.core :as statements]
            [com.greed.statements.summary :as summary]
            [com.greed.ui.app.statements :as spending]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.cards :as cards]
            [com.greed.ui.components.forms :as forms]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.home :as home]
            [com.greed.ui.components.hero :as hero]
            [com.greed.ui.components.stats :as stats])
  (:import [java.time LocalDate YearMonth]))

(def ^:private card "rounded-lg bg-white ring-1 ring-zinc-200/70")

(defn coverage
  "Which of the last `n` months each bank's statements cover:
   {bank [{:month \"yyyy-MM\" :state :full|:partial|:missing}]}, oldest first."
  [summaries n ^LocalDate today]
  (let [months (map #(str (.minusMonths (YearMonth/from today) %)) (range (dec n) -1 -1))]
    (into (sorted-map)
          (for [[bank ss] (group-by :statement-summary/bank summaries)
                :let [by-month (group-by home/month-of ss)]]
            [bank (vec (for [m months
                             :let [s (by-month m)]]
                         {:month m
                          :state (cond (empty? s) :missing
                                       (some home/whole-month? s) :full
                                       :else :partial)}))]))))

(defn monthly-fees
  "Bank fees per month, all banks together, oldest first, at most the last
   `n` months: [{:month :cents :count}]."
  [summaries n]
  (->> (group-by home/month-of summaries)
       (map (fn [[m ss]] {:month m
                          :cents (reduce + 0 (map #(or (:statement-summary/fees-cents %) 0) ss))
                          :count (reduce + 0 (map #(or (:statement-summary/fee-count %) 0) ss))}))
       (sort-by :month)
       (take-last n)
       vec))

;; ---------------------------------------------------------------------------
;; Sections

(defn- account-section
  "Personal: the bank card beside the bank details. A business: the banks
   its statements come from."
  [ctx business summaries]
  (if business
    [:section {:class (str card " p-6") :aria-labelledby "acc-h"}
     [:h2 {:id "acc-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "The business's banks"]
     [:p {:class "mt-0.5 text-xs text-zinc-500"} "Every bank the business has uploaded statements from."]
     (if (seq summaries)
       [:ul {:class "mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3"}
        (for [[bank ss] (group-by :statement-summary/bank summaries)
              :let [months (sort (map home/month-of ss))]]
          [:li {:class "rounded-md bg-zinc-50 px-4 py-3"}
           [:p {:class "text-sm font-semibold text-zinc-900"} (spending/bank-names bank (name bank))]
           [:p {:class "mt-0.5 text-xs text-zinc-500"}
            (str (count months) (if (= 1 (count months)) " month" " months") ", "
                 (home/month-name (first months) "MMM yyyy")
                 (when (> (count months) 1) (str " to " (home/month-name (last months) "MMM yyyy"))))]])]
       [:p {:class "mt-4 text-sm text-zinc-600"} "No statements yet. Upload one below."])]
    (let [{:keys [session]} ctx
          user     (data/get-user ctx (:uid session))
          finances (data/get-finances ctx (:uid session))
          items    (data/items-for-month (data/get-budget-items ctx (:uid session)) (data/current-month))
          tax      (c.greed/get-income-tax-data user finances)]
      [:div {:class "grid grid-cols-1 items-stretch gap-4 lg:grid-cols-3"}
       [:div {:class "flex items-center justify-center lg:col-span-1"}
        (cards/bank-card :finances finances :budget-items items
                         :net-monthly-income (when-let [net (:net-income tax)] (/ net 12)))]
       [:div {:class "lg:col-span-2"} (forms/bank-details ctx)]])))

(defn- coverage-card [summaries]
  (let [cov (coverage summaries 12 (LocalDate/now))
        cls {:full "bg-brand-600" :partial "bg-brand-600/40" :missing "bg-zinc-100"}]
    [:section {:class (str card " p-6") :aria-labelledby "cov-h"}
     [:div {:class "flex flex-wrap items-start justify-between gap-4"}
      [:div
       [:h2 {:id "cov-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "Months covered"]
       [:p {:class "mt-0.5 text-xs text-zinc-500"} "The last 12 months, per bank. Upload the missing ones to complete the picture."]]
      [:div {:class "flex items-center gap-3 text-xs text-zinc-600"}
       (for [[k label] [[:full "Full month"] [:partial "Part"] [:missing "Missing"]]]
         [:span {:class "flex items-center gap-1.5"} [:span {:class (str "h-2.5 w-2.5 rounded-sm " (cls k))}] label])]]
     (if (seq cov)
       [:div {:class "mt-5 space-y-4 overflow-x-auto"}
        (for [[bank months] cov]
          [:div {:class "min-w-[32rem]"}
           [:p {:class "mb-1.5 text-xs font-medium text-zinc-700"} (spending/bank-names bank (name bank))]
           [:div {:class "grid grid-cols-12 gap-1"}
            (for [{:keys [month state]} months]
              [:div {:title (str (home/month-name month "MMMM yyyy") ": "
                                 (case state :full "full month" :partial "part of the month" :missing "no statement"))}
               [:div {:class (str "h-6 rounded-sm " (cls state))}]
               [:p {:class "mt-1 text-center text-[11px] text-zinc-500"} (home/month-name month "MMM")]])]])]
       [:p {:class "mt-4 text-sm text-zinc-600"} "No statements yet."])]))

(defn- fees-card [summaries]
  (let [fees  (monthly-fees summaries 6)
        total (reduce + 0 (map :cents fees))
        n     (reduce + 0 (map :count fees))
        top   (max 1 (apply max 1 (map :cents fees)))]
    [:section {:class (str card " flex h-full flex-col p-6") :aria-labelledby "fees-h"}
     [:h2 {:id "fees-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "Bank fees"]
     [:p {:class "mt-0.5 text-xs text-zinc-500"} "What the bank charged each month, from the statements."]
     (if (seq fees)
       [:<>
        [:p {:class "mt-4 flex items-baseline gap-2"}
         [:span {:class "text-2xl font-semibold tabular-nums text-zinc-900"} (spending/rands total)]
         [:span {:class "text-sm text-zinc-500"} (str "in " (count fees) (if (= 1 (count fees)) " month" " months") ", " n " charges")]]
        [:div {:class "mt-5 flex h-28 items-end gap-3"}
         (for [{:keys [month cents]} fees]
           [:div {:class "flex h-full min-w-0 flex-1 flex-col justify-end" :title (str (home/month-name month "MMMM yyyy") ": " (spending/rands cents))}
            [:span {:class "mb-1 text-center text-[11px] tabular-nums text-zinc-600"} (spending/whole cents)]
            [:span {:class "block rounded-t-sm bg-zinc-400 greed-col-grow" :style {:height (str (max 2 (Math/round (* 100.0 (/ cents top)))) "%")}}]
            [:span {:class "mt-1.5 text-center text-xs text-zinc-500"} (home/month-name month "MMM")]])]
        [:p {:class "mt-auto pt-4 text-xs text-zinc-500"}
         "Fewer small payments, or a cheaper account, can lower these."]]
       [:p {:class "mt-4 text-sm text-zinc-600"} "No statements yet."])]))

(defn- plan-note
  "On the Free plan, how much of the statement allowance is used."
  [ctx summaries]
  (let [uid (get-in ctx [:session :uid])]
    (when-not (plans/at-least? ctx uid :premium)
      (let [used  (count (distinct (keep :statement-summary/month summaries)))
            limit (plans/free-statement-months)]
        [:div {:class "mt-3 flex flex-col gap-3 rounded-lg bg-brand-50 px-4 py-3 text-sm ring-1 ring-brand-600/20 sm:flex-row sm:items-center sm:justify-between"}
         [:p {:class "text-brand-900"}
          [:span {:class "font-semibold"}
           (if (> used limit)
             (str "Free plan: " used " months read. ")
             (str "Free plan: " used " of " limit " statement month" (when (not= 1 limit) "s") " used. "))]
          (cond
            (> used limit) "They stay here; reading new months needs Premium."
            (= used limit) "Statements for other months need Premium."
            :else "Premium reads every month you upload.")]
         [:a {:href "/app/plan" :class "flex-shrink-0 font-semibold text-brand-800 underline underline-offset-4"} "See plans"]]))))

(defn- upload-card [ctx error business? summaries]
  [:div
   (spending/upload-panel :error error)
   (when-not business? (plan-note ctx summaries))
   [:p {:class "mt-2 px-1 text-xs text-zinc-500"}
    (if business?
      "Statements uploaded here belong to the business, kept apart from your own."
      "Statements uploaded here are your own. To upload a business's, switch to it first.")]])

(defn- statements-hero
  "Banking's header card, like every tab's: how many months are read, each
   bank's share of the last 12 months as bars, and what the bank charged."
  [summaries]
  (let [months (distinct (map home/month-of summaries))
        latest (some-> (sort months) last)
        cov    (coverage summaries 12 (LocalDate/now))
        fees   (monthly-fees summaries 6)]
    (hero/card
     {:label "Statements"
      :value (str (count months))
      :unit (if (= 1 (count months)) "month of statements" "months of statements")
      :verdict (when latest
                 (let [last-month (str (.minusMonths (YearMonth/now) 1))]
                   (if (neg? (compare latest last-month))
                     {:text (str "Last month's statement is missing") :tone :warn}
                     {:text "Up to date" :tone :good})))
      :reading (if latest
                 (str "Latest: " (home/month-name latest "MMMM yyyy") ". Only totals are kept, never the files.")
                 "Upload a PDF statement below. Only totals are kept, never the files.")
      :picture (when (seq cov)
                 (hero/bars {:title "The last 12 months, per bank"
                             :of 12
                             :rows (for [[bank ms] cov
                                         :let [n (count (remove #(= :missing (:state %)) ms))]]
                                     {:label (spending/bank-names bank (name bank)) :value (str n " of 12")
                                      :amount n :cls "bg-brand-600"})}))
      :stats [["Banks" (str (count cov))]
              ["Fees, 6 months" (spending/whole (reduce + 0 (map :cents fees))) nil :bank-fees]
              ["Times charged" (str (reduce + 0 (map :count fees)))]]})))

;; ---------------------------------------------------------------------------
;; Page and actions

(defn- problem-or-nil [params]
  (when (= "bank-invalid" (:error params))
    [:p {:role "alert" :class "rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-700"} "Choose one of the banks in the list."]))


(defn page
  "GET /app/banking."
  [{:keys [params] :as ctx} & {:keys [error]}]
  (let [business  (data/session-business ctx)
        summaries (data/get-statement-summaries ctx (data/owner ctx))]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (headers/pages-heading ["Banking"]
                             (if business
                               (str (:business/name business) "'s bank statements: upload them, see which months are in, and what the bank charged.")
                               "Your bank, your statements, and what the bank charges you."))
      (problem-or-nil params)
      (statements-hero summaries)
      [:div
       (stats/section-header (if business "Accounts" "Your account"))
       (account-section ctx business summaries)]
      [:div
       (stats/section-header "Statements" :href "/app/spending" :link-label "See them in Spending")
       [:div {:class "space-y-4"}
        (upload-card ctx error (some? business) summaries)
        (when (seq summaries)
          [:div {:class "grid grid-cols-1 items-stretch gap-4 lg:grid-cols-5"}
           [:div {:class "lg:col-span-3"} (coverage-card summaries)]
           [:div {:class "lg:col-span-2"} (fees-card summaries)]])]]
      (when (seq summaries)
        [:div
         (stats/section-header "History")
         (spending/history summaries nil)])])))

(defn save-bank
  "POST /app/banking/save-bank: the person's bank and account type."
  [ctx]
  (let [result (data/save-bank-details! ctx (:banking/banks c/common-config))]
    {:status 303
     :headers {"location" (if (= :ok result) "/app/banking?alert=bank-saved" "/app/banking?error=bank-invalid")}}))

(defn upload
  "POST /app/banking/upload: read, summarise and save the statement for the
   current profile; never keep the file. Then show it in Spending."
  [{:keys [params] :as ctx}]
  (let [bs (get-in params [:statement :bytes])]
    (if (or (nil? bs) (zero? (alength ^bytes bs)))
      (page ctx :error :no-file)
      (let [owner (data/owner ctx)
            {:keys [error] :as result} (statements/read-statement-pdf bs :business? (some? (:business-id owner)))]
        (if error
          (page ctx :error error)
          (let [months (summary/summarise-months result (data/budget-match-items ctx owner))
                ;; on Free, only the allowance is saved, not the whole statement
                saved  (plans/months-to-save ctx owner months)
                left   (- (count months) (count saved))]
            (cond
              (empty? saved)
              {:status 303 :headers {"location" "/app/plan?alert=need-premium"}}

              :else
              (do (data/save-statement-summaries ctx owner saved)
                  {:status 303
                   :headers {"location" (str "/app/spending?month=" (:month (last saved))
                                             "&alert=" (if (pos? left) "statement-read-partly" "statement-read"))}}))))))))
