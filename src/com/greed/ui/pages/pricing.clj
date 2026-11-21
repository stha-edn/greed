(ns com.greed.ui.pages.pricing
  "GET /pricing: the three plans, the trial, and plain answers about paying."
  (:require [com.biffweb :as biff]
            [com.greed.data.plans :as plans]
            [com.greed.ui.components.marketing :as m]
            [com.greed.ui.components.plans :as plan-cards]))

(def ^:private btn-dark
  "inline-flex w-full items-center justify-center rounded-md bg-zinc-900 px-5 py-2.5 text-sm font-semibold text-white transition-[transform,background-color] duration-150 ease-out hover:bg-zinc-700 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 focus-visible:ring-offset-2")

(def ^:private btn-outline
  "inline-flex w-full items-center justify-center rounded-md bg-white px-5 py-2.5 text-sm font-semibold text-zinc-900 ring-1 ring-zinc-300 transition-[transform,background-color] duration-150 ease-out hover:bg-zinc-50 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 focus-visible:ring-offset-2")

(defn- action [signed-in?]
  (fn [tier]
    (let [href (if signed-in? "/app/plan" "/signup")]
      (case tier
        :free     [:a {:href href :class btn-outline} (if signed-in? "Your account" "Start free")]
        :premium  [:a {:href href :class btn-dark} (if signed-in? "Choose Premium" "Start with a free trial")]
        :business [:a {:href (if signed-in? "/app/plan" "/signup?as=business") :class btn-outline}
                   (if signed-in? "Choose Business" "Start with a free trial")]))))

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
    [:div
     [:style (biff/unsafe m/enter-css)]
     [:div {:class "container mx-auto px-6"}
      (m/page-head "Start free. Pay when it earns its place."
                   (str "Budget, goals, the calendar, insights and every tax calculator are free. Premium reads all your statements; Business adds your businesses. Try everything free for " (plans/trial-days) " days, no card needed."))
      [:div {:class "enter" :style {"--d" "120ms"}}
       [:div {:class "mb-6"} (plan-cards/interval-switch "/pricing" interval)]
       (plan-cards/cards {:interval interval :action (action signed-in?)})
       [:p {:class "mt-4 text-sm text-zinc-500"} "Prices in rand. Paid by card through Paystack."]]
      (m/split {:id "faq" :heading "Questions"}
               [:dl {:class "divide-y divide-zinc-200 border-y border-zinc-200"}
                (for [[q a] faq]
                  [:div {:class "py-6"}
                   [:dt {:class "text-lg font-semibold tracking-[-0.01em] text-zinc-900"} q]
                   [:dd {:class "mt-2 max-w-[60ch] text-base leading-relaxed text-zinc-600"} a]])])]
     [:script (biff/unsafe m/enter-js)]]))
