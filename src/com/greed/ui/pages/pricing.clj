(ns com.greed.ui.pages.pricing
  "GET /pricing: the three plans, the trial, and plain answers about paying."
  (:require [com.biffweb :as biff]
            [com.greed.data.plans :as plans]
            [com.greed.ui.components.marketing :as m]
            [com.greed.ui.components.plans :as plan-cards]))

(defn- action [signed-in?]
  (fn [tier]
    (let [href (cond (and signed-in? (= :free tier)) "/app" signed-in? "/app/plan" (= :business tier) "/signup?as=business" :else "/signup")]
      [:a {:href href :class (str (if (= :premium tier) m/btn-green m/btn-outline) " w-full")}
       (cond (and signed-in? (= :free tier)) "Go to your account"
             signed-in? (str "Choose " (plans/tier-name tier))
             (= :free tier) "Start free"
             :else "Start your free trial")])))

(def ^:private faq
  [["How does the free trial work?"
    (str "Every new account gets " (plans/trial-days) " days of Business, with everything unlocked. No card is needed. When it ends you are on Free, and nothing you added is deleted.")]
   ["What happens to my statements on Free?"
    "Months you have already read stay in Spending and Banking. Free reads one statement month; to add more, choose Premium."]
   ["Do the people I invite to my business pay?"
    "No. A partner or bookkeeper you invite works in your business under your Business plan."]
   ["How do I pay, and can I cancel?"
    "By card, through Paystack. Plans renew each month or year until you cancel, and you can cancel at any time. You keep your plan until the end of the period you paid for."]
   ["Is greed tax or financial advice?"
    "No. greed works out estimates from SARS tables and your own figures to help you plan. For advice on your situation, speak to a registered tax practitioner or financial adviser."]])

(defn page [{:keys [session params]}]
  (let [signed-in? (some? (:uid session))
        interval   (plan-cards/interval-of params)]
    [:div {:class "bg-white"}
     [:style (biff/unsafe m/enter-css)]
     (m/page-hero
      {:kicker "Pricing"
       :title "Start free. Pay when it earns its place."
       :line (str "Budget, goals, the calendar, insights and the income tax and bonus calculators are free. Premium reads all your statements and simulates your ITR12 return; Business adds your businesses. Try everything free for " (plans/trial-days) " days, no card needed.")})
     [:section {:class "container mx-auto px-6 py-16 lg:py-20" :aria-label "Plans"}
      [:div {:class "enter mb-8 flex flex-wrap items-center justify-between gap-4"}
       [:p {:class "text-sm text-zinc-500"} "Prices in rand. Paid by card through Paystack. Cancel any time."]
       (plan-cards/interval-switch "/pricing" interval)]
      [:div {:class "enter" :style {"--d" "80ms"}}
       (plan-cards/cards {:interval interval :action (action signed-in?)})]]
     [:section {:id "faq" :class "bg-zinc-50 py-20 lg:py-24" :aria-labelledby "faq-h"}
      [:div {:class "container mx-auto grid grid-cols-1 gap-12 px-6 lg:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)]"}
       (m/section-head {:kicker "Questions" :id "faq-h" :title "Good to know before you start."
                        :line [:span "Something else? " [:a {:href "/contact" :class m/link-ink} "Ask us"] "."]})
       [:div {:class "enter space-y-3" :style {"--d" "80ms"}}
        (for [[q a] faq]
          [:details {:class "group rounded-2xl bg-white p-5 ring-1 ring-zinc-200/70 open:shadow-card-md sm:p-6"}
           [:summary {:class "flex cursor-pointer list-none items-center justify-between gap-4 text-[17px] font-semibold text-zinc-900 [&::-webkit-details-marker]:hidden"}
            q
            [:span {:aria-hidden "true" :class "flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-brand-50 text-brand-700 transition group-open:rotate-45"} "+"]]
           [:p {:class "mt-3 max-w-[60ch] text-[15px] leading-relaxed text-zinc-600"} a]])]]]
     (m/action-tile
      {:id "close"
       :heading "Try everything free for 14 days."
       :line "No card needed. When the trial ends you choose a plan, or stay on Free."
       :primary {:href (if signed-in? "/app/plan" "/signup") :label (if signed-in? "Open your plan" "Start free")}
       :secondary {:href "/#features" :label "See the features"}
       :facts :none})
     [:script (biff/unsafe m/enter-js)]]))
