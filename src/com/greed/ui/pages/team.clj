(ns com.greed.ui.pages.team
  (:require [com.biffweb :as biff]
            [com.greed.ui.components.marketing :as m]))

(defn- person [{:keys [img name role linkedin instagram pos d]}]
  [:figure {:class "enter group overflow-hidden rounded-3xl bg-white ring-1 ring-zinc-200/70 transition duration-300 hover:-translate-y-1 hover:shadow-card-hover" :style {"--d" d}}
   [:div {:class "relative overflow-hidden"}
    [:img {:src img :alt name :width 600 :height 600 :loading "lazy"
           :class (str "aspect-square w-full bg-zinc-200 object-cover transition duration-500 group-hover:scale-[1.03] " pos)}]
    [:div {:aria-hidden "true" :class "absolute inset-x-0 bottom-0 h-1/3 bg-gradient-to-t from-brand-950/60 to-transparent"}]]
   [:figcaption {:class "p-6"}
    [:h2 {:class "text-xl font-semibold tracking-[-0.02em] text-zinc-900"} name]
    [:p {:class "mt-1 text-[15px] text-zinc-600"} role]
    [:p {:class "mt-4 flex gap-2"}
     (for [[href label] [[linkedin "LinkedIn"] [instagram "Instagram"]]]
       [:a {:href href :target "_blank" :rel "noopener noreferrer"
            :class "rounded-full bg-zinc-100 px-3 py-1 text-xs font-semibold text-zinc-700 transition hover:bg-brand-50 hover:text-brand-700"} label])]]])

(defn page []
  [:div {:class "bg-white"}
   [:style (biff/unsafe m/enter-css)]
   (m/page-hero
    {:kicker "Team"
     :title "Meet the team."
     :line "Two South Africans making money simple, transparent and calm, for every person and small business that wants to know where the money goes."
     :actions [[:a {:href "/contact" :class m/btn-white} "Get in touch" [:span {:aria-hidden "true"} "→"]]]})
   [:section {:class "container mx-auto px-6 py-20 lg:py-24" :aria-label "Team"}
    [:div {:class "mx-auto grid max-w-4xl grid-cols-1 gap-8 sm:grid-cols-2"}
     (person {:img "/img/IMG_3777.webp" :name "Sithabiso Makhathini" :role "Founder & CEO"
              :linkedin "https://www.linkedin.com/in/makhathinisithabiso/"
              :instagram "https://www.instagram.com/stha.edn/"
              :pos "object-center" :d "60ms"})
     (person {:img "/img/nkululeko-mnyandu.jpg" :name "Nkululeko Mnyandu" :role "Co-founder"
              :linkedin "https://www.linkedin.com/in/nkululeko-mnyandu-208654181/"
              :instagram "https://www.instagram.com/nsizwenhle_mpangazitha"
              :pos "object-[50%_20%]" :d "120ms"})]]
   [:script (biff/unsafe m/enter-js)]])
