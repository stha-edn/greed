(ns com.greed.ui.tools.core
  (:require [com.greed.ui.components.svgs :as svgs]
            [com.greed.ui.components.stats :as stats]))

(defn whole->rands
  "Compact Rand figure with thousands separators, no decimals — the hero scale."
  [n]
  (format "R%,d" (long (Math/round (double (or n 0))))))

(defn pct
  "Formats an already-percentage value (e.g. 31.0) as '31%'."
  [p]
  (str (int (Math/round (double (or p 0)))) "%"))

(defn panel
  "Tool-page surface in the dashboard panel chrome: white, hairline ring.
   Sections inside provide their own padding, like the other app panels."
  [& body]
  (into [:div {:class "bg-white ring-1 ring-zinc-200/70 rounded-2xl shadow-card overflow-hidden"}]
        body))

(defn panel-heading
  "Heading row for a tool panel: title on the left, optional trailing badge."
  [title & {:keys [badge description]}]
  [:div {:class "flex items-center justify-between gap-3 px-5 pt-5 pb-4 sm:px-6"}
   [:div {:class "min-w-0"}
    [:h3 {:class "text-sm font-semibold text-zinc-900 tracking-tight"} title]
    (when description
      [:p {:class "mt-0.5 text-xs text-zinc-400 leading-relaxed"} description])]
   (when badge badge)])

(defn result-hero
  "The headline result for a tool page — the same visual hierarchy as the
   dashboard heroes: eyebrow label, one big number, a one-line reading and
   three supporting figures."
  [& {:keys [eyebrow badge headline suffix status tone substats body]}]
  (let [tone (or tone "text-zinc-900")]
    [:div {:class "relative overflow-hidden rounded-2xl bg-white ring-1 ring-zinc-200/70 shadow-card-md"}
     [:div {:class "absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-emerald-500/40 to-transparent"}]
     [:div {:class "absolute -top-24 -right-24 h-72 w-72 rounded-full bg-emerald-400/10 blur-3xl"}]
     [:div {:class "relative px-6 py-6 sm:px-8 sm:py-7"}
      [:div {:class "flex items-center gap-2.5"}
       [:span {:class "h-1.5 w-1.5 rounded-full bg-emerald-500"}]
       [:p {:class "text-[11px] sm:text-xs font-semibold text-emerald-600 uppercase tracking-[0.18em]"} eyebrow]
       [:div {:class "flex-1"}]
       (when badge
         [:span {:class "flex-shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700 ring-1 ring-emerald-600/15"} badge])]
      [:div {:class "mt-5 flex items-baseline gap-2.5 sm:mt-6"}
       [:p {:class (str "text-4xl sm:text-5xl font-bold leading-none tracking-[-0.04em] tabular-nums " tone)}
        headline]
       [:span {:class "text-sm font-medium text-zinc-400"} suffix]]
      [:p {:class "mt-3 text-sm text-zinc-500"} status]
      (when body body)
      (into [:div {:class "grid grid-cols-3 gap-4 pt-5 mt-6 border-t border-zinc-100"}]
            substats)]]))

(defn hero-substat
  "One supporting figure: tiny uppercase label over a medium value."
  [label value & [value-cls]]
  [:div {:class "min-w-0"}
   [:p {:class "text-[11px] font-medium text-zinc-500 uppercase tracking-wider whitespace-nowrap"} label]
   [:p {:class (str "mt-1 text-sm font-semibold whitespace-nowrap tabular-nums sm:text-lg "
                    (or value-cls "text-zinc-900"))} value]])

(defn row [label value & [value-cls]]
  [:div {:class "flex items-center justify-between gap-3 px-5 py-3 sm:px-6"}
   [:p {:class "text-sm text-zinc-600"} label]
   [:p {:class (str "text-sm font-semibold tabular-nums " (or value-cls "text-zinc-900"))} value]])

(defn bold-row [label value & [value-cls]]
  [:div {:class "flex items-center justify-between gap-3 bg-zinc-50 px-5 py-3 sm:px-6"}
   [:p {:class "text-sm font-semibold text-zinc-800"} label]
   [:p {:class (str "text-sm font-bold tabular-nums " (or value-cls "text-zinc-900"))} value]])

(defn breakdown-section [label & rows]
  [:div {:class "border-t border-zinc-100"}
   [:p {:class "px-5 pt-4 pb-1 text-[11px] font-medium text-zinc-500 uppercase tracking-wider sm:px-6"} label]
   [:div {:class "divide-y divide-zinc-100"} rows]])

(defn form-section
  "Groups related form fields under a small uppercase label, so a long form
   reads as related clusters instead of one undifferentiated grid."
  [label & fields]
  [:div {:class "border-t border-zinc-100 pt-5 mt-5 first:mt-0 first:border-t-0 first:pt-0"}
   [:p {:class "mb-3 text-[11px] font-medium text-zinc-500 uppercase tracking-wider"} label]
   (into [:div {:class "grid grid-cols-1 gap-5 sm:grid-cols-2"}] fields)])

(defn glossary-item [term & description]
  [:div {:class "px-5 py-3 sm:px-6"}
   [:p {:class "text-sm font-medium text-zinc-900"} term]
   [:p {:class "mt-0.5 text-xs leading-relaxed text-zinc-500"} description]])

(defn notice
  "Soft amber caveat panel used under the guides."
  [& content]
  [:div {:class "rounded-xl bg-amber-50 p-5 ring-1 ring-amber-600/15"}
   (into [:p {:class "text-sm leading-relaxed text-amber-900"}] content)])

(defn panel-footer [& body]
  [:div {:class "flex justify-end border-t border-zinc-100 px-5 py-3 sm:px-6"} body])

(defn- tool-card [& {:keys [title description link badge icon]}]
  [:a {:href link
       :class "group flex flex-col rounded-2xl bg-white p-6 ring-1 ring-zinc-200/70 shadow-card transition-all duration-200 hover:-translate-y-0.5 hover:shadow-card-hover hover:ring-zinc-300/70"}
   [:div {:class "mb-4 flex items-start justify-between"}
    [:div {:class "flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 ring-1 ring-emerald-600/10 transition-transform duration-200 group-hover:scale-105"}
     icon]
    (when badge
      [:span {:class "text-xs font-medium text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full"} badge])]
   [:h3 {:class "text-sm font-semibold text-zinc-900 tracking-tight group-hover:text-emerald-600 transition-colors"} title]
   [:p {:class "mt-1 flex-1 text-sm text-zinc-500 leading-relaxed"} description]
   [:div {:class "mt-4 flex items-center gap-1 text-xs font-medium text-zinc-400 group-hover:text-emerald-600 transition-colors"}
    "Open tool"
    (svgs/->next {:class "size-3.5 -translate-x-0.5 transition-transform group-hover:translate-x-0"})]])

(defn tools []
  [:div
   (stats/section-header "Tax tools"
                         :href "https://www.sars.gov.za/tax-rates/income-tax/rates-of-tax-for-individuals/"
                         :link-label "SARS rates" :external? true)
   [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3"}
    (tool-card
     :title "Income Tax Calculator"
     :description "Quickly estimate your annual income tax, rebates, and take-home pay based on SARS 2026/27 brackets."
     :link "/app/tax/income-tax-calculator"
     :badge "2026/27"
     :icon (svgs/percent-badge))
    (tool-card
     :title "Tax Returns (ITR12)"
     :description "Simulate your SARS tax return including medical aid credits, RA deductions, and travel allowances."
     :link "/app/tax/tax-returns"
     :badge "2026 year"
     :icon (svgs/document-text))
    (tool-card
     :title "Bonus Tax Calculator"
     :description "See how much PAYE will be withheld from a bonus or 13th cheque, and what you'll actually take home."
     :link "/app/tax/bonus-tax-calculator"
     :badge "2026/27"
     :icon (svgs/gift))]])
