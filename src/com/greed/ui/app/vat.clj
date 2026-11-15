(ns com.greed.ui.app.vat
  "The VAT page of a VAT-registered business: the current tax period, when
   its VAT201 is due, and what it is likely to owe SARS, from the budget and
   from bank statements. Estimates only; see com.greed.utilities.vat."
  (:require [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.ui.components.headers :as headers]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.vat :as vat]
            [com.greed.ui.components.hero :as hero])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]
           [java.time.temporal ChronoUnit]))

(defn- whole [cents] (utilities/whole->rands (/ cents 100.0)))

(defn- fmt [^LocalDate d pattern] (.format d (DateTimeFormatter/ofPattern pattern)))

(defn- period-label [{:keys [start end]}]
  (if (= (.getMonth ^LocalDate start) (.getMonth ^LocalDate end))
    (fmt start "MMMM yyyy")
    (str (fmt start "MMM") " to " (fmt end "MMM yyyy"))))

(defn- owes [net]
  (cond (pos? net) (str (whole net) " to pay")
        (neg? net) (str (whole (- net)) " back from SARS")
        :else "Nothing to pay"))

(def ^:private card "rounded-lg bg-white ring-1 ring-zinc-200/70 p-6")

(defn- vat-set-aside
  "The business's set-aside items that mention VAT, per month, in cents."
  [items]
  (* 100 (reduce + 0 (for [{:budget-item/keys [type title amount]} items
                           :when (and (= :savings type) (re-find #"(?i)\bvat\b" (str title)))]
                       amount))))

(defn- estimate-rows [{:keys [output input net]}]
  [:dl {:class "mt-4 space-y-2 text-sm"}
   [:div {:class "flex justify-between"} [:dt {:class "text-zinc-600"} "VAT on sales (output)"] [:dd {:class "tabular-nums text-zinc-900"} (whole output)]]
   [:div {:class "flex justify-between"} [:dt {:class "text-zinc-600"} "VAT on costs (input)"] [:dd {:class "tabular-nums text-zinc-900"} (str "−" (whole input))]]
   [:div {:class "flex justify-between border-t border-zinc-100 pt-2 font-semibold"}
    [:dt {:class "text-zinc-900"} "For this period"] [:dd {:class (str "tabular-nums " (if (pos? net) "text-zinc-900" "text-brand-700"))} (owes net)]]])

(defn- not-registered []
  [:section {:class card}
   [:h2 {:class "text-base font-semibold text-zinc-900"} "This business is not registered for VAT"]
   [:p {:class "mt-2 max-w-[65ch] text-sm text-zinc-700"}
    (str "Registering is compulsory once taxable sales pass " (utilities/whole->rands (vat/compulsory-threshold))
         " in 12 months, and allowed from " (utilities/whole->rands (vat/voluntary-threshold))
         ". If the business is registered, add its VAT number and period in Settings.")]
   [:a {:href "/app/settings" :class "mt-3 inline-block text-sm font-medium text-zinc-900 underline underline-offset-4"} "Go to Settings"]])

(defn page
  "GET /app/vat."
  [ctx]
  (if-let [{:business/keys [name vat-registered vat-category vat-number] :xt/keys [id]} (data/session-business ctx)]
    (ui/app
     ctx
     [:div {:class "space-y-7"}
      (headers/pages-heading ["VAT"] (str name (when vat-registered (str " · VAT number " vat-number))))
      (if-not (and vat-registered vat-category)
        (not-registered)
        (let [today     (LocalDate/now)
              items     (data/get-business-budget-items ctx id)
              summaries (data/get-statement-summaries ctx {:business-id id})
              per-month (vat-set-aside items)
              estimate  (fn [p]
                          (let [months (set (vat/period-months p))
                                in-p   (filter #(months (:statement-summary/month %)) summaries)]
                            {:budget (vat/budget-estimate items (:months p))
                             :statements (when (seq in-p) (vat/statement-estimate in-p items))}))
              [current & older] (vat/periods vat-category today 6)
              {:keys [budget statements]} (estimate current)
              days-left (.between ChronoUnit/DAYS today (:due current))]
          [:<>
           (let [set-aside (* per-month (:months current))
                 gap       (- (:net budget) set-aside)]
             (hero/card
              {:label (str "Current period · " (period-label current))
               :badge (hero/badge (str "Due " (fmt (:due current) "d MMM")) :tone (when (< days-left 14) :rose))
               :value (owes (:net budget))
               :unit "this period"
               :reading (str "Expected from your budget. The VAT201 is due on " (fmt (:due current) "EEEE d MMMM yyyy") ".")
               :picture (hero/bars
                         {:title "VAT on sales and on costs"
                          :rows [{:label "On sales" :value (whole (:output budget)) :amount (:output budget) :cls "bg-zinc-700"}
                                 {:label "On costs" :value (whole (:input budget)) :amount (:input budget) :cls "bg-brand-600"}]
                          :note (if (pos? per-month)
                                  (str "Your VAT set-aside items put away " (whole set-aside) " for this period"
                                       (if (pos? gap) (str ", " (whole gap) " less than expected.") ", enough to cover it."))
                                  "Tip: add a set-aside item with VAT in its name, so the money is put away before the due date.")})
               :stats [["Days left" (str days-left)]
                       ["Months in period" (str (:months current))]
                       ["Set aside for it" (whole set-aside)]]}))
           [:div {:class "grid grid-cols-1 gap-6 lg:grid-cols-2"}
            [:section {:class card :aria-labelledby "budget-h"}
             [:h2 {:id "budget-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "From your budget"]
             [:p {:class "mt-0.5 text-xs text-zinc-500"}
              (str "Each item's VAT type, times the " (:months current) (if (= 1 (:months current)) " month" " months") " in the period.")]
             (estimate-rows budget)]
            [:section {:class card :aria-labelledby "stmt-h"}
             [:h2 {:id "stmt-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "From your bank statements"]
             (if statements
               [:<>
                [:p {:class "mt-0.5 text-xs text-zinc-500"}
                 (str (:months statements) " of " (:months current) (if (= 1 (:months current)) " month" " months")
                      " read. Money in counts as standard-rated sales; costs count when their category usually carries VAT, or by their budget item's VAT type.")]
                (estimate-rows statements)]
               [:p {:class "mt-2 text-sm text-zinc-600"}
                "No statement months in this period yet. Upload the business's statements on the Banking page."])]]
           [:section {:class card :aria-labelledby "past-h"}
            [:h2 {:id "past-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "Earlier periods"]
            [:div {:class "mt-4 overflow-x-auto"}
             [:table {:class "w-full min-w-[32rem] text-sm"}
              [:thead
               [:tr {:class "text-left text-xs text-zinc-500"}
                [:th {:class "py-2 font-medium"} "Period"]
                [:th {:class "py-2 font-medium"} "Due"]
                [:th {:class "py-2 text-right font-medium"} "From budget"]
                [:th {:class "py-2 text-right font-medium"} "From statements"]]]
              [:tbody {:class "divide-y divide-zinc-100"}
               (for [p older
                     :let [{:keys [budget statements]} (estimate p)]]
                 [:tr
                  [:td {:class "py-2.5 text-zinc-900"} (period-label p)]
                  [:td {:class "py-2.5 text-zinc-600"} (fmt (:due p) "d MMM yyyy")]
                  [:td {:class "py-2.5 text-right tabular-nums"} (owes (:net budget))]
                  [:td {:class "py-2.5 text-right tabular-nums text-zinc-700"}
                   (if statements
                     (str (owes (:net statements))
                          (when (< (:months statements) (:months p)) (str " (" (:months statements) " of " (:months p) " months)")))
                     "–")]])]]]]
           [:p {:class "max-w-[75ch] text-xs leading-relaxed text-zinc-500"}
            (str "These are estimates to plan with, not a VAT201. greed does not see tax invoices, which your return needs, "
                 "and cannot tell zero-rated or exempt sales apart on a bank statement. VAT rate " (vat/rate) "%. "
                 "Due dates skip weekends but not public holidays.")]]))])
    {:status 303 :headers {"location" "/app/choose"}}))
