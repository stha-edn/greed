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
        [:div {:class "text-sm"}
         [:div {:class "flex items-baseline justify-between gap-3"}
          [:span {:class "truncate text-zinc-600"} label]
          [:span {:class "flex-shrink-0 font-semibold tabular-nums text-zinc-900"} value]]
         [:span {:class "mt-1.5 block h-2 overflow-hidden rounded-full bg-zinc-200/70"}
          [:span {:class (str "block h-full rounded-full greed-bar-grow " (or cls "bg-brand-600"))
                  :style {:width (str (if (pos? (or amount 0)) (max 2.0 (pct amount top)) 0) "%")}}]]])]
     (when note [:p {:class "mt-3 text-xs text-zinc-500"} note])]))

;; ---------------------------------------------------------------------------
;; Plain words: one name for each idea, the same on every page and for both
;; profiles, and a one-line explanation for the few terms that must stay.

(def tips
  "What a term means, in a sentence. Shown by `tip` as a small (i)."
  {:money-in        "Everything that came into your account: salary, payments from clients, refunds. Money moved from your own other accounts is left out."
   :money-out       "Everything that left your account, apart from money moved to your own other accounts."
   :left-over       "Money in minus money out: what you still have from the month."
   :planned         "What your budget says you will spend in the month."
   :in-budget       "Payments on the statement that match a budget item by its reference, such as a debit order. They are checked against that item, not counted twice."
   :own-accounts    "Money moved between your own accounts, such as to savings. It is not spending, so it is left out of money in and out."
   :tax-per-rand    "Of every R1 you earn in the year, how much goes to SARS, after rebates and deductions."
   :top-rate        "The rate on your next rand of income. Only the part of your income inside the top bracket is taxed at this rate, not all of it."
   :saved           "Of every R1 that comes in, how much your budget puts away as savings. 20c (20%) is a strong goal."
   :pay-period      "The days from one payday to the next. The daily amount is what is left over, spread over the days until payday."
   :bank-fees       "What the bank charged you, from the fee lines on your statements."
   :set-aside       "Money the business puts away each month for SARS, VAT and reserves, so it is ready when payments are due."
   :vat-to-pay      "VAT charged on sales, minus VAT paid on costs. This is what goes to SARS on the VAT201 for the period."
   :vat-period      "The two months each VAT return covers. Category A periods end in odd months, B in even months; C is monthly."
   :provisional-tax "A company pays its income tax in advance, in two payments during its financial year and a top-up after it ends."
   :sbc             "A small business corporation pays less company tax on its first R550,000 or so of taxable income, on SARS's sliding scale. Above that it is the normal 27%."
   :taxable-income  "Income minus the costs SARS lets you deduct. Tax is worked out on this amount."})

(defn tip
  "A small (i) after a label that opens the plain meaning of `term` (a key of
   `tips`), in a popover above the page. No script needed."
  [term]
  (when-let [text (tips term)]
    (let [id (str "tip-" (name term) "-" (subs (str (random-uuid)) 0 8))]
      [:<>
       [:button {:type "button" :popovertarget id :aria-label "What does this mean?"
                 :class "ml-1 inline-flex h-4 w-4 items-center justify-center rounded-full align-[-2px] text-[10px] font-semibold text-zinc-400 ring-1 ring-zinc-300 transition-colors hover:text-zinc-900 hover:ring-zinc-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-500"}
        "i"]
       [:div {:id id :popover "auto" :role "tooltip"
              :class "m-auto w-[min(88vw,22rem)] rounded-lg bg-zinc-900 p-4 text-left text-sm font-normal leading-relaxed text-white shadow-card-md backdrop:bg-black/10"}
        text]])))

(defn verdict
  "The card's verdict: a short coloured status that says at a glance whether
   the number is good. `tone` is :good, :warn, :bad or :neutral."
  [{:keys [text tone]}]
  (when text
    [:span {:class (str "inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ring-1 "
                        (case tone
                          :good "bg-brand-50 text-brand-800 ring-brand-600/20"
                          :warn "bg-amber-50 text-amber-800 ring-amber-600/20"
                          :bad  "bg-rose-50 text-rose-700 ring-rose-600/20"
                          "bg-zinc-100 text-zinc-700 ring-zinc-200"))}
     [:span {:aria-hidden "true"
             :class (str "h-1.5 w-1.5 rounded-full "
                         (case tone :good "bg-brand-600" :warn "bg-amber-500" :bad "bg-rose-600" "bg-zinc-400"))}]
     text]))

;; ---------------------------------------------------------------------------
;; The card

(defn stat
  "One supporting figure: [label value], [label value value-class] or
   [label value value-class tip-term]."
  [[label value cls term]]
  [:div {:class "min-w-0"}
   [:p {:class "whitespace-nowrap text-xs font-medium text-zinc-500"} label (when term (tip term))]
   [:p {:class (str "mt-1 truncate text-sm font-semibold tabular-nums sm:text-lg " (or cls "text-zinc-900"))} value]])

(defn card
  "The header card. Options:
     :label   the small grey label        :badge  a pill beside it
     :value   the headline figure         :unit   its muted unit
     :reading the line under it           :tone   :rose for a warning
     :verdict {:text :tone} a short status, e.g. {:text \"R760 under plan\" :tone :good}
     :picture (bars ...), always given   :stats  three [label value cls tip-term]
     :footer  anything after the stats (a link, say)"
  [{:keys [label badge value unit reading tone picture stats footer] verdict-map :verdict}]
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
      (when verdict-map [:div {:class "mt-3"} (verdict verdict-map)])
      (when reading
        [:p {:class (str "mt-2 text-sm " (if (= :rose tone) "text-rose-700" "text-zinc-500"))} reading])]
     picture]
    (when (seq stats)
      [:div {:class "mt-6 grid grid-cols-3 gap-4 border-t border-zinc-100 pt-4"}
       (map stat (take 3 stats))])
    footer]])
