(ns com.greed.ui.pages.about
  (:require [com.biffweb :as biff]
            [com.greed.ui.components.marketing :as m]
            [com.greed.ui.components.svgs :as svgs]))

;; About is about the people and the promise. What greed does is shown in
;; full on Home; here it is only summarised, with a link there.

(defn- what-greed-does []
  [:section {:id "get" :class "container mx-auto px-6 py-20 lg:py-24" :aria-labelledby "get-h"}
   [:div {:class "grid grid-cols-1 gap-12 lg:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)]"}
    [:div
     (m/section-head {:kicker "What greed does" :id "get-h" :title "One calm place for your money."})
     [:a {:href "/#features" :class (str "enter mt-6 inline-flex items-center gap-1.5 text-[15px] font-semibold " m/link-ink)}
      "See it in action on the home page" [:span {:aria-hidden "true"} "→"]]]
    [:ol {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
     (for [[i [t l]] (map-indexed vector
                                  [["Understand what you earn" "Take-home pay and tax on the SARS tables."]
                                   ["See where it went" "Bank statements read and sorted for you."]
                                   ["Plan before payday" "A budget for every month."]
                                   ["Run the business too" "VAT, company tax and a team, in the same account."]])]
       [:li {:class "enter rounded-2xl bg-zinc-50 p-5 ring-1 ring-zinc-200/70" :style {"--d" (str (* 60 i) "ms")}}
        [:p {:class "text-sm font-semibold tabular-nums text-brand-600"} (str "0" (inc i))]
        [:p {:class "mt-1 font-semibold text-zinc-900"} t]
        [:p {:class "mt-1 text-[15px] text-zinc-600"} l]])]]])

(defn page [{:keys [session]}]
  (let [signed-in? (some? (:uid session))]
    [:div {:class "bg-white"}
     [:style (biff/unsafe m/enter-css)]
     (m/page-hero
      {:kicker "About greed"
       :title "We built the plan we wished we had."
       :line "greed was started by a small South African team who got tired of guessing their way through tax, budgets and payday. So we built one calm, private place that reads your bank statements, shows what you earn and what SARS takes, and plans the rest. Then we built the same for the businesses we run."
       :actions [[:a {:href "/team" :class m/btn-white} "Meet the team" [:span {:aria-hidden "true"} "→"]]
                 [:a {:href "#why" :class m/btn-glass} "Read our story"]]
       :aside [:dl {:class "grid grid-cols-1 gap-3 sm:grid-cols-3 lg:grid-cols-1"}
               (for [[k v] [["2" "founders, both in South Africa"]
                            ["1" "account for you and your business"]
                            ["0" "rands made selling your data"]]]
                 [:div {:class "rounded-2xl bg-white/[0.06] p-5 ring-1 ring-white/10 backdrop-blur"}
                  [:dt {:class "font-semibold tracking-[-0.03em] text-white [font-size:clamp(32px,3.4vw,48px)] leading-none tabular-nums"} k]
                  [:dd {:class "mt-2 text-sm leading-snug text-brand-100/80"} v]])]})

     (what-greed-does)

     ;; From the founder
     [:section {:id "why" :class "bg-zinc-50 py-20 lg:py-28" :aria-labelledby "why-h"}
      [:div {:class "container mx-auto px-6"}
       [:div {:class "enter mx-auto max-w-3xl rounded-[2rem] bg-white p-8 shadow-[0_30px_60px_-30px_rgba(0,50,34,.25)] ring-1 ring-zinc-200/70 sm:p-12"}
        [:div {:class "flex items-center gap-4"}
         [:img {:src "/img/IMG_3777.webp" :alt "Sithabiso Makhathini" :width 64 :height 64 :loading "lazy"
                :class "h-16 w-16 rounded-full object-cover object-top ring-4 ring-brand-50"}]
         [:div
          [:p {:class m/eyebrow} "From the founder"]
          [:h2 {:id "why-h" :class "mt-2 text-2xl font-semibold tracking-[-0.02em] text-zinc-900"} "Why we built greed"]]]
        [:div {:class "mt-8 space-y-5 text-[17px] leading-[1.7] text-zinc-700"}
         [:p "Money is private, and so is ambition. We built greed to take your financial life off the messy back of your mind and put it in "
          [:mark {:class "rounded bg-brand-100 px-1 text-zinc-900"} "one clear place"]
          ": salary, tax, spending, savings and the dates that matter."]
         [:p "Many of us also run something on the side, or full time: a studio, a trade, a small company. Its money deserves the same clarity, without a second app or a spreadsheet nobody updates. So greed runs your business too, from the same sign-in."]
         [:p "There is no judgement and no jargon here, and no promise of instant wealth. Your information stays yours. We will never sell your data."]
         [:p "We are a small team in South Africa, and we read every message ourselves. Write to us at "
          [:a {:href "mailto:support@mygreed.co.za" :class m/link-ink} "support@mygreed.co.za"] "."]]
        [:div {:class "mt-10 border-t border-zinc-100 pt-6"}
         [:p {:class "text-xl font-semibold italic text-zinc-900"} "Sithabiso Makhathini"]
         [:p {:class "mt-1 text-sm text-zinc-500"} "Founder & CEO"]]]]]

     ;; Why the name
     [:section {:class "container mx-auto px-4 py-16 sm:px-6 lg:py-20" :aria-labelledby "name-h"}
      (m/glow-panel {:class "px-6 py-16 sm:px-10 lg:px-14 lg:py-20"}
                    [:div {:class "grid grid-cols-1 items-center gap-12 lg:grid-cols-[minmax(0,0.6fr)_minmax(0,1fr)]"}
                     [:div {:class "enter flex justify-center lg:justify-start"}
                      [:img {:src "/img/greed-symbol-white-tight.svg" :alt "" :aria-hidden "true" :width 192 :height 192 :loading "lazy"
                             :class "float block aspect-square w-40 lg:w-64"}]]
                     [:div {:class "enter" :style {"--d" "80ms"}}
                      [:p {:class "inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.08em] text-brand-200 ring-1 ring-white/15"} "Why the name"]
                      [:h2 {:id "name-h" :class "mt-5 max-w-[20ch] font-semibold text-white [font-size:clamp(28px,3.4vw,44px)] leading-[1.12] tracking-[-0.03em]"}
                       "greed means refusing to settle for financial confusion."]
                      [:p {:class "mt-6 max-w-[50ch] text-lg leading-relaxed text-brand-100/80"}
                       "It does not mean taking without limits. It means wanting more: more security, more ownership, more choice, disciplined by a plan you can see. The mark says it too: one level line, cut where enough is."]]])]

     ;; Privacy
     [:section {:id "privacy" :class "container mx-auto px-6 py-16 lg:py-24" :aria-labelledby "privacy-h"}
      [:div {:class "grid grid-cols-1 gap-12 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]"}
       (m/section-head {:kicker "Privacy" :id "privacy-h" :title "Your financial life stays yours."
                        :line "Keeping your data secure is our priority. Your information is stored safely and access is protected."})
       [:ul {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
        (for [[i [t l]] (map-indexed vector
                                     [["Statements are read, then let go" "greed keeps the monthly totals it needs. The PDF itself is never stored."]
                                      ["Profiles stay apart" "People you invite to a business see that business, never your personal budget."]
                                      ["No ads" "Nothing on greed is there to sell you something."]
                                      ["We never sell your data" [:span "Read our " [:a {:href "/privacy" :class m/link-ink} "privacy policy"] ", or " [:a {:href "/contact" :class m/link-ink} "ask us"] " how we protect it."]]])]
          [:li {:class "enter rounded-2xl bg-zinc-50 p-6 ring-1 ring-zinc-200/70" :style {"--d" (str (* 70 i) "ms")}}
           [:span {:class "flex h-10 w-10 items-center justify-center rounded-xl bg-brand-600 text-white"} (svgs/shield-check)]
           [:p {:class "mt-4 font-semibold text-zinc-900"} t]
           [:p {:class "mt-1 text-[15px] leading-relaxed text-zinc-600"} l]])]]]
     [:script (biff/unsafe m/enter-js)]]))
