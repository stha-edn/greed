(ns com.greed.ui.components.plans
  "The three plans side by side, shared by /pricing and the app's Plan page:
   name, price for the chosen interval, what it adds, and one action each."
  (:require [com.greed.data.plans :as plans]
            [com.greed.utilities.core :as utilities]))

(def features
  {:free     {:line "Plan your month and your tax."
              :items ["Budget, goals and calendar"
                      "Insights and your savings rate"
                      "Income tax, bonus and ITR12 calculators"
                      "One statement month in Spending"]}
   :premium  {:line "See where every rand went, every month."
              :items ["Everything in Free"
                      "Unlimited statements: FNB and Standard Bank"
                      "Spending and Banking for every month"
                      "Planned against spent, month by month"]}
   :business {:line "Your money and your business, in one account."
              :items ["Everything in Premium"
                      "Businesses: budget, statements, VAT and company tax"
                      "Owner's pay on your personal Tax page"
                      "Invite a partner or bookkeeper"
                      "SARS dates on the calendar"]}})

(defn rands
  "4900 -> \"R49\", 49050 -> \"R490.50\"."
  [cents]
  (if (zero? (mod cents 100))
    (utilities/whole->rands (quot cents 100))
    (format "R%,.2f" (/ cents 100.0))))

(defn price-line
  "[big small] for a tier and interval, e.g. [\"R49\" \"a month\"]."
  [tier interval]
  (if (= :free tier)
    ["R0" "for ever"]
    (let [cents (plans/price tier interval)]
      [(rands cents) (if (= :yearly interval) "a year" "a month")])))

(defn interval-switch
  "Monthly or yearly, as two links (works without script)."
  [href interval]
  [:div {:role "group" :aria-label "Billing period" :class "inline-flex gap-1 rounded-lg bg-white p-1 ring-1 ring-zinc-200/70"}
   (for [[k label] [[:monthly "Monthly"] [:yearly "Yearly · 2 months free"]]]
     [:a {:href (str href "?interval=" (name k))
          :aria-current (when (= k interval) "true")
          :class (str "rounded-md px-3 py-1.5 text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 "
                      (if (= k interval) "bg-zinc-900 text-white" "text-zinc-600 hover:text-zinc-900"))}
      label])])

(defn interval-of [params]
  (if (= "yearly" (:interval params)) :yearly :monthly))

(defn cards
  "One card per tier. `action` is (fn [tier] hiccup-or-nil) for its button;
   `current` the tier to mark as the person's own."
  [{:keys [interval current action]}]
  [:div {:class "grid grid-cols-1 gap-5 lg:grid-cols-3"}
   (for [tier plans/tiers
         :let [{:keys [line items]} (features tier)
               [big small] (price-line tier interval)
               ;; a paid plan of your own is the one picked out; otherwise Premium
               own-paid? (contains? #{:premium :business} current)
               featured? (if own-paid? (= tier current) (= :premium tier))]]
     [:section {:aria-labelledby (str "tier-" (name tier))
                :class (str "flex flex-col rounded-lg bg-white p-6 ring-1 "
                            (if featured? "ring-2 ring-zinc-900" "ring-zinc-200/70"))}
      [:div {:class "flex items-center gap-2"}
       [:h3 {:id (str "tier-" (name tier)) :class "text-lg font-semibold tracking-[-0.01em] text-zinc-900"} (plans/tier-name tier)]
       [:div {:class "flex-1"}]
       (cond
         (= tier current) [:span {:class "rounded-full bg-brand-50 px-2.5 py-1 text-xs font-semibold text-brand-700 ring-1 ring-brand-600/20"} "Your plan"]
         (and featured? (not own-paid?)) [:span {:class "rounded-full bg-zinc-100 px-2.5 py-1 text-xs font-semibold text-zinc-900 ring-1 ring-zinc-200"} "Most popular"])]
      [:p {:class "mt-1 text-sm text-zinc-600"} line]
      [:p {:class "mt-5 flex items-baseline gap-1.5"}
       [:span {:class "text-4xl font-semibold tracking-[-0.03em] tabular-nums text-zinc-900"} big]
       [:span {:class "text-sm font-medium text-zinc-500"} small]]
      (when (and (= :yearly interval) (not= :free tier))
        [:p {:class "mt-1 text-xs text-zinc-500"}
         (str "That is " (rands (quot (plans/price tier :yearly) 12)) " a month.")])
      [:ul {:class "mt-6 flex-1 space-y-2.5 text-sm text-zinc-700"}
       (for [i items]
         [:li {:class "flex gap-2.5"}
          [:span {:aria-hidden "true" :class "mt-0.5 font-semibold text-brand-600"} "✓"]
          i])]
      [:div {:class "mt-7"} (action tier)]])])
