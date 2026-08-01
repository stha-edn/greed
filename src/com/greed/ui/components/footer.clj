(ns com.greed.ui.components.footer)

(defn- footer-link [href label]
  [:a {:href href :class "text-sm text-zinc-400 hover:text-zinc-900 transition-colors"}
   label])

(defn footer []
  [:footer {:class "border-t border-zinc-200 bg-white"}
   [:div {:class "container mx-auto px-6 py-12"}
    [:div {:class "grid grid-cols-1 sm:grid-cols-3 gap-8 sm:gap-10"}
     [:div
      [:a {:href "/" :class "inline-block"}
       [:span {:class "text-3xl font-giza font-bold text-zinc-900 leading-none"} "greed."]]
      [:p {:class "mt-3 text-sm text-zinc-400 max-w-xs leading-relaxed"}
       "Making personal finance simple, transparent, and empowering for every South African."]]
     [:div {:class "sm:justify-self-center mt-4 sm:mt-0"}
      [:p {:class "text-sm font-semibold text-zinc-900 mb-3"} "Explore"]
      [:nav {:class "flex flex-col gap-2.5"}
       (footer-link "/" "Home")
       (footer-link "/about" "About")
       (footer-link "/team" "Team")
       (footer-link "/signin" "Sign In")
       (footer-link "/signup" "Sign Up")]]
     [:div {:class "sm:justify-self-end mt-4 sm:mt-0"}
      [:p {:class "text-sm font-semibold text-zinc-900 mb-3"} "Follow us"]
      [:a {:href "https://www.instagram.com/greed_za/"
           :target "_blank"
           :rel "noopener noreferrer"
           :aria-label "greed on Instagram"
           :class "flex items-center gap-2 text-sm font-medium text-zinc-500 hover:text-zinc-900 transition-colors"}
       [:svg {:xmlns "http://www.w3.org/2000/svg" :class "w-5 h-5" :viewBox "0 0 512 512"}
        [:path {:fill "currentColor" :d "M349.33,69.33a93.62,93.62,0,0,1,93.34,93.34V349.33a93.62,93.62,0,0,1-93.34,93.34H162.67a93.62,93.62,0,0,1-93.34-93.34V162.67a93.62,93.62,0,0,1,93.34-93.34H349.33m0-37.33H162.67C90.8,32,32,90.8,32,162.67V349.33C32,421.2,90.8,480,162.67,480H349.33C421.2,480,480,421.2,480,349.33V162.67C480,90.8,421.2,32,349.33,32Z M377.33,162.67a28,28,0,1,1,28-28A27.94,27.94,0,0,1,377.33,162.67Z M256,181.33A74.67,74.67,0,1,1,181.33,256,74.75,74.75,0,0,1,256,181.33M256,144A112,112,0,1,0,368,256,112,112,0,0,0,256,144Z"}]]
       "greed_za"]]]
    [:div {:class "mt-10 pt-6 border-t border-zinc-100 flex flex-col sm:flex-row items-center justify-between gap-2"}
     [:p {:class "text-xs text-zinc-400"} "© 2026 greed. All rights reserved."]
     [:p {:class "text-xs text-zinc-400"} "Made in South Africa"]]]])
