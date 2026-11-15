(ns com.greed.ui.pages.home
  "Home page: one app, two profiles. It opens on personal money and shows
   that the same account runs a business too. Every product picture uses
   the app's own header-card bars (hero/bars), with sample figures that
   add up, so the page looks like the product it describes. Interface
   fragments carry data-ui."
  (:require [clojure.string :as string]
            [com.biffweb :as biff]
            [com.core :as c]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.tax :as tax]
            [com.greed.ui.components.hero :as hero]
            [com.greed.ui.components.marketing :as m]))

;; ---------------------------------------------------------------------------
;; Data

(def ^:private sample-salary
  "Monthly salary the page opens on. Matches the sample fragments below."
  25000)

(defn- tax-config-json
  "The SARS tables the server uses (config/tax.edn), handed to the hero's
   calculator so the page and the product never disagree."
  [cfg]
  (let [brackets (:tax-brackets cfg)
        {:keys [primary secondary]} (:rebates cfg)]
    (str "{\"brackets\":["
         (string/join "," (for [{:keys [threshold rate base-tax]} brackets]
                            (str "[" threshold "," rate "," base-tax "]")))
         "],\"primary\":" (or primary 0)
         ",\"secondary\":" (or secondary 0) "}")))

(defn- monthly-take-home [cfg monthly age]
  (let [{:keys [net-tax]} (tax/calculate-income-tax (* 12 monthly) age cfg)
        monthly-tax (/ (double net-tax) 12)]
    {:keep (- monthly monthly-tax)
     :tax monthly-tax
     :cents (Math/round (* 100 (/ (double net-tax) (* 12 monthly))))}))


;; ---------------------------------------------------------------------------
;; Small pieces

(def ^:private btn-dark
  "inline-flex items-center justify-center rounded-md bg-zinc-900 px-6 py-3 text-base font-semibold text-white transition-[transform,background-color] duration-150 ease-out hover:bg-zinc-700 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 focus-visible:ring-offset-2")

(def ^:private btn-light
  "inline-flex items-center justify-center rounded-md bg-white px-6 py-3 text-base font-semibold text-zinc-900 transition-[transform,background-color] duration-150 ease-out hover:bg-zinc-200 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-zinc-950")

(def ^:private h2-class
  "max-w-[20ch] [text-wrap:balance] font-semibold [font-size:clamp(32px,4vw,56px)] leading-[1.04] tracking-[-0.035em]")

;; ---------------------------------------------------------------------------
;; Sample figures. Personal: a R25,000 salary (R21,619 after tax).
;; Business: Mosa Studio (Pty) Ltd, a VAT vendor and small business corporation.

(defn- personal-shot []
  (m/shot {:label "October 2026" :badge "Personal"
         :value "R11,240" :unit "spent"
         :reading "R760 less than the R12,000 the budget plans for expenses."
         :bars {:title "Planned against spent"
                :rows [{:label "Planned" :value "R12,000" :amount 12000 :cls "bg-zinc-400"}
                       {:label "Spent" :value "R11,240" :amount 11240 :cls "bg-brand-600"}]}
         :stats [["Money in" "R21,619"] ["Money out" "R11,240"] ["In minus out" "R10,379"]]}))

(defn- business-shot []
  (m/shot {:label "October 2026" :badge "Mosa Studio (Pty) Ltd"
         :value "R31,200" :unit "spent"
         :reading "R2,800 less than the R34,000 the budget plans for costs."
         :bars {:title "Money in against money out"
                :rows [{:label "Money in" :value "R86,400" :amount 86400 :cls "bg-brand-600"}
                       {:label "Money out" :value "R31,200" :amount 31200 :cls "bg-zinc-400"}]}
         :stats [["VAT to pay" "R7,200"] ["Owner's pay" "R20,000"] ["Team" "3 people"]]}))

;; ---------------------------------------------------------------------------
;; Hero: one account, two profiles, switched like the app does

(defn- hero-section [signed-in?]
  [:section {:class "grid grid-cols-1 items-center gap-14 pb-20 pt-12 sm:pt-16 lg:grid-cols-12 lg:gap-12 lg:pb-28 lg:pt-20"}
   [:div {:class "lg:col-span-6"}
    [:h1 {:class "enter max-w-[14ch] [text-wrap:balance] font-semibold text-zinc-900 [font-size:clamp(48px,6.4vw,104px)] leading-[0.95] tracking-[-0.05em]"}
     "Know where every rand goes."]
    [:p {:class "enter mt-8 max-w-[46ch] text-xl leading-relaxed text-zinc-700" :style {"--d" "80ms"}}
     "greed reads your bank statements, plans your month and works out what SARS takes. For your own money, and for your business, in one account."]
    [:div {:class "enter mt-10 flex flex-wrap items-center gap-x-6 gap-y-4" :style {"--d" "120ms"}}
     (if signed-in?
       [:a {:href "/app" :class btn-dark} "Go to your account"]
       [:a {:href "/signup" :class btn-dark} "Create your free account"])
     [:a {:href "#how" :class (str "text-base " m/link-ink)} "See how it works"]]]
   [:div {:class "enter lg:col-span-6" :style {"--d" "140ms"}}
    [:div {:class "mx-auto max-w-[540px] rounded-2xl bg-stone-100 p-4 sm:p-6"}
     [:div {:role "tablist" :aria-label "Profiles" :class "mb-4 flex gap-1 rounded-lg bg-white p-1 ring-1 ring-zinc-200/70"}
      (for [[id label sel?] [["pf-personal" "Personal" true] ["pf-business" "Business" false]]]
        [:button {:type "button" :role "tab" :id (str id "-tab") :aria-controls id :aria-selected (str sel?)
                  :class "pf-tab flex-1 rounded-md px-3 py-2 text-sm font-semibold text-zinc-600 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"}
         label])]
     [:div {:id "pf-personal" :role "tabpanel" :aria-labelledby "pf-personal-tab" :class "pf-panel"} (personal-shot)]
     [:div {:id "pf-business" :role "tabpanel" :aria-labelledby "pf-business-tab" :class "pf-panel" :hidden true} (business-shot)]
     [:p {:class "mt-4 text-center text-sm text-zinc-600"} "One sign-in. Switch between you and your business at any time."]]]])

(defn- facts []
  [:dl {:class "enter grid grid-cols-2 gap-x-6 gap-y-8 border-y border-zinc-200 py-10 lg:grid-cols-4"}
   (for [[k v] [["FNB & Standard Bank" "PDF statements, read for you"]
                ["SARS 2026/27" "tables for income, VAT and company tax"]
                ["Free" "to use, for you and your business"]
                ["Never" "sells your data, and never keeps your statements"]]]
     [:div
      [:dt {:class "text-lg font-semibold tracking-[-0.02em] text-zinc-900"} k]
      [:dd {:class "mt-1 max-w-[28ch] text-base leading-snug text-zinc-600"} v]])])

;; ---------------------------------------------------------------------------
;; How it works: statement, plan, what is left

(defn- step [n title line visual & [d]]
  [:li {:class "enter flex flex-col" :style {"--d" (or d "0ms")}}
   [:p {:class "text-sm font-semibold tabular-nums text-brand-700"} (str "0" n)]
   [:h3 {:class "mt-2 text-2xl font-semibold tracking-[-0.02em] text-zinc-900"} title]
   [:p {:class "mt-2 max-w-[38ch] text-base leading-relaxed text-zinc-600"} line]
   [:div {:class "mt-8 flex-1"} visual]])

(defn- how []
  [:section {:id "how" :class "py-24 lg:py-32" :aria-labelledby "how-h"}
   [:h2 {:id "how-h" :class (str "enter text-zinc-900 " h2-class)} "From bank statement to a plan, in three steps."]
   [:ol {:class "mt-16 grid grid-cols-1 gap-14 lg:grid-cols-3 lg:gap-10"}
    (step 1 "Upload a statement"
          "Drop in a PDF from FNB or Standard Bank. greed reads every line, sorts it into categories, then keeps only the totals."
          (m/shot {:label "Banking" :value "4" :unit "months read"
                 :reading "Only totals are kept, never the files."
                 :bars {:title "The last 12 months, per bank" :of 12
                        :rows [{:label "FNB" :value "4 of 12" :amount 4 :cls "bg-brand-600"}
                               {:label "Standard Bank" :value "3 of 12" :amount 3 :cls "bg-brand-600"}]}}))
    (step 2 "Plan the month"
          "Add what you expect to earn and spend, or add straight from a statement. A reference links a budget item to its debit order, even when the bank changes the number."
          [:div {:data-ui "" :class "rounded-lg bg-white px-5 py-2 shadow-card-md ring-1 ring-zinc-200/70 divide-y divide-zinc-100"} ; taste-skill-ignore: rows inside a data-ui fragment
           (m/item-row "Car finance" "R3,500" "Monthly" "WESBANK")
           (m/item-row "Rent" "R7,000" "Monthly")
           (m/item-row "Plumber" "R1,150" "From Oct 2026 statement")]
          "80ms")
    (step 3 "See what is left"
          "Spending compares each month with the plan, and leaves out what you already budgeted for. Home shows it all at a glance."
          (m/shot {:label "Spending" :value "R11,240" :unit "spent"
                 :reading "R760 less than planned."
                 :bars {:title "Planned against spent"
                        :rows [{:label "Planned" :value "R12,000" :amount 12000 :cls "bg-zinc-400"}
                               {:label "Spent" :value "R11,240" :amount 11240 :cls "bg-brand-600"}]}})
          "160ms")]])

;; ---------------------------------------------------------------------------
;; The tax calculator (personal tax, live)

(defn- calculator [cfg signed-in?]
  (let [{:keys [keep tax cents]} (monthly-take-home cfg sample-salary 30)]
    [:section {:id "keep" :aria-labelledby "keep-h" :class "py-24 text-center lg:py-32"
               :data-tax (tax-config-json cfg)}
     ;; The heading is the calculation, centred and as large as the page allows
     [:h2 {:id "keep-h" :class "enter mx-auto w-full font-semibold text-zinc-900 [font-size:var(--keep-fs,clamp(68px,12vw,208px))] leading-[0.9] tracking-[-0.055em]"}
      [:span {:class "block whitespace-nowrap"} "You keep"]
      [:span {:id "keep-out" :class "block whitespace-nowrap tabular-nums" :aria-live "polite"}
       (utilities/whole->rands keep)]]
     ;; The controls as one calculator strip under it
     [:div {:data-ui "" :class "enter mx-auto mt-12 grid max-w-4xl grid-cols-1 divide-y divide-zinc-200 rounded-lg border border-zinc-300 bg-white text-left md:grid-cols-[1.5fr_1fr_1fr] md:divide-x md:divide-y-0" :style {"--d" "80ms"}} ; taste-skill-ignore: dividers inside a data-ui control strip
      [:div {:class "p-5"}
       [:label {:for "salary" :class "flex items-baseline justify-between gap-3"}
        [:span {:class "text-[15px] font-medium text-zinc-600"} "Monthly salary"]
        [:output {:id "salary-out" :for "salary" :class "text-2xl font-semibold tabular-nums text-zinc-900"}
         (utilities/whole->rands sample-salary)]]
       [:input {:id "salary" :type "range" :min 5000 :max 200000 :step 500 :value sample-salary
                :class "salary-range mt-3 w-full"}]]
      [:div {:class "p-5"}
       [:p {:class "text-[15px] font-medium text-zinc-600"} "SARS takes"]
       [:p {:class "mt-1 text-2xl font-semibold tabular-nums text-zinc-900"}
        [:span {:id "tax-out"} (utilities/whole->rands tax)] [:span {:class "text-base font-medium text-zinc-600"} " a month"]]
       [:p {:class "mt-0.5 text-sm tabular-nums text-zinc-600"}
        [:span {:id "cents-out"} cents] "c of every R1"]]
      [:label {:class "flex cursor-pointer items-center justify-between gap-3 p-5"}
       [:span
        [:span {:class "block text-[15px] font-semibold text-zinc-900"} "65 or older"]
        [:span {:class "block text-sm text-zinc-600"} "Adds the secondary rebate"]]
       [:input {:id "older" :type "checkbox" :role "switch" :class "age-switch"}]]]
     [:p {:class "enter mx-auto mt-10 max-w-[44ch] text-xl leading-relaxed text-zinc-700" :style {"--d" "120ms"}}
      "Your take-home on the SARS 2026/27 tables. Drag your salary. The full calculators also do bonuses and a practice ITR12 return."]
     [:div {:class "enter mt-6 flex flex-wrap items-center justify-center gap-x-6 gap-y-4" :style {"--d" "160ms"}}
      [:a {:href (if signed-in? "/app/tax/income-tax-calculator" "/signin")
           :class "group inline-flex items-center gap-1.5 rounded-sm text-base font-medium text-zinc-900 underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"}
       "Open the full tax calculator"
       [:span {:aria-hidden "true" :class "transition-transform duration-150 ease-out group-hover:translate-x-0.5"} "→"]]]]))


;; ---------------------------------------------------------------------------
;; Everything for your own money

(defn- tile [{:keys [title line href d]} & body]
  [:a {:href href :style {"--d" (or d "0ms")}
       :class "enter group flex flex-col rounded-lg bg-white p-6 ring-1 ring-zinc-200/70 transition-shadow duration-200 hover:shadow-card-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900"}
   [:h3 {:class "flex items-center justify-between gap-3 text-lg font-semibold tracking-[-0.01em] text-zinc-900"}
    title [:span {:aria-hidden "true" :class "text-zinc-400 transition-transform duration-150 group-hover:translate-x-0.5 group-hover:text-zinc-900"} "→"]]
   [:p {:class "mt-1.5 text-[15px] leading-relaxed text-zinc-600"} line]
   (into [:div {:class "mt-6"}] body)])

(defn- personal-features [signed-in?]
  (let [go (fn [href] (if signed-in? href "/signin"))]
    [:section {:id "personal" :class "py-24 lg:py-32" :aria-labelledby "personal-h"}
     [:div {:class "grid grid-cols-1 gap-6 lg:grid-cols-12 lg:items-end"}
      [:h2 {:id "personal-h" :class (str "enter text-zinc-900 lg:col-span-7 " h2-class)} "Everything for your own money."]
      [:p {:class "enter max-w-[44ch] text-lg leading-relaxed text-zinc-600 lg:col-span-5" :style {"--d" "80ms"}}
       "Each tab opens on the same kind of card: one number, what it means, and bars that show it."]]
     [:div {:class "mt-14 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3"}
      (tile {:title "Budget" :href (go "/app/finances/")
             :line "Income, expenses and savings for each month. Items from a statement count only in their month."}
            (hero/bars {:of 21619 :rows [{:label "Expenses" :value "R12,000" :amount 12000 :cls "bg-zinc-700"}
                                         {:label "Savings" :value "R2,500" :amount 2500 :cls "bg-zinc-400"}
                                         {:label "Left" :value "R7,119" :amount 7119 :cls "bg-brand-600"}]}))
      (tile {:title "Spending" :href (go "/app/spending") :d "60ms"
             :line "What actually left your account, by month and category, with refunds and transfers set apart."}
            (hero/bars {:rows [{:label "Groceries" :value "R4,180" :amount 4180 :cls "bg-brand-600"}
                               {:label "Transport" :value "R2,240" :amount 2240 :cls "bg-brand-600"}
                               {:label "Eating out" :value "R1,310" :amount 1310 :cls "bg-brand-600"}]}))
      (tile {:title "Tax" :href (go "/app/tax") :d "120ms"
             :line "Your take-home and tax bracket, a bonus or 13th cheque, and a practice ITR12 return."}
            (hero/bars {:of 300000 :rows [{:label "Take-home" :value "R259,428" :amount 259428 :cls "bg-brand-600"}
                                          {:label "Tax" :value "R40,572" :amount 40572 :cls "bg-zinc-400"}]}))
      (tile {:title "Calendar" :href (go "/app/calendar")
             :line "Payday, bills and to-dos on one month, so nothing slips between one salary and the next."}
            (hero/bars {:of 31 :rows [{:label "Days gone" :value "15 of 31" :amount 15 :cls "bg-zinc-400"}
                                      {:label "To payday" :value "10" :amount 10 :cls "bg-brand-600"}]}))
      (tile {:title "Goals" :href (go "/app/goals") :d "60ms"
             :line "Set a target and a date. See what is left to save, and which goal falls behind."}
            (hero/bars {:of 7500 :rows [{:label "Saved" :value "R3,000" :amount 3000 :cls "bg-brand-600"}
                                        {:label "Still to save" :value "R4,500" :amount 4500 :cls "bg-zinc-400"}]}))
      (tile {:title "Insights" :href (go "/app/insights") :d "120ms"
             :line "Your savings rate against a 20% benchmark, and planned against spent for the month."}
            (hero/bars {:of 21619 :rows [{:label "Saved" :value "R2,500" :amount 2500 :cls "bg-brand-600"}
                                         {:label "20% benchmark" :value "R4,324" :amount 4324 :cls "bg-zinc-400"}]}))]]))

;; ---------------------------------------------------------------------------
;; Your business, in the same account

(defn- business [signed-in?]
  [:section {:id "business" :class "bg-zinc-950 text-white" :aria-labelledby "business-h"}
   [:div {:class "container mx-auto px-6 py-24 lg:py-32"}
    [:div {:class "grid grid-cols-1 gap-6 lg:grid-cols-12 lg:items-end"}
     [:h2 {:id "business-h" :class (str "enter text-white lg:col-span-7 " h2-class)} "Run your business from the same account."]
     [:p {:class "enter max-w-[44ch] text-lg leading-relaxed text-zinc-300 lg:col-span-5" :style {"--d" "80ms"}}
      "Add a sole proprietorship, a (Pty) Ltd, a partnership, a trust or a non-profit. It gets its own budget, statements and tax, and you switch to it in one click."]]
    [:div {:class "mt-14 grid grid-cols-1 gap-5 lg:grid-cols-3"}
     [:div {:class "enter" :style {"--d" "0ms"}}
      (m/shot {:label "VAT" :badge "Due 30 Nov" :value "R7,200" :unit "to pay"
             :reading "For September and October. The VAT201 is due on the last business day of the next month."
             :bars {:title "VAT on sales against VAT on costs"
                    :rows [{:label "On sales" :value "R11,270" :amount 11270 :cls "bg-brand-600"}
                           {:label "On costs" :value "R4,070" :amount 4070 :cls "bg-zinc-400"}]}
             :stats [["Period" "Sep–Oct"] ["Category" "B"] ["Set aside" "R7,500"]]})]
     [:div {:class "enter" :style {"--d" "80ms"}}
      (m/shot {:label "Company tax" :badge "Small business corporation" :value "R30,170" :unit "this year"
             :reading "On R420,000 taxable income, with the SBC rates. Provisional payment dates go on the calendar."
             :bars {:title "Where the profit goes" :of 420000
                    :rows [{:label "Kept" :value "R389,830" :amount 389830 :cls "bg-brand-600"}
                           {:label "Income tax" :value "R30,170" :amount 30170 :cls "bg-zinc-400"}]}
             :stats [["Effective rate" "7%"] ["Year end" "February"] ["Next payment" "31 Aug"]]})]
     [:div {:class "enter" :style {"--d" "160ms"}}
      (m/shot {:label "Owner's pay" :value "R20,000" :unit "a month"
             :reading "Paid to you from the business budget. It appears on your personal Tax page, so both sides agree."
             :bars {:title "Profit shares" :of 100
                    :rows [{:label "You" :value "60%" :amount 60 :cls "bg-brand-600"}
                           {:label "Partner" :value "40%" :amount 40 :cls "bg-zinc-400"}]}
             :stats [["Members" "3"] ["Owners" "2"] ["Invites" "1"]]})]]
    [:ul {:class "enter mt-14 grid grid-cols-1 gap-x-10 gap-y-6 text-base sm:grid-cols-2 lg:grid-cols-4" :style {"--d" "120ms"}}
     (for [[t l] [["Money in and out" "Read from the business's statements, in business categories like SARS, salaries and stock."]
                  ["A team" "Invite a partner or a bookkeeper by email. Owners manage invites and roles, members work in the budget."]
                  ["SARS dates" "VAT201 and provisional tax dates appear on the calendar by themselves."]
                  ["One sign-in" "Your personal money stays private from the business, and the other way round."]]]
       [:li
        [:p {:class "font-semibold text-white"} t]
        [:p {:class "mt-1.5 leading-relaxed text-zinc-400"} l]])]
    [:div {:class "enter mt-14" :style {"--d" "160ms"}}
     [:a {:href (if signed-in? "/app/choose?add=1#add-business" "/signup") :class btn-light}
      (if signed-in? "Add your business" "Sign up with a business")]]]])

;; ---------------------------------------------------------------------------
;; Privacy

(defn- privacy []
  [:section {:id "private" :class "py-24 lg:py-32" :aria-labelledby "private-h"}
   [:div {:class "grid grid-cols-1 gap-12 lg:grid-cols-12 lg:gap-10"}
    [:h2 {:id "private-h" :class (str "enter text-zinc-900 lg:col-span-5 " h2-class)} "Your statements are read, then let go."]
    [:dl {:class "grid grid-cols-1 gap-10 sm:grid-cols-3 lg:col-span-7"}
     (for [[i [k v]] (map-indexed vector
                                  [["Totals, not files" "greed keeps the monthly totals it needs. The PDF itself is never stored."]
                                   ["No ads, no selling" "We never sell your data, and there are no advertising trackers."]
                                   ["Profiles kept apart" "Business members see the business, never your personal budget."]])]
       [:div {:class "enter" :style {"--d" (str (* 80 i) "ms")}}
        [:dt {:class "text-lg font-semibold tracking-[-0.01em] text-zinc-900"} k]
        [:dd {:class "mt-2 text-base leading-relaxed text-zinc-600"} v]])]]])

;; ---------------------------------------------------------------------------
;; Close

(defn- action-tile [signed-in?]
  (m/action-tile
   {:id "start" :ground "bg-stone-100" :facts :none
    :heading "Plan your next payday, and your next VAT return."
    :line "Your money and your business, read from your statements and planned in one calm, private place."
    :primary {:href (if signed-in? "/app" "/signup") :label (if signed-in? "Go to your account" "Create your free account")}
    :secondary {:href (if signed-in? "/app/tax/income-tax-calculator" "#keep") :label "Try the tax calculator"}}))

;; ---------------------------------------------------------------------------
;; Page-local style and behaviour

(def ^:private page-css
  "
.salary-range{accent-color:#0F6B4F;height:28px}
.age-switch{appearance:none;-webkit-appearance:none;flex-shrink:0;width:44px;height:26px;border-radius:999px;background:#d4d4d8;position:relative;cursor:pointer;transition:background-color .2s ease-out}
.age-switch::after{content:'';position:absolute;top:3px;left:3px;width:20px;height:20px;border-radius:999px;background:#fff;transition:transform .2s cubic-bezier(.16,1,.3,1)}
.age-switch:checked{background:#0F6B4F}
.age-switch:checked::after{transform:translateX(18px)}
.age-switch:focus-visible{outline:2px solid #18181b;outline-offset:2px}
#keep-out.tick{animation:keep-tick .35s cubic-bezier(.16,1,.3,1)}
@keyframes keep-tick{from{opacity:.4;transform:translateY(6px)}to{opacity:1;transform:none}}
.pf-tab[aria-selected=true]{background:#18181b;color:#fff}
.pf-tab[aria-selected=false]:hover{color:#18181b}
.pf-panel:not([hidden]){animation:pf-in .45s cubic-bezier(.16,1,.3,1)}
@keyframes pf-in{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:none}}
html.js .enter{opacity:0;transform:translateY(24px);transition:opacity .7s cubic-bezier(.16,1,.3,1) var(--d,0ms),transform .7s cubic-bezier(.16,1,.3,1) var(--d,0ms)}
html.js .enter.in{opacity:1;transform:none}
html.js .enter:not(.in) .greed-bar-grow{animation:none;transform:scaleX(0)}
@media (prefers-reduced-motion:reduce){
  html.js .enter{opacity:1;transform:none;transition:none}
  html.js .enter:not(.in) .greed-bar-grow{transform:none}
  .pf-panel:not([hidden]){animation:none}
  #keep-out.tick{animation:none}
}
")

(def ^:private page-js
  "
(function(){
  var root=document.documentElement; root.classList.add('js');
  var reduce=window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  function rands(n){return 'R'+Math.round(n).toLocaleString('en-US');}

  // Entrances: each part rises once as its section arrives; its bars grow then.
  var io=new IntersectionObserver(function(es){es.forEach(function(e){if(e.isIntersecting){e.target.classList.add('in');io.unobserve(e.target);}});},{rootMargin:'0px 0px -8% 0px'});
  document.querySelectorAll('.enter').forEach(function(el){io.observe(el);});
  setTimeout(function(){document.querySelectorAll('.enter').forEach(function(el){el.classList.add('in');});},2500);

  // Profiles: Personal and Business, switched like the app's profile switcher.
  var tabs=[].slice.call(document.querySelectorAll('.pf-tab'));
  function show(i){
    tabs.forEach(function(t,k){var on=k===i; t.setAttribute('aria-selected',on?'true':'false'); t.tabIndex=on?0:-1;
      document.getElementById(t.getAttribute('aria-controls')).hidden=!on;});
  }
  var cur=0, held=false, timer=null;
  function hold(){held=true; clearInterval(timer);}
  tabs.forEach(function(t,k){
    t.addEventListener('click',function(){hold(); cur=k; show(k);});
    t.addEventListener('keydown',function(e){if(e.key==='ArrowRight'||e.key==='ArrowLeft'){e.preventDefault(); hold(); cur=(cur+1)%tabs.length; show(cur); tabs[cur].focus();}});
  });
  show(0);
  if(!reduce){timer=setInterval(function(){if(!held && !document.hidden){cur=(cur+1)%tabs.length; show(cur);}},5000);}

  // Tax: the headline is the calculation, on the server's own SARS tables.
  var hero=document.getElementById('keep');
  var cfg=JSON.parse(hero.getAttribute('data-tax'));
  var salary=document.getElementById('salary'), older=document.getElementById('older');
  var keepOut=document.getElementById('keep-out'), salOut=document.getElementById('salary-out');
  var taxOut=document.getElementById('tax-out'), centsOut=document.getElementById('cents-out');
  function annualTax(a,old){
    var b=null; cfg.brackets.forEach(function(x){if(a>=x[0]) b=x;});
    var g=b?b[2]+(a-b[0])*b[1]:0;
    return Math.max(0,g-cfg.primary-(old?cfg.secondary:0));
  }
  function update(){
    var m=+salary.value, t=annualTax(m*12,older.checked)/12;
    salOut.textContent=rands(m); taxOut.textContent=rands(t);
    centsOut.textContent=Math.round(t/m*100);
    var k=rands(m-t);
    if(keepOut.textContent!==k){keepOut.textContent=k; if(!reduce){keepOut.classList.remove('tick'); void keepOut.offsetWidth; keepOut.classList.add('tick');}}
    fit();
  }
  var h1=document.getElementById('keep-h');
  function fit(){
    h1.style.removeProperty('--keep-fs');
    var w=h1.clientWidth, need=Math.max(keepOut.scrollWidth, h1.firstElementChild.scrollWidth);
    if(need>w){var fs=parseFloat(getComputedStyle(h1).fontSize); h1.style.setProperty('--keep-fs', Math.floor(fs*w/need)+'px');}
  }
  window.addEventListener('resize', fit); fit();
  salary.addEventListener('input',update); older.addEventListener('change',update);

})();
")

;; ---------------------------------------------------------------------------

(defn page [{:keys [session]}]
  (let [signed-in? (some? (:uid session))
        cfg (c/get-tax-config)]
    [:div
     [:style (biff/unsafe page-css)]
     [:div {:class "container mx-auto px-6"}
      (hero-section signed-in?)
      (facts)
      (how)]
     [:div {:class "bg-stone-100"}
      [:div {:class "container mx-auto px-6"} (calculator cfg signed-in?)]]
     [:div {:class "container mx-auto px-6"} (personal-features signed-in?)]
     (business signed-in?)
     [:div {:class "container mx-auto px-6"} (privacy)]
     (action-tile signed-in?)
     [:script (biff/unsafe page-js)]]))
