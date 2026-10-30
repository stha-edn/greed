(ns com.greed.ui.pages.contact
  (:require [com.biffweb :as biff]
            [com.greed.ui.components.illustrations :as ill]
            [com.greed.ui.components.marketing :as m]))

(defn- channel [{:keys [name detail href external?]}]
  [:li
   [:a (cond-> {:href href
                :class "group grid grid-cols-1 items-baseline gap-1 rounded-md px-3 py-5 -mx-3 transition-colors duration-150 hover:bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 sm:grid-cols-[minmax(0,1fr)_auto] sm:gap-6"}
         external? (assoc :target "_blank" :rel "noopener noreferrer"))
    [:span {:class "font-semibold tracking-[-0.03em] text-zinc-900 [font-size:clamp(28px,3.4vw,52px)] leading-[1.1]"} name]
    [:span {:class "flex items-center gap-2 text-lg text-zinc-700"}
     detail
     [:span {:aria-hidden "true" :class "transition-transform duration-150 ease-out group-hover:translate-x-1"} "→"]]]])

(defn page [{:keys [session]}]
  (let [signed-in? (some? (:uid session))]
    [:div
     [:style (biff/unsafe m/enter-css)]

     [:div {:class "container mx-auto grid grid-cols-1 items-center gap-12 px-6 pb-16 pt-14 sm:pt-20 lg:grid-cols-12 lg:pb-24 lg:pt-24"}
      [:div {:class "lg:col-span-6"}
       [:h1 {:class "enter max-w-[14ch] font-semibold text-zinc-900 [font-size:clamp(44px,6vw,92px)] leading-[1.0] tracking-[-0.045em]"}
        "Talk to us directly."]
       [:p {:class "enter mt-8 max-w-[44ch] text-xl leading-relaxed text-zinc-700" :style {"--d" "80ms"}}
        "We are a small team and we read every message ourselves. Ask about your account, your tax, or anything else, and we will help you find the right answer."]]
      [:div {:class "enter flex justify-center lg:col-span-6 lg:justify-end" :style {"--d" "140ms"}}
       (ill/envelope :class "w-full max-w-[460px] -rotate-[3deg]")]]

     [:section {:id "reach" :class "bg-stone-100" :aria-labelledby "reach-h"}
      [:div {:class "container mx-auto grid grid-cols-1 items-center gap-12 px-6 py-16 lg:grid-cols-12 lg:gap-10 lg:py-24"}
       [:div {:class "lg:col-span-7"}
        [:h2 {:id "reach-h" :class "enter text-[28px] font-semibold tracking-[-0.02em] text-zinc-900"} "Ways to reach us"]
        [:ul {:class "enter mt-6" :style {"--d" "80ms"}}
         (channel {:name "Email us" :detail "support@mygreed.co.za" :href "mailto:support@mygreed.co.za"})
         (channel {:name "Instagram" :detail "@greed_za" :href "https://www.instagram.com/greed_za/" :external? true})
         (channel {:name "Meet the team" :detail "Who builds greed" :href "/team"})]]
       [:div {:class "enter flex justify-center lg:col-span-4 lg:col-start-9 lg:justify-end" :style {"--d" "140ms"}}
        (ill/phone-message :class "w-full max-w-[260px] rotate-[4deg]")]]]

     [:div {:class "container mx-auto px-6"}
      [:section {:id "help" :class "grid grid-cols-1 items-center gap-10 py-20 lg:grid-cols-12 lg:py-32" :aria-labelledby "help-h"}
       [:div {:class "enter lg:col-span-5"}
        (ill/calendar-sheet :class "w-full max-w-[260px] rotate-[-2deg]")]
       [:div {:class "enter lg:col-span-6 lg:col-start-7" :style {"--d" "80ms"}}
        [:h2 {:id "help-h" :class "text-[32px] font-semibold leading-[1.15] tracking-[-0.03em] text-zinc-900"} "No question is too small"]
        [:p {:class "mt-4 max-w-[46ch] text-[17px] leading-relaxed text-zinc-800"}
         "Payday questions are welcome any day of the month. People write to us about:"]
        [:ul {:class "mt-6 grid grid-cols-1 gap-x-10 gap-y-3 text-[17px] leading-relaxed text-zinc-900 sm:grid-cols-2"}
         (for [item ["Your account and sign-in"
                     "Tax calculations and returns"
                     "Budgets, goals and the calendar"
                     "Privacy, your data, or deleting your account"
                     "Feedback, feature ideas and bug reports"
                     "Anything else on your mind"]]
           [:li {:class "flex gap-3"} [:span {:aria-hidden "true" :class "mt-[0.6em] h-1.5 w-1.5 shrink-0 rounded-full bg-brand-600"}] item])]]]]

     (m/action-tile
      {:id "close"
       :heading "See what your money can do"
       :line "Create a free account and know what you earn, what SARS takes, and where the rest goes."
       :primary {:href (if signed-in? "/app" "/signup") :label (if signed-in? "Go to dashboard" "Create your free account")}
       :secondary {:href "mailto:support@mygreed.co.za" :label "Email us"}})
     [:script (biff/unsafe m/enter-js)]]))
