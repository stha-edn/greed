(ns com.greed.ui.pages.about
  (:require [com.biffweb :as biff]
            [com.greed.ui.components.illustrations :as ill]
            [com.greed.ui.components.marketing :as m]))

;; Small, true pieces of the app beside each claim (sample salary R25,000).

(defn- frag-split []
  [:div {:data-ui "" :class "w-full max-w-[460px] rounded-lg border border-zinc-200 bg-white p-6"}
   [:p {:class "text-sm font-medium text-zinc-500"} "Where your income goes"]
   [:div {:class "mt-4 flex h-2.5 w-full overflow-hidden rounded-full bg-zinc-100"}
    [:div {:class "h-full bg-brand-600" :style {:width "86%"}}]
    [:div {:class "h-full bg-zinc-400" :style {:width "14%"}}]]
   [:div {:class "mt-4 space-y-2 text-sm tabular-nums"}
    [:p {:class "flex justify-between"} [:span {:class "flex items-center gap-2 text-zinc-600"} [:span {:class "h-2 w-2 rounded-full bg-brand-600"}] "Take-home"] [:span {:class "font-semibold text-zinc-900"} "R259,428"]]
    [:p {:class "flex justify-between"} [:span {:class "flex items-center gap-2 text-zinc-600"} [:span {:class "h-2 w-2 rounded-full bg-zinc-400"}] "Tax"] [:span {:class "font-semibold text-zinc-900"} "R40,572"]]]
   [:p {:class "mt-4 text-sm text-zinc-600"} "Of every R1 you earn, 14c goes to SARS."]])

(defn- frag-budget-row []
  [:div {:data-ui "" :class "w-full max-w-[380px] rounded-lg border border-zinc-200 bg-white"}
   [:div {:class "flex items-center justify-between px-5 py-4"}
    [:p {:class "text-sm font-semibold text-zinc-900"} "Expenses"]
    [:span {:class "rounded-md border border-zinc-200 px-2.5 py-1 text-xs font-medium text-zinc-700"} "+ Add item"]]
   [:div {:class "flex items-center justify-between bg-zinc-100 px-5 py-3 text-sm"}
    [:span {:class "text-zinc-800"} "Rent · debit order on the 1st"] [:span {:class "font-semibold tabular-nums"} "R7,000"]]
   [:div {:class "flex items-center justify-between px-5 py-3 text-sm"}
    [:span {:class "text-zinc-800"} "Car payment"] [:span {:class "font-semibold tabular-nums"} "R3,500"]]
   [:div {:class "flex items-center justify-between px-5 py-3 text-sm"}
    [:span {:class "text-zinc-800"} "R370 a day to spend until payday"]]])

(defn- frag-goal []
  [:div {:data-ui "" :class "w-full max-w-[320px] rounded-lg border border-zinc-200 bg-white p-5"}
   [:div {:class "flex items-center justify-between gap-3"}
    [:p {:class "text-sm font-semibold text-zinc-900"} "Emergency fund"]
    [:span {:class "rounded-full bg-zinc-100 px-2 py-0.5 text-xs font-medium text-zinc-700"} "By 1 Mar 2027"]]
   [:p {:class "mt-4 flex items-baseline justify-between tabular-nums"}
    [:span {:class "text-2xl font-semibold text-zinc-900"} "R6,000"] [:span {:class "text-sm text-zinc-500"} "of R10,000"]]
   [:div {:class "mt-2 h-2 w-full overflow-hidden rounded-full bg-zinc-100"}
    [:div {:class "h-full rounded-full bg-brand-600" :style {:width "60%"}}]]
   [:p {:class "mt-2 text-sm text-zinc-600"} "60% funded · R1,000 a month keeps you on track"]])

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
         "greed was started by a small South African team who got tired of guessing their way through tax, budgets and payday. So we built one calm, private place that shows what you earn, what SARS takes, and what you can do with the rest."]
        [:dl {:class "enter grid grid-cols-3 gap-6 lg:col-span-5 lg:col-start-8" :style {"--d" "160ms"}}
         (for [[k v] [["2" "founders, both in South Africa"]
                      ["R0" "to use, for everyone"]
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
                  :body "See your real take-home pay and tax for the right SARS year of assessment, before you plan anything else."}
                 (frag-split))
      (claim-row {:title "Plan before payday."
                  :body "Give every rand a job, from commitments to savings and goals, before the first debit order goes off."}
                 (frag-budget-row) :flip? true)
      (claim-row {:title "See what is next."
                  :body "Spot shortfalls early, track your tax readiness and watch real progress towards the goals that matter to you."}
                 (frag-goal))]

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
     [:section {:id "privacy" :class "bg-stone-100" :aria-labelledby "privacy-h"}
      [:div {:class "container mx-auto grid grid-cols-1 gap-8 px-6 py-20 lg:grid-cols-12 lg:gap-10 lg:py-28"}
       [:h2 {:id "privacy-h" :class "enter text-[32px] font-semibold leading-[1.15] tracking-[-0.03em] text-zinc-900 lg:col-span-5"}
        "Your financial life stays yours."]
       [:div {:class "enter lg:col-span-6 lg:col-start-7" :style {"--d" "80ms"}}
        [:p {:class "max-w-[52ch] text-[17px] leading-relaxed text-zinc-800"}
         "Keeping your data secure is our priority. Your information is stored safely and access is protected, so your money and your details stay private."]
        [:p {:class "mt-6 text-[24px] font-semibold tracking-[-0.02em] text-zinc-900"} "We will never sell your data."]
        [:p {:class "mt-6 max-w-[52ch] text-[17px] leading-relaxed text-zinc-800"}
         "Read our " [:a {:href "/privacy" :class m/link-ink} "privacy policy"]
         ", or " [:a {:href "/contact" :class m/link-ink} "ask us"] " how we protect it."]]]]

     [:div
      (m/action-tile
       {:id "close" :ground "bg-white"
        :heading "Built by people who refuse to settle"
        :line "A small South African team, tired of guessing through tax, budgets and payday, built the tool they wished they had."
        :primary {:href (if signed-in? "/app" "/signup") :label (if signed-in? "Go to dashboard" "Create your free account")}
        :secondary {:href "/team" :label "Meet the team"}})]
     [:script (biff/unsafe m/enter-js)]]))
