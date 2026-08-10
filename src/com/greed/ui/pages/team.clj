(ns com.greed.ui.pages.team)

(defn- social-link [aria-label href icon-path]
  [:a {:href href :aria-label aria-label
       :target "_blank" :rel "noopener noreferrer"
       :class "text-zinc-400 transition-colors hover:text-zinc-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 active:text-zinc-600"}
   [:svg {:xmlns "http://www.w3.org/2000/svg" :class "w-4 h-4" :viewBox "0 0 512 512"}
    [:path {:fill "currentColor" :d icon-path}]]])

(defn- social-icons [{:keys [linkedin instagram]}]
  [:div {:class "flex items-center justify-center gap-3 mt-4"}
   (social-link "LinkedIn" (or linkedin "#")
     "M444.17,32H70.28C49.85,32,32,46.7,32,66.89V441.61C32,461.91,49.85,480,70.28,480H444.06C464.6,480,480,461.79,480,441.61V66.89C480.12,46.7,464.6,32,444.17,32ZM170.87,405.43H106.69V205.88h64.18ZM141,175.54h-.46c-20.54,0-33.84-15.29-33.84-34.43,0-19.49,13.65-34.42,34.65-34.42s33.85,14.82,34.31,34.42C175.65,160.25,162.35,175.54,141,175.54ZM405.43,405.43H341.25V296.32c0-26.14-9.34-44-32.56-44-17.74,0-28.24,12-32.91,23.69-1.75,4.2-2.22,9.92-2.22,15.76V405.43H209.38V205.88h64.18v27.77c9.34-13.3,23.93-32.44,57.88-32.44,42.13,0,74,27.77,74,87.64Z")
   (social-link "Instagram" (or instagram "#")
     "M349.33,69.33a93.62,93.62,0,0,1,93.34,93.34V349.33a93.62,93.62,0,0,1-93.34,93.34H162.67a93.62,93.62,0,0,1-93.34-93.34V162.67a93.62,93.62,0,0,1,93.34-93.34H349.33m0-37.33H162.67C90.8,32,32,90.8,32,162.67V349.33C32,421.2,90.8,480,162.67,480H349.33C421.2,480,480,421.2,480,349.33V162.67C480,90.8,421.2,32,349.33,32Z M377.33,162.67a28,28,0,1,1,28-28A27.94,27.94,0,0,1,377.33,162.67Z M256,181.33A74.67,74.67,0,1,1,181.33,256,74.75,74.75,0,0,1,256,181.33M256,144A112,112,0,1,0,368,256,112,112,0,0,0,256,144Z")])

(defn- team-member [{:keys [img name role linkedin instagram]}]
  [:div {:class "overflow-hidden bg-white border border-zinc-200/70 rounded-2xl shadow-card"}
   [:div {:class "px-8 pt-8 pb-2"}
    [:img {:src img
           :alt name
           :class "object-cover w-32 h-32 mx-auto border-4 border-white rounded-full shadow-card-md"}]]
   [:div {:class "text-center px-8 pb-8"}
    [:h3 {:class "mt-4 text-lg font-semibold text-zinc-900"} name]
    [:p {:class "mt-0.5 text-sm text-zinc-400"} role]
    (social-icons {:linkedin linkedin :instagram instagram})]])

(defn page []
  [:div {:class "container mx-auto px-6"}
   [:div {:class "py-16 lg:py-24"}
    [:div {:class "text-center mb-14"}
     [:div {:class "inline-flex items-center gap-2 px-3 py-1.5 mb-6 text-xs font-semibold text-emerald-700 bg-emerald-50 border border-emerald-100 rounded-full"}
      [:span {:class "w-1.5 h-1.5 bg-emerald-500 rounded-full"}]
      "The people behind greed"]
     [:h1 {:class "text-4xl font-bold text-zinc-900 leading-tight tracking-tight lg:text-5xl"}
      "Meet our "
      [:span {:class "text-emerald-600"} "team."]]
     [:p {:class "max-w-xl mx-auto mt-4 text-lg text-zinc-500"}
      "A small team on a mission to make personal finance simple, transparent, and empowering for every South African."]]
    [:div {:class "flex justify-center"}
      [:div {:class "grid grid-cols-1 gap-12 w-full max-w-2xl sm:grid-cols-2"}
      (team-member {:img "/img/IMG_3777.webp"
                    :name "Sithabiso Makhathini"
                    :role "Founder & CEO"
                    :instagram "https://www.instagram.com/stha.edn/"
                    :linkedin "https://www.linkedin.com/in/makhathinisithabiso/"})
      (team-member {:img "/img/nkululeko-mnyandu.jpg"
                    :name "Nkululeko Mnyandu"
                    :role "Co-founder"
                    :instagram "https://www.instagram.com/nsizwenhle_mpangazitha"
                    :linkedin "https://www.linkedin.com/in/nkululeko-mnyandu-208654181/"})]]]])
