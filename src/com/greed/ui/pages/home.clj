(ns com.greed.ui.pages.home
  "Home page, built to the taste-skill direction 'The product performs'
   (seed 1129384765). The product does its job in front of the visitor:
   the hero headline is a live take-home calculation, the showcase is a
   row of real interface fragments, and the capability list is the
   product's own command menu. Interface fragments carry data-ui."
  (:require [clojure.string :as string]
            [com.biffweb :as biff]
            [com.core :as c]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.tax :as tax]
            [com.greed.ui.components.illustrations :as ill]
            [com.greed.ui.components.marketing :as m]))

;; ---------------------------------------------------------------------------
;; Data

(def ^:private sample-salary
  "Monthly salary the page opens on. Matches the sample fragments below."
  52500)

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

(def ^:private btn-primary
  "inline-flex items-center justify-center rounded-md bg-brand-600 px-6 py-3 text-base font-semibold text-white transition-[transform,background-color] duration-150 ease-out hover:bg-brand-700 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:ring-offset-2")

(def ^:private btn-dark
  "inline-flex items-center justify-center rounded-md bg-zinc-900 px-6 py-3 text-base font-semibold text-white transition-[transform,background-color] duration-150 ease-out hover:bg-zinc-700 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 focus-visible:ring-offset-2 focus-visible:ring-offset-stone-100")

(defn- arrow-link [href label]
  [:a {:href href
       :class "group inline-flex items-center gap-1.5 text-base font-medium text-zinc-900 underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 rounded-sm"}
   label
   [:span {:aria-hidden "true" :class "transition-transform duration-150 ease-out group-hover:translate-x-0.5"} "→"]])

(defn- row [label value & [cls]]
  [:div {:class "flex items-center justify-between gap-4 py-2.5"}
   [:span {:class "text-sm text-zinc-600"} label]
   [:span {:class (str "text-sm font-semibold tabular-nums " (or cls "text-zinc-900"))} value]])

;; ---------------------------------------------------------------------------
;; Hero: the headline is the calculation

(defn- hero [cfg signed-in?]
  (let [{:keys [keep tax cents]} (monthly-take-home cfg sample-salary 30)]
    [:section {:id "keep" :class "relative pb-20 pt-14 sm:pt-20 lg:pb-28 lg:pt-24"
               :data-tax (tax-config-json cfg)}
     ;; On large screens: controls in the side columns, the headline in the
     ;; middle column, so a longer figure can never run under a card.
     [:div {:class "mx-auto grid max-w-7xl grid-cols-1 justify-items-center gap-6 lg:grid-cols-[260px_minmax(0,1fr)_260px] lg:grid-rows-[auto_auto] lg:items-center lg:gap-x-8 lg:gap-y-6"}
      ;; Salary chip: left, beside the first line
      [:div {:data-ui "" :class "enter chip chip-a w-full max-w-[300px] rounded-md border border-zinc-300 bg-white p-5 lg:col-start-1 lg:row-start-1 lg:self-start lg:max-w-none"}
       [:label {:for "salary" :class "flex items-baseline justify-between gap-3"}
        [:span {:class "text-[15px] font-medium text-zinc-600"} "Monthly salary"]
        [:output {:id "salary-out" :for "salary" :class "text-xl font-semibold tabular-nums text-zinc-900"}
         (utilities/whole->rands sample-salary)]]
       [:input {:id "salary" :type "range" :min 5000 :max 200000 :step 500 :value sample-salary
                :class "salary-range mt-3 w-full"}]]
      [:h1 {:id "keep-h" :class "enter w-full text-center font-semibold text-zinc-900 [font-size:var(--keep-fs,clamp(72px,10vw,176px))] leading-[0.95] tracking-[-0.05em] lg:col-start-2 lg:row-span-2 lg:row-start-1 lg:py-6"
            :style {"--d" "60ms"}}
       [:span {:class "block whitespace-nowrap"} "You keep"]
       [:span {:id "keep-out" :class "block whitespace-nowrap tabular-nums" :aria-live "polite"}
        (utilities/whole->rands keep)]]
      ;; SARS chip: left, beside the second line
      [:div {:data-ui "" :class "enter chip chip-b w-full max-w-[300px] rounded-md border border-zinc-300 bg-white p-5 lg:col-start-1 lg:row-start-2 lg:self-end lg:max-w-none"
             :style {"--d" "120ms"}}
       [:p {:class "text-[15px] font-medium text-zinc-600"} "SARS takes"]
       [:p {:class "mt-1 text-xl font-semibold tabular-nums text-zinc-900"}
        [:span {:id "tax-out"} (utilities/whole->rands tax)] " a month"]
       [:p {:class "mt-0.5 text-sm tabular-nums text-zinc-600"}
        [:span {:id "cents-out"} cents] "c of every R1"]]
      ;; Age switch: right, beside the second line
      [:div {:data-ui "" :class "enter chip chip-c w-full max-w-[300px] rounded-md border border-zinc-300 bg-white p-5 lg:col-start-3 lg:row-start-2 lg:self-end lg:max-w-none"
             :style {"--d" "180ms"}}
       [:label {:class "flex cursor-pointer items-center justify-between gap-3"}
        [:span
         [:span {:class "block text-[15px] font-semibold text-zinc-900"} "65 or older"]
         [:span {:class "block text-sm text-zinc-600"} "Adds the secondary rebate"]]
        [:input {:id "older" :type "checkbox" :role "switch" :class "age-switch"}]]]]
     [:div {:class "enter mx-auto mt-10 max-w-[40ch] text-center lg:mt-16" :style {"--d" "240ms"}}
      [:p {:class "text-lg leading-relaxed text-zinc-700"}
       "Your take-home on SARS 2026/27 tables. Drag your salary, then let greed plan the rest with you: budget, goals and payday."]
      [:div {:class "mt-8 flex flex-wrap items-center justify-center gap-x-6 gap-y-4"}
       (if signed-in?
         [:a {:href "/app" :class btn-dark} "Go to dashboard"]
         [:a {:href "/signup" :class btn-dark} "Create your free account"])
       [:a {:href (if signed-in? "/app/tax/income-tax-calculator" "/signin")
            :class "group inline-flex items-center gap-1.5 rounded-sm text-base font-medium text-zinc-900 underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"}
        "Open the full tax calculator"
        [:span {:aria-hidden "true" :class "transition-transform duration-150 ease-out group-hover:translate-x-0.5"} "→"]]]]]))

;; ---------------------------------------------------------------------------
;; Showcase: lit panel, dark row. Each panel is a cropped piece of the app,
;; with sample data that adds up (R52,500 salary, see the hero).

(defn- panel [{:keys [id name line]} & body]
  [:article {:data-ui "" :data-name name :data-line line :id id
             :class "panel snap-center shrink-0 w-[min(86vw,400px)] rounded-lg bg-white p-6 text-zinc-900"}
   body])

(defn- frag-month []
  (panel {:id "p-month" :name "Your month"
          :line "One number to plan by: what is left after expenses and savings, and how far you are from payday."}
   [:p {:class "text-sm font-medium text-zinc-500"} "Your month"]
   [:p {:class "mt-3 flex items-baseline gap-2"}
    [:span {:class "text-4xl font-semibold tracking-tight tabular-nums"} "R23,024"]
    [:span {:class "text-sm text-zinc-500"} "left to plan"]]
   [:p {:class "mt-1 text-sm text-zinc-500"} "On track: spending is under income so far."]
   [:div {:class "mt-5 h-1.5 w-full overflow-hidden rounded-full bg-zinc-100"}
    [:div {:class "h-full rounded-full bg-brand-600" :style {:width "19%"}}]]
   [:p {:class "mt-2 text-sm text-zinc-500"} "Day 6 of 32 · payday in 26 days"]
   [:div {:class "mt-5 grid grid-cols-3 gap-3 border-t border-zinc-100 pt-4"} ; taste-skill-ignore: rule inside a data-ui fragment
    (for [[l v] [["Savings rate" "9%"] ["Income" "R40,524"] ["Payday" "25th"]]]
      [:div [:p {:class "text-xs text-zinc-500"} l] [:p {:class "mt-1 text-sm font-semibold tabular-nums"} v]])]))

(defn- frag-budget []
  (panel {:id "p-budget" :name "Budget"
          :line "Every rand gets a job. Your salary flows in from settings; add rent, the car and what you save."}
   [:div {:class "flex items-center justify-between"}
    [:p {:class "text-sm font-semibold"} "Expenses"]
    [:span {:class "rounded-md border border-zinc-200 px-2.5 py-1 text-xs font-medium text-zinc-700"} "+ Add item"]]
   [:div {:class "mt-3 divide-y divide-zinc-100"} ; taste-skill-ignore: rule inside a data-ui fragment
    (row "Rent" "R8,000")
    [:div {:class "-mx-2 rounded-md bg-zinc-100 px-2"} (row "Car payment" "R6,000")]
    (row "TFSA (savings)" "R3,500")]
   [:div {:class "mt-3 border-t border-zinc-200 pt-3"} ; taste-skill-ignore: rule inside a data-ui fragment
    (row "Salary after tax" "R40,524")
    (row "Left to plan" "R23,024" "text-brand-700")]))

(defn- frag-bracket []
  (panel {:id "p-bracket" :name "Your tax bracket"
          :line "See your marginal rate, and how far your income is from the next SARS bracket."}
   [:div {:class "flex items-start justify-between"}
    [:p {:class "text-sm font-semibold"} "Your tax bracket"]
    [:p {:class "text-right"} [:span {:class "block text-2xl font-semibold tabular-nums"} "36%"]
     [:span {:class "text-xs text-zinc-500"} "Marginal rate"]]]
   [:div {:class "mt-3 h-1.5 w-full overflow-hidden rounded-full bg-zinc-100"}
    [:div {:class "h-full rounded-full bg-brand-600" :style {:width "60%"}}]]
   [:p {:class "mt-2 text-sm tabular-nums text-zinc-600"} "R65,800 until the 39% bracket"]
   [:div {:class "mt-3 divide-y divide-zinc-100 text-sm tabular-nums"} ; taste-skill-ignore: rule inside a data-ui fragment
    (for [[r p active?] [["R383,100 – R530,199" "31%" false]
                         ["R530,200 – R695,799" "36%" true]
                         ["R695,800 – R886,999" "39%" false]]]
      [:div {:class (str "flex justify-between py-2.5 " (when active? "-mx-2 rounded-md bg-zinc-100 px-2 font-semibold"))}
       [:span r] [:span p]])]))

(defn- frag-goals []
  (panel {:id "p-goals" :name "Goals"
          :line "Set a target and a date. greed shows what is left to save, and tells you when one falls behind."}
   (for [[t date saved target pct over?] [["Emergency fund" "By 1 Sep 2026" "R1,000" "R2,500" 40 true]
                                          ["Holiday" "By 1 Dec 2026" "R0" "R5,000" 0 false]]]
     [:div {:class "py-3 first:pt-0"}
      [:div {:class "flex items-center justify-between gap-3"}
       [:p {:class "text-sm font-semibold"} t]
       (when over? [:span {:class "rounded-full bg-rose-50 px-2 py-0.5 text-xs font-medium text-rose-700"} "Overdue"])]
      [:p {:class "text-xs text-zinc-500"} date]
      [:p {:class "mt-3 flex items-baseline justify-between text-sm tabular-nums"}
       [:span {:class "text-lg font-semibold"} saved] [:span {:class "text-zinc-500"} (str "of " target)]]
      [:div {:class "mt-2 h-1.5 w-full overflow-hidden rounded-full bg-zinc-100"}
       [:div {:class "h-full rounded-full bg-brand-600" :style {:width (str pct "%")}}]]
      [:p {:class "mt-1.5 text-xs tabular-nums text-zinc-600"} (str pct "% funded")]])))

(defn- frag-calendar []
  ;; November 2026 starts on a Sunday.
  (panel {:id "p-cal" :name "Payday calendar"
          :line "Payday, bills and to-dos on one month, so nothing slips between one salary and the next."}
   [:div {:class "flex items-center justify-between"}
    [:p {:class "text-base font-semibold"} "November " [:span {:class "font-normal text-zinc-500"} "2026"]]
    [:div {:class "flex gap-3 text-xs text-zinc-600"}
     [:span {:class "flex items-center gap-1"} [:span {:class "h-1.5 w-1.5 rounded-full bg-brand-600"}] "Payday"]
     [:span {:class "flex items-center gap-1"} [:span {:class "h-1.5 w-1.5 rounded-full bg-zinc-700"}] "Bill"]]]
   [:div {:class "mt-4 grid grid-cols-7 gap-y-1 text-center text-xs text-zinc-500"}
    (for [d ["S" "M" "T" "W" "T" "F" "S"]] [:span {:class "py-1"} d])
    (for [n (range 1 31)]
      [:span {:class (str "relative flex h-9 flex-col items-center justify-center rounded-md text-sm tabular-nums "
                          (cond (= n 25) "bg-brand-600 font-semibold text-white"
                                :else "text-zinc-800"))}
       n
       (when (#{1 15} n) [:span {:class "absolute bottom-1 h-1 w-1 rounded-full bg-zinc-700"}])])]
   [:p {:class "mt-3 text-sm text-zinc-600"} "Rent on the 1st · car on the 15th · salary on the 25th"]))

(defn- frag-bonus []
  (panel {:id "p-bonus" :name "Bonus tax"
          :line "Know what a bonus or 13th cheque really adds before it lands, taxed at your marginal rate."}
   [:p {:class "text-sm font-semibold"} "13th cheque"]
   [:div {:class "mt-3 divide-y divide-zinc-100"} ; taste-skill-ignore: rule inside a data-ui fragment
    (row "Bonus" "R52,500")
    (row "PAYE at 36%" "R18,900")]
   [:div {:class "mt-3 rounded-md bg-zinc-100 px-4 py-3"}
    [:p {:class "text-sm text-zinc-600"} "Lands in your account"]
    [:p {:class "mt-1 text-2xl font-semibold tabular-nums text-brand-700"} "R33,600"]]))

(defn- showcase []
  [:section {:id "inside" :class "showcase relative bg-zinc-950 py-20 text-white lg:py-28" :aria-labelledby "inside-h"}
   [:div {:class "container mx-auto px-6"}
    [:h2 {:id "inside-h" :class "enter text-2xl font-semibold tracking-tight text-white"} "Inside your account"]]
   [:div {:class "relative mt-10 lg:grid lg:grid-cols-[minmax(0,1fr)_24ch] lg:items-start lg:gap-10 lg:pr-10"}
    [:div {:id "shelf" :tabindex "0" :aria-label "Account screens, scroll sideways"
           :class "shelf flex snap-x snap-mandatory items-center gap-[3vw] lg:gap-[6vw] overflow-x-auto pb-6 [scrollbar-width:none] focus-visible:outline-none"}
     (frag-month) (frag-budget) (frag-bracket) (frag-goals) (frag-calendar) (frag-bonus)]
    [:div {:class "mt-2 px-6 lg:mt-0 lg:px-0" :aria-live "polite"}
     [:h3 {:id "shelf-name" :class "text-2xl font-light text-white"} "Your month"]
     [:p {:id "shelf-line" :class "mt-3 text-base leading-relaxed text-zinc-300"}
      "One number to plan by: what is left after expenses and savings, and how far you are from payday."]
     [:div {:class "mt-6 flex gap-2"}
      [:button {:type "button" :id "shelf-prev" :aria-label "Previous screen"
                :class "rounded-md bg-zinc-800 px-3 py-2 text-base text-white transition-[transform,background-color] hover:bg-zinc-700 active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white"} "←"]
      [:button {:type "button" :id "shelf-next" :aria-label "Next screen"
                :class "rounded-md bg-zinc-800 px-3 py-2 text-base text-white transition-[transform,background-color] hover:bg-zinc-700 active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white"} "→"]]]]])

;; ---------------------------------------------------------------------------
;; Features: the product's own command menu, scaled up

(defn- commands [signed-in?]
  (let [go (fn [href] (if signed-in? href "/signin"))]
    [{:name "Work out my income tax" :href (go "/app/tax/income-tax-calculator")
      :line "Enter a salary and age. See tax, rebates, your effective and marginal rate, and take-home pay."}
     {:name "Simulate my ITR12 return" :href (go "/app/tax/tax-returns")
      :line "Add medical aid, a retirement annuity and travel allowance, and see if SARS owes you a refund."}
     {:name "Tax my bonus" :href (go "/app/tax/bonus-tax-calculator")
      :line "See the PAYE on a bonus or 13th cheque, and what lands in your account."}
     {:name "Plan this month" :href (go "/app/finances/")
      :line "Income, expenses and savings in one budget, with what is left to spend each day until payday."}
     {:name "Save towards a goal" :href (go "/app/goals")
      :line "Set a target and a date, add to it as you go, and see when a goal falls behind."}
     {:name "Mark payday and bills" :href (go "/app/calendar")
      :line "A month of paydays, bills and to-dos, so nothing slips between salaries."}
     {:name "Read my savings rate" :href (go "/app/insights")
      :line "Your savings rate against a 20% benchmark, and where every rand went this month."}]))

(defn- command-list [signed-in?]
  [:section {:id "do" :class "grid grid-cols-1 gap-10 py-24 lg:grid-cols-12 lg:py-40" :aria-labelledby "do-h"}
   [:div {:class "enter lg:col-span-4"}
    [:h2 {:id "do-h" :class "text-[28px] font-semibold leading-tight tracking-[-0.02em] text-zinc-900"} "What greed does"]
    [:p {:class "mt-4 max-w-[34ch] text-[17px] leading-relaxed text-zinc-700"}
     "Type what you want to do, or pick a line. Enter opens it."]]
   [:div {:data-ui "" :class "cmd enter lg:col-span-7 lg:col-start-6" :style {"--d" "80ms"}}
    [:label {:for "cmd-q" :class "sr-only"} "What do you want to do?"]
    [:div {:class "relative"}
     [:svg {:class "pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-zinc-500" :fill "none" :stroke "currentColor" :stroke-width "2" :viewBox "0 0 24 24" :aria-hidden "true"}
      [:circle {:cx "11" :cy "11" :r "7"}]
      [:path {:stroke-linecap "round" :d "m20 20-3.5-3.5"}]]
     [:input {:id "cmd-q" :type "search" :autocomplete "off" :placeholder "What do you want to do?"
              :role "combobox" :aria-controls "cmd-list" :aria-expanded "true"
              :class "block h-14 w-full rounded-md border border-zinc-300 bg-white pl-12 pr-4 text-lg text-zinc-900 placeholder:text-zinc-500 transition-colors focus:border-zinc-900 focus:outline-none focus:ring-0"}]]
    [:ul {:id "cmd-list" :role "listbox" :class "mt-3"}
     (for [[i {:keys [name href line]}] (map-indexed vector (commands signed-in?))]
       [:li {:role "option" :aria-selected (str (zero? i)) :data-href href
             :class (str "cmd-row cursor-pointer rounded-md px-4 py-3 text-zinc-900" (when (zero? i) " is-sel"))}
        [:a {:href href :tabindex "-1" :class "flex items-baseline justify-between gap-4 focus:outline-none"}
         [:span {:class "font-semibold tracking-[-0.02em] [font-size:clamp(20px,2vw,28px)] leading-snug"} name]
         [:span {:class "cmd-go hidden shrink-0 text-base font-medium"} "Open →"]]
        [:p {:class "cmd-line hidden mt-1 max-w-[56ch] text-base leading-relaxed"} line]])]
    [:p {:id "cmd-none" :class "mt-4 hidden px-4 text-base text-zinc-700"} "Nothing matches. Try “tax”, “budget” or “goal”."]]])

;; ---------------------------------------------------------------------------
;; About: a letter

(defn- letter []
  [:section {:id "why" :class "grid grid-cols-1 items-center gap-12 py-24 lg:grid-cols-12 lg:gap-10 lg:py-32" :aria-labelledby "why-h"}
   [:div {:class "enter hidden lg:col-span-5 lg:flex lg:justify-start"}
    (ill/payslip :class "w-full max-w-[340px] -rotate-[2deg]")]
   [:div {:class "max-w-[560px] lg:col-span-6 lg:col-start-7"}
    [:div {:class "enter flex items-center gap-3"}
     [:img {:src "/img/IMG_3777.webp" :alt "Sithabiso Makhathini" :width 44 :height 44 :loading "lazy"
            :class "h-11 w-11 rounded-full object-cover object-top"}]
     [:h2 {:id "why-h" :class "text-2xl font-semibold tracking-tight text-zinc-900"} "Why we built greed"]]
    [:div {:class "enter mt-7 space-y-6 text-[17px] leading-[1.55] text-zinc-800" :style {"--d" "80ms"}}
     [:p "Money is private, and so is ambition. We built greed to take your financial life off the messy back of your mind and put it in "
      [:mark {:class "rounded-sm bg-brand-100 text-zinc-900"} "one clear place"]
      ": salary, tax, spending, savings and the dates that matter."]
     [:p "To us, greed does not mean taking without limits. It means wanting more: more security, more ownership, more choice, with a plan you can actually see."]
     [:p "There is no judgement and no jargon here, and no promise of instant wealth. Your information stays yours. We will never sell your data."]
     [:p "We are a small team in South Africa, and we read every message ourselves. Write to us at "
      [:a {:href "mailto:support@mygreed.co.za" :class "font-medium text-zinc-900 underline underline-offset-4 hover:text-zinc-600"} "support@mygreed.co.za"] "."]]
    [:div {:class "enter mt-10" :style {"--d" "140ms"}}
     [:p {:class "text-[22px] italic text-zinc-900"} "Sithabiso Makhathini"]
     [:p {:class "mt-1 text-sm text-zinc-600"} "Founder & CEO"]]]])

;; ---------------------------------------------------------------------------
;; CTA: the action tile

(defn- action-tile [signed-in?]
  (m/action-tile
   {:id "start" :ground "bg-white"
    :heading "Plan your next payday"
    :line "Your salary, what SARS takes and the plan for the rest, in one calm, private place."
    :primary {:href (if signed-in? "/app" "/signup") :label (if signed-in? "Go to dashboard" "Create your free account")}
    :secondary {:href (if signed-in? "/app/tax/income-tax-calculator" "/signin") :label "Try the tax calculator"}}))

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
@media (min-width:1024px){.chip-a{rotate:-3deg}.chip-b{rotate:2deg}.chip-c{rotate:-2deg}}
.shelf{padding-inline:calc(50% - min(43vw,200px))}
.shelf::-webkit-scrollbar{display:none}
.shelf .panel{transition:filter .9s ease, transform .9s cubic-bezier(.16,1,.3,1)}
.shelf .panel:not(.is-lit){filter:invert(.9) hue-rotate(180deg);transform:scale(.96);cursor:pointer}
@media (min-width:1024px){.shelf{padding-inline:calc(50% - 200px)}}
.cmd-row{transition:background-color .15s ease-out,color .15s ease-out}
.cmd-row.is-sel{background:#0F6B4F;color:#fff}
.cmd-row.is-sel .cmd-line,.cmd-row.is-sel .cmd-go{display:block}
.cmd-row:not(.is-sel):hover{background:#f4f4f5}
html.js .enter{opacity:0;transform:translateY(24px);transition:opacity .7s cubic-bezier(.16,1,.3,1) var(--d,0ms),transform .7s cubic-bezier(.16,1,.3,1) var(--d,0ms)}
html.js .enter.chip{transform:translateY(24px)}
html.js .enter.in{opacity:1;transform:none}
#keep-out.tick{animation:keep-tick .35s cubic-bezier(.16,1,.3,1)}
@keyframes keep-tick{from{opacity:.4;transform:translateY(6px)}to{opacity:1;transform:none}}
@media (prefers-reduced-motion:reduce){
  html.js .enter{opacity:1;transform:none;transition:none}
  .shelf .panel{transition:none}
  #keep-out.tick{animation:none}
}
")

(def ^:private page-js
  "
(function(){
  var root=document.documentElement; root.classList.add('js');
  var reduce=window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  function rands(n){return 'R'+Math.round(n).toLocaleString('en-US');}

  // Entrances: each part rises once as its section arrives.
  var io=new IntersectionObserver(function(es){es.forEach(function(e){if(e.isIntersecting){e.target.classList.add('in');io.unobserve(e.target);}});},{rootMargin:'0px 0px -8% 0px'});
  document.querySelectorAll('.enter').forEach(function(el){io.observe(el);});
  setTimeout(function(){document.querySelectorAll('.enter').forEach(function(el){el.classList.add('in');});},2500);

  // Hero: the headline is the calculation, on the server's own SARS tables.
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

  // Showcase: the panel at the centre is lit; the caption follows it.
  var shelf=document.getElementById('shelf');
  var panels=[].slice.call(shelf.querySelectorAll('.panel'));
  var nameEl=document.getElementById('shelf-name'), lineEl=document.getElementById('shelf-line');
  var lit=-1;
  function light(i){
    if(i===lit) return; lit=i;
    panels.forEach(function(p,k){p.classList.toggle('is-lit',k===i); if(k===i){p.removeAttribute('inert');}else{p.setAttribute('inert','');}});
    nameEl.textContent=panels[i].getAttribute('data-name'); lineEl.textContent=panels[i].getAttribute('data-line');
  }
  function nearest(){
    var c=shelf.getBoundingClientRect(), mid=c.left+c.width/2, best=0, d=1e9;
    panels.forEach(function(p,k){var r=p.getBoundingClientRect(), dd=Math.abs(r.left+r.width/2-mid); if(dd<d){d=dd;best=k;}});
    return best;
  }
  function go(i){i=(i+panels.length)%panels.length; panels[i].scrollIntoView({behavior:reduce?'auto':'smooth',block:'nearest',inline:'center'});}
  var raf=0; shelf.addEventListener('scroll',function(){cancelAnimationFrame(raf); raf=requestAnimationFrame(function(){light(nearest());});},{passive:true});
  panels.forEach(function(p,k){p.addEventListener('click',function(){if(k!==lit){go(k);}});});
  document.getElementById('shelf-prev').addEventListener('click',function(){hold(); go(lit-1);});
  document.getElementById('shelf-next').addEventListener('click',function(){hold(); go(lit+1);});
  shelf.addEventListener('keydown',function(e){if(e.key==='ArrowRight'){e.preventDefault();hold();go(lit+1);} if(e.key==='ArrowLeft'){e.preventDefault();hold();go(lit-1);}});
  light(0);
  // Signature move: a slow change of the lit panel, like light shifting.
  var held=false, timer=null;
  function hold(){held=true; clearInterval(timer);}
  ['pointerdown','wheel','touchstart','focusin'].forEach(function(t){shelf.addEventListener(t,hold,{passive:true});});
  if(!reduce){
    var vis=false;
    new IntersectionObserver(function(es){vis=es[0].isIntersecting;}).observe(shelf);
    timer=setInterval(function(){if(!held && vis && !document.hidden){go(lit+1);}},6000);
  }

  // Command list: type to filter, arrows or hover to select, Enter to open.
  var q=document.getElementById('cmd-q'), rows=[].slice.call(document.querySelectorAll('.cmd-row')), none=document.getElementById('cmd-none');
  var sel=0;
  function shown(){return rows.filter(function(r){return !r.hidden;});}
  function pick(r){rows.forEach(function(x){var on=x===r; x.classList.toggle('is-sel',on); x.setAttribute('aria-selected',on?'true':'false');});}
  q.addEventListener('input',function(){
    var v=q.value.trim().toLowerCase();
    rows.forEach(function(r){r.hidden=v!=='' && (r.textContent.toLowerCase().indexOf(v)<0);});
    var s=shown(); none.classList.toggle('hidden',s.length>0); if(s.length){sel=0; pick(s[0]);}
  });
  q.addEventListener('keydown',function(e){
    var s=shown(); if(!s.length) return;
    if(e.key==='ArrowDown'){e.preventDefault(); sel=Math.min(sel+1,s.length-1); pick(s[sel]);}
    if(e.key==='ArrowUp'){e.preventDefault(); sel=Math.max(sel-1,0); pick(s[sel]);}
    if(e.key==='Enter'){e.preventDefault(); window.location.href=s[sel].getAttribute('data-href');}
  });
  rows.forEach(function(r){
    r.addEventListener('pointerenter',function(){if(window.matchMedia('(hover: hover)').matches){sel=shown().indexOf(r); pick(r);}});
    r.addEventListener('click',function(e){if(!r.classList.contains('is-sel')){e.preventDefault(); sel=shown().indexOf(r); pick(r);}});
  });
})();
")

;; ---------------------------------------------------------------------------

(defn page [{:keys [session]}]
  (let [signed-in? (some? (:uid session))
        cfg (c/get-tax-config)]
    [:div
     [:style (biff/unsafe page-css)]
     [:div {:class "bg-stone-100"} [:div {:class "container mx-auto px-6"} (hero cfg signed-in?)]]
     (showcase)
     [:div {:class "container mx-auto px-6"}
      (command-list signed-in?)]
     [:div {:class "bg-stone-100"}
      [:div {:class "container mx-auto px-6"} (letter)]]
     (action-tile signed-in?)
     [:script (biff/unsafe page-js)]]))
