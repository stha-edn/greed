(ns com.greed.ui.components.footer)

(def ^:private link
  "text-zinc-900 underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 rounded-sm")

(defn footer
  "Marketing close: one quiet line on a single tone step below the page. What this is on the left,
   the links in the middle, the year and privacy on the right."
  []
  [:footer {:class "bg-zinc-100"}
   [:div {:class "container mx-auto grid grid-cols-1 gap-2 px-6 py-10 text-sm sm:grid-cols-[1fr_auto_1fr] sm:items-baseline sm:gap-6 sm:text-[15px]"}
    [:p {:class "flex items-center gap-2 text-zinc-600"}
     [:a {:href "/" :class "inline-block"}
      [:img {:src "/img/greed-symbol-green-tight.svg" :alt "greed" :width 192 :height 192 :class "block h-5 w-5"}]]
     "Personal finance for South Africans"]
    [:nav {:aria-label "Footer" :class "flex flex-wrap gap-x-6 gap-y-1"}
     [:a {:href "/about" :class link} "About"]
     [:a {:href "/team" :class link} "Team"]
     [:a {:href "/contact" :class link} "Contact"]
     [:a {:href "https://www.instagram.com/greed_za/" :target "_blank" :rel "noopener noreferrer" :class link} "Instagram"]]
    [:p {:class "flex gap-6 text-zinc-600 sm:justify-end"}
     [:span "© 2026 greed"]
     [:a {:href "/privacy" :class link} "Privacy"]
     [:a {:href "/terms" :class link} "Terms"]]]])

(defn app-footer []
  [:footer {:class "border-t border-zinc-200 bg-white"}
   [:div {:class "container mx-auto px-6 py-6"}
    [:div {:class "flex flex-col items-center justify-between gap-4 sm:flex-row"}
     [:img {:src "/img/greed-wordmark-green-tight.svg"
            :alt "greed"
            :width 648 :height 192
            :class "h-6 w-auto"}]
     [:nav {:class "flex items-center gap-6"}
      [:a {:href "/privacy" :class "text-sm text-zinc-500 hover:text-zinc-900 transition-colors"} "Privacy"]
      [:a {:href "/terms" :class "text-sm text-zinc-500 hover:text-zinc-900 transition-colors"} "Terms"]
      [:a {:href "/contact" :class "text-sm text-zinc-500 hover:text-zinc-900 transition-colors"} "Contact"]]
     [:p {:class "text-xs text-zinc-500"} "© 2026 · Made in South Africa"]]]])
