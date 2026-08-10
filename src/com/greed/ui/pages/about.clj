(ns com.greed.ui.pages.about)

(defn- badge [label]
  [:div {:class "inline-flex items-center gap-2 px-3 py-1.5 mb-4 text-xs font-semibold text-emerald-700 bg-emerald-50 border border-emerald-100 rounded-full"}
   [:span {:class "w-1.5 h-1.5 bg-emerald-500 rounded-full"}]
   label])

(defn- heading [title]
  [:h2 {:class "text-3xl font-bold text-zinc-900 tracking-tight lg:text-4xl"} title])

(defn- paragraph [text]
  [:p {:class "mt-4 text-zinc-500 leading-relaxed"} text])

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
       (heading "Your money, made manageable.")]
      [:div {:class "grid grid-cols-1 gap-6 max-w-4xl mx-auto md:grid-cols-3"}
       [:div {:class "reveal reveal-1 p-8 bg-white border border-zinc-200/70 rounded-2xl shadow-card transition-all duration-200 hover:shadow-card-hover hover:-translate-y-0.5"}
        [:h3 {:class "text-lg font-semibold text-zinc-900"} "Understand what you earn."]
        [:p {:class "mt-2 text-sm text-zinc-500 leading-relaxed"} "See your real take-home pay and tax for the right SARS assessment year — before you plan anything else."]]
       [:div {:class "reveal reveal-2 p-8 bg-white border border-zinc-200/70 rounded-2xl shadow-card transition-all duration-200 hover:shadow-card-hover hover:-translate-y-0.5"}
        [:h3 {:class "text-lg font-semibold text-zinc-900"} "Plan before payday."]
        [:p {:class "mt-2 text-sm text-zinc-500 leading-relaxed"} "Give every rand a job — commitments, savings and goals — before the first debit order moves."]]
       [:div {:class "reveal reveal-3 p-8 bg-white border border-zinc-200/70 rounded-2xl shadow-card transition-all duration-200 hover:shadow-card-hover hover:-translate-y-0.5"}
        [:h3 {:class "text-lg font-semibold text-zinc-900"} "See what's next."]
        [:p {:class "mt-2 text-sm text-zinc-500 leading-relaxed"} "Spot shortfalls early, track your tax readiness and watch real progress toward the goals that matter to you."]]]]

     ;; Why the name
     [:div {:class "reveal pb-20 lg:pb-28"}
      [:div {:class "max-w-3xl mx-auto"}
       (badge "Why the name")
       (heading "Greed means refusing to settle for financial confusion.")
       (paragraph
        "For us, Greed doesn't mean taking without limits. It means wanting more — more security, more ownership, more choice — disciplined by a plan you can actually see.")]]

     ;; Security
     [:div {:class "reveal pb-20 lg:pb-28"}
      [:div {:class "max-w-3xl mx-auto"}
        [:div {:class "px-8 py-12 bg-white border border-zinc-200/70 rounded-3xl shadow-card lg:px-16"}
        [:div {:class "text-center"}
         (badge "Your data, secured")
         [:h2 {:class "text-3xl font-bold text-zinc-900 tracking-tight text-balance lg:text-4xl"} "Your financial life stays yours."]
         [:p {:class "max-w-2xl mx-auto mt-4 text-zinc-500 leading-relaxed"}
          "Keeping your data secure is our priority. Your information is stored safely and access is protected, so your money and your details stay private. We will never sell your data — and if you would like to know more about how we protect it, just ask."]]]]]

     ;; CTA
     [:div {:class "reveal pb-24 lg:pb-32"}
      [:div {:class "relative overflow-hidden px-8 py-16 bg-zinc-900 rounded-3xl lg:px-16"}
       [:div {:class "absolute -top-32 -right-24 w-96 h-96 rounded-full bg-emerald-500/20 blur-3xl"}]
       [:div {:class "absolute -bottom-32 -left-24 w-96 h-96 rounded-full bg-emerald-500/10 blur-3xl"}]
       [:div {:class "relative text-center"}
        [:h2 {:class "text-3xl font-bold text-white tracking-tight text-balance lg:text-4xl"}
         "Your money should " [:span {:class "text-emerald-500"} "make sense."]]
        [:p {:class "max-w-xl mx-auto mt-4 text-lg text-zinc-400"}
         "In one short session, know what you earn, what you owe, where your money is going and what you can build next."]
        [:a {:href (if signed-in? "/app" "/signup")
             :class "inline-block px-8 py-3.5 mt-8 text-sm font-semibold text-emerald-700 bg-white rounded-xl transition-colors hover:bg-emerald-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 active:bg-emerald-100 active:scale-[0.97]"}
         "Build my money plan"]]]]]))
