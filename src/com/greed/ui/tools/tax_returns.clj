(ns com.greed.ui.tools.tax-returns
  (:require [com.biffweb :as biff]
            [com.core :as c]
            [com.greed.ui :as ui]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.svgs :as svgs]
            [com.greed.ui.tools.core :as tools]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.tax :as tax]
            [com.greed.data.core :as data]))

(defn- taxable-travel [travel-allowance logbook?]
  (* travel-allowance (if logbook? 0.20 0.80)))

(defn- info-item [source-code label description]
  [:div {:class "flex gap-3 px-5 py-3 sm:px-6"}
   [:div {:class "shrink-0 self-start w-24 rounded-md bg-zinc-100 px-2 py-1 text-center text-[11px] font-medium text-zinc-600 font-mono"}
    source-code]
   [:div {:class "min-w-0"}
    [:p {:class "text-sm font-medium text-zinc-900"} label]
    [:p {:class "mt-0.5 text-xs leading-relaxed text-zinc-500"} description]]])

(defn- guide []
  [:div {:class "space-y-4"}
   (tools/panel
    (tools/panel-heading "How this tool works")
    [:div {:class "px-5 pb-5 sm:px-6"}
     [:p {:class "text-sm text-zinc-500 leading-relaxed"}
      "Enter figures from your IRP5 (employee tax certificate) and other documents. The simulator applies SARS 2026 year of assessment tax brackets, rebates, and credits to estimate whether you are owed a refund or have tax to pay."]
     [:p {:class "mt-2 text-sm text-zinc-500 leading-relaxed"}
      "Filing opens annually in July on "
      [:a {:href "https://www.sars.gov.za" :class "text-emerald-600 hover:underline" :target "_blank"} "eFiling"]
      ". Non-provisional taxpayers (salaried employees) must submit by late October."]])

   (tools/panel
    (tools/panel-heading "Where to find each value"
                         :description "Your employer must issue an IRP5 by 31 May each year. Log in to eFiling — it is usually pre-populated there.")
    [:div {:class "divide-y divide-zinc-100 border-t border-zinc-100"}
     (info-item "3699 / 3601" "Gross Annual Income"
                "Source code 3699 (or 3601 for regular employment income) on your IRP5. Exclude any travel allowance — enter that separately below.")
     (info-item "4102" "Total PAYE Paid to SARS"
                "Source code 4102 on your IRP5. This is the total PAYE your employer deducted and paid to SARS on your behalf during the tax year.")
     (info-item "4005 / MedCert" "Medical Aid Contributions"
                "Source code 4005 on your IRP5, or your annual medical scheme contribution certificate. Enter your monthly share (excluding any employer contribution).")
     (info-item "Med Cert" "Medical Aid Dependants"
                "The number of registered dependants on your medical aid, excluding yourself. Found on your membership or contribution certificate.")
     (info-item "4006 / RA Cert" "Retirement Annuity Contributions"
                "Source code 4006 on your IRP5 for employer-contributed pension, or your RA fund's annual contribution statement for personal RA contributions. Enter the annual total.")
     (info-item "3701" "Travel Allowance"
                "Source code 3701 on your IRP5. Enter the full allowance — the simulator applies the correct taxable portion (80% without a logbook, 20% with one).")
     (info-item "Med Receipts" "Out-of-pocket Medical Expenses"
                "Medical costs you paid directly that were not covered or reimbursed by your medical aid. Keep all receipts. Applies the Section 6B additional medical credit.")])

   (tools/panel
    (tools/panel-heading "Medical aid & tax credits"
                         :description "Medical aid gives you two potential tax benefits — leave both fields at 0 if you are not on medical aid.")
    [:div {:class "divide-y divide-zinc-100 border-t border-zinc-100"}
     (tools/glossary-item "Medical Aid Tax Credit (MTC)"
       "SARS gives every medical aid member a fixed monthly credit that reduces your tax bill directly — it is not a deduction from income. For 2026: "
       [:span {:class "font-medium text-zinc-700"} "R364/month"]
       " for yourself and your first dependant, "
       [:span {:class "font-medium text-zinc-700"} "R246/month"]
       " for each additional dependant. The credit is applied automatically once you enter your monthly contribution amount.")
     (tools/glossary-item "No medical aid?"
       "Leave medical contributions at 0. No MTC will be applied and your result will reflect your tax position accurately without it.")
     (tools/glossary-item "Out-of-pocket medical expenses (Section 6B)"
       "If you paid medical costs that your scheme did not cover (co-payments, dentist, spectacles, medicines etc.), you may claim an additional credit. SARS applies "
       [:span {:class "font-medium text-zinc-700"} "33.3%"]
       " of qualifying expenses exceeding 4x your annual MTC (under 65), or 33.3% of all such expenses (65 and older). Only enter amounts you have receipts for.")])

   (tools/notice
    "This simulator is an estimate only and does not account for all deductions (e.g. home office, commission expenses). Consult a registered tax practitioner or SARS for your official assessment.")])

(defn- stat [label value sub]
  [:div {:class "rounded-xl bg-zinc-50 p-4 ring-1 ring-zinc-200/50"}
   [:p {:class "text-[11px] font-medium text-zinc-500 uppercase tracking-wider"} label]
   [:p {:class "mt-1 text-lg font-bold text-zinc-900 tabular-nums"} value]
   (when sub [:p {:class "mt-0.5 text-xs text-zinc-400"} sub])])

(defn- auto-assessment-card [ctx]
  (let [user-id    (data/get-user-id-from-session ctx)
        user       (data/get-user ctx user-id)
        finances   (data/get-finances ctx user-id)
        tp         (data/get-tax-profile ctx user-id)
        age        (or (:user/age user) 0)
        salary     (:finances/salary finances)
        med-monthly  (or (:tax-profile/medical-monthly tp) 0)
        dependants   (or (:tax-profile/medical-dependants tp) 0)
        ra-annual    (or (:tax-profile/ra-annual tp) 0)
        has-profile? (some? tp)]
    (if (and salary age (pos? salary))
      (let [annual-income  (utilities/income->annual-income salary)
            ra-ded         (tax/ra-deduction annual-income ra-annual)
            taxable-income (max 0 (- annual-income ra-ded))
            {:keys [gross-tax rebates net-tax effective-rate]}
              (tax/calculate-income-tax taxable-income age (c/get-tax-returns-config))
            mtc            (if (pos? med-monthly) (tax/medical-tax-credit dependants) 0)
            add-med        (tax/additional-medical-credit age med-monthly 0 mtc)
            final-tax      (max 0 (- net-tax mtc add-med))
            monthly-tax    (utilities/annual-income->monthly-income final-tax)
            monthly-net    (utilities/annual-income->monthly-income (- annual-income final-tax))
            credits-applied? (or (pos? mtc) (pos? ra-ded))]
        (tools/panel
         (tools/panel-heading "Auto Assessment"
                              :badge [:span {:class "flex-shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700 ring-1 ring-emerald-600/15"}
                                      "2026 Year of Assessment"])
         [:div {:class "px-5 pb-4 sm:px-6"}
          [:p {:class "text-sm text-zinc-500"}
           "Based on your salary of "
           [:span {:class "font-medium text-zinc-700"} (utilities/amount->rands salary) "/month"]
           " and age " [:span {:class "font-medium text-zinc-700"} age] "."]
          (when credits-applied?
            [:div {:class "flex flex-wrap gap-2 mt-3"}
             (when (pos? ra-ded)
               [:span {:class "flex-shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700 ring-1 ring-emerald-600/15"}
                (str "RA deduction: " (utilities/amount->rands ra-ded))])
             (when (pos? mtc)
               [:span {:class "flex-shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700 ring-1 ring-emerald-600/15"}
                (str "Medical credit: " (utilities/amount->rands mtc))])
             (when (pos? add-med)
               [:span {:class "flex-shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700 ring-1 ring-emerald-600/15"}
                (str "Additional med credit: " (utilities/amount->rands add-med))])])]
         [:div {:class "grid grid-cols-2 gap-3 px-5 pb-5 sm:grid-cols-3 sm:px-6 lg:grid-cols-6"}
          (stat "Gross Annual Income"  (utilities/amount->rands annual-income) nil)
          (stat "Gross Tax"            (utilities/amount->rands gross-tax)      nil)
          (stat "Rebates"              (utilities/amount->rands rebates)        nil)
          (stat "Estimated Annual Tax" (utilities/amount->rands final-tax)      nil)
          (stat "Net Annual Income"    (utilities/amount->rands (- annual-income final-tax)) nil)
          (stat "Net Monthly Income"   (utilities/amount->rands monthly-net)    nil)]
         [:div {:class "border-t border-zinc-100 px-5 py-4 sm:px-6"}
          [:div {:class "flex flex-wrap items-center justify-between gap-4 rounded-xl bg-zinc-50 p-5 ring-1 ring-zinc-200/50"}
           [:div
            [:p {:class "text-sm text-zinc-600"}
             "Your employer should withhold approximately "
             [:span {:class "font-semibold text-zinc-900"} (utilities/amount->rands monthly-tax) "/month"]
             " in PAYE."]
            [:p {:class "text-xs text-zinc-400 mt-1"}
             "Effective tax rate: " (tools/pct effective-rate) "."
             (when-not has-profile?
                " Add your medical aid and RA details in Settings for a more accurate estimate.")]]]]))
      (tools/panel
       [:div {:class "flex flex-col items-center justify-center px-6 py-12 text-center"}
        [:div {:class "mx-auto mb-3 flex h-10 w-10 items-center justify-center rounded-full bg-emerald-50"}
         [:span {:class "text-emerald-500"} (svgs/wallet)]]
        [:p {:class "text-sm font-medium text-zinc-500"} "No salary data yet"]
        [:p {:class "mt-1 max-w-xs text-xs text-zinc-400"}
         "Add your salary in "
         [:a {:href "/app/finances/" :class "font-medium text-emerald-600 hover:underline"} "Finances"]
         " or "
         [:a {:href "/app/settings" :class "font-medium text-emerald-600 hover:underline"} "Settings"]
         " to enable auto assessment."]]))))

(defn- field [id label type & [hint required? value]]
  [:div
   [:label {:for id :class "block text-sm font-medium text-zinc-700 mb-1"} label]
   (when hint [:p {:class "text-xs text-zinc-400 mb-1"} hint])
   [:input {:id id :name id :type type :min "0" :step "any"
            :class (shared/base-input-class)
            :required (boolean required?)
            :placeholder "0"
            :value (or value "")}]])

(defn- logbook-select [selected]
  [:div
   [:label {:for "logbook" :class "block text-sm font-medium text-zinc-700 mb-1"} "Travel logbook kept?"]
   [:select {:id "logbook" :name "logbook"
             :class (shared/base-input-class)}
    [:option {:value "no" :selected (= selected "no")} "No (80% taxable)"]
    [:option {:value "yes" :selected (= selected "yes")} "Yes (20% taxable)"]]])

(defn- form-card [params]
  (tools/panel
   (tools/panel-heading "ITR12 Tax Return Simulator")
   [:div {:class "px-5 pb-6 sm:px-6"}
    [:p {:class "text-sm text-zinc-500 mb-5"}
     "Enter figures from your IRP5, then click Simulate Return to estimate your SARS refund or amount owed."]
    (biff/form
     {:hx-post    "/app/tax/tax-returns"
      :hx-target  "#tax-result"
      :hx-swap    "outerHTML"
      :hx-trigger "submit"}
     (tools/form-section "Income & PAYE"
       (field "annual-income" "Gross Annual Income (R)" "number" "Exclude travel allowance" true (:annual-income params))
       (field "age" "Age" "number" nil true (:age params))
       (field "paye-paid" "Total PAYE Paid to SARS (R)" "number" "Source code 4102 on your IRP5" true (:paye-paid params)))
     (tools/form-section "Medical Aid"
       (field "medical-contributions" "Medical Aid Contributions p/m (R)" "number" nil false (:medical-contributions params))
       (field "dependants" "Medical Aid Dependants" "number" "Excluding yourself" false (:dependants params))
       (field "out-of-pocket-medical" "Out-of-pocket Medical Expenses p/a (R)" "number" "Not covered by medical aid" false (:out-of-pocket-medical params)))
     (tools/form-section "Retirement & Travel"
       (field "ra-annual" "Retirement Annuity Contributions p/a (R)" "number" "Max deduction: 27.5% of income or R350,000" false (:ra-annual params))
       (field "travel-allowance" "Travel Allowance p/a (R)" "number" "Source code 3701" false (:travel-allowance params))
       (logbook-select (:logbook params)))
     [:div {:class "mt-6 flex justify-end"}
      (shared/btn :variant :primary :size :md :class "px-8" :type "submit"
                  "Simulate Return")])]))

(defn- result-region [params]
  (let [->n             #(try (double (BigDecimal. (or % "0")))
                            (catch Exception _ 0.0))
        annual-income   (->n (:annual-income params))
        age             (or (utilities/->int (:age params)) 0)
        paye-paid       (->n (:paye-paid params))
        medical-monthly (->n (:medical-contributions params))
        dependants      (or (utilities/->int (:dependants params)) 0)
        ra-annual       (->n (:ra-annual params))
        travel          (->n (:travel-allowance params))
        logbook?        (= "yes" (:logbook params))
        out-of-pocket   (->n (:out-of-pocket-medical params))

        taxable-travel  (taxable-travel travel logbook?)
        total-income    (+ annual-income taxable-travel)
        ra-ded          (tax/ra-deduction total-income ra-annual)
        taxable-income  (max 0 (- total-income ra-ded))
        {:keys [gross-tax rebates net-tax]} (tax/calculate-income-tax taxable-income age (c/get-tax-returns-config))
        mtc             (if (pos? medical-monthly) (tax/medical-tax-credit dependants) 0)
        add-med-credit  (tax/additional-medical-credit age medical-monthly out-of-pocket mtc)
        final-tax       (max 0 (- net-tax mtc add-med-credit))
        refund?         (>= paye-paid final-tax)
        difference      (Math/abs (double (- paye-paid final-tax)))]
    (if (not (pos? annual-income))
      [:div#tax-result
       (tools/panel
        [:div {:class "flex flex-col items-center justify-center px-6 py-16 text-center"}
         [:div {:class "mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-emerald-50"}
          [:span {:class "text-emerald-500"} (svgs/wallet)]]
         [:p {:class "text-sm font-medium text-zinc-500"} "Enter your IRP5 details to estimate your return"]
         [:p {:class "mt-1 max-w-xs text-xs text-zinc-400"} "Fill in the form and click Simulate Return to see your refund or amount owed."]])]
      [:div#tax-result {:class "space-y-4"}
       (tools/result-hero
        :eyebrow "Your 2026 tax return"
        :badge "2026 year"
        :headline (tools/whole->rands difference)
        :suffix (if refund? "refund" "owed to SARS")
        :tone (if refund? "text-emerald-600" "text-rose-600")
        :status (if refund?
                  (str "You paid " (utilities/amount->rands paye-paid) " in PAYE against "
                       (utilities/amount->rands final-tax) " of tax — SARS owes you this back.")
                  (str "You paid " (utilities/amount->rands paye-paid) " in PAYE against "
                       (utilities/amount->rands final-tax) " of tax — you still owe this to SARS."))
        :substats [(tools/hero-substat "Gross income" (utilities/amount->rands total-income))
                   (tools/hero-substat "Net tax payable" (utilities/amount->rands final-tax))
                   (tools/hero-substat "PAYE paid" (utilities/amount->rands paye-paid))])
       (tools/panel
        (tools/panel-heading "2026 Tax Summary")
        (tools/breakdown-section "Income"
          (tools/row "Gross Annual Income" (utilities/amount->rands annual-income))
          (when (pos? travel)
            (tools/row (str "Taxable Travel (" (if logbook? "20%" "80%") ")")
                       (utilities/amount->rands taxable-travel)))
          (tools/row "Retirement Annuity Deduction" (str "(" (utilities/amount->rands ra-ded) ")"))
          (tools/bold-row "Taxable Income" (utilities/amount->rands taxable-income)))
        (tools/breakdown-section "Tax Calculation"
          (tools/row "Gross Tax" (utilities/amount->rands gross-tax))
          (tools/row "Primary / Age Rebates" (str "(" (utilities/amount->rands rebates) ")"))
          (when (pos? mtc)
            (tools/row "Medical Aid Tax Credit" (str "(" (utilities/amount->rands mtc) ")")))
          (when (pos? add-med-credit)
            (tools/row "Additional Medical Credit (s6B)" (str "(" (utilities/amount->rands add-med-credit) ")")))
          (tools/bold-row "Net Tax Payable" (utilities/amount->rands final-tax)))
        (tools/breakdown-section "PAYE Reconciliation"
          (tools/row "PAYE Paid" (utilities/amount->rands paye-paid))
          (tools/row "Net Tax Payable" (utilities/amount->rands final-tax)))
        [:div {:class (str "flex items-center justify-between gap-3 px-5 py-4 sm:px-6 "
                           (if refund? "bg-emerald-50/60" "bg-rose-50/60"))}
         [:span {:class (str "text-sm font-semibold " (if refund? "text-emerald-800" "text-rose-800"))}
          (if refund? "Estimated Refund" "Estimated Amount Owed")]
         [:span {:class (str "text-lg font-bold tabular-nums " (if refund? "text-emerald-700" "text-rose-700"))}
          (utilities/amount->rands difference)]]
        [:div {:class "px-5 py-4 sm:px-6"}
         [:p {:class "text-xs text-zinc-400"}
          "This is an estimate only. Consult a tax practitioner for advice."]])])))

(defn- page-template [ctx params]
  (ui/app
   ctx
   [:div {:class "space-y-7"}
    (headers/pages-heading ["Tax" "Tax Returns"]
                           "Estimate your SARS tax refund or amount owed for the 2026 year of assessment.")
    (auto-assessment-card ctx)
    [:div {:class "grid grid-cols-1 gap-4 lg:grid-cols-2 lg:items-start"}
     (form-card params)
     (result-region params)]
    (guide)]))

(defn page-get [ctx]
  (page-template ctx {}))

(defn page [{:keys [params] :as ctx}]
  (if (get-in ctx [:headers "hx-request"])
    (result-region params)
    (page-template ctx params)))
