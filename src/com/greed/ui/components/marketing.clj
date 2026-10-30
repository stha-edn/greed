(ns com.greed.ui.components.marketing
  "Shared building blocks for the marketing pages, set to the same system as
   the home page: one left edge, display type in the text face, no eyebrows,
   no cards, flat colour.")

(def btn-primary
  "inline-flex items-center justify-center rounded-md bg-brand-600 px-6 py-3 text-base font-semibold text-white transition-[transform,background-color] duration-150 ease-out hover:bg-brand-700 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:ring-offset-2")

(def link-ink
  "font-medium text-zinc-900 underline underline-offset-4 decoration-zinc-400 transition-colors hover:decoration-zinc-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 rounded-sm")

(defn page-head
  "First screen of a marketing page: the h1 and one sentence, on the page's
   left edge."
  [title lede]
  [:header {:class "pb-16 pt-14 sm:pt-20 lg:pb-24 lg:pt-28"}
   [:h1 {:class "enter max-w-[18ch] [text-wrap:balance] font-semibold text-zinc-900 [font-size:clamp(40px,5.5vw,80px)] leading-[1.02] tracking-[-0.04em]"}
    title]
   [:p {:class "enter mt-8 max-w-[52ch] text-xl leading-relaxed text-zinc-700" :style {"--d" "80ms"}}
    lede]])

(defn split
  "A section in two parts on the shared grid: its heading on the left, its
   content across the right seven columns."
  [{:keys [id heading]} & body]
  [:section {:id id :class "grid grid-cols-1 gap-8 py-16 lg:grid-cols-12 lg:gap-10 lg:py-28" :aria-labelledby (str id "-h")}
   [:h2 {:id (str id "-h") :class "enter text-[28px] font-semibold leading-tight tracking-[-0.02em] text-zinc-900 lg:col-span-4"}
    heading]
   (into [:div {:class "enter lg:col-span-7 lg:col-start-6" :style {"--d" "80ms"}}] body)])

(defn- preview-card
  "A cropped piece of the dashboard, so the close shows what the visitor gets."
  []
  [:div {:data-ui "" :class "w-full max-w-[380px] rounded-lg border border-zinc-200 bg-white p-6"}
   [:p {:class "text-sm font-medium text-zinc-500"} "Your month"]
   [:p {:class "mt-3 flex items-baseline gap-2"}
    [:span {:class "text-4xl font-semibold tracking-tight tabular-nums text-zinc-900"} "R23,024"]
    [:span {:class "text-sm text-zinc-500"} "left to plan"]]
   [:div {:class "mt-5 flex h-2 w-full overflow-hidden rounded-full bg-zinc-100"}
    [:div {:class "h-full bg-zinc-800" :style {:width "35%"}}]
    [:div {:class "h-full bg-zinc-400" :style {:width "8%"}}]
    [:div {:class "h-full bg-brand-500" :style {:width "57%"}}]]
   [:div {:class "mt-4 grid grid-cols-3 gap-3 text-sm tabular-nums"}
    (for [[l v] [["Expenses" "R14,000"] ["Savings" "R3,500"] ["Payday" "25th"]]]
      [:div [:p {:class "text-xs text-zinc-500"} l] [:p {:class "mt-0.5 font-semibold text-zinc-900"} v]])]])

(defn action-tile
  "The close of a marketing page: the invitation on the left, a piece of the
   dashboard it leads to on the right, and three plain facts underneath.
   Neutral ground; green only inside the product preview."
  [{:keys [id heading line primary secondary ground]}]
  [:section {:id id :class (or ground "bg-stone-100") :aria-labelledby (str id "-h")}
   [:div {:class "container mx-auto px-6 py-20 lg:py-28"}
    [:div {:class "grid grid-cols-1 items-center gap-12 lg:grid-cols-12 lg:gap-10"}
     [:div {:class "enter lg:col-span-7"}
      [:h2 {:id (str id "-h") :class "max-w-[16ch] [text-wrap:balance] font-semibold text-zinc-900 [font-size:clamp(36px,4.6vw,68px)] leading-[1.02] tracking-[-0.04em]"} heading]
      [:p {:class "mt-6 max-w-[46ch] text-xl leading-relaxed text-zinc-700"} line]
      [:div {:class "mt-10 flex flex-wrap items-center gap-x-6 gap-y-4"}
       (when primary
         [:a {:href (:href primary)
              :class "inline-flex items-center justify-center rounded-md bg-zinc-900 px-6 py-3 text-base font-semibold text-white transition-[transform,background-color] duration-150 ease-out hover:bg-zinc-700 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 focus-visible:ring-offset-2"}
          (:label primary)])
       (when secondary
         [:a {:href (:href secondary) :class (str "text-base " link-ink)} (:label secondary)])]]
     [:div {:class "enter flex justify-center lg:col-span-5 lg:justify-end" :style {"--d" "100ms"}}
      [:div {:class "rotate-[1.5deg]"} (preview-card)]]]
    [:dl {:class "enter mt-16 grid grid-cols-1 gap-8 sm:grid-cols-3 lg:mt-24" :style {"--d" "160ms"}}
     (for [[k v] [["Free" "to use. There is nothing to pay."]
                  ["SARS 2026/27" "tables, the same ones the calculator uses"]
                  ["Never" "sells your data, to anyone"]]]
       [:div
        [:dt {:class "text-[28px] font-semibold tracking-[-0.02em] text-zinc-900"} k]
        [:dd {:class "mt-1 max-w-[30ch] text-[17px] leading-relaxed text-zinc-700"} v]])]]])

(def enter-css
  "Entrance for the parts of each section; content stays visible without script."
  "
html.js .enter{opacity:0;transform:translateY(24px);transition:opacity .7s cubic-bezier(.16,1,.3,1) var(--d,0ms),transform .7s cubic-bezier(.16,1,.3,1) var(--d,0ms)}
html.js .enter.in{opacity:1;transform:none}
@media (prefers-reduced-motion:reduce){html.js .enter{opacity:1;transform:none;transition:none}}
")

(def enter-js
  "
(function(){
  document.documentElement.classList.add('js');
  var io=new IntersectionObserver(function(es){es.forEach(function(e){if(e.isIntersecting){e.target.classList.add('in');io.unobserve(e.target);}});},{rootMargin:'0px 0px -8% 0px'});
  document.querySelectorAll('.enter').forEach(function(el){io.observe(el);});
  setTimeout(function(){document.querySelectorAll('.enter').forEach(function(el){el.classList.add('in');});},2500);
})();
")
