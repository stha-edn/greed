(ns com.greed.ui.pages.contact
  (:require [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.svgs :as svgs]))

(defn- badge [label]
  [:div {:class "inline-flex items-center gap-2 px-3 py-1.5 mb-4 text-xs font-semibold text-emerald-700 bg-emerald-50 border border-emerald-100 rounded-full"}
   [:span {:class "w-1.5 h-1.5 bg-emerald-500 rounded-full"}]
   label])

(defn- contact-tile [& {:keys [icon title description href class]}]
  [:a {:href href
       :class (str "reveal group flex flex-col p-8 bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card transition-all duration-200 hover:-translate-y-0.5 hover:shadow-card-hover hover:ring-zinc-300/70 active:scale-[0.98] " class)}
   [:div {:class "flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 ring-1 ring-emerald-600/10 transition-transform duration-200 group-hover:scale-105"}
    icon]
   [:h3 {:class "mt-5 text-lg font-semibold text-zinc-900 tracking-tight group-hover:text-emerald-600 transition-colors"} title]
   [:p {:class "mt-2 text-sm text-zinc-500 leading-relaxed"} description]
   [:span {:class "inline-flex items-center gap-2 mt-6 text-sm font-semibold text-zinc-900 transition-colors group-hover:text-emerald-600"}
    "Get in touch"
    (svgs/->next {:class "size-3.5 -translate-x-0.5 transition-transform group-hover:translate-x-0"})]])

(defn page [{:keys [session]}]
  (let [signed-in? (some? (:uid session))]
    [:div {:class "container mx-auto px-6"}

     ;; Hero
     [:div {:class "py-16 lg:py-24"}
      [:div {:class "text-center max-w-2xl mx-auto"}
       (badge "Contact us")
       [:h1 {:class "text-4xl font-bold text-zinc-900 leading-[1.08] tracking-tight text-balance lg:text-5xl"}
        "Talk to us " [:span {:class "text-emerald-600"} "directly."]]
       [:p {:class "mt-6 text-lg text-zinc-500 leading-relaxed"}
        "We're a small team and we read every message ourselves. Ask about your account, your tax, or anything else — we'll help you find the right answer."]]]

     ;; Contact channels
     [:div {:class "pb-20 lg:pb-28"}
      [:div {:class "grid grid-cols-1 gap-6 max-w-4xl mx-auto md:grid-cols-3"}
       (contact-tile :icon (svgs/envelope)
                     :title "Email us"
                     :description "support@mygreed.co.za — the fastest way to reach us. We reply personally."
                     :href "mailto:support@mygreed.co.za"
                     :class "reveal-1")
       (contact-tile :icon (svgs/instagram)
                     :title "Instagram"
                     :description "@greed_za — follow along and send us a message."
                     :href "https://www.instagram.com/greed_za/"
                     :class "reveal-2")
       (contact-tile :icon (svgs/users)
                     :title "Meet the team"
                     :description "Prefer a person? See who's building Greed."
                     :href "/team"
                     :class "reveal-3")]]

     ;; What we can help with
     [:div {:class "reveal pb-20 lg:pb-28"}
      [:div {:class "max-w-3xl mx-auto"}
       [:div {:class "text-center mb-10"}
        (badge "We can help with")
        [:h2 {:class "text-3xl font-bold text-zinc-900 tracking-tight text-balance lg:text-4xl"}
         "No question is too small."]]
       [:div {:class "grid grid-cols-1 gap-3 sm:grid-cols-2"}
        (for [item ["Your account and sign-in"
                    "Tax calculations and returns"
                    "Budgets, goals and the calendar"
                    "Privacy, your data, or deleting your account"
                    "Feedback, feature ideas, and bug reports"
                    "Anything else on your mind — just ask."]]
          [:div {:class "flex items-start gap-2.5 text-sm text-zinc-600"}
           [:span {:class "mt-0.5 flex h-4 w-4 flex-shrink-0 items-center justify-center rounded-full bg-emerald-50 text-emerald-600"}
            [:svg {:class "size-3" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24" :stroke-width "3"}
             [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M5 13l4 4L19 7"}]]]
           item])]]]

     ;; Closing
     [:div {:class "reveal pb-24 lg:pb-32"}
      [:div {:class "relative overflow-hidden px-8 py-16 bg-zinc-900 rounded-3xl lg:px-16"}
       [:div {:class "absolute -top-32 -right-24 w-96 h-96 rounded-full bg-emerald-500/20 blur-3xl"}]
       [:div {:class "absolute -bottom-32 -left-24 w-96 h-96 rounded-full bg-emerald-500/10 blur-3xl"}]
       [:div {:class "relative text-center"}
        [:h2 {:class "text-3xl font-bold text-white tracking-tight text-balance lg:text-4xl"}
         "See what your money " [:span {:class "text-emerald-500"} "can do."]]
        [:p {:class "max-w-xl mx-auto mt-4 text-lg text-zinc-400"}
         "Create a free account and know what you earn, what you owe, and where it's all going."]
        [:div {:class "flex flex-wrap justify-center gap-3 mt-8"}
         [:a {:href (if signed-in? "/app" "/signup")
              :class "inline-block px-8 py-3.5 text-sm font-semibold text-emerald-700 bg-white rounded-xl transition-colors hover:bg-emerald-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 active:bg-emerald-100 active:scale-[0.97]"}
          (if signed-in? "Go to dashboard" "Create a free plan")]
         (shared/btn :variant :emerald-ghost :size :lg
                     :href "mailto:support@mygreed.co.za"
                     "Email us")]]]]]))
