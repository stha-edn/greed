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
    [:div {:class "relative flex w-full max-w-sm aspect-[1.586/1] flex-col overflow-hidden rounded-2xl bg-gradient-to-br from-emerald-50/60 via-white to-white p-6 ring-1 ring-emerald-500/15 shadow-card-md lg:from-zinc-800 lg:via-zinc-900 lg:to-black lg:shadow-card-hover lg:ring-white/10"}
     [:div {:class "absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-emerald-500/40 to-transparent"}]
     [:div {:class "hidden absolute inset-x-6 top-px h-px bg-gradient-to-r from-transparent via-white/25 to-transparent lg:block"}]
     [:div {:class "absolute -top-24 -right-20 h-64 w-64 rounded-full bg-emerald-400/10 blur-3xl lg:bg-emerald-500/20"}]
     [:div {:class "hidden absolute -top-16 -left-16 h-56 w-56 rounded-full bg-white/[0.06] blur-3xl lg:block"}]
     [:div {:class "relative"}
      [:div {:class "flex items-start justify-between"}
       [:span {:class "text-zinc-400 lg:rounded-[5px] lg:bg-gradient-to-br lg:from-emerald-300/25 lg:via-emerald-500/15 lg:to-emerald-800/20 lg:p-1 lg:text-emerald-200/90 lg:ring-1 lg:ring-white/10"} (svgs/card-chip)]
       [:span {:class "text-zinc-400 lg:text-zinc-300"} (svgs/contactless)]]
      [:div {:class "mt-4"}
       (if bank
         [:<>
          [:p {:class "text-xs font-semibold uppercase tracking-widest text-zinc-500 lg:text-zinc-300"} (utilities/->string bank)]
          [:p {:class "mt-0.5 text-xs text-zinc-400 lg:text-zinc-500"} (or account-type "Debit Card")]]
         [:a {:href "/app/settings"
              :class "inline-flex items-center gap-1 text-xs font-medium text-emerald-600 transition-colors hover:text-emerald-700 active:scale-[0.97] lg:text-zinc-400 lg:hover:text-zinc-200"}
          "Add your bank"
          (svgs/->next {:class "size-3"})])]]
     [:div {:class "relative mt-auto"}
      [:div {:class "flex items-center gap-2"}
       (for [_ (range 3)]
         [:span {:class "text-sm tracking-[0.2em] text-zinc-300 lg:text-zinc-500"} "••••"])
       [:span {:class "text-sm font-mono tracking-[0.2em] text-zinc-500 lg:text-zinc-300"} last-four]]
      [:div {:class "mt-4 flex items-baseline justify-between gap-4"}
       [:p {:class "text-xs uppercase tracking-wider text-zinc-400 lg:text-zinc-500"} "Balance"]
       [:p {:class (str "text-2xl font-bold tracking-tight tabular-nums lg:text-3xl "
                        (if (neg? balance) "text-rose-600 lg:text-rose-400" "text-zinc-900 lg:text-white"))}
        (utilities/amount->rands balance)]]]]))
