(ns com.greed.ui.components.cards
  (:require [com.greed.ui.core :as c.ui]
   [com.greed.ui.components.svgs :as svgs]
   [com.greed.utilities.core :as utilities]))

(defn testiminial [& {:keys [img title body author]}]
  [:div {:class "bg-white rounded-2xl border border-zinc-200 shadow-card p-8 max-w-md"}
   [:div {:class "flex items-center gap-4 mb-4"}
    [:img {:class "w-12 h-12 rounded-full object-cover border-2 border-zinc-100 flex-shrink-0 bg-zinc-50"
           :alt author :src img}]
    [:div
     [:h3 {:class "font-semibold text-zinc-900"} title]
     [:p {:class "text-sm text-zinc-400"} author]]]
   [:p {:class "text-zinc-600 text-sm leading-relaxed"} body]])

(defn note-from-greed []
  [:div {:class "relative bg-zinc-900 rounded-2xl border border-zinc-800 shadow-card-md p-8 max-w-md overflow-hidden"}
   [:div {:class "absolute inset-x-0 top-0 h-1 bg-emerald-500"}]
   [:div {:class "absolute -top-24 -right-24 w-64 h-64 rounded-full bg-emerald-500/10 blur-3xl"}]
   [:div {:class "relative"}
    [:p {:class "text-xs font-semibold uppercase tracking-[0.2em] text-emerald-500"} "A note from Greed"]
    [:p {:class "mt-5 text-zinc-100 leading-relaxed"}
     "Money is private — and so is your ambition. Greed exists to take every part of your financial life off the messy back of your mind and put it in one clear place: salary, tax, spending, savings and the dates that matter."]
    [:p {:class "mt-4 text-zinc-400 leading-relaxed"}
     "No judgment, no jargon, no instant-wealth promises. Just a system that lets you feel what you earn and build control, one decision at a time."]
    [:div {:class "mt-8 flex items-center gap-4"}
     [:div {:class "h-px w-10 bg-emerald-500"}]
     [:span {:class "text-xl font-giza font-bold text-zinc-100 leading-none"} "greed."]]]])

(defn- mock-last-four
  "Deterministic decorative last-4 digits for the card mockup, derived from a
   stable seed so it doesn't shuffle on every render. Purely cosmetic — never
   sourced from or resembling a real card number."
  [seed]
  (format "%04d" (mod (Math/abs (hash seed)) 10000)))

(defn bank-card [& {:keys [budget-items finances net-monthly-income]}]
  (let [{:keys [total-income total-expenses]} (c.ui/get-budget-data budget-items)
        {:finances/keys [bank user-id account-type]} finances
        last-four (mock-last-four (or user-id bank))
        salary-budget-amount (or (some (fn [item]
                                         (when (and (= (:budget-item/type item) :income)
                                                 (= (:budget-item/title item) "Salary"))
                                           (:budget-item/amount item)))
                                   (or budget-items []))
                               0)
        other-income (- (or total-income 0) salary-budget-amount)
        income       (if net-monthly-income
                       (+ net-monthly-income (max 0 other-income))
                       (or total-income 0))
        balance      (- income total-expenses)
        bank         (or bank :bank)]
    [:div {:class "group relative h-48 w-full max-w-sm rounded-2xl p-6 text-white shadow-card-md ring-1 ring-white/10 bg-gradient-to-br from-zinc-800 via-zinc-900 to-black overflow-hidden transition-all duration-300 hover:shadow-card-hover hover:-translate-y-0.5"}
     [:div {:class "absolute top-0 right-0 w-44 h-44 bg-emerald-500/20 rounded-full blur-2xl -translate-y-1/2 translate-x-1/3 transition-transform duration-500 group-hover:scale-125"}]
     [:div {:class "absolute bottom-0 left-0 w-28 h-28 bg-white/5 rounded-full blur-xl translate-y-1/2 -translate-x-1/2"}]
     [:div {:class "relative flex justify-between items-start"}
      [:div
       [:p {:class "text-xs font-semibold text-zinc-300 uppercase tracking-widest"} (utilities/->string bank)]
       [:p {:class "mt-0.5 text-xs text-zinc-500"} (or account-type "Debit Card")]]
      [:div {:class "flex items-center gap-2 text-zinc-300 opacity-90"}
       (svgs/contactless)
       (svgs/card-chip)]]
     [:div {:class "absolute bottom-6 left-6 right-6"}
      [:div {:class "flex items-center gap-2 mb-4"}
       (for [_ (range 3)]
         [:span {:class "text-zinc-500 text-sm tracking-widest"} "...."])
       [:span {:class "text-sm font-mono text-zinc-300"} last-four]]
      [:div
       [:p {:class "text-xs text-zinc-500 uppercase tracking-wider"} "Balance"]
       [:p {:class "mt-0.5 text-2xl font-bold text-white tabular-nums tracking-tight"} (utilities/amount->rands balance)]]]]))
