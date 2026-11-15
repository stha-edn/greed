(ns com.greed.ui.components.hero
  "The header card at the top of every tab, built one way: a small label
   (with an optional badge), one headline figure with its unit, one line
   reading it, bars on the right, and three supporting figures underneath.
   Every card uses bars, never rings: bars can show both a share of a whole
   (scaled to the whole with `:of`) and amounts side by side, so one picture
   fits every tab. They animate the same way as the rest of the app.")

(def panel-class "relative overflow-hidden rounded-lg bg-white ring-1 ring-zinc-200/70")

(defn badge
  "A small pill for the label row, e.g. a payday or a due date."
  [text & {:keys [tone]}]
  [:span {:class (str "flex-shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold ring-1 "
                      (case tone
                        :brand "bg-brand-50 text-brand-700 ring-brand-600/20"
                        :rose  "bg-rose-50 text-rose-700 ring-rose-600/20"
                        "bg-zinc-100 text-zinc-900 ring-zinc-200"))}
   text])

(defn- pct [part whole]
  (if (and whole (pos? whole)) (min 100.0 (max 0.0 (* 100.0 (/ (double part) whole)))) 0.0))

;; ---------------------------------------------------------------------------
;; Pictures

(defn bars
  "The card's picture: one row per amount [{:label :value (text) :amount
   :cls}], scaled to `of` (the whole, for shares) or else to the largest.
   A short `note` may follow."
  [{:keys [title rows of note]}]
  (let [top (or of (apply max 1 (map :amount rows)))]
    [:div {:class "rounded-md bg-zinc-50 p-5 ring-1 ring-zinc-200/50"}
     (when title [:p {:class "text-xs font-medium text-zinc-500"} title])
     [:div {:class (str (when title "mt-3 ") "space-y-3")}
      (for [{:keys [label value amount cls]} rows]
        [:div {:class "grid grid-cols-[7.5rem_minmax(0,1fr)_6rem] items-center gap-3 text-sm"}
         [:span {:class "truncate text-zinc-600"} label]
         [:span {:class "h-2.5 overflow-hidden rounded-full bg-zinc-200/70"}
          [:span {:class (str "block h-full rounded-full greed-bar-grow " (or cls "bg-brand-600"))
                  :style {:width (str (if (pos? (or amount 0)) (max 2.0 (pct amount top)) 0) "%")}}]]
         [:span {:class "truncate text-right font-semibold tabular-nums text-zinc-900"} value]])]
     (when note [:p {:class "mt-3 text-xs text-zinc-500"} note])]))

;; ---------------------------------------------------------------------------
;; The card

(defn stat
  "One supporting figure: [label value] or [label value value-class]."
  [[label value cls]]
  [:div {:class "min-w-0"}
   [:p {:class "whitespace-nowrap text-xs font-medium text-zinc-500"} label]
   [:p {:class (str "mt-1 truncate text-sm font-semibold tabular-nums sm:text-lg " (or cls "text-zinc-900"))} value]])

(defn card
  "The header card. Options:
     :label   the small grey label        :badge  a pill beside it
     :value   the headline figure         :unit   its muted unit
     :reading the line under it           :tone   :rose for a warning
     :picture (bars ...), always given   :stats  three [label value cls]
     :footer  anything after the stats (a link, say)"
  [{:keys [label badge value unit reading tone picture stats footer]}]
  [:section {:class panel-class}
   [:div {:class "relative flex flex-col px-6 py-6 sm:px-8 sm:py-7"}
    [:div {:class "grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)] lg:items-center"}
     [:div {:class "min-w-0"}
      [:div {:class "flex items-center gap-2.5"}
       [:p {:class "text-sm font-medium text-zinc-500"} label]
       [:div {:class "flex-1"}]
       badge]
      [:div {:class "mt-4 flex flex-wrap items-baseline gap-x-2.5 gap-y-1 sm:mt-5"}
       [:p {:class (str "text-3xl font-semibold leading-none tracking-[-0.03em] tabular-nums sm:text-4xl "
                        (if (= :rose tone) "text-rose-700" "text-zinc-900"))}
        value]
       (when unit [:span {:class "text-sm font-medium text-zinc-500"} unit])]
      (when reading
        [:p {:class (str "mt-2 text-sm " (if (= :rose tone) "text-rose-700" "text-zinc-500"))} reading])]
     picture]
    (when (seq stats)
      [:div {:class "mt-6 grid grid-cols-3 gap-4 border-t border-zinc-100 pt-4"}
       (map stat (take 3 stats))])
    footer]])
