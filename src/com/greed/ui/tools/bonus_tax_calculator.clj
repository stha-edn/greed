(ns com.greed.ui.tools.bonus-tax-calculator
  (:require [com.biffweb :as biff]
            [com.greed.ui :as ui]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.ui.tools.core :as tools]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.tax :as tax]
            [com.greed.data.core :as data]))

(defn- ->n [s]
  (try (double (BigDecimal. (or s "0")))
       (catch Exception _ 0.0)))

(defn- tier->age [tier]
  (case tier
    "under-65" 30
    "65-74"    70
    "75-plus"  80
    30))

(defn- age->tier [age]
  (cond
    (>= age 75) "75-plus"
    (>= age 65) "65-74"
    :else       "under-65"))

(def ^:private bonus-options
  [{:mult 0.5  :title "0.5×"  :subtitle "Half month"}
   {:mult 0.75 :title "0.75×" :subtitle "Three quarters"}
   {:mult 1.0  :title "1×"    :subtitle "13th cheque"}
   {:mult 1.25 :title "1.25×" :subtitle "Month & a quarter"}])

(defn- tier-label [tier]
  (case tier
    "under-65" "Under 65"
    "65-74"    "65–74"
    "75-plus"  "75+"
    "Under 65"))

(defn- auto-assessment-card [ctx]
  (let [user-id  (data/get-user-id-from-session ctx)
        user     (data/get-user ctx user-id)
        finances (data/get-finances ctx user-id)
        age      (or (:user/age user) 0)
        salary   (or (:finances/salary finances) 0)]
    (if (and (pos? age) (pos? salary))
      (let [tier (age->tier age)]
        (tools/panel
         (tools/panel-heading "Auto Assessment"
                              :badge [:span {:class "flex-shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700 ring-1 ring-emerald-600/15"}
                                      "2026/27"])
         [:div {:class "px-5 pb-5 sm:px-6"}
          [:p {:class "text-sm text-zinc-500"}
           "Based on your salary of "
           [:span {:class "font-medium text-zinc-700"} (utilities/amount->rands salary) "/month"]
           " and age " [:span {:class "font-medium text-zinc-700"} age]
           " (" (tier-label tier) " tier). Pick a bonus size to calculate instantly:"]
          (biff/form
           {:hx-post    "/app/tax/bonus-tax-calculator"
            :hx-target  "#bonus-result"
            :hx-swap    "outerHTML"
            :hx-trigger "submit"}
           [:input {:type "hidden" :name "mode" :value "auto"}]
           [:div {:class "mt-4 grid grid-cols-2 gap-2 sm:grid-cols-4"}
            (for [{:keys [mult title subtitle]} bonus-options]
              (shared/btn :variant :outline :size :md
                          :class "flex-col px-2 py-2.5"
                          :type "submit"
                          :attrs {:name "mult" :value (str mult)}
                          [:span {:class "text-base font-semibold text-zinc-900"} title]
                          [:span {:class "text-[11px] font-medium text-zinc-400"} subtitle]))])
          [:p {:class "mt-3 text-xs text-zinc-400"}
           "Uses the salary saved in Finances and your age in Settings."]]))
      (tools/panel
       [:div {:class "flex flex-col items-center justify-center px-6 py-12 text-center"}
        [:div {:class "mx-auto mb-3 flex h-10 w-10 items-center justify-center rounded-full bg-emerald-50"}
         [:span {:class "text-emerald-500"} (svgs/wallet)]]
        [:p {:class "text-sm font-medium text-zinc-500"} "No salary data yet"]
        [:p {:class "mt-1 max-w-xs text-xs text-zinc-400"}
         "Add your salary in "
         [:a {:href "/app/finances/" :class "font-medium text-emerald-600 hover:underline"} "Finances"]
         " or your age in "
         [:a {:href "/app/settings" :class "font-medium text-emerald-600 hover:underline"} "Settings"]
         " to enable auto assessment."]]))))

(defn- guide []
  [:div {:class "space-y-4"}
   (tools/panel
    (tools/panel-heading "How this tool works")
    [:div {:class "px-5 pb-5 sm:px-6"}
     [:p {:class "text-sm text-zinc-500 leading-relaxed"}
      "A bonus (or 13th cheque) is taxed at your marginal rate — the rate on your top slice of income. This calculator works out the tax by comparing your annual tax with and without the bonus; the difference is the PAYE withheld from the bonus."]
     [:p {:class "mt-2 text-sm text-zinc-500 leading-relaxed"}
      "Use Auto Assessment to see your bonus tax at 0.5× to 1.25× of your saved salary, or enter a custom salary, bonus amount and rebate tier below to model a what-if scenario, then click Calculate. The result shows the tax on the bonus and what you'll actually take home."]])

   (tools/panel
    (tools/panel-heading "Understanding your results")
    [:div {:class "divide-y divide-zinc-100 border-t border-zinc-100"}
     (tools/glossary-item "Tax on bonus" "The additional PAYE created by adding the bonus to your annual income. Because the bonus sits on top of your salary, it is taxed at your highest (marginal) rate.")
     (tools/glossary-item "Net bonus" "What lands in your account after PAYE. UIF and other deductions are not included here.")
     (tools/glossary-item "Effective bonus rate" "The percentage of the bonus that goes to tax. This is typically higher than the effective rate on your salary because the whole bonus is taxed at the margin.")])

   (tools/notice
    "This is an estimate based on SARS 2026/27 brackets and the primary/age-tier rebates. It does not account for medical aid credits or retirement contributions. Use the "
    [:a {:href "/app/tax/tax-returns" :class "font-semibold text-amber-900 underline underline-offset-2 hover:text-amber-950"} "Tax Returns simulator"]
    " for a fuller picture.")])

(defn- amount-field [{:keys [id label hint value lg?]}]
  [:div
   [:label {:for id :class "block text-sm font-medium text-zinc-700 mb-1"} label]
   [:p {:class "text-xs text-zinc-400 mb-1"} hint]
   [:div {:class "relative mt-1"}
    [:span {:class "absolute left-3.5 top-1/2 -translate-y-1/2 text-sm font-medium text-zinc-400"} "R"]
    [:input {:id id :name id :type "number" :min "0" :step "any"
             :class (str "block w-full pl-8 pr-3 bg-white border border-zinc-200 rounded-xl transition-colors duration-150 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 "
                         (if lg?
                           "py-3 text-lg font-semibold tabular-nums text-zinc-900 placeholder-zinc-400"
                           "py-2 text-sm font-medium text-zinc-700 placeholder-zinc-400"))
             :placeholder "0" :value (or value "")}]]])

(defn- tier-field [selected]
  [:div
   [:label {:class "block text-sm font-medium text-zinc-700 mb-1"} "Your rebate tier"]
   [:p {:class "text-xs text-zinc-400 mb-2"} "Age 65+ and 75+ earn extra annual rebates."]
   [:div {:class "relative grid grid-cols-3 gap-1 rounded-xl bg-zinc-100 p-1"}
    [:input {:type "radio" :name "rebate-tier" :value "under-65" :id "rebate-tier-1"
             :class "sr-only peer peer/1" :checked (= selected "under-65")}]
    [:input {:type "radio" :name "rebate-tier" :value "65-74" :id "rebate-tier-2"
             :class "sr-only peer peer/2" :checked (= selected "65-74")}]
    [:input {:type "radio" :name "rebate-tier" :value "75-plus" :id "rebate-tier-3"
             :class "sr-only peer peer/3" :checked (= selected "75-plus")}]
    [:label {:for "rebate-tier-1"
             :class "relative z-10 cursor-pointer rounded-lg px-3 py-2 text-center text-sm font-medium transition-colors active:scale-[0.97] text-zinc-500 hover:text-zinc-700 peer-focus-visible/1:ring-2 peer-focus-visible/1:ring-emerald-500/50 peer-checked/1:text-zinc-900"}
     "Under 65"]
    [:label {:for "rebate-tier-2"
             :class "relative z-10 cursor-pointer rounded-lg px-3 py-2 text-center text-sm font-medium transition-colors active:scale-[0.97] text-zinc-500 hover:text-zinc-700 peer-focus-visible/2:ring-2 peer-focus-visible/2:ring-emerald-500/50 peer-checked/2:text-zinc-900"}
     "65–74"]
    [:label {:for "rebate-tier-3"
             :class "relative z-10 cursor-pointer rounded-lg px-3 py-2 text-center text-sm font-medium transition-colors active:scale-[0.97] text-zinc-500 hover:text-zinc-700 peer-focus-visible/3:ring-2 peer-focus-visible/3:ring-emerald-500/50 peer-checked/3:text-zinc-900"}
     "75+"]
    [:div {:class "pointer-events-none absolute inset-y-1 left-1 w-[calc((100%-1rem)/3)] rounded-lg bg-white shadow-sm ring-1 ring-zinc-900/5 transition-transform duration-300 ease-out peer-checked/1:translate-x-0 peer-checked/2:translate-x-[calc(100%+0.25rem)] peer-checked/3:translate-x-[calc(200%+0.5rem)]"}]]])

(defn- form-card [params]
  (let [selected (or (:rebate-tier params)
                     (some-> (:age params) utilities/->int age->tier)
                     "under-65")]
    (tools/panel
     (tools/panel-heading "Calculate your bonus tax")
     [:div {:class "px-5 pb-6 sm:px-6"}
      (biff/form
       {:hx-post     "/app/tax/bonus-tax-calculator"
        :hx-target   "#bonus-result"
        :hx-swap     "outerHTML"
        :hx-trigger  "submit"}
       [:div {:class "space-y-5"}
        (amount-field {:id "income" :label "Monthly gross salary" :lg? true
                       :hint "Your regular salary before the bonus — for custom scenarios. Leave it to Auto Assessment above for your saved salary."
                       :value (:income params)})
        (amount-field {:id "bonus" :label "Bonus amount"
                       :hint "The once-off bonus or 13th cheque — type a custom amount"
                       :value (:bonus params)})
        (tier-field selected)]
       [:div {:class "mt-6 flex items-center justify-end"}
        (shared/btn :variant :primary :size :md :class "px-8" :type "submit" "Calculate")])])))

(defn- bonus-split [take-share tax-share net-bonus bonus-tax]
  [:div {:class "mt-4 rounded-xl bg-zinc-50 p-5 ring-1 ring-zinc-200/50"}
   [:p {:class "text-xs font-semibold text-zinc-500 uppercase tracking-wider"} "Where your bonus goes"]
   [:div {:class "mt-3 flex h-2.5 w-full overflow-hidden rounded-full bg-zinc-200/70"}
    [:div {:class "h-full rounded-l-full bg-emerald-500"
           :style {:width (str take-share "%")}}]
    [:div {:class "h-full rounded-r-full bg-rose-400"
           :style {:width (str tax-share "%")}}]]
   [:div {:class "mt-3 flex items-center justify-between gap-3 text-sm"}
    [:span {:class "flex items-center gap-2 text-zinc-500"}
     [:span {:class "h-2 w-2 rounded-full bg-emerald-500"}] "Take-home"]
    [:span {:class "font-semibold text-zinc-900 tabular-nums"} (utilities/amount->rands net-bonus)]]
   [:div {:class "mt-1.5 flex items-center justify-between gap-3 text-sm"}
    [:span {:class "flex items-center gap-2 text-zinc-500"}
     [:span {:class "h-2 w-2 rounded-full bg-rose-400"}] "Tax"]
    [:span {:class "font-semibold text-rose-600 tabular-nums"} (utilities/amount->rands bonus-tax)]]])

(defn- breakdown-panel [income annual-salary bonus tax-without tax-with bonus-tax]
  (tools/panel
   (tools/panel-heading "Bonus Breakdown")
   (tools/breakdown-section "Inputs"
                            (tools/row "Monthly Gross Salary" (utilities/amount->rands income))
                            (tools/row "Annual Salary" (utilities/amount->rands annual-salary))
                            (tools/bold-row "Bonus Amount" (utilities/amount->rands bonus)))
   (tools/breakdown-section "Tax on Bonus"
                            (tools/row "Annual tax without bonus" (utilities/amount->rands tax-without))
                            (tools/row "Annual tax with bonus" (utilities/amount->rands tax-with))
                            (tools/bold-row "Tax on Bonus" (utilities/amount->rands bonus-tax) "text-rose-600"))))

(defn- bank-total [net-salary net-bonus month-total]
  [:div {:class "mt-4 rounded-xl bg-emerald-50 p-5 ring-1 ring-emerald-600/15"}
   [:div {:class "flex items-center justify-between gap-3"}
    [:span {:class "text-sm font-medium text-emerald-900"} "Lands in your bank this month"]
    [:span {:class "text-lg font-bold tabular-nums text-emerald-700"}
     (utilities/amount->rands month-total)]]
   [:p {:class "mt-1 text-xs text-zinc-500"}
    (utilities/amount->rands net-salary) " net salary + " (utilities/amount->rands net-bonus) " net bonus"]])

(defn- result-region [params user finances]
  (let [auto?         (= "auto" (:mode params))
        db-salary     (or (:finances/salary finances) 0)
        db-age        (or (:user/age user) 0)
        income        (if auto? (double db-salary) (->n (:income params)))
        bonus         (if auto?
                        (double (Math/round (* db-salary (->n (:mult params)))))
                        (->n (:bonus params)))
        tier          (if auto?
                        (age->tier db-age)
                        (or (:rebate-tier params)
                            (some-> (:age params) utilities/->int age->tier)
                            "under-65"))
        age           (tier->age tier)
        annual-salary (utilities/income->annual-income income)
        tax-without   (:net-tax (tax/calculate-income-tax annual-salary age))
        tax-with      (:net-tax (tax/calculate-income-tax (+ annual-salary bonus) age))
        bonus-tax     (max 0.0 (- tax-with tax-without))
        net-bonus     (- bonus bonus-tax)
        net-salary    (utilities/annual-income->monthly-income (- annual-salary tax-without))
        month-total   (+ net-salary net-bonus)
        eff-rate      (if (pos? bonus) (* 100.0 (/ bonus-tax bonus)) 0.0)
        tax-share     (min 100.0 (max 0.0 eff-rate))
        take-share    (max 0.0 (- 100.0 tax-share))
        status        (str (if (pos? bonus-tax)
                             (str "You'll take home " (utilities/amount->rands net-bonus)
                                  " of your " (utilities/amount->rands bonus)
                                  " bonus — the whole bonus is taxed at your marginal rate, so you pay " (utilities/pct-label eff-rate) ".")
                             (str "You'll take home " (utilities/amount->rands net-bonus)
                                  " of your " (utilities/amount->rands bonus) " bonus — no extra tax on this bonus."))
                           " Together with your regular salary, " (utilities/amount->rands month-total)
                           " lands in your bank account this month.")]
    (if (not (pos? bonus))
      [:div#bonus-result
       (tools/panel
        [:div {:class "flex flex-col items-center justify-center px-6 py-16 text-center"}
         [:div {:class "mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-emerald-50"}
          [:span {:class "text-emerald-500"} (svgs/gift)]]
         [:p {:class "text-sm font-medium text-zinc-500"} "Enter a bonus amount to see what you'll take home"]
         [:p {:class "mt-1 max-w-xs text-xs text-zinc-400"} "Pick a quick option above or type a bonus amount, then click Calculate to see your net bonus."]])]
      [:div#bonus-result {:class "space-y-4"}
       (tools/result-hero
        :eyebrow "Net bonus"
        :badge "2026/27"
        :headline (utilities/whole->rands net-bonus)
        :suffix "take-home"
        :status status
        :body [:<> (bonus-split take-share tax-share net-bonus bonus-tax)
               (bank-total net-salary net-bonus month-total)]
        :substats [(tools/hero-substat "Bonus amount" (utilities/amount->rands bonus))
                   (tools/hero-substat "Tax on bonus" (utilities/amount->rands bonus-tax) "text-rose-600")
                   (tools/hero-substat "Effective rate" (utilities/pct-label eff-rate) "text-rose-600")])
       (breakdown-panel income annual-salary bonus tax-without tax-with bonus-tax)])))

(defn- page-template [ctx params user finances]
  (ui/app
   ctx
   [:div {:class "space-y-7"}
    (headers/pages-heading [["Tax" "/app/tax"] "Bonus Tax Calculator"]
                           "See what a bonus or 13th cheque is really worth after PAYE.")
    (auto-assessment-card ctx)
    (tools/tool-layout
     (form-card params)
     (guide)
     (result-region params user finances))]))

(defn page-get [ctx]
  (let [user-id  (data/get-user-id-from-session ctx)
        user     (data/get-user ctx user-id)
        finances (data/get-finances ctx user-id)]
    (page-template ctx {} user finances)))

(defn page [{:keys [params session] :as ctx}]
  (let [user-id  (:uid session)
        user     (data/get-user ctx user-id)
        finances (data/get-finances ctx user-id)]
    (if (get-in ctx [:headers "hx-request"])
      (result-region params user finances)
      (page-template ctx params user finances))))
