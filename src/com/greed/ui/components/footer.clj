(ns com.greed.ui.components.footer)

(def ^:private link
  "text-sm text-zinc-600 transition-colors hover:text-brand-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 rounded-sm")

(defn- column [title links]
  [:div
   [:p {:class "text-xs font-semibold uppercase tracking-[0.08em] text-zinc-900"} title]
   [:ul {:class "mt-4 space-y-3"}
    (for [[href label external?] links]
      [:li [:a (cond-> {:href href :class link} external? (assoc :target "_blank" :rel "noopener noreferrer")) label]])]])

(defn footer
  "Marketing close: what greed is and its promise on the left, then the
   product, company and legal links, and the year underneath."
  []
  [:footer {:class "border-t border-zinc-200/70 bg-zinc-50"}
   [:div {:class "container mx-auto px-6 pb-10 pt-16"}
    [:div {:class "grid grid-cols-2 gap-10 lg:grid-cols-[minmax(0,1.6fr)_repeat(3,minmax(0,1fr))]"}
     [:div {:class "col-span-2 lg:col-span-1"}
      [:a {:href "/" :class "inline-block" :aria-label "greed home"}
       [:img {:src "/img/greed-symbol-green-tight.svg" :alt "greed" :width 192 :height 192 :class "block h-9 w-9"}]]
      [:p {:class "mt-4 max-w-[34ch] text-sm leading-relaxed text-zinc-600"}
       "Money and business finance for South Africans. Read your statements, plan your month, and know what SARS takes."]]
     (column "Product" [["/#features" "Features"] ["/#business" "For business"] ["/pricing" "Pricing"] ["/signup" "Start free"]])
     (column "Company" [["/about" "About"] ["/team" "Team"] ["/contact" "Contact"] ["https://www.instagram.com/greed_za/" "Instagram" true]])
     (column "Legal" [["/privacy" "Privacy"] ["/terms" "Terms"]])]
    [:div {:class "mt-14 flex flex-col gap-2 border-t border-zinc-200/70 pt-6 text-xs text-zinc-500 sm:flex-row sm:items-center sm:justify-between"}
     [:p "© 2026 greed. Made in South Africa."]
     [:p "Estimates only, not tax or financial advice."]]]])

(defn app-footer []
  [:footer {:class "border-t border-zinc-200 bg-white"}
   [:div {:class "container mx-auto px-6 py-6"}
    [:div {:class "flex flex-col items-center justify-between gap-4 sm:flex-row"}
     [:img {:src "/img/greed-symbol-green-tight.svg"
            :alt "greed"
            :width 192 :height 192
            :class "h-6 w-auto"}]
     [:nav {:class "flex items-center gap-6"}
      [:a {:href "/privacy" :class "text-sm text-zinc-500 hover:text-zinc-900 transition-colors"} "Privacy"]
      [:a {:href "/terms" :class "text-sm text-zinc-500 hover:text-zinc-900 transition-colors"} "Terms"]
      [:a {:href "/contact" :class "text-sm text-zinc-500 hover:text-zinc-900 transition-colors"} "Contact"]]
     [:p {:class "text-xs text-zinc-500"} "© 2026 · Made in South Africa"]]]])
