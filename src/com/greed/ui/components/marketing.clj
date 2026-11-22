(ns com.greed.ui.components.marketing
  "Shared building blocks for the marketing pages, in the home page's rich
   and modern look: deep green rounded panels with soft glows, rounded-full
   buttons, small uppercase kickers, and real app cards as pictures."
  (:require [com.greed.ui.components.hero :as hero]))

(def btn-primary
  "inline-flex items-center justify-center rounded-md bg-brand-600 px-6 py-3 text-base font-semibold text-white transition-[transform,background-color] duration-150 ease-out hover:bg-brand-700 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:ring-offset-2")

(def link-ink
  "font-medium text-brand-700 underline underline-offset-4 decoration-brand-300 transition-colors hover:text-brand-800 hover:decoration-brand-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 rounded-sm")

(def btn-white
  "inline-flex items-center justify-center gap-2 rounded-full bg-white px-6 py-3 text-[15px] font-semibold text-brand-950 shadow-[0_10px_40px_-10px_rgba(97,177,145,.7)] transition duration-200 hover:-translate-y-0.5 hover:bg-brand-50 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-brand-950")

(def btn-glass
  "inline-flex items-center justify-center gap-2 rounded-full px-6 py-3 text-[15px] font-semibold text-white ring-1 ring-white/25 transition duration-200 hover:bg-white/10 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white")

(def btn-green
  "inline-flex items-center justify-center gap-2 rounded-full bg-brand-600 px-6 py-3 text-[15px] font-semibold text-white shadow-[0_10px_30px_-12px_rgba(15,107,79,.8)] transition duration-200 hover:-translate-y-0.5 hover:bg-brand-700 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:ring-offset-2")

(def btn-outline
  "inline-flex items-center justify-center gap-2 rounded-full bg-white px-6 py-3 text-[15px] font-semibold text-zinc-900 ring-1 ring-zinc-300 transition duration-200 hover:bg-zinc-50 active:scale-[0.97] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400")

(def eyebrow
  "inline-flex items-center gap-2 rounded-full bg-brand-50 px-3 py-1 text-xs font-semibold uppercase tracking-[0.08em] text-brand-700 ring-1 ring-brand-600/15")

(def h2-class
  "max-w-[22ch] [text-wrap:balance] font-semibold text-zinc-900 [font-size:clamp(32px,4.2vw,56px)] leading-[1.05] tracking-[-0.035em]")

(defn section-head
  "A section's kicker, title and line, left or centred, on light or dark."
  [{:keys [kicker title line center? dark? id]}]
  [:div {:class (str "enter " (when center? "mx-auto text-center"))}
   (when kicker [:p {:class (if dark? "inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.08em] text-brand-200 ring-1 ring-white/15" eyebrow)} kicker])
   [:h2 {:id id :class (str "mt-4 " h2-class (when center? " mx-auto") (when dark? " !text-white"))} title]
   (when line [:p {:class (str "mt-5 max-w-[52ch] text-lg leading-relaxed " (when center? "mx-auto ") (if dark? "text-brand-100/80" "text-zinc-600"))} line])])

(defn glow-panel
  "The deep green rounded panel with soft glows and a faint grid that the
   home hero and business section use. `class` adds padding and the like."
  [{:keys [class]} & body]
  [:div {:class (str "relative overflow-hidden rounded-[2rem] bg-brand-950 text-white " class)}
   [:div {:aria-hidden "true" :class "pointer-events-none absolute -right-40 -top-40 h-[40rem] w-[40rem] rounded-full bg-brand-500/30 blur-[120px]"}]
   [:div {:aria-hidden "true" :class "pointer-events-none absolute -bottom-60 -left-40 h-[36rem] w-[36rem] rounded-full bg-brand-400/20 blur-[120px]"}]
   [:div {:aria-hidden "true" :class "hero-grid pointer-events-none absolute inset-0 opacity-[0.07]"}]
   (into [:div {:class "relative"}] body)])

(defn page-hero
  "The first screen of a marketing page: a deep green panel with a kicker,
   the h1, one line, optional buttons, and an optional picture on the right."
  [{:keys [kicker title line actions aside]}]
  [:section {:class "container mx-auto px-4 pt-4 sm:px-6"}
   (glow-panel {:class "px-6 py-14 sm:px-10 lg:px-14 lg:py-20"}
               [:div {:class (if aside "grid grid-cols-1 items-center gap-12 lg:grid-cols-[minmax(0,1fr)_minmax(0,0.9fr)]" "")}
                [:div
                 (when kicker
                   [:p {:class "enter inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.08em] text-brand-200 ring-1 ring-white/15"} kicker])
                 [:h1 {:class "enter mt-5 max-w-[16ch] [text-wrap:balance] font-semibold text-white [font-size:clamp(38px,5vw,68px)] leading-[1.02] tracking-[-0.045em]" :style {"--d" "60ms"}} title]
                 (when line
                   [:p {:class "enter mt-6 max-w-[52ch] text-lg leading-relaxed text-brand-100/80 sm:text-xl" :style {"--d" "120ms"}} line])
                 (when (seq actions)
                   (into [:div {:class "enter mt-9 flex flex-wrap items-center gap-3" :style {"--d" "180ms"}}] actions))]
                (when aside [:div {:class "enter" :style {"--d" "200ms"}} aside])])])

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
  [{:keys [label badge value unit reading bars stats tone] verdict-map :verdict}]
  [:div {:data-ui "" :class "rounded-lg bg-white p-5 text-left text-zinc-900 shadow-card-md ring-1 ring-zinc-200/70 sm:p-6"}
   [:div {:class "flex items-center gap-2.5"}
    [:p {:class "text-sm font-medium text-zinc-500"} label]
    [:div {:class "flex-1"}]
    (when badge (hero/badge badge :tone tone))]
   [:p {:class "mt-3 flex flex-wrap items-baseline gap-x-2"}
    [:span {:class (str "text-3xl font-semibold tracking-[-0.03em] tabular-nums " (if (= :rose tone) "text-rose-700" "text-zinc-900"))} value]
    (when unit [:span {:class "text-sm font-medium text-zinc-500"} unit])]
   (when verdict-map [:div {:class "mt-3"} (hero/verdict verdict-map)])
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

(defn action-tile
  "The close of a marketing page: a green gradient panel with the
   invitation and its buttons. Words only, so it never repeats a picture
   shown elsewhere on the site."
  [{:keys [id heading line primary secondary]}]
  [:section {:id id :class "container mx-auto px-4 py-16 sm:px-6 lg:py-20" :aria-labelledby (str id "-h")}
   [:div {:class "relative overflow-hidden rounded-[2rem] bg-gradient-to-br from-brand-600 via-brand-700 to-brand-900 px-6 py-14 text-center text-white sm:px-10 lg:py-20"}
    [:div {:aria-hidden "true" :class "pointer-events-none absolute left-1/2 top-0 h-80 w-[40rem] -translate-x-1/2 rounded-full bg-brand-300/30 blur-[100px]"}]
    [:div {:class "enter relative"}
     [:h2 {:id (str id "-h") :class "mx-auto max-w-[18ch] [text-wrap:balance] font-semibold text-white [font-size:clamp(34px,4.4vw,60px)] leading-[1.02] tracking-[-0.04em]"} heading]
     [:p {:class "mx-auto mt-5 max-w-[46ch] text-lg leading-relaxed text-white/85"} line]
     [:div {:class "mt-9 flex flex-wrap items-center justify-center gap-3"}
      (when primary [:a {:href (:href primary) :class btn-white} (:label primary) [:span {:aria-hidden "true"} "→"]])
      (when secondary [:a {:href (:href secondary) :class btn-glass} (:label secondary)])]]]])

(def enter-css
  "Shared motion and texture for the marketing pages: parts rise in as they
   arrive (content stays visible without script), bars grow then, floating
   pictures drift, and the faint grid on green panels. Respects reduced motion."
  "
html.js .enter{opacity:0;transform:translateY(24px);transition:opacity .8s cubic-bezier(.16,1,.3,1) var(--d,0ms),transform .8s cubic-bezier(.16,1,.3,1) var(--d,0ms)}
html.js .enter.in{opacity:1;transform:none}
html.js .enter:not(.in) .greed-bar-grow{animation:none;transform:scaleX(0)}
.hero-grid{background-image:linear-gradient(rgba(255,255,255,.9) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,.9) 1px,transparent 1px);background-size:48px 48px;-webkit-mask-image:radial-gradient(ellipse at 70% 30%,#000 20%,transparent 70%);mask-image:radial-gradient(ellipse at 70% 30%,#000 20%,transparent 70%)}
.float{animation:float 7s ease-in-out infinite}
.float-slow{animation:float 9s ease-in-out infinite reverse}
@keyframes float{0%,100%{transform:translateY(0)}50%{transform:translateY(-10px)}}
@media (prefers-reduced-motion:reduce){html.js .enter{opacity:1;transform:none;transition:none}html.js .enter:not(.in) .greed-bar-grow{transform:none}.float,.float-slow{animation:none}}
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
