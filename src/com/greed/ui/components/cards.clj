(ns com.greed.ui.components.cards
  (:require [com.greed.ui.core :as c.ui]
   [com.greed.ui.components.svgs :as svgs]
   [com.greed.utilities.core :as utilities]))

(defn note-from-greed []
  [:div {:class "relative overflow-hidden max-w-md p-8 bg-zinc-900 border border-zinc-800 rounded-2xl shadow-card-md"}
   [:div {:class "absolute top-0 inset-x-0 h-1 bg-emerald-500"}]
   [:div {:class "absolute -top-24 -right-24 w-64 h-64 bg-emerald-500/10 rounded-full blur-3xl"}]
   [:div {:class "relative"}
    [:p {:class "text-xs font-semibold text-emerald-500 uppercase tracking-[0.2em]"} "A note from Greed"]
    [:p {:class "mt-5 text-zinc-100 leading-relaxed"}
     "Money is private — and so is your ambition. Greed exists to take every part of your financial life off the messy back of your mind and put it in one clear place: salary, tax, spending, savings and the dates that matter."]
    [:p {:class "mt-4 text-zinc-400 leading-relaxed"}
     "No judgment, no jargon, no instant-wealth promises. Just a system that lets you feel what you earn and build control, one decision at a time."]
    [:div {:class "flex items-center gap-4 mt-8"}
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
        balance      (- income total-expenses)]
    [:div {:class "relative flex h-48 w-full max-w-sm flex-col overflow-hidden rounded-2xl bg-gradient-to-br from-zinc-800 via-zinc-900 to-black p-6 text-white shadow-card-md ring-1 ring-white/10 lg:h-full"}
     [:div {:class "absolute -top-24 -right-20 h-64 w-64 rounded-full bg-emerald-500/20 blur-3xl"}]
     [:div {:class "relative"}
      [:div {:class "flex items-start justify-between"}
       [:span {:class "text-zinc-300"} (svgs/card-chip)]
       [:span {:class "text-zinc-300"} (svgs/contactless)]]
      [:div {:class "mt-4"}
       (if bank
         [:<>
          [:p {:class "text-xs font-semibold uppercase tracking-widest text-zinc-300"} (utilities/->string bank)]
          [:p {:class "mt-0.5 text-xs text-zinc-500"} (or account-type "Debit Card")]]
         [:a {:href "/app/settings"
              :class "inline-flex items-center gap-1 text-xs font-medium text-zinc-400 transition-colors hover:text-zinc-200 active:scale-[0.97]"}
          "Add your bank"
          (svgs/->next {:class "size-3"})])]]
     [:div {:class "relative mt-auto"}
      [:div {:class "flex items-center gap-2"}
       (for [_ (range 3)]
         [:span {:class "text-sm tracking-[0.2em] text-zinc-500"} "••••"])
       [:span {:class "text-sm font-mono tracking-[0.2em] text-zinc-300"} last-four]]
      [:div {:class "mt-4 flex items-baseline justify-between gap-4"}
       [:p {:class "text-xs uppercase tracking-wider text-zinc-500"} "Balance"]
       [:p {:class "text-2xl font-bold tracking-tight tabular-nums text-white lg:text-3xl"}
        (utilities/amount->rands balance)]]]]))
