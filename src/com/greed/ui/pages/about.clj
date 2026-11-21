(ns com.greed.ui.pages.about
  (:require [com.biffweb :as biff]
            [com.greed.ui.components.marketing :as m]))

;; Small, true pieces of the app beside each claim, in the same header-card
;; style as the app (sample salary R25,000; sample business Mosa Studio).

(defn- frag-earn []
  [:div {:class "w-full max-w-[440px]"}
   (m/shot {:label "Your tax year" :badge "2026/27" :value "R259,428" :unit "take-home this year"
            :reading "Of every R1 you earn, 14c goes to SARS."
            :bars {:title "Where your income goes" :of 300000
                   :rows [{:label "Take-home" :value "R259,428" :amount 259428 :cls "bg-brand-600"}
                          {:label "Tax" :value "R40,572" :amount 40572 :cls "bg-zinc-400"}]}})])

(defn- frag-spent []
  [:div {:class "w-full max-w-[440px]"}
   (m/shot {:label "October 2026" :value "R11,240" :unit "spent"
            :reading "R760 less than the plan, read from your FNB statement."
            :bars {:title "Where it went"
                   :rows [{:label "Groceries" :value "R4,180" :amount 4180 :cls "bg-brand-600"}
                          {:label "Transport" :value "R2,240" :amount 2240 :cls "bg-brand-600"}
                          {:label "Eating out" :value "R1,310" :amount 1310 :cls "bg-brand-600"}]}})])

(defn- frag-plan []
  [:div {:data-ui "" :class "w-full max-w-[400px] divide-y divide-zinc-100 rounded-lg bg-white px-5 py-2 shadow-card-md ring-1 ring-zinc-200/70"} ; taste-skill-ignore: rows inside a data-ui fragment
   (m/item-row "Car finance" "R3,500" "Monthly" "WESBANK")
   (m/item-row "Rent" "R7,000" "Monthly")
   (m/item-row "Emergency fund" "R1,000" "Monthly")
   (m/item-row "Plumber" "R1,150" "From Oct 2026 statement")])

(defn- frag-business []
  [:div {:class "w-full max-w-[440px]"}
   (m/shot {:label "VAT" :badge "Mosa Studio (Pty) Ltd" :value "R7,200" :unit "to pay"
            :reading "For September and October, due 30 November."
            :bars {:title "VAT on sales against VAT on costs"
                   :rows [{:label "On sales" :value "R11,270" :amount 11270 :cls "bg-brand-600"}
                          {:label "On costs" :value "R4,070" :amount 4070 :cls "bg-zinc-400"}]}
            :stats [["Company tax" "R30,170"] ["Owner's pay" "R20,000"] ["Team" "3 people"]]})])

(defn- claim-row [{:keys [title body d]} fragment & {:keys [flip?]}]
  [:div {:class "grid grid-cols-1 items-center gap-8 py-10 lg:grid-cols-12 lg:gap-10 lg:py-14"}
   [:div {:class (str "enter lg:col-span-5 " (when flip? "lg:order-2 lg:col-start-8"))}
    [:h3 {:class "text-[32px] font-semibold leading-[1.15] tracking-[-0.03em] text-zinc-900"} title]
    [:p {:class "mt-4 max-w-[44ch] text-[17px] leading-relaxed text-zinc-800"} body]]
   [:div {:class (str "enter flex lg:col-span-6 " (if flip? "lg:order-1 lg:col-start-1 lg:justify-start" "lg:col-start-7 lg:justify-end"))
          :style {"--d" (or d "80ms")}}
    fragment]])

(defn page [{:keys [session]}]
  (let [signed-in? (some? (:uid session))]
    [:div
     [:style (biff/unsafe m/enter-css)]

     ;; First screen: who is behind greed and why, as one large statement
     [:div {:class "bg-stone-100"}
      [:div {:class "container mx-auto px-6 pb-16 pt-14 sm:pt-20 lg:pb-24 lg:pt-28"}
       [:h1 {:class "enter max-w-[18ch] [text-wrap:balance] font-semibold text-zinc-900 [font-size:clamp(48px,7vw,120px)] leading-[0.95] tracking-[-0.05em]" :style {"--d" "60ms"}}
        "We built the plan we wished we had."]
       [:div {:class "mt-12 grid grid-cols-1 gap-10 lg:mt-16 lg:grid-cols-12"}
        [:p {:class "enter max-w-[46ch] text-xl leading-relaxed text-zinc-700 lg:col-span-6" :style {"--d" "120ms"}}
         "greed was started by a small South African team who got tired of guessing their way through tax, budgets and payday. So we built one calm, private place that reads your bank statements, shows what you earn and what SARS takes, and plans the rest. Then we built the same for the businesses we run."]
        [:dl {:class "enter grid grid-cols-3 gap-6 lg:col-span-5 lg:col-start-8" :style {"--d" "160ms"}}
         (for [[k v] [["2" "founders, both in South Africa"]
                      ["R0" "to start, with a free trial of everything"]
                      ["0" "rands made selling your data"]]]
           [:div
            [:dt {:class "font-semibold tracking-[-0.03em] text-zinc-900 [font-size:clamp(36px,4vw,56px)] leading-none tabular-nums"} k]
            [:dd {:class "mt-2 text-base leading-snug text-zinc-700"} v]])]]
       [:div {:class "enter mt-12 flex flex-wrap items-center gap-x-6 gap-y-4" :style {"--d" "200ms"}}
        [:a {:href "/team"
             :class "inline-flex items-center justify-center rounded-md bg-zinc-900 px-6 py-3 text-base font-semibold text-white transition-[transform,background-color] duration-150 ease-out hover:bg-zinc-700 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 focus-visible:ring-offset-2"}
         "Meet the team"]
        [:a {:href "#get" :class (str "text-base " m/link-ink)} "See what you get"]]]]

     ;; What you get: each claim with its proof from the app
     [:section {:id "get" :class "container mx-auto px-6 py-20 lg:py-32" :aria-labelledby "get-h"}
      [:h2 {:id "get-h" :class "enter text-[28px] font-semibold tracking-[-0.02em] text-zinc-900"} "What you get"]
      (claim-row {:title "Understand what you earn."
                  :body "See your real take-home pay and tax for the right SARS year of assessment, before you plan anything else. Work out a bonus, or practise your ITR12 return."}
                 (frag-earn))
      (claim-row {:title "See where it actually went."
                  :body "Upload a PDF statement from FNB or Standard Bank. greed reads every line, sorts it into categories and compares the month with your plan. It keeps the totals, never the file."}
                 (frag-spent) :flip? true)
      (claim-row {:title "Plan before payday."
                  :body "Give every rand a job, from debit orders to savings and goals. Each month has its own budget, and items you add from a statement count only in their month."}
                 (frag-plan))
      (claim-row {:title "Run the business side too."
                  :body "Add your sole proprietorship or company to the same account. It gets its own budget, statements, VAT and company tax, a team you invite, and owner's pay that lands on your personal Tax page."}
                 (frag-business) :flip? true)]

     ;; From the founder: the letter that used to close the home page
     [:section {:id "why" :class "bg-stone-100" :aria-labelledby "why-h"}
      [:div {:class "container mx-auto grid grid-cols-1 gap-10 px-6 py-20 lg:grid-cols-12 lg:py-28"}
       [:div {:class "enter flex items-center gap-4 lg:col-span-4 lg:items-start"}
        [:img {:src "/img/IMG_3777.webp" :alt "Sithabiso Makhathini" :width 56 :height 56 :loading "lazy"
               :class "h-14 w-14 rounded-full object-cover object-top"}]
        [:h2 {:id "why-h" :class "text-[28px] font-semibold leading-tight tracking-[-0.02em] text-zinc-900"} "From the founder"]]
       [:div {:class "enter max-w-[600px] lg:col-span-7 lg:col-start-6" :style {"--d" "80ms"}}
        [:div {:class "space-y-6 text-[17px] leading-[1.6] text-zinc-800"}
         [:p "Money is private, and so is ambition. We built greed to take your financial life off the messy back of your mind and put it in "
          [:mark {:class "rounded-sm bg-brand-100 text-zinc-900"} "one clear place"]
          ": salary, tax, spending, savings and the dates that matter."]
         [:p "Many of us also run something on the side, or full time: a studio, a trade, a small company. Its money deserves the same clarity, without a second app or a spreadsheet nobody updates. So greed runs your business too, from the same sign-in."]
         [:p "There is no judgement and no jargon here, and no promise of instant wealth. Your information stays yours. We will never sell your data."]
         [:p "We are a small team in South Africa, and we read every message ourselves. Write to us at "
          [:a {:href "mailto:support@mygreed.co.za" :class m/link-ink} "support@mygreed.co.za"] "."]]
        [:p {:class "mt-10 text-[22px] italic text-zinc-900"} "Sithabiso Makhathini"]
        [:p {:class "mt-1 text-sm text-zinc-600"} "Founder & CEO"]]]]

     ;; Why the name: the logo's own idea, drawn large
     [:section {:id "name" :class "bg-zinc-950 text-white" :aria-labelledby "name-h"}
      [:div {:class "container mx-auto grid grid-cols-1 items-start gap-12 px-6 py-20 lg:grid-cols-12 lg:gap-10 lg:py-32"}
       [:div {:class "enter lg:col-span-5"}
        [:img {:src "/img/greed-symbol-white-tight.svg" :alt "" :aria-hidden "true" :width 192 :height 192 :loading "lazy"
               :class "block aspect-square w-1/2 max-w-[420px] lg:w-full"}]]
       ;; The statement starts level with the symbol's cut, 38.2% down.
       [:div {:class "enter lg:col-span-6 lg:col-start-7 lg:pt-[124px]" :style {"--d" "80ms"}}
        [:h2 {:id "name-h" :class "text-base font-medium text-zinc-300"} "Why the name"]
        [:p {:class "mt-4 max-w-[20ch] text-[36px] font-medium leading-[1.15] tracking-[-0.02em] text-white"}
         "greed means refusing to settle for financial confusion."]
        [:p {:class "mt-6 max-w-[48ch] text-[17px] leading-relaxed text-zinc-300"}
         "It does not mean taking without limits. It means wanting more: more security, more ownership, more choice, disciplined by a plan you can see. The mark says it too: one level line, cut where enough is."]]]]

     ;; Privacy: on the stone tone, beside the shape of a vault door
     [:section {:id "privacy" :class "bg-white" :aria-labelledby "privacy-h"}
      [:div {:class "container mx-auto grid grid-cols-1 gap-8 px-6 py-20 lg:grid-cols-12 lg:gap-10 lg:py-28"}
       [:h2 {:id "privacy-h" :class "enter text-[32px] font-semibold leading-[1.15] tracking-[-0.03em] text-zinc-900 lg:col-span-5"}
        "Your financial life stays yours."]
       [:div {:class "enter lg:col-span-6 lg:col-start-7" :style {"--d" "80ms"}}
        [:p {:class "max-w-[52ch] text-[17px] leading-relaxed text-zinc-800"}
         "Keeping your data secure is our priority. Your information is stored safely and access is protected, so your money and your details stay private."]
        [:ul {:class "mt-6 max-w-[52ch] space-y-3 text-[17px] leading-relaxed text-zinc-800"}
         [:li [:span {:class "font-semibold text-zinc-900"} "Statements are read, then let go. "] "greed keeps the monthly totals it needs. The PDF itself is never stored."]
         [:li [:span {:class "font-semibold text-zinc-900"} "Profiles stay apart. "] "People you invite to a business see that business, never your personal budget."]
         [:li [:span {:class "font-semibold text-zinc-900"} "No ads. "] "Nothing on greed is there to sell you something, and we never sell your data."]]
        [:p {:class "mt-6 text-[24px] font-semibold tracking-[-0.02em] text-zinc-900"} "We will never sell your data."]
        [:p {:class "mt-6 max-w-[52ch] text-[17px] leading-relaxed text-zinc-800"}
         "Read our " [:a {:href "/privacy" :class m/link-ink} "privacy policy"]
         ", or " [:a {:href "/contact" :class m/link-ink} "ask us"] " how we protect it."]]]]

     [:div
      (m/action-tile
       {:id "close" :ground "bg-stone-100"
        :heading "Built by people who refuse to settle"
        :line "A small South African team, tired of guessing through tax, budgets, payday and VAT returns, built the tool they wished they had."
        :primary {:href (if signed-in? "/app" "/signup") :label (if signed-in? "Go to your account" "Create your free account")}
        :secondary {:href "/team" :label "Meet the team"}})]
     [:script (biff/unsafe m/enter-js)]]))
