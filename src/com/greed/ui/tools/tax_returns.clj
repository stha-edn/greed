(ns com.greed.ui.tools.tax-returns
  (:require [com.biffweb :as biff]
            [com.core :as c]
            [com.greed.ui :as ui]
            [com.greed.ui.components.headers :as headers]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.tax :as tax]
            [com.greed.data.core :as data]))

;; SARS 2026 year of assessment medical aid tax credit (monthly)
(def ^:private mtc-main 364)
(def ^:private mtc-additional 246)

(defn- medical-tax-credit [dependants]
  (let [main-and-first (min (inc dependants) 2)
        extra (max 0 (dec dependants))]
    (* 12 (+ (* main-and-first mtc-main)
             (* extra mtc-additional)))))

(defn- ra-deduction [annual-income ra-annual]
  (min ra-annual (* 0.275 annual-income) 350000))

(defn- taxable-travel [travel-allowance logbook?]
  (* travel-allowance (if logbook? 0.20 0.80)))

(defn- additional-medical-credit [age medical-monthly out-of-pocket mtc]
  (let [annual-contributions (* medical-monthly 12)
        total-medical (+ annual-contributions out-of-pocket)]
    (if (>= age 65)
      (* 0.333 total-medical)
      (max 0 (* 0.333 (- total-medical (* 4 mtc)))))))

(defn- row [label value]
  [:div {:class "flex justify-between py-2 border-b border-gray-100 text-sm"}
   [:span {:class "text-gray-500"} label]
   [:span {:class "text-gray-800"} value]])

(defn- bold-row [label value]
  [:div {:class "flex justify-between py-2 border-b border-gray-200 text-sm font-semibold"}
   [:span {:class "text-gray-700"} label]
   [:span {:class "text-gray-900"} value]])

(defn- info-item [source-code label description]
  [:div {:class "flex gap-4 py-3 border-b border-gray-100 last:border-0"}
   [:div {:class "shrink-0 w-24 text-xs font-mono bg-gray-100 text-gray-600 rounded px-2 py-1 self-start text-center"}
    source-code]
   [:div
    [:p {:class "text-sm font-medium text-gray-800"} label]
    [:p {:class "text-xs text-gray-500 mt-0.5"} description]]])

(defn- guide []
  [:div {:class "space-y-6"}
   [:div {:class "bg-white rounded-lg shadow p-6"}
    [:h3 {:class "text-base font-semibold text-gray-800 mb-1"} "How this tool works"]
    [:p {:class "text-sm text-gray-500"}
     "Enter figures from your IRP5 (employee tax certificate) and other documents. The simulator applies SARS 2026 year of assessment tax brackets, rebates, and credits to estimate whether you are owed a refund or have tax to pay."]
    [:p {:class "text-sm text-gray-500 mt-2"}
     "Filing opens annually in July on "
     [:a {:href "https://www.sars.gov.za" :class "text-emerald-600 hover:underline" :target "_blank"} "eFiling"]
     ". Non-provisional taxpayers (salaried employees) must submit by late October."]]

   [:div {:class "bg-white rounded-lg shadow p-6"}
    [:h3 {:class "text-base font-semibold text-gray-800 mb-3"} "Where to find each value"]
    [:p {:class "text-xs text-gray-400 mb-3"} "Your employer must issue an IRP5 by 31 May each year. Log in to eFiling — it is usually pre-populated there."]
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
               "Medical costs you paid directly that were not covered or reimbursed by your medical aid. Keep all receipts. Applies the Section 6B additional medical credit.")]

   [:div {:class "bg-white rounded-lg shadow p-6"}
    [:h3 {:class "text-base font-semibold text-gray-800 mb-3"} "Medical aid & tax credits"]
    [:p {:class "text-sm text-gray-500 mb-4"}
     "Medical aid gives you two potential tax benefits — leave both fields at 0 if you are not on medical aid."]
    [:div {:class "space-y-4"}
     [:div
      [:p {:class "text-sm font-medium text-gray-800"} "Medical Aid Tax Credit (MTC)"]
      [:p {:class "text-xs text-gray-500 mt-1"}
       "SARS gives every medical aid member a fixed monthly credit that reduces your tax bill directly — it is not a deduction from income. For 2026: "
       [:span {:class "font-medium text-gray-700"} "R364/month"]
       " for yourself and your first dependant, "
       [:span {:class "font-medium text-gray-700"} "R246/month"]
       " for each additional dependant. The credit is applied automatically once you enter your monthly contribution amount."]]
     [:div
      [:p {:class "text-sm font-medium text-gray-800"} "No medical aid?"]
      [:p {:class "text-xs text-gray-500 mt-1"}
       "Leave medical contributions at 0. No MTC will be applied and your result will reflect your tax position accurately without it."]]
     [:div
      [:p {:class "text-sm font-medium text-gray-800"} "Out-of-pocket medical expenses (Section 6B)"]
      [:p {:class "text-xs text-gray-500 mt-1"}
       "If you paid medical costs that your scheme did not cover (co-payments, dentist, spectacles, medicines etc.), you may claim an additional credit. SARS applies "
       [:span {:class "font-medium text-gray-700"} "33.3%"]
       " of qualifying expenses exceeding 4x your annual MTC (under 65), or 33.3% of all such expenses (65 and older). Only enter amounts you have receipts for."]]]]

   [:div {:class "bg-amber-50 border border-amber-200 rounded-lg p-4"}
    [:p {:class "text-xs text-amber-800"}
     "This simulator is an estimate only and does not account for all deductions (e.g. home office, commission expenses). Consult a registered tax practitioner or SARS for your official assessment."]]])

(defn- stat [label value sub]
  [:div {:class "bg-gray-50 rounded-lg p-4"}
   [:p {:class "text-xs text-gray-400 mb-1"} label]
   [:p {:class "text-lg font-bold text-gray-900"} value]
   (when sub [:p {:class "text-xs text-gray-500 mt-0.5"} sub])])

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
            ra-ded         (ra-deduction annual-income ra-annual)
            taxable-income (max 0 (- annual-income ra-ded))
            {:keys [gross-tax rebates net-tax effective-rate]}
              (tax/calculate-income-tax taxable-income age (c/get-tax-returns-config))
            mtc            (if (pos? med-monthly) (medical-tax-credit dependants) 0)
            add-med        (additional-medical-credit age med-monthly 0 mtc)
            final-tax      (max 0 (- net-tax mtc add-med))
            monthly-tax    (utilities/annual-income->monthly-income final-tax)
            monthly-net    (utilities/annual-income->monthly-income (- annual-income final-tax))
            credits-applied? (or (pos? mtc) (pos? ra-ded))]
        [:div {:class "bg-white rounded-lg shadow p-6"}
         [:div {:class "flex flex-wrap items-center justify-between gap-2 mb-1"}
          [:h2 {:class "text-lg font-semibold text-gray-800"} "Auto Assessment"]
          [:span {:class "text-xs bg-emerald-100 text-emerald-700 px-2 py-1 rounded-full font-medium"} "2026 Year of Assessment"]]
         [:p {:class "text-sm text-gray-500 mb-4"}
          "Based on your salary of "
          [:span {:class "font-medium text-gray-700"} (utilities/amount->rands salary) "/month"]
          " and age " [:span {:class "font-medium text-gray-700"} age] "."]

         (when credits-applied?
           [:div {:class "flex flex-wrap gap-2 mb-4"}
            (when (pos? ra-ded)
              [:span {:class "text-xs bg-gray-100 text-gray-600 rounded-full px-2.5 py-1"}
               (str "RA deduction: " (utilities/amount->rands ra-ded))])
            (when (pos? mtc)
              [:span {:class "text-xs bg-gray-100 text-gray-600 rounded-full px-2.5 py-1"}
               (str "Medical credit: " (utilities/amount->rands mtc))])
            (when (pos? add-med)
              [:span {:class "text-xs bg-gray-100 text-gray-600 rounded-full px-2.5 py-1"}
               (str "Additional med credit: " (utilities/amount->rands add-med))])])

         [:div {:class "grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6 mb-5"}
          (stat "Gross Annual Income"  (utilities/amount->rands annual-income) nil)
          (stat "Gross Tax"            (utilities/amount->rands gross-tax)      nil)
          (stat "Rebates"              (utilities/amount->rands rebates)        nil)
          (stat "Estimated Annual Tax" (utilities/amount->rands final-tax)      nil)
          (stat "Net Annual Income"    (utilities/amount->rands (- annual-income final-tax)) nil)
          (stat "Net Monthly Income"   (utilities/amount->rands monthly-net)    nil)]

         [:div {:class "flex flex-wrap items-center justify-between gap-4 bg-gray-50 rounded-lg px-4 py-3"}
          [:div
           [:p {:class "text-sm text-gray-600"}
            "Your employer should withhold approximately "
            [:span {:class "font-semibold text-gray-900"} (utilities/amount->rands monthly-tax) "/month"]
            " in PAYE."]
           [:p {:class "text-xs text-gray-400 mt-1"}
            "Effective tax rate: " (utilities/->percentage effective-rate) "."
            (when-not has-profile?
              " Add your medical aid and RA details in Settings for a more accurate estimate.")]]]])
      [:div {:class "mt-6 bg-amber-50 border border-amber-200 rounded-lg p-4"}
       [:p {:class "text-sm text-amber-800"}
        "No salary data found. Add your salary in "
        [:a {:href "/app/finances/" :class "font-medium underline"} "Finances"]
        " or "
        [:a {:href "/app/settings" :class "font-medium underline"} "Settings"]
        " to enable auto assessment."]])))

(defn- field [id label type & [hint required?]]
  [:div
   [:label {:for id :class "block text-sm font-medium text-gray-700 mb-1"} label]
   (when hint [:p {:class "text-xs text-gray-400 mb-1"} hint])
   [:input {:id id :name id :type type :min "0" :step "any"
            :class "block w-full px-4 py-2 text-gray-700 bg-white border border-gray-200 rounded-md focus:border-blue-400 focus:outline-none focus:ring focus:ring-blue-300 focus:ring-opacity-40"
            :required (boolean required?)
            :placeholder "0"}]])

(defn- logbook-select []
  [:div
   [:label {:for "logbook" :class "block text-sm font-medium text-gray-700 mb-1"} "Travel logbook kept?"]
   [:select {:id "logbook" :name "logbook"
             :class "block w-full px-4 py-2 text-gray-700 bg-white border border-gray-200 rounded-md focus:border-blue-400 focus:outline-none focus:ring focus:ring-blue-300 focus:ring-opacity-40"}
    [:option {:value "no"} "No (80% taxable)"]
    [:option {:value "yes"} "Yes (20% taxable)"]]])

(defn- form-card []
  [:div {:class "bg-white rounded-lg shadow p-6"}
   [:h2 {:class "text-lg font-semibold text-gray-800"} "ITR12 Tax Return Simulator"]
   [:p {:class "mt-1 text-sm text-gray-500 mb-6"}
    "Estimate your SARS tax refund or amount owed for the 2026 year of assessment."]
   (biff/form
    {:action "/app/tools/tax-returns"}
    [:div {:class "grid grid-cols-1 gap-5 sm:grid-cols-2"}
     (field "annual-income" "Gross Annual Income (R)" "number" "Exclude travel allowance" true)
     (field "age" "Age" "number" nil true)
     (field "paye-paid" "Total PAYE Paid to SARS (R)" "number" "Source code 4102 on your IRP5" true)
     (field "medical-contributions" "Medical Aid Contributions p/m (R)" "number")
     (field "dependants" "Medical Aid Dependants" "number" "Excluding yourself")
     (field "ra-annual" "Retirement Annuity Contributions p/a (R)" "number" "Max deduction: 27.5% of income or R350,000")
     (field "travel-allowance" "Travel Allowance p/a (R)" "number" "Source code 3701")
     (logbook-select)
     (field "out-of-pocket-medical" "Out-of-pocket Medical Expenses p/a (R)" "number" "Not covered by medical aid")]
    [:div {:class "mt-6 flex justify-end"}
     [:button {:type "submit"
               :class "px-8 py-2.5 text-white bg-zinc-900 rounded-md hover:bg-gray-700 focus:outline-none"}
      "Simulate Return"]])])

(defn page [ctx]
  (ui/app
   ctx
   [:div {:class "space-y-4"}
    (headers/pages-heading ["Tools" "Tax Returns"])
    (auto-assessment-card ctx)
    [:div {:class "grid grid-cols-1 lg:grid-cols-2 gap-6 items-start"}
     (guide)
     (form-card)]]))

(defn result-page [{:keys [params] :as ctx}]
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
        ra-ded          (ra-deduction total-income ra-annual)
        taxable-income  (max 0 (- total-income ra-ded))
        {:keys [gross-tax rebates net-tax]} (tax/calculate-income-tax taxable-income age (c/get-tax-returns-config))
        mtc             (if (pos? medical-monthly) (medical-tax-credit dependants) 0)
        add-med-credit  (additional-medical-credit age medical-monthly out-of-pocket mtc)
        final-tax       (max 0 (- net-tax mtc add-med-credit))
        refund?         (>= paye-paid final-tax)
        difference      (Math/abs (double (- paye-paid final-tax)))]

    (ui/app
     ctx
     [:div {:class "space-y-4"}
      (headers/pages-heading ["Tools" "Tax Returns"])
      [:div {:class "grid grid-cols-1 lg:grid-cols-2 gap-6 items-start"}
       (guide)
       [:div {:class "bg-white rounded-lg shadow p-6"}
        [:h2 {:class "text-lg font-semibold text-gray-800 mb-4"} "2026 Tax Summary"]

        [:p {:class "text-xs font-semibold uppercase tracking-wide text-gray-400 mb-1"} "Income"]
        (row "Gross Annual Income" (utilities/amount->rands annual-income))
        (when (pos? travel)
          (row (str "Taxable Travel (" (if logbook? "20%" "80%") ")")
               (utilities/amount->rands taxable-travel)))
        (row "Retirement Annuity Deduction" (str "(" (utilities/amount->rands ra-ded) ")"))
        (bold-row "Taxable Income" (utilities/amount->rands taxable-income))

        [:p {:class "text-xs font-semibold uppercase tracking-wide text-gray-400 mt-4 mb-1"} "Tax Calculation"]
        (row "Gross Tax" (utilities/amount->rands gross-tax))
        (row "Primary / Age Rebates" (str "(" (utilities/amount->rands rebates) ")"))
        (when (pos? mtc)
          (row "Medical Aid Tax Credit" (str "(" (utilities/amount->rands mtc) ")")))
        (when (pos? add-med-credit)
          (row "Additional Medical Credit (s6B)" (str "(" (utilities/amount->rands add-med-credit) ")")))
        (bold-row "Net Tax Payable" (utilities/amount->rands final-tax))

        [:p {:class "text-xs font-semibold uppercase tracking-wide text-gray-400 mt-4 mb-1"} "PAYE Reconciliation"]
        (row "PAYE Paid" (utilities/amount->rands paye-paid))
        (row "Net Tax Payable" (utilities/amount->rands final-tax))

        [:div {:class (str "flex justify-between items-center py-3 mt-4 px-4 rounded-lg "
                           (if refund? "bg-green-50" "bg-red-50"))}
         [:span {:class (str "font-bold " (if refund? "text-green-700" "text-red-700"))}
          (if refund? "Estimated Refund" "Estimated Amount Owed")]
         [:span {:class (str "text-lg font-bold " (if refund? "text-green-700" "text-red-700"))}
          (utilities/amount->rands difference)]]

        [:p {:class "mt-4 text-xs text-gray-400"}
         "This is an estimate only. Consult a tax practitioner for advice."]

        [:div {:class "mt-5"}
         [:a {:href "/app/tools/tax-returns"
              :class "text-sm text-emerald-600 hover:underline"}
          "<- Run another simulation"]]]]])))
