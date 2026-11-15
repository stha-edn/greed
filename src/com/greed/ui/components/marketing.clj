(ns com.greed.ui.components.marketing
  "Shared building blocks for the marketing pages, set to the same system as
   the home page: one left edge, display type in the text face, no eyebrows,
   no cards, flat colour."
  (:require [com.greed.ui.components.hero :as hero]))

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

(defn shot
  "A piece of the app as it looks today: the header card's label, figure,
   reading, bars and up to three stats. Used by every marketing page."
  [{:keys [label badge value unit reading bars stats tone]}]
  [:div {:data-ui "" :class "rounded-lg bg-white p-5 text-left text-zinc-900 shadow-card-md ring-1 ring-zinc-200/70 sm:p-6"}
   [:div {:class "flex items-center gap-2.5"}
    [:p {:class "text-sm font-medium text-zinc-500"} label]
    [:div {:class "flex-1"}]
    (when badge (hero/badge badge :tone tone))]
   [:p {:class "mt-3 flex flex-wrap items-baseline gap-x-2"}
    [:span {:class (str "text-3xl font-semibold tracking-[-0.03em] tabular-nums " (if (= :rose tone) "text-rose-700" "text-zinc-900"))} value]
    (when unit [:span {:class "text-sm font-medium text-zinc-500"} unit])]
   (when reading [:p {:class "mt-1.5 text-sm text-zinc-500"} reading])
   (when bars [:div {:class "mt-5"} (hero/bars bars)])
   (when (seq stats)
     [:div {:class "mt-5 grid grid-cols-3 gap-3 border-t border-zinc-100 pt-4"} ; taste-skill-ignore: rule inside a data-ui fragment
      (for [[l v] stats]
        [:div {:class "min-w-0"}
         [:p {:class "truncate text-xs font-medium text-zinc-500"} l]
         [:p {:class "mt-1 truncate text-sm font-semibold tabular-nums text-zinc-900"} v]])])])

(defn item-row
  "A budget row as the app shows it: title, its Monthly or From-statement
   tag, an optional reference match, and the amount." [title amount tag & [ref]]
  [:div {:class "flex items-center justify-between gap-4 py-3"}
   [:div {:class "min-w-0"}
    [:p {:class "truncate text-sm font-medium text-zinc-900"} title]
    [:p {:class "mt-1 flex flex-wrap items-center gap-2 text-xs text-zinc-500"}
     [:span {:class (str "rounded-full px-2 py-0.5 font-medium ring-1 "
                         (if (= tag "Monthly") "bg-zinc-100 text-zinc-700 ring-zinc-200" "bg-brand-50 text-brand-700 ring-brand-600/20"))}
      tag]
     (when ref [:span (str "Matches: " ref)])]]
   [:span {:class "text-sm font-semibold tabular-nums text-zinc-900"} amount]])

(defn- preview-card
  "A cropped piece of the app's Home card, so the close shows what the
   visitor gets: the same bars as every tab's header card."
  []
  [:div {:data-ui "" :class "w-full max-w-[380px] rounded-lg bg-white p-6 shadow-card-md ring-1 ring-zinc-200/70"}
   [:p {:class "text-sm font-medium text-zinc-500"} "October 2026"]
   [:p {:class "mt-3 flex items-baseline gap-2"}
    [:span {:class "text-3xl font-semibold tracking-[-0.03em] tabular-nums text-zinc-900"} "R11,240"]
    [:span {:class "text-sm font-medium text-zinc-500"} "spent"]]
   [:p {:class "mt-1.5 text-sm text-zinc-500"} "R760 less than planned."]
   [:div {:class "mt-5"}
    (hero/bars {:rows [{:label "Planned" :value "R12,000" :amount 12000 :cls "bg-zinc-400"}
                       {:label "Spent" :value "R11,240" :amount 11240 :cls "bg-brand-600"}]})]])

(defn action-tile
  "The close of a marketing page: the invitation on the left, a piece of the
   dashboard it leads to on the right, and three plain facts underneath.
   Neutral ground; green only inside the product preview."
  [{:keys [id heading line primary secondary ground facts]}]
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
    (when-not (= :none facts)
      [:dl {:class "enter mt-16 grid grid-cols-1 gap-8 sm:grid-cols-3 lg:mt-24" :style {"--d" "160ms"}}
     (for [[k v] (or facts
                     [["Free" "to use, for you and your business"]
                      ["SARS 2026/27" "tables, the same ones the calculators use"]
                      ["Never" "sells your data, to anyone"]])]
       [:div
        [:dt {:class "text-[28px] font-semibold tracking-[-0.02em] text-zinc-900"} k]
        [:dd {:class "mt-1 max-w-[30ch] text-[17px] leading-relaxed text-zinc-700"} v]])])]])

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
