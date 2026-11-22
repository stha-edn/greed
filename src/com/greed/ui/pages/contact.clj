(ns com.greed.ui.pages.contact
  (:require [com.biffweb :as biff]
            [com.greed.ui.components.marketing :as m]
            [com.greed.ui.components.svgs :as svgs]))

(defn- channel [{:keys [icon name detail href external? d]}]
  [:li {:class "enter" :style {"--d" (or d "0ms")}}
   [:a (cond-> {:href href
                :class "group flex h-full flex-col rounded-3xl bg-white p-7 ring-1 ring-zinc-200/70 transition duration-300 hover:-translate-y-1 hover:shadow-card-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"}
         external? (assoc :target "_blank" :rel "noopener noreferrer"))
    [:span {:class "flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-brand-700 ring-1 ring-brand-600/10"} icon]
    [:span {:class "mt-6 text-xl font-semibold tracking-[-0.02em] text-zinc-900"} name]
    [:span {:class "mt-1 flex items-center gap-2 text-[15px] text-zinc-600"}
     detail
     [:span {:aria-hidden "true" :class "text-brand-600 transition-transform duration-150 group-hover:translate-x-1"} "→"]]]])

(defn page [{:keys [session]}]
  (let [signed-in? (some? (:uid session))]
    [:div {:class "bg-white"}
     [:style (biff/unsafe m/enter-css)]
     (m/page-hero
      {:kicker "Contact"
       :title "Talk to us directly."
       :line "We are a small team and we read every message ourselves. Ask about your account, your tax, or anything else, and we will help you find the right answer."})

     [:section {:id "reach" :class "container mx-auto px-6 py-20 lg:py-24" :aria-labelledby "reach-h"}
      (m/section-head {:kicker "Ways to reach us" :id "reach-h" :title "Pick whatever suits you."})
      [:ul {:class "mt-12 grid grid-cols-1 gap-5 md:grid-cols-3"}
       (channel {:icon (svgs/envelope) :name "Email us" :detail "support@mygreed.co.za" :href "mailto:support@mygreed.co.za"})
       (channel {:icon (svgs/heart) :name "Instagram" :detail "@greed_za" :href "https://www.instagram.com/greed_za/" :external? true :d "80ms"})
       (channel {:icon (svgs/users) :name "Meet the team" :detail "Who builds greed" :href "/team" :d "160ms"})]]

     [:section {:id "help" :class "bg-zinc-50 py-20 lg:py-24" :aria-labelledby "help-h"}
      [:div {:class "container mx-auto grid grid-cols-1 items-start gap-12 px-6 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]"}
       (m/section-head {:kicker "Any question" :id "help-h" :title "No question is too small."
                        :line "Payday questions are welcome any day of the month. People write to us about:"})
       [:ul {:class "enter grid grid-cols-1 gap-3 sm:grid-cols-2" :style {"--d" "80ms"}}
        (for [item ["Your account and sign-in"
                    "Tax calculations and returns"
                    "Budgets, goals and the calendar"
                    "Plans, billing and cancelling"
                    "Privacy, your data, or deleting your account"
                    "Feedback, feature ideas and bug reports"]]
          [:li {:class "flex items-start gap-3 rounded-2xl bg-white p-4 text-[15px] text-zinc-800 ring-1 ring-zinc-200/70"}
           [:span {:class "mt-0.5 flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full bg-brand-600 text-white"} (svgs/check {:class "h-3 w-3"})]
           item])]]]
     [:script (biff/unsafe m/enter-js)]]))
