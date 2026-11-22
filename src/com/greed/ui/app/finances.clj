(ns com.greed.ui.app.finances
  (:require [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.stats :as stats]
            [com.greed.ui.components.tables :as tables]
            [com.greed.ui.components.forms :as forms]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.headers :as headers]
            [com.greed.utilities.core :as utilities]
            [com.greed.ui.components.hero :as hero]))

(defn budget-lists [& {:keys [budget-items]}]
  [:div {:class "grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3"}
   (tables/budget-table
    {:title "income"
     :items (filterv #(= (:budget-item/type %) :income) budget-items)
     :protected-titles #{"Salary"}})
   (tables/budget-table
    {:title "expenses"
     :items (filterv #(= (:budget-item/type %) :expenses) budget-items)
     :protected-titles #{"Medical Aid" "Annual RA Contributions"}})
   (tables/budget-table
    {:title "savings"
     :items (filterv #(= (:budget-item/type %) :savings) budget-items)})])

(defn- business-lists [budget-items vat? owners]
  (let [of-type (fn [t] (filterv #(= (:budget-item/type %) t) budget-items))]
    [:div {:class "grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3"}
     (tables/budget-table {:title "income" :items (of-type :income) :business? true :vat? vat?})
     (tables/budget-table {:title "expenses" :items (of-type :expenses) :business? true :vat? vat? :owners owners})
     (tables/budget-table {:title "savings" :label "Set aside" :items (of-type :savings) :business? true})]))

(defn- business-hero
  "A business's month at a glance, in the same header card as Personal's:
   what is left after costs and set-aside, and where the income goes."
  [budget-items period]
  (let [{:keys [total-income total-expenses total-savings]} (data/get-budget-data budget-items)
        left (- total-income total-expenses total-savings)
        r    utilities/amount->rands]
    (hero/card
     {:label (if (neg? left) "Planned beyond its income" "Left over in the plan")
      :value (utilities/whole->rands (Math/abs (double left)))
      :unit (or period "this month")
      :verdict (cond
                 (not (pos? total-income)) {:text "No money in planned" :tone :neutral}
                 (neg? left) {:text "Over its income" :tone :bad}
                 (< left (* 0.05 total-income)) {:text "Tight" :tone :warn}
                 :else {:text "Within its income" :tone :good})
      :reading (cond
                 (not (pos? total-income)) "Add the money the business expects to come in, then its costs and what it sets aside."
                 (neg? left) (str "The plan spends and sets aside " (utilities/whole->rands (Math/abs (double left)))
                                  " more than the " (utilities/whole->rands total-income) " coming in. Trim a cost or a set-aside item.")
                 :else (str "Of the " (utilities/whole->rands total-income) " coming in, this is what is not yet spent or set aside."))
      :tone (when (neg? left) :rose)
      :picture (hero/bars
                {:title "Where the money goes"
                 :of total-income
                 :rows [{:label "Money out" :value (r total-expenses) :amount total-expenses :cls "bg-zinc-700"}
                         {:label "Set aside" :value (r total-savings) :amount total-savings :cls "bg-zinc-400"}
                         {:label "Left over" :value (r (max 0 left)) :amount (max 0 left) :cls "bg-brand-600"}]})
      :stats [["Money in" (r total-income) nil :money-in]
              ["Money out" (r total-expenses) nil :money-out]
              ["Set aside" (r total-savings) nil :set-aside]]})))

(defn- chosen-month
  "The budget period in the address (?month=2026-10), else this month."
  [params]
  (let [m (str (:month params))]
    (if (re-matches #"\d{4}-\d{2}" m) m (data/current-month))))

(defn- month-label [m pattern]
  (.format (java.time.YearMonth/parse m) (java.time.format.DateTimeFormatter/ofPattern pattern)))

(defn- period-bar
  "The budget period: which month the page counts. Monthly items count in
   every month; an item added from a statement counts only in its month."
  [month all-items]
  (let [ym      (java.time.YearMonth/parse month)
        prev    (str (.minusMonths ym 1))
        next    (str (.plusMonths ym 1))
        current (data/current-month)
        in-month (count (filter #(= month (:budget-item/from-statement %)) all-items))
        monthly  (count (data/monthly-items all-items))
        others   (->> all-items (keep :budget-item/from-statement) (remove #{month}) frequencies (sort-by key))
        arrow    (fn [m label text]
                   [:a {:href (str "/app/finances/?month=" m) :aria-label label
                        :class "rounded-md bg-white px-3 py-1.5 text-sm text-zinc-900 ring-1 ring-zinc-200/70 transition hover:bg-zinc-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"}
                    text])]
    [:section {:class "rounded-lg bg-white p-4 ring-1 ring-zinc-200/70 sm:px-6" :aria-label "Budget period"}
     [:div {:class "flex flex-wrap items-center justify-between gap-3"}
      [:div {:class "flex items-center gap-2"}
       (arrow prev "Previous month" "←")
       [:p {:class "min-w-[9rem] text-center text-sm font-semibold text-zinc-900"} (month-label month "MMMM yyyy")]
       (arrow next "Next month" "→")
       (if (= month current)
         [:span {:class "ml-1 rounded-full bg-brand-50 px-2 py-0.5 text-[11px] font-medium text-brand-700 ring-1 ring-brand-600/20"} "This month"]
         [:a {:href "/app/finances/" :class "ml-1 text-xs font-medium text-zinc-900 underline underline-offset-2"} "Back to this month"])]
      [:p {:class "text-xs text-zinc-500"}
       (str monthly (if (= 1 monthly) " monthly item" " monthly items")
            (when (pos? in-month) (str " · " in-month " from this month's statement")))]]
     (when (seq others)
       [:p {:class "mt-3 border-t border-zinc-100 pt-3 text-xs text-zinc-500"}
        "Items from other months' statements count only in their month: "
        (interpose ", "
                   (for [[m n] others]
                     [:a {:href (str "/app/finances/?month=" m) :class "font-medium text-zinc-900 underline underline-offset-2"}
                      (str (month-label m "MMM yyyy") " (" n ")")]))
        "."])]))

(defn- business-page [{:keys [params] :as ctx} {:business/keys [name] :as business}]
  (let [month        (chosen-month params)
        all-items    (data/get-business-budget-items ctx (:xt/id business))
        budget-items (data/items-for-month all-items month)]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (headers/pages-heading ["Budget"] (str name "'s monthly plan. Set aside is money put away for SARS, VAT, reserves and equipment."))
      (period-bar month all-items)
      (business-hero budget-items (when-not (= month (data/current-month)) (str "in " (month-label month "MMMM yyyy"))))
      (business-lists budget-items (:business/vat-registered business)
                      ;; people an Owner's drawings item can be paid to
                      (for [{:keys [user] :business-member/keys [user-id]} (data/get-business-members ctx (:xt/id business))]
                        {:id user-id :name (str (:user/firstname user) " " (:user/lastname user))}))])))

(defn- personal-page [{:keys [session params] :as ctx}]
  (let [user-id      (:uid session)
        month        (chosen-month params)
        all-items    (data/get-budget-items ctx user-id)
        budget-items (data/items-for-month all-items month)
        finances     (data/get-finances ctx user-id)
        payday       (:finances/payday finances)]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (headers/pages-heading ["Budget"] "Your monthly plan: what comes in, what goes out, and what you save.")
      (period-bar month all-items)
      (stats/finance-hero budget-items payday
                          :period (when-not (= month (data/current-month)) (str "in " (month-label month "MMMM yyyy"))))
      ;; the salary feeds the Income list, and payday sets the pay period
      (forms/salary-strip ctx)
      (budget-lists :budget-items budget-items)])))

(defn page
  "GET /app/finances/: the chosen business's budget, or the personal one."
  [ctx]
  (if-let [business (data/session-business ctx)]
    (business-page ctx business)
    (personal-page ctx)))
