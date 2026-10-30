(ns com.greed.ui.pages.team
  (:require [com.biffweb :as biff]
            [com.greed.ui.components.marketing :as m]))

(defn- person [{:keys [img name role linkedin instagram pos d]}]
  [:figure {:class "enter" :style {"--d" d}}
   [:img {:src img :alt name :width 600 :height 600 :loading "lazy"
          :class (str "aspect-square w-full rounded-lg bg-zinc-200 object-cover " pos)}]
   [:figcaption {:class "mt-5"}
    [:h2 {:class "text-2xl font-semibold tracking-[-0.02em] text-zinc-900"} name]
    [:p {:class "mt-1 text-[17px] text-zinc-700"} role]
    [:p {:class "mt-3 flex gap-5 text-base"}
     [:a {:href linkedin :target "_blank" :rel "noopener noreferrer" :class m/link-ink} "LinkedIn"]
     [:a {:href instagram :target "_blank" :rel "noopener noreferrer" :class m/link-ink} "Instagram"]]]])

(defn page []
  [:div
   [:style (biff/unsafe m/enter-css)]
   [:div {:class "bg-stone-100"}
    [:div {:class "container mx-auto grid grid-cols-1 gap-12 px-6 pb-20 pt-14 sm:pt-20 lg:grid-cols-12 lg:gap-10 lg:pb-28 lg:pt-24"}
     [:div {:class "lg:col-span-5"}
      [:h1 {:class "enter max-w-[12ch] font-semibold text-zinc-900 [font-size:clamp(44px,6vw,92px)] leading-[1.0] tracking-[-0.045em]"}
       "Meet the team."]
      [:p {:class "enter mt-8 max-w-[40ch] text-xl leading-relaxed text-zinc-700" :style {"--d" "80ms"}}
       "Two South Africans making personal finance simple, transparent and calm, for every salaried person who wants to know where their money goes."]
      [:p {:class "enter mt-8 text-[17px] text-zinc-800" :style {"--d" "120ms"}}
       "Want to talk to us? " [:a {:href "/contact" :class m/link-ink} "Get in touch"] "."]]
     [:section {:class "grid grid-cols-1 gap-10 sm:grid-cols-2 lg:col-span-7 lg:col-start-6" :aria-label "Team"}
      (person {:img "/img/IMG_3777.webp" :name "Sithabiso Makhathini" :role "Founder & CEO"
               :linkedin "https://www.linkedin.com/in/makhathinisithabiso/"
               :instagram "https://www.instagram.com/stha.edn/"
               :pos "object-center" :d "60ms"})
      (person {:img "/img/nkululeko-mnyandu.jpg" :name "Nkululeko Mnyandu" :role "Co-founder"
               :linkedin "https://www.linkedin.com/in/nkululeko-mnyandu-208654181/"
               :instagram "https://www.instagram.com/nsizwenhle_mpangazitha"
               :pos "object-[50%_20%]" :d "120ms"})]]]
   [:script (biff/unsafe m/enter-js)]])
