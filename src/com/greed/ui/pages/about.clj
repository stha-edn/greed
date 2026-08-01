(ns com.greed.ui.pages.about)

(defn- badge [label]
  [:div {:class "inline-flex items-center gap-2 px-3 py-1.5 bg-emerald-50 text-emerald-700 text-xs font-semibold rounded-full mb-4 border border-emerald-100"}
   [:span {:class "w-1.5 h-1.5 rounded-full bg-emerald-500"}]
   label])

(defn- heading [title]
  [:h2 {:class "text-3xl font-bold text-zinc-900 lg:text-4xl"} title])

(defn- paragraph [text]
  [:p {:class "mt-4 text-zinc-500 leading-relaxed"} text])

(defn page [{:keys [session]}]
  (let [signed-in? (some? (:uid session))]
  [:div {:class "container mx-auto px-6"}

   ;; Hero
   [:div {:class "py-16 lg:py-24"}
    [:div {:class "max-w-2xl mx-auto text-center"}
     (badge "The story behind greed")
     [:h1 {:class "text-4xl font-bold text-zinc-900 lg:text-5xl leading-tight"}
      "Ambition, "
      [:span {:class "text-emerald-600"} "organised."]]
     [:p {:class "mt-6 text-lg text-zinc-500 leading-relaxed"}
      "Greed is a personal finance wellbeing platform built for South Africa. We help people turn what they earn into the life they want — by connecting salary, tax, spending, savings, goals and important dates into one clear plan, without judgment."]]]

   ;; Why the name
   [:div {:class "pb-20 lg:pb-28"}
    [:div {:class "max-w-3xl mx-auto"}
     (badge "Why the name")
     (heading "Greed means refusing to settle for financial confusion.")
     (paragraph
      "For us, Greed does not mean taking without limits. It means refusing to settle for financial confusion, fragility or a future designed by accident. It is the desire for more security, more ownership, more choice and more possibility — disciplined by a plan.")
     (paragraph
      "The full stop in \"greed.\" gives the word a boundary. Ambition is powerful when it is contained by intention, responsibility and action. The period also makes the wordmark feel decisive: this is not a question or a promise of instant wealth. It is a system.")]]

   ;; Manifesto
   [:div {:class "pb-20 lg:pb-28"}
    [:div {:class "max-w-3xl mx-auto"}
     [:div {:class "text-center mb-8"}
      (badge "Our manifesto")]
     [:div {:class "relative bg-zinc-900 rounded-3xl px-8 py-16 lg:px-16 overflow-hidden"}
      [:div {:class "absolute inset-x-0 top-0 h-1 bg-emerald-500"}]
      [:svg {:class "mx-auto w-10 h-10 text-emerald-500 mb-8" :fill "currentColor" :viewBox "0 0 24 24"}
       [:path {:d "M14.017 21v-7.391C14.017 8.263 16.47 5.5 21 5.5v3c-2.278.001-3.983 1.14-3.983 2.5h3.966v10H14.017zM3 21v-7.391C3 8.263 5.453 5.5 10 5.5v3C7.722 8.501 6.017 9.64 6.017 11H10v10H3z"}]]
      [:p {:class "text-xl lg:text-2xl font-medium text-zinc-100 leading-relaxed"}
       "We want more. Not more noise, pressure or empty status. More certainty when payday arrives. More confidence when tax season opens. More room to help the people who depend on us without abandoning our own future. More progress we can actually see."]
      [:p {:class "mt-6 text-lg text-zinc-400 leading-relaxed"}
       "We believe ambition is not the problem. The problem is ambition without a plan. So we bring every part of our financial life into one clear place — and build control, one decision at a time."]
      [:div {:class "mt-10 flex items-center justify-center gap-4"}
       [:div {:class "h-px w-10 bg-emerald-500"}]
       [:p {:class "text-sm font-semibold uppercase tracking-[0.2em] text-emerald-500"} "Ambition, organised."]
       [:div {:class "h-px w-10 bg-emerald-500"}]]]]]

   ;; The imprint
   [:div {:class "pb-20 lg:pb-28"}
    [:div {:class "mb-10 text-center"}
     (badge "The imprint")
     (heading "Whenever someone meets Greed, they should feel three things.")
     (paragraph "In order: understood, in control, and clear on what to do next.")]
    [:div {:class "grid grid-cols-1 md:grid-cols-3 gap-6 max-w-4xl mx-auto"}
     [:div {:class "bg-white rounded-2xl border border-zinc-100 shadow-card p-8"}
      [:span {:class "text-2xl"} "01"]
      [:h3 {:class "mt-4 text-lg font-semibold text-zinc-900"} "They understand my reality."]
      [:p {:class "mt-2 text-sm text-zinc-500 leading-relaxed"} "We speak to the real pressure of a month that disappears — without ever making the user feel irresponsible, unintelligent or alone."]]
     [:div {:class "bg-white rounded-2xl border border-zinc-100 shadow-card p-8"}
      [:span {:class "text-2xl"} "02"]
      [:h3 {:class "mt-4 text-lg font-semibold text-zinc-900"} "This makes my money feel manageable."]
      [:p {:class "mt-2 text-sm text-zinc-500 leading-relaxed"} "One clear picture instead of fragmented bank screens: what is committed, what is safe, and what is available to plan."]]
     [:div {:class "bg-white rounded-2xl border border-zinc-100 shadow-card p-8"}
      [:span {:class "text-2xl"} "03"]
      [:h3 {:class "mt-4 text-lg font-semibold text-zinc-900"} "I can see what to do next."]
      [:p {:class "mt-2 text-sm text-zinc-500 leading-relaxed"} "Every number leads to one useful action — and every action becomes visible progress toward a goal that actually matters."]]]]

   ;; Who it's for
   [:div {:class "pb-20 lg:pb-28"}
    [:div {:class "grid grid-cols-1 lg:grid-cols-2 gap-12 items-center"}
     [:div
      (badge "Who it's for")
      (heading "Built for the Responsible Striver.")
      (paragraph
       "Our primary user is a mobile-first salaried South African, usually 23 to 40, managing recurring commitments and at least one meaningful future goal. They are not careless — their financial life is simply more complex than their current tools help them organise.")
      [:div {:class "mt-6 bg-white rounded-2xl border border-zinc-100 shadow-card p-6"}
       [:p {:class "text-zinc-400 text-sm font-medium"} "PRIVATE THOUGHT"]
       [:p {:class "mt-2 text-lg font-semibold text-zinc-900"} "“I earn money, so why do I still feel uncertain?”"]
       [:p {:class "mt-3 text-sm text-zinc-500 leading-relaxed"} "The pain is not only shortage. It is the absence of a reliable system — the sense that every month disappears faster than expected."]]]
     [:div {:class "space-y-4"}
      (for [{:keys [title body]} [{:title "Salary decoded"
                                    :body "See your true take-home pay and tax for the right SARS assessment year — before you plan anything else."}
                                   {:title "Give every rand a job"
                                    :body "Allocate income to commitments, savings and goals before the first debit order moves."}
                                   {:title "See risk early"
                                    :body "Upcoming shortfalls, tax readiness and an emergency buffer — early warning instead of surprise."}
                                   {:title "Make the future visible"
                                    :body "Turn one meaningful goal into a date, a monthly checkpoint and progress you can actually see."}]]
        [:div {:class "bg-white rounded-2xl border border-zinc-100 shadow-card p-6"}
         [:h4 {:class "text-sm font-semibold text-zinc-900"} title]
         [:p {:class "mt-1.5 text-sm text-zinc-500 leading-relaxed"} body]])]]]

   ;; What Greed is / is not
   [:div {:class "pb-20 lg:pb-28"}
    [:div {:class "mb-10 text-center"}
     (badge "What we stand for")
     (heading "Greed is — and is not.")]
    [:div {:class "grid grid-cols-1 md:grid-cols-2 gap-6 max-w-4xl mx-auto"}
     [:div {:class "bg-white rounded-2xl border border-zinc-100 shadow-card p-8"}
      [:p {:class "text-sm font-semibold text-zinc-900 mb-5"} "GREED IS"]
      (for [item ["A calm co-pilot"
                  "A connected plan, not a collection of calculators"
                  "Ambitious and responsible"
                  "South Africa by default — real tax, rand and reality"
                  "Educational, explainable and progressive"]]
        [:div {:class "flex items-start gap-3 py-2"}
         [:span {:class "mt-0.5 flex-shrink-0 w-5 h-5 rounded-full bg-emerald-50 flex items-center justify-center"}
          [:svg {:class "w-3 h-3 text-emerald-600" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
           [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "3" :d "M5 13l4 4L19 7"}]]]
         [:span {:class "text-sm text-zinc-600"} item]])]
     [:div {:class "bg-white rounded-2xl border border-zinc-100 shadow-card p-8"}
      [:p {:class "text-sm font-semibold text-zinc-900 mb-5"} "GREED IS NOT"]
      (for [item ["A scolding financial parent"
                  "Get-rich-quick, trading hype or gambling energy"
                  "A global template with rand symbols added"
                  "Opaque advice you must trust blindly"
                  "Overwhelming from the first session"]]
        [:div {:class "flex items-start gap-3 py-2"}
         [:span {:class "mt-0.5 flex-shrink-0 w-5 h-5 rounded-full bg-zinc-100 flex items-center justify-center"}
          [:svg {:class "w-3 h-3 text-zinc-400" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
           [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "3" :d "M6 18L18 6M6 6l12 12"}]]]
         [:span {:class "text-sm text-zinc-600"} item]])]]]

   ;; Principles
   [:div {:class "pb-20 lg:pb-28"}
    [:div {:class "mb-10 text-center"}
     (badge "Our principles")
     (heading "The rules we won't break.")]
    [:div {:class "grid grid-cols-1 sm:grid-cols-2 gap-6 max-w-4xl mx-auto"}
     [:div {:class "bg-zinc-900 rounded-2xl p-8"}
      [:h3 {:class "text-sm font-semibold text-emerald-500 mb-2"} "Relief before ambition"]
      [:p {:class "text-sm text-zinc-300 leading-relaxed"} "We earn ambition by first creating relief, organisation and control. The user must feel safe enough to look before we ask them to want more."]]
     [:div {:class "bg-zinc-900 rounded-2xl p-8"}
      [:h3 {:class "text-sm font-semibold text-emerald-500 mb-2"} "No shame, no fear"]
      [:p {:class "text-sm text-zinc-300 leading-relaxed"} "We never use shame, fear, fake scarcity or unrealistic financial promises to drive engagement. Money is private — we build trust with honesty."]]
     [:div {:class "bg-zinc-900 rounded-2xl p-8"}
      [:h3 {:class "text-sm font-semibold text-emerald-500 mb-2"} "Proudly South African"]
      [:p {:class "text-sm text-zinc-300 leading-relaxed"} "Our logic, tax tables, currency, examples and financial realities are South African by default — never a global template with rand symbols added."]]
     [:div {:class "bg-zinc-900 rounded-2xl p-8"}
      [:h3 {:class "text-sm font-semibold text-emerald-500 mb-2"} "Trust before connection"]
      [:p {:class "text-sm text-zinc-300 leading-relaxed"} "We start with manual entry and CSV import. Secure bank integrations come only after trust, security and consent are proven — and your data stays yours."]]]]

   ;; CTA
   [:div {:class "pb-24 lg:pb-32"}
    [:div {:class "relative bg-zinc-900 rounded-3xl px-8 py-16 lg:px-16 overflow-hidden"}
     [:div {:class "absolute -top-32 -right-24 w-96 h-96 rounded-full bg-emerald-500/20 blur-3xl"}]
     [:div {:class "absolute -bottom-32 -left-24 w-96 h-96 rounded-full bg-emerald-500/10 blur-3xl"}]
     [:div {:class "relative text-center"}
      [:h2 {:class "text-3xl lg:text-4xl font-bold text-white"}
       "Your money should "
       [:span {:class "text-emerald-500"} "make sense."]]
      [:p {:class "mt-4 text-lg text-zinc-400 max-w-xl mx-auto"}
       "In one short session, know what you earn, what you owe, where your money is going and what you can build next."]
      [:a {:href (if signed-in? "/app" "/signup")
           :class "mt-8 inline-block px-8 py-3.5 text-sm font-semibold text-emerald-700 bg-white rounded-xl hover:bg-emerald-50 transition-colors"}
       "Build my money plan"]
      [:div {:class "mt-12 pt-6 border-t border-zinc-800 flex flex-wrap justify-center gap-x-8 gap-y-3"}
       (for [text ["SARS-aligned estimates" "No bank access required" "Your data stays yours"]]
         [:div {:class "flex items-center gap-2 text-sm text-zinc-400"}
          [:svg {:class "w-4 h-4 text-emerald-500" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
           [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2.5" :d "M5 13l4 4L19 7"}]]
          text])]]]]]))
