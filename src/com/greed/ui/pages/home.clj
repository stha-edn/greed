(ns com.greed.ui.pages.home
  "Home page, rich and modern: a deep green hero with the app on a laptop
   and a phone, then what greed does as a bento grid of real app cards, how
   it works, the take-home calculator, the business side, the plans and a
   closing call to action. Product pictures are built from the app's own
   components (hero/bars, marketing/shot) with sample figures that add up,
   so the page always looks like the product."
  (:require [clojure.string :as string]
            [com.biffweb :as biff]
            [com.core :as c]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.tax :as tax]
            [com.greed.data.plans :as plans]
            [com.greed.ui.components.hero :as hero]
            [com.greed.ui.components.marketing :as m]
            [com.greed.ui.components.plans :as plan-cards]
            [com.greed.ui.components.svgs :as svgs]))

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
;; The app, drawn small: a laptop and a phone with real greed screens

(defn- mini-nav [active]
  [:div {:class "hidden w-36 flex-shrink-0 border-r border-zinc-200 bg-zinc-50 p-3 sm:block"}
   [:div {:class "mb-4 h-6 w-6 rounded-md bg-brand-600"}]
   (for [l ["Home" "Budget" "Spending" "Banking" "Tax" "Calendar" "Goals" "Insights"]]
     [:div {:class (str "mb-1 rounded-md px-2 py-1.5 text-[11px] font-medium "
                        (if (= l active) "bg-white text-zinc-900 shadow-card ring-1 ring-zinc-200/70" "text-zinc-500"))}
      l])])

(defn- laptop []
  [:div {:class "relative mx-auto w-full max-w-[640px]"}
   [:div {:class "rounded-t-[16px] bg-zinc-900 p-[7px] shadow-[0_40px_80px_-20px_rgba(0,0,0,.55)] ring-1 ring-white/10"}
    [:div {:class "aspect-[16/10] overflow-hidden rounded-[10px] bg-zinc-50 sm:aspect-auto"}
     [:div {:class "flex h-7 items-center gap-1.5 border-b border-zinc-200 bg-white px-3"}
      [:span {:class "h-2 w-2 rounded-full bg-rose-400"}] [:span {:class "h-2 w-2 rounded-full bg-amber-400"}] [:span {:class "h-2 w-2 rounded-full bg-brand-400"}]
      [:span {:class "mx-auto rounded-md bg-zinc-100 px-8 py-0.5 text-[10px] text-zinc-500"} "mygreed.co.za/app/finances"]]
     ;; Below sm the screen keeps its shape and the app is drawn at desktop
     ;; width, scaled down, so it doesn't reflow into a tall narrow screen.
     [:div {:class "flex w-[950px] [zoom:0.42] min-[480px]:[zoom:0.55] sm:w-auto sm:[zoom:0.66]"}
      (mini-nav "Budget")
      [:div {:class "min-w-0 flex-1 space-y-4 p-5"}
       [:p {:class "text-xl font-semibold tracking-tight text-zinc-900"} "Budget"]
       (m/shot {:label "Left over in your plan" :badge "Payday · 25 Nov" :value "R9,619" :unit "this month"
                :verdict {:text "Within your income" :tone :good}
                :bars {:title "Where your money goes" :of 21619
                       :rows [{:label "Money out" :value "R10,500" :amount 10500 :cls "bg-zinc-700"}
                              {:label "Saved" :value "R1,500" :amount 1500 :cls "bg-zinc-400"}
                              {:label "Left over" :value "R9,619" :amount 9619 :cls "bg-brand-600"}]}
                :stats [["Money in" "R21,619"] ["Money out" "R10,500"] ["Saved per R1" "7c"]]})
       [:div {:class "grid grid-cols-3 gap-3"}
        (for [[t a] [["Income" "R21,619"] ["Expenses" "R10,500"] ["Savings" "R1,500"]]]
          [:div {:class "rounded-lg bg-white p-3 ring-1 ring-zinc-200/70"}
           [:p {:class "text-[11px] font-semibold text-zinc-900"} t]
           [:p {:class "mt-2 text-[11px] tabular-nums text-zinc-500"} a]])]]]]]
   [:div {:class "relative mx-auto h-3 w-[112%] -translate-x-[5.4%] rounded-b-2xl bg-gradient-to-b from-zinc-300 to-zinc-500 shadow-[0_20px_40px_-10px_rgba(0,0,0,.6)]"}
    [:div {:class "mx-auto h-1.5 w-24 rounded-b-lg bg-zinc-400/80"}]]])

(defn- phone []
  [:div {:class "float w-[100px] rounded-[1.3rem] bg-zinc-900 p-[5px] shadow-[0_30px_60px_-15px_rgba(0,0,0,.6)] ring-1 ring-white/15 min-[480px]:w-[140px] sm:w-[210px] sm:rounded-[2.2rem] sm:p-[7px]"}
   [:div {:class "relative aspect-[9/19] overflow-hidden rounded-[1rem] bg-zinc-50 sm:aspect-auto sm:rounded-[1.8rem]"}
    [:div {:class "absolute left-1/2 top-1 z-10 h-2.5 w-9 -translate-x-1/2 rounded-full bg-zinc-900 sm:top-1.5 sm:h-4 sm:w-16"}]
    [:div {:class "w-[350px] space-y-3 p-4 pt-8 [zoom:0.26] min-[480px]:[zoom:0.37] sm:w-auto sm:[zoom:0.56]"}
     [:p {:class "text-lg font-semibold tracking-tight text-zinc-900"} "Banking"]
     (m/shot {:label "Statements" :value "4" :unit "months read"
              :verdict {:text "Up to date" :tone :good}
              :bars {:title "The last 12 months, per bank" :of 12
                     :rows [{:label "FNB" :value "4 of 12" :amount 4 :cls "bg-brand-600"}
                            {:label "Standard Bank" :value "3 of 12" :amount 3 :cls "bg-brand-600"}]}})
     (m/shot {:label "Bank fees" :value "R1,627" :unit "in 6 months"
              :bars {:rows [{:label "Monthly fee" :value "R1,140" :amount 1140 :cls "bg-zinc-500"}
                            {:label "Card fees" :value "R487" :amount 487 :cls "bg-zinc-400"}]}})]]])

(defn- devices []
  [:div {:class "relative pb-10 lg:pb-0"}
   (laptop)
   [:div {:class "absolute -bottom-6 right-0 sm:-bottom-2 sm:-right-4 lg:-bottom-10 lg:-right-8"} (phone)]
   ;; a floating notice, like the app's own
   [:div {:class "float-slow absolute -left-2 -top-6 hidden rounded-2xl bg-white/95 px-4 py-3 shadow-card-md ring-1 ring-zinc-200/70 backdrop-blur sm:block lg:-left-6"}
    [:p {:class "flex items-center gap-2 text-xs font-semibold text-zinc-900"}
     [:span {:class "flex h-6 w-6 items-center justify-center rounded-full bg-brand-50 text-brand-700"} (svgs/check {:class "h-3.5 w-3.5"})]
     "Debit order matched"]
    [:p {:class "mt-0.5 pl-8 text-[11px] text-zinc-500"} "Car finance, R3,500, already in your budget"]]])

;; ---------------------------------------------------------------------------
;; Hero

(defn- hero-section [signed-in?]
  [:section {:class "container mx-auto px-4 pt-4 sm:px-6"}
   [:div {:class "relative overflow-hidden rounded-[2rem] bg-brand-950 px-6 pb-16 pt-14 text-white sm:px-10 lg:px-14 lg:pb-24 lg:pt-20"}
    ;; light: two soft glows and a faint grid
    [:div {:aria-hidden "true" :class "pointer-events-none absolute -right-40 -top-40 h-[40rem] w-[40rem] rounded-full bg-brand-500/30 blur-[120px]"}]
    [:div {:aria-hidden "true" :class "pointer-events-none absolute -bottom-60 -left-40 h-[36rem] w-[36rem] rounded-full bg-brand-400/20 blur-[120px]"}]
    [:div {:aria-hidden "true" :class "hero-grid pointer-events-none absolute inset-0 opacity-[0.07]"}]
    [:div {:class "relative grid grid-cols-1 items-center gap-16 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)] lg:gap-14"}
     [:div
      [:a {:href "#business" :class "enter inline-flex items-center gap-2 rounded-full bg-white/10 py-1 pl-1 pr-3 text-xs font-medium text-brand-100 ring-1 ring-white/15 transition hover:bg-white/15"}
       [:span {:class "rounded-full bg-brand-400 px-2 py-0.5 font-semibold text-brand-950"} "New"]
       "Run your business in greed too" [:span {:aria-hidden "true"} "→"]]
      [:h1 {:class "enter mt-6 max-w-[12ch] [text-wrap:balance] font-semibold text-white [font-size:clamp(40px,5vw,72px)] leading-[1] tracking-[-0.045em]" :style {"--d" "60ms"}}
       "Know where every " [:span {:class "bg-gradient-to-r from-brand-200 to-brand-400 bg-clip-text text-transparent"} "rand"] " goes."]
      [:p {:class "enter mt-6 max-w-[44ch] text-lg leading-relaxed text-brand-100/80 sm:text-xl" :style {"--d" "120ms"}}
       "greed reads your bank statements, plans your month and works out what SARS takes. For your own money and your business, in one calm place."]
      [:div {:class "enter mt-9 flex flex-wrap items-center gap-3" :style {"--d" "180ms"}}
       (if signed-in?
         [:a {:href "/app" :class m/btn-white} "Go to your account" [:span {:aria-hidden "true"} "→"]]
         [:a {:href "/signup" :class m/btn-white} "Start free" [:span {:aria-hidden "true"} "→"]])
       [:a {:href "#how" :class m/btn-glass} "See how it works"]]
      [:ul {:class "enter mt-9 flex flex-wrap gap-x-6 gap-y-2 text-sm text-brand-100/80" :style {"--d" "240ms"}}
       (for [t ["Free to start" (str (plans/trial-days) "-day trial, no card") "Statements never stored"]]
         [:li {:class "flex items-center gap-2"}
          [:span {:class "flex h-4 w-4 items-center justify-center rounded-full bg-brand-400/20 text-brand-300"} (svgs/check {:class "h-3 w-3"})]
          t])]]
     [:div {:class "enter" :style {"--d" "200ms"}} (devices)]]]])

;; ---------------------------------------------------------------------------
;; Trust strip

(defn- trust []
  [:section {:class "container mx-auto px-6 py-14"}
   [:ul {:class "enter grid grid-cols-2 gap-6 lg:grid-cols-4"}
    (for [[icon t l] [[(svgs/document-text) "FNB & Standard Bank" "PDF statements, read for you"]
                      [(svgs/percent-badge) "SARS 2026/27" "Income tax, VAT and company tax"]
                      [(svgs/users) "You and your business" "One account, kept apart"]
                      [(svgs/heart) "Made in South Africa" "By a small local team"]]]
      [:li {:class "flex items-start gap-3"}
       [:span {:class "flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700 ring-1 ring-brand-600/10"} icon]
       [:span
        [:span {:class "block text-[15px] font-semibold text-zinc-900"} t]
        [:span {:class "block text-sm text-zinc-500"} l]]])]])

;; ---------------------------------------------------------------------------
;; Bento: everything for your own money

(defn- tile [{:keys [class kicker title line d]} & body]
  [:div {:class (str "enter group relative flex flex-col overflow-hidden rounded-3xl p-6 ring-1 transition duration-300 hover:-translate-y-1 hover:shadow-card-hover sm:p-8 " (or class "bg-white ring-zinc-200/70"))
         :style {"--d" (or d "0ms")}}
   (when kicker [:p {:class "text-xs font-semibold uppercase tracking-[0.08em] text-brand-700"} kicker])
   [:h3 {:class "mt-2 text-xl font-semibold tracking-[-0.02em] text-zinc-900"} title]
   [:p {:class "mt-2 max-w-[40ch] text-[15px] leading-relaxed text-zinc-600"} line]
   (into [:div {:class "mt-6 flex-1"}] body)])

(defn- bento [signed-in?]
  [:section {:id "features" :class "container mx-auto px-6 py-20 lg:py-28" :aria-labelledby "features-h"}
   (m/section-head {:kicker "Your money" :title [:span {:id "features-h"} "Everything you need, nothing you don't."]
                  :line "Each part of greed opens on one clear card: a number, what it means, and whether it's good."})
   [:div {:class "mt-14 grid grid-cols-1 gap-5 lg:grid-cols-6"}
    (tile {:class "bg-gradient-to-br from-brand-50 to-white ring-brand-600/10 lg:col-span-4" :kicker "Spending"
           :title "See where it actually went"
           :line "Upload a statement and greed sorts every payment into categories, sets transfers aside, and compares the month with your plan."}
          [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
           (m/shot {:label "October 2026" :value "R11,240" :unit "money out"
                    :verdict {:text "R760 under plan" :tone :good}
                    :bars {:title "Planned against spent"
                           :rows [{:label "Planned" :value "R12,000" :amount 12000 :cls "bg-zinc-400"}
                                  {:label "Spent" :value "R11,240" :amount 11240 :cls "bg-brand-600"}]}})
           [:div {:class "rounded-lg bg-white p-5 shadow-card-md ring-1 ring-zinc-200/70"}
            (hero/bars {:title "Where it went"
                        :rows [{:label "Groceries" :value "R4,180" :amount 4180 :cls "bg-brand-600"}
                               {:label "Transport" :value "R2,240" :amount 2240 :cls "bg-brand-500"}
                               {:label "Eating out" :value "R1,310" :amount 1310 :cls "bg-brand-400"}
                               {:label "Bank fees" :value "R245" :amount 245 :cls "bg-zinc-400"}]})]])
    (tile {:class "bg-brand-950 ring-white/10 lg:col-span-2" :d "80ms"
           :kicker [:span {:class "text-brand-300"} "Insights"]
           :title [:span {:class "text-white"} "How much you really save"]
           :line [:span {:class "text-brand-100/70"} "Every month, against a goal of 20c of every R1."]}
          [:p {:class "font-semibold tracking-[-0.04em] text-white [font-size:clamp(56px,6vw,84px)] leading-none"} "12c"]
          [:p {:class "mt-2 text-sm text-brand-100/80"} "of every R1 coming in, saved"]
          [:div {:class "mt-6 h-2 overflow-hidden rounded-full bg-white/10"}
           [:div {:class "h-full w-[60%] rounded-full bg-gradient-to-r from-brand-400 to-brand-200 greed-bar-grow"}]]
          [:p {:class "mt-2 flex justify-between text-xs text-brand-100/70"} [:span "You: 12c"] [:span "Goal: 20c"]])
    (tile {:class "bg-white ring-zinc-200/70 lg:col-span-3" :kicker "Goals" :d "0ms"
           :title "Save for what matters"
           :line "Set a target and a date. See what's left, and which goal is falling behind."}
          (hero/bars {:of 10000 :rows [{:label "Emergency fund" :value "R6,000 of R10,000" :amount 6000 :cls "bg-brand-600"}
                                       {:label "Holiday" :value "R1,200 of R5,000" :amount 2400 :cls "bg-brand-400"}]}))
    (tile {:class "bg-white ring-zinc-200/70 lg:col-span-3" :kicker "Calendar" :d "80ms"
           :title "Payday, bills and to-dos"
           :line "One month at a glance, so nothing slips between one salary and the next."}
          [:div {:class "grid grid-cols-7 gap-1 text-center text-[11px] text-zinc-500"}
           (for [n (range 1 29)]
             [:span {:class (str "rounded-md py-1.5 tabular-nums "
                                 (cond (= n 25) "bg-brand-600 font-semibold text-white"
                                       (#{1 15} n) "bg-zinc-100 font-semibold text-zinc-900"
                                       :else ""))} n])])]])

;; ---------------------------------------------------------------------------
;; How it works

(defn- how []
  [:section {:id "how" :class "bg-zinc-50 py-20 lg:py-28" :aria-labelledby "how-h"}
   [:div {:class "container mx-auto px-6"}
    (m/section-head {:kicker "How it works" :title [:span {:id "how-h"} "From statement to plan in three steps."] :center? true})
    [:ol {:class "relative mt-16 grid grid-cols-1 gap-10 md:grid-cols-3 md:gap-6"}
     [:div {:aria-hidden "true" :class "absolute left-[16%] right-[16%] top-7 hidden h-px bg-gradient-to-r from-brand-200 via-brand-400 to-brand-200 md:block"}]
     (for [[i icon t l] [[1 (svgs/document-text) "Upload a statement" "Drop in a PDF from FNB or Standard Bank. greed reads every line, then keeps only the totals."]
                         [2 (svgs/wallet) "Plan the month" "Add what comes in and goes out, or add straight from the statement. Debit orders match by reference."]
                         [3 (svgs/chart-bar) "See what's left" "Every page shows one clear number and whether it's good, from Home to Tax."]]]
       [:li {:class "enter relative text-center" :style {"--d" (str (* 90 (dec i)) "ms")}}
        [:span {:class "relative mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-white text-brand-700 shadow-card-md ring-1 ring-brand-600/15"}
         icon
         [:span {:class "absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-brand-600 text-xs font-semibold text-white"} i]]
        [:h3 {:class "mt-6 text-lg font-semibold tracking-[-0.01em] text-zinc-900"} t]
        [:p {:class "mx-auto mt-2 max-w-[32ch] text-[15px] leading-relaxed text-zinc-600"} l]])]]])

;; ---------------------------------------------------------------------------
;; The take-home calculator, live

(defn- calculator [cfg signed-in?]
  (let [{:keys [keep tax cents]} (monthly-take-home cfg sample-salary 30)]
    [:section {:id "keep" :aria-labelledby "keep-h" :class "container mx-auto px-6 py-20 lg:py-28"
               :data-tax (tax-config-json cfg)}
     [:div {:class "grid grid-cols-1 items-center gap-12 lg:grid-cols-2"}
      [:div
       (m/section-head {:kicker "Try it" :title "What do you actually take home?"
                      :line "Drag your salary. greed works it out on the SARS 2026/27 tables, the same ones the app uses."})
       [:a {:href (if signed-in? "/app/tax/income-tax-calculator" "/signup")
            :class "enter mt-8 inline-flex items-center gap-1.5 text-[15px] font-semibold text-brand-700 hover:text-brand-800"}
        "Open the full tax calculator" [:span {:aria-hidden "true"} "→"]]]
      [:div {:data-ui "" :class "enter relative overflow-hidden rounded-3xl bg-white p-6 shadow-[0_30px_60px_-30px_rgba(0,50,34,.35)] ring-1 ring-zinc-200/70 sm:p-8" :style {"--d" "80ms"}}
       [:div {:aria-hidden "true" :class "pointer-events-none absolute -right-20 -top-20 h-60 w-60 rounded-full bg-brand-100 blur-3xl"}]
       [:div {:class "relative"}
        [:p {:class "text-sm font-medium text-zinc-500"} "You keep, a month"]
        [:h2 {:id "keep-h" :class "mt-1 w-full font-semibold text-zinc-900 [font-size:var(--keep-fs,clamp(48px,6vw,80px))] leading-none tracking-[-0.045em]"}
         [:span {:class "sr-only"} "You keep "]
         [:span {:id "keep-out" :class "block whitespace-nowrap tabular-nums" :aria-live "polite"} (utilities/whole->rands keep)]]
        [:div {:class "mt-8"}
         [:label {:for "salary" :class "flex items-baseline justify-between gap-3"}
          [:span {:class "text-sm font-medium text-zinc-600"} "Monthly salary"]
          [:output {:id "salary-out" :for "salary" :class "text-lg font-semibold tabular-nums text-zinc-900"} (utilities/whole->rands sample-salary)]]
         [:input {:id "salary" :type "range" :min 5000 :max 200000 :step 500 :value sample-salary :class "salary-range mt-3 w-full"}]]
        [:div {:class "mt-6 grid grid-cols-2 gap-4"}
         [:div {:class "rounded-2xl bg-zinc-50 p-4 ring-1 ring-zinc-200/70"}
          [:p {:class "text-xs font-medium text-zinc-500"} "SARS takes"]
          [:p {:class "mt-1 text-xl font-semibold tabular-nums text-zinc-900"} [:span {:id "tax-out"} (utilities/whole->rands tax)]]
          [:p {:class "text-xs text-zinc-500"} "a month"]]
         [:div {:class "rounded-2xl bg-brand-50 p-4 ring-1 ring-brand-600/10"}
          [:p {:class "text-xs font-medium text-brand-700"} "Tax per R1"]
          [:p {:class "mt-1 text-xl font-semibold tabular-nums text-brand-900"} [:span {:id "cents-out"} cents] "c"]
          [:p {:class "text-xs text-brand-700/80"} "of every R1 you earn"]]]
        [:label {:class "mt-5 flex cursor-pointer items-center justify-between gap-3 rounded-2xl p-1"}
         [:span
          [:span {:class "block text-sm font-semibold text-zinc-900"} "65 or older"]
          [:span {:class "block text-xs text-zinc-500"} "Adds the secondary rebate"]]
         [:input {:id "older" :type "checkbox" :role "switch" :class "age-switch"}]]]]]]))

;; ---------------------------------------------------------------------------
;; Business

(defn- business [signed-in?]
  [:section {:id "business" :class "container mx-auto px-4 sm:px-6" :aria-labelledby "business-h"}
   [:div {:class "relative overflow-hidden rounded-[2rem] bg-brand-950 px-6 py-20 text-white sm:px-10 lg:px-14 lg:py-28"}
    [:div {:aria-hidden "true" :class "pointer-events-none absolute -left-40 -top-40 h-[34rem] w-[34rem] rounded-full bg-brand-500/25 blur-[120px]"}]
    [:div {:aria-hidden "true" :class "hero-grid pointer-events-none absolute inset-0 opacity-[0.06]"}]
    [:div {:class "relative"}
     (m/section-head {:kicker "For your business" :title [:span {:id "business-h"} "Your business, in the same account."] :dark? true
                    :line "Add a sole proprietorship, a (Pty) Ltd, a partnership, a trust or a non-profit. It gets its own budget, statements, VAT and company tax, and you switch to it in one click."})
     [:div {:class "mt-14 grid grid-cols-1 gap-5 lg:grid-cols-3"}
      (for [[d s] [["0ms" (m/shot {:label "VAT for Nov to Dec" :badge "Due 29 Jan" :value "R7,200" :unit "to pay"
                                   :verdict {:text "Due in 68 days" :tone :neutral}
                                   :bars {:title "VAT charged and VAT paid"
                                          :rows [{:label "On your sales" :value "R11,270" :amount 11270 :cls "bg-zinc-700"}
                                                 {:label "On your costs" :value "R4,070" :amount 4070 :cls "bg-brand-600"}]}})]
                   ["90ms" (m/shot {:label "Company tax, year to Feb 2027" :badge "Small business corporation" :value "R30,170" :unit "this year"
                                    :verdict {:text "Enough put away" :tone :good}
                                    :bars {:title "Where the profit goes" :of 420000
                                           :rows [{:label "The business keeps" :value "R389,830" :amount 389830 :cls "bg-brand-600"}
                                                  {:label "SARS" :value "R30,170" :amount 30170 :cls "bg-zinc-400"}]}})]
                   ["180ms" (m/shot {:label "Owner's pay" :value "R20,000" :unit "a month"
                                     :verdict {:text "On your personal Tax page" :tone :neutral}
                                     :bars {:title "Profit shares" :of 100
                                            :rows [{:label "You" :value "60%" :amount 60 :cls "bg-brand-600"}
                                                   {:label "Partner" :value "40%" :amount 40 :cls "bg-zinc-400"}]}})]]]
        [:div {:class "enter transition duration-300 hover:-translate-y-1" :style {"--d" d}} s])]
     [:ul {:class "enter mt-12 grid grid-cols-1 gap-6 sm:grid-cols-3"}
      (for [[t l] [["Money in and out" "From the business's own statements, in business categories."]
                   ["Invite your team" "A partner or bookkeeper works under your plan."]
                   ["SARS dates" "VAT201 and provisional tax on the calendar by themselves."]]]
        [:li {:class "border-l border-white/15 pl-4"}
         [:p {:class "font-semibold text-white"} t]
         [:p {:class "mt-1 text-sm leading-relaxed text-brand-100/70"} l]])]
     [:div {:class "enter mt-12"}
      [:a {:href (if signed-in? "/app/choose?add=1#add-business" "/signup?as=business") :class m/btn-white}
       (if signed-in? "Add your business" "Start with your business") [:span {:aria-hidden "true"} "→"]]]]]])

;; ---------------------------------------------------------------------------
;; Plans

(defn- pricing
  "A summary of the plans; the full comparison lives on /pricing."
  [signed-in?]
  [:section {:id "plans" :class "container mx-auto px-6 py-16 lg:py-20" :aria-labelledby "plans-h"}
   [:div {:class "enter flex flex-col gap-8 rounded-3xl bg-zinc-50 p-8 ring-1 ring-zinc-200/70 sm:p-10 lg:flex-row lg:items-center lg:justify-between"}
    [:div
     [:p {:class m/eyebrow} "Pricing"]
     [:h2 {:id "plans-h" :class "mt-4 max-w-[20ch] text-[clamp(26px,2.8vw,36px)] font-semibold leading-tight tracking-[-0.03em] text-zinc-900"}
      "Start free. Upgrade when it pays for itself."]
     [:p {:class "mt-3 text-[15px] text-zinc-600"} (str (plans/trial-days) "-day trial of everything, no card needed. Cancel any time.")]]
    [:div {:class "flex flex-col gap-5 sm:flex-row sm:items-center"}
     [:dl {:class "flex gap-6"}
      (for [tier plans/tiers]
        [:div
         [:dt {:class "text-xs font-medium text-zinc-500"} (plans/tier-name tier)]
         [:dd {:class "mt-1 text-2xl font-semibold tracking-[-0.02em] tabular-nums text-zinc-900"}
          (first (plan-cards/price-line tier :monthly))
          (when-not (= :free tier) [:span {:class "text-sm font-medium text-zinc-500"} "/mo"])]])]
     [:a {:href "/pricing" :class m/btn-green} "Compare plans" [:span {:aria-hidden "true"} "→"]]]]])

;; ---------------------------------------------------------------------------
;; Close

(defn- closing [signed-in?]
  [:section {:class "container mx-auto px-4 pb-20 sm:px-6"}
   [:div {:class "relative overflow-hidden rounded-[2rem] bg-gradient-to-br from-brand-600 via-brand-700 to-brand-900 px-6 py-16 text-center text-white sm:px-10 lg:py-24"}
    [:div {:aria-hidden "true" :class "pointer-events-none absolute left-1/2 top-0 h-80 w-[40rem] -translate-x-1/2 rounded-full bg-brand-300/30 blur-[100px]"}]
    [:div {:class "relative"}
     [:h2 {:class "enter mx-auto max-w-[18ch] [text-wrap:balance] font-semibold text-white [font-size:clamp(34px,4.6vw,64px)] leading-[1.02] tracking-[-0.04em]"}
      "Your next payday, already planned."]
     [:p {:class "enter mx-auto mt-5 max-w-[46ch] text-lg text-white/85" :style {"--d" "60ms"}}
      "Join greed free. Upload a statement and see your month in minutes."]
     [:div {:class "enter mt-9 flex flex-wrap justify-center gap-3" :style {"--d" "120ms"}}
      (if signed-in?
        [:a {:href "/app" :class m/btn-white} "Go to your account" [:span {:aria-hidden "true"} "→"]]
        [:a {:href "/signup" :class m/btn-white} "Create your free account" [:span {:aria-hidden "true"} "→"]])
      [:a {:href "/pricing" :class m/btn-glass} "See pricing"]]]]])

;; ---------------------------------------------------------------------------
;; Page-local style and behaviour

(def ^:private page-css
  "
.hero-grid{background-image:linear-gradient(rgba(255,255,255,.9) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,.9) 1px,transparent 1px);background-size:48px 48px;-webkit-mask-image:radial-gradient(ellipse at 70% 30%,#000 20%,transparent 70%);mask-image:radial-gradient(ellipse at 70% 30%,#000 20%,transparent 70%)}
.float{animation:float 7s ease-in-out infinite}
.float-slow{animation:float 9s ease-in-out infinite reverse}
@keyframes float{0%,100%{transform:translateY(0)}50%{transform:translateY(-10px)}}
.salary-range{accent-color:#0F6B4F;height:28px}
.age-switch{appearance:none;-webkit-appearance:none;flex-shrink:0;width:44px;height:26px;border-radius:999px;background:#d4d4d8;position:relative;cursor:pointer;transition:background-color .2s ease-out}
.age-switch::after{content:'';position:absolute;top:3px;left:3px;width:20px;height:20px;border-radius:999px;background:#fff;transition:transform .2s cubic-bezier(.16,1,.3,1)}
.age-switch:checked{background:#0F6B4F}
.age-switch:checked::after{transform:translateX(18px)}
.age-switch:focus-visible{outline:2px solid #18181b;outline-offset:2px}
#keep-out.tick{animation:keep-tick .35s cubic-bezier(.16,1,.3,1)}
@keyframes keep-tick{from{opacity:.4;transform:translateY(6px)}to{opacity:1;transform:none}}
html.js .enter{opacity:0;transform:translateY(24px);transition:opacity .8s cubic-bezier(.16,1,.3,1) var(--d,0ms),transform .8s cubic-bezier(.16,1,.3,1) var(--d,0ms)}
html.js .enter.in{opacity:1;transform:none}
html.js .enter:not(.in) .greed-bar-grow{animation:none;transform:scaleX(0)}
@media (prefers-reduced-motion:reduce){
  html.js .enter{opacity:1;transform:none;transition:none}
  html.js .enter:not(.in) .greed-bar-grow{transform:none}
  .float,.float-slow,#keep-out.tick{animation:none}
}
")

(def ^:private page-js
  "
(function(){
  var root=document.documentElement; root.classList.add('js');
  var reduce=window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  function rands(n){return 'R'+Math.round(n).toLocaleString('en-US');}

  // Entrances: each part rises once as it arrives; its bars grow then.
  var io=new IntersectionObserver(function(es){es.forEach(function(e){if(e.isIntersecting){e.target.classList.add('in');io.unobserve(e.target);}});},{rootMargin:'0px 0px -8% 0px'});
  document.querySelectorAll('.enter').forEach(function(el){io.observe(el);});
  setTimeout(function(){document.querySelectorAll('.enter').forEach(function(el){el.classList.add('in');});},2500);

  // The take-home calculator, on the server's own SARS tables.
  var box=document.getElementById('keep');
  var cfg=JSON.parse(box.getAttribute('data-tax'));
  var salary=document.getElementById('salary'), older=document.getElementById('older');
  var keepOut=document.getElementById('keep-out'), salOut=document.getElementById('salary-out');
  var taxOut=document.getElementById('tax-out'), centsOut=document.getElementById('cents-out');
  function annualTax(a,old){
    var b=null; cfg.brackets.forEach(function(x){if(a>=x[0]) b=x;});
    var g=b?b[2]+(a-b[0])*b[1]:0;
    return Math.max(0,g-cfg.primary-(old?cfg.secondary:0));
  }
  var h=document.getElementById('keep-h');
  function fit(){
    h.style.removeProperty('--keep-fs');
    var w=h.clientWidth, need=keepOut.scrollWidth;
    if(need>w){var fs=parseFloat(getComputedStyle(h).fontSize); h.style.setProperty('--keep-fs', Math.floor(fs*w/need)+'px');}
  }
  function update(){
    var m=+salary.value, t=annualTax(m*12,older.checked)/12;
    salOut.textContent=rands(m); taxOut.textContent=rands(t);
    centsOut.textContent=Math.round(t/m*100);
    var k=rands(m-t);
    if(keepOut.textContent!==k){keepOut.textContent=k; if(!reduce){keepOut.classList.remove('tick'); void keepOut.offsetWidth; keepOut.classList.add('tick');}}
    fit();
  }
  window.addEventListener('resize', fit); fit();
  salary.addEventListener('input',update); older.addEventListener('change',update);
})();
")

;; ---------------------------------------------------------------------------

(defn page [{:keys [session]}]
  (let [signed-in? (some? (:uid session))
        cfg (c/get-tax-config)]
    [:div {:class "bg-white"}
     [:style (biff/unsafe page-css)]
     (hero-section signed-in?)
     (trust)
     (bento signed-in?)
     (how)
     (calculator cfg signed-in?)
     (business signed-in?)
     (pricing signed-in?)
     (closing signed-in?)
     [:script (biff/unsafe page-js)]]))
