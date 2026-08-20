(ns com.greed.ui.pages.about
  (:require [com.greed.ui.components.shared :as shared]))

(defn- badge [label]
  [:div {:class "inline-flex items-center gap-2 px-3 py-1.5 mb-4 text-xs font-semibold text-emerald-700 bg-emerald-50 border border-emerald-100 rounded-full"}
   [:span {:class "w-1.5 h-1.5 bg-emerald-500 rounded-full"}]
   label])

(defn- value-card [title description class]
  [:div {:class (str "reveal p-8 bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card transition-all duration-200 hover:shadow-card-hover hover:-translate-y-0.5 hover:ring-zinc-300/70 " class)}
   [:h3 {:class "text-lg font-semibold text-zinc-900 tracking-tight"} title]
   [:p {:class "mt-2 text-sm text-zinc-500 leading-relaxed"} description]])

(defn page [{:keys [session]}]
  (let [signed-in? (some? (:uid session))]
    [:div {:class "container mx-auto px-6"}

     ;; Hero
     [:div {:class "py-16 lg:py-24"}
      [:div {:class "text-center max-w-2xl mx-auto"}
       (badge "About Greed")
       [:h1 {:class "text-4xl font-bold text-zinc-900 leading-[1.08] tracking-tight text-balance lg:text-5xl"}
        "Your money, " [:span {:class "text-emerald-600"} "in one clear plan."]]
       [:p {:class "mt-6 text-lg text-zinc-500 leading-relaxed"}
        "Greed is a personal finance wellbeing platform built for South Africa — for you. We bring your salary, tax, spending, savings and goals into one place, so you always know where you stand. No jargon, no judgment."]]]

     ;; What you get
     [:div {:class "pb-20 lg:pb-28"}
      [:div {:class "reveal text-center mb-10"}
       (badge "What you get")
       [:h2 {:class "text-3xl font-bold text-zinc-900 tracking-tight text-balance lg:text-4xl"}
        "Your money, " [:span {:class "text-emerald-600"} "made manageable."]]]
      [:div {:class "grid grid-cols-1 gap-6 max-w-4xl mx-auto md:grid-cols-3"}
       (value-card "Understand what you earn."
                   "See your real take-home pay and tax for the right SARS assessment year — before you plan anything else."
                   "reveal-1")
       (value-card "Plan before payday."
                   "Give every rand a job — commitments, savings and goals — before the first debit order moves."
                   "reveal-2")
       (value-card "See what's next."
                   "Spot shortfalls early, track your tax readiness and watch real progress toward the goals that matter to you."
                   "reveal-3")]]

     ;; Why the name
     [:div {:class "pb-20 lg:pb-28"}
      [:div {:class "reveal text-center max-w-2xl mx-auto"}
       [:div
        (badge "Why the name")
        [:h2 {:class "text-3xl font-bold text-zinc-900 tracking-tight text-balance lg:text-4xl"}
         "Greed means refusing to settle for "
         [:span {:class "text-emerald-600"} "financial confusion."]]
        [:p {:class "mt-6 text-lg text-zinc-500 leading-relaxed"}
         "For us, Greed doesn't mean taking without limits. It means wanting more — more security, more ownership, more choice — disciplined by a plan you can actually see."]]]]

     ;; Security
     [:div {:class "reveal pb-20 lg:pb-28"}
      [:div {:class "max-w-3xl mx-auto"}
       [:div {:class "px-8 py-16 bg-white ring-1 ring-zinc-200/70 rounded-3xl shadow-card text-center lg:px-16"}
        [:h2 {:class "text-3xl font-bold text-zinc-900 tracking-tight text-balance lg:text-4xl"}
         "Your financial life stays yours."]
        [:p {:class "max-w-2xl mx-auto mt-4 text-zinc-500 leading-relaxed"}
         "Keeping your data secure is our priority. Your information is stored safely and access is protected, so your money and your details stay private. We will never sell your data — and if you would like to know more about how we protect it, just ask."]]]]

     ;; Closing
     [:div {:class "reveal pb-24 lg:pb-32"}
      [:div {:class "relative overflow-hidden px-8 py-16 bg-zinc-900 rounded-3xl lg:px-16"}
       [:div {:class "absolute -top-32 -right-24 w-96 h-96 rounded-full bg-emerald-500/20 blur-3xl"}]
       [:div {:class "absolute -bottom-32 -left-24 w-96 h-96 rounded-full bg-emerald-500/10 blur-3xl"}]
       [:div {:class "relative text-center"}
        [:h2 {:class "text-3xl font-bold text-white tracking-tight text-balance lg:text-4xl"}
         "Built by people who " [:span {:class "text-emerald-500"} "refuse to settle."]]
        [:p {:class "max-w-xl mx-auto mt-4 text-lg text-zinc-400"}
         "Greed was started by a small South African team who got tired of guessing their way through tax, budgets and payday. We built the tool we wished we had — one that keeps your money clear, honest and yours."]
        [:div {:class "flex flex-wrap justify-center gap-3 mt-8"}
         [:a {:href "/team"
              :class "inline-block px-8 py-3.5 text-sm font-semibold text-emerald-700 bg-white rounded-xl transition-colors hover:bg-emerald-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 active:bg-emerald-100 active:scale-[0.97]"}
          "Meet the team"]
         (shared/btn :variant :emerald-ghost :size :lg
                     :href (if signed-in? "/app" "/signup")
                     "Start your own plan")]]]]]))
