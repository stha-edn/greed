(ns com.greed.ui.app.company-tax
  "The Tax page of a business: for a (Pty) Ltd, its income tax for the
   financial year and the provisional tax payments, estimated from the
   budget and from bank statements. Other entity types get a note on how
   they are taxed. Estimates only; see com.greed.utilities.company-tax."
  (:require [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.headers :as headers]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.company-tax :as ct]
            [com.greed.ui.components.hero :as hero])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]))

(defn- rands [r] (utilities/whole->rands r))
(defn- whole [cents] (utilities/whole->rands (/ cents 100.0)))
(defn- fmt [^LocalDate d] (.format d (DateTimeFormatter/ofPattern "d MMMM yyyy")))

(def ^:private card "rounded-lg bg-white ring-1 ring-zinc-200/70 p-6")

(def ^:private other-entities
  {:sole-proprietor "A sole proprietor's business is not taxed on its own. Its profit is part of the owner's personal income and is taxed in their personal return, with provisional tax if they earn income other than a salary."
   :partnership "A partnership is not taxed on its own. Each partner's share of the profit is taxed in their personal return."
   :npc "A non-profit company is taxed under different rules (for example as an approved public benefit organisation). greed does not cover those yet."
   :trust "A trust is taxed under its own rules, at a flat 45% on income it keeps. greed does not cover trusts yet."})

(defn- tax-set-aside
  "Set-aside items that mention tax but not VAT, per month in cents."
  [items]
  (* 100 (reduce + 0 (for [{:budget-item/keys [type title amount]} items
                           :let [t (str title)]
                           :when (and (= :savings type)
                                      (re-find #"(?i)\b(tax|provisional)\b" t)
                                      (not (re-find #"(?i)\bvat\b" t)))]
                       amount))))

(defn- estimate-card [title sub taxable-cents tax-rands]
  [:section {:class card}
   [:h2 {:class "text-sm font-semibold tracking-tight text-zinc-900"} title]
   [:p {:class "mt-0.5 text-xs text-zinc-500"} sub]
   [:dl {:class "mt-4 space-y-2 text-sm"}
    [:div {:class "flex justify-between"} [:dt {:class "text-zinc-600"} "Taxable income for the year"]
     [:dd {:class (str "tabular-nums " (if (neg? taxable-cents) "text-rose-700" "text-zinc-900"))} (whole taxable-cents)]]
    [:div {:class "flex justify-between border-t border-zinc-100 pt-2 font-semibold"} [:dt {:class "text-zinc-900"} "Income tax"]
     [:dd {:class "tabular-nums text-zinc-900"} (rands tax-rands)]]]])

(defn page
  "GET /app/tax while a business is chosen."
  [ctx]
  (if-let [{:business/keys [name entity-type year-end-month sbc vat-registered] :xt/keys [id]} (data/session-business ctx)]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (headers/pages-heading ["Tax"] name)
      (if-not (= :pty-ltd entity-type)
        [:section {:class card}
         [:h2 {:class "text-base font-semibold text-zinc-900"} (str "How a " (data/entity-types entity-type) " is taxed")]
         [:p {:class "mt-2 max-w-[65ch] text-sm text-zinc-700"} (other-entities entity-type)]]
        (let [today   (LocalDate/now)
              fy      (ct/financial-year (or year-end-month 2) today)
              items   (data/get-business-budget-items ctx id)
              fy-months (set (ct/months fy))
              in-year (filter #(fy-months (:statement-summary/month %)) (data/get-statement-summaries ctx {:business-id id}))
              b-taxable (ct/budget-taxable items vat-registered)
              b-tax     (ct/tax (/ b-taxable 100.0) sbc (:end fy))
              s-est     (ct/statement-taxable in-year items vat-registered)
              s-tax     (when s-est (ct/tax (/ (:year s-est) 100.0) sbc (:end fy)))
              {first-due :first second-due :second third-due :third} (ct/provisional-dates fy)
              p1        (Math/round (/ b-tax 2.0))
              set-aside (tax-set-aside items)
              table     (ct/sbc-table (:end fy))]
          [:<>
           (let [taxable-r (max 0 (/ b-taxable 100.0))
                 kept      (max 0 (- taxable-r b-tax))
                 gap       (- (* 100 b-tax) (* 12 set-aside))]
             (hero/card
              {:label (str "Financial year to " (fmt (:end fy)))
               :badge (hero/badge (if sbc "Small business corporation" (str (ct/company-rate) "% company rate")))
               :value (rands b-tax)
               :unit "income tax this year"
               :reading (str "Expected from your budget"
                             (when-not year-end-month " (February year end until you set it)") "."
                             (when (and sbc (:latest? table))
                               " greed has no small business corporation table for this year yet, so the newest one is used."))
               :picture (hero/bars
                         {:title "Where the profit goes"
                          :of (+ kept b-tax)
                          :rows [{:label "Kept in the business" :value (rands kept) :amount kept :cls "bg-brand-600"}
                                  {:label "Income tax" :value (rands b-tax) :amount b-tax :cls "bg-zinc-400"}]
                          :note (if (pos? set-aside)
                                  (str "Your tax set-aside items put away " (whole (* 12 set-aside)) " a year"
                                       (if (pos? gap) (str ", " (whole gap) " less than expected.") ", enough to cover it."))
                                  "Tip: add a set-aside item with \"tax\" in its name, so the provisional payments are ready.")})
               :stats [["Taxable income" (whole b-taxable)]
                       ["Effective rate" (if (pos? taxable-r) (str (Math/round (* 100.0 (/ b-tax taxable-r))) "%") "–")]
                       ["Set aside a year" (whole (* 12 set-aside))]]}))
           [:section {:class card :aria-labelledby "prov-h"}
            [:h2 {:id "prov-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "Provisional tax"]
            [:p {:class "mt-0.5 text-xs text-zinc-500"}
             "Based on the budget estimate. SARS can charge a penalty if the estimate for the year is too low, so update it as the year goes on."]
            [:ol {:class "mt-4 divide-y divide-zinc-100 text-sm"}
             (for [[label date amount] [["First payment" first-due p1]
                                        ["Second payment" second-due (- b-tax p1)]
                                        ["Third payment (optional top-up)" third-due nil]]]
               [:li {:class "flex items-center justify-between gap-4 py-2.5"}
                [:span {:class "min-w-0"}
                 [:span {:class "block text-zinc-900"} label]
                 [:span {:class (str "block text-xs " (if (.isBefore ^LocalDate date today) "text-zinc-400" "text-zinc-500"))}
                  (str "Due " (fmt date) (when (.isBefore ^LocalDate date today) " · passed"))]]
                [:span {:class "flex-shrink-0 tabular-nums font-semibold text-zinc-900"}
                 (if amount (rands amount) "If the final figure is higher")]])]]
           [:div {:class "grid grid-cols-1 gap-6 lg:grid-cols-2"}
            (estimate-card "From your budget"
                           (str "Income less expenses, times 12."
                                (when vat-registered " Without VAT.")
                                " Set-aside items, SARS and owner's drawings are not costs.")
                           b-taxable b-tax)
            (if s-est
              (estimate-card "From your bank statements"
                             (str (:months s-est) " of 12 months read: " (whole (:so-far s-est))
                                  " so far, scaled to a full year. SARS payments, drawings, transfers, cash and loans are left out.")
                             (:year s-est) s-tax)
              [:section {:class card}
               [:h2 {:class "text-sm font-semibold tracking-tight text-zinc-900"} "From your bank statements"]
               [:p {:class "mt-2 text-sm text-zinc-600"}
                "No statement months in this financial year yet. Upload the business's statements on the Banking page."]])]
           [:p {:class "max-w-[75ch] text-xs leading-relaxed text-zinc-500"}
            "These are estimates to plan with, not a tax return. Taxable income here is a rough guide: it does not know about wear-and-tear allowances, assessed losses, non-deductible costs, or income received but not yet banked. Due dates skip weekends but not public holidays. Confirm the figures with your accountant before filing."]]))])
    {:status 303 :headers {"location" "/app/choose"}}))
