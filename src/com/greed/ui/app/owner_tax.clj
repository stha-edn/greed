(ns com.greed.ui.app.owner-tax
  "What a person's businesses mean for their personal tax, for the personal
   Tax page: a sole proprietor's or partner's share of the profit is their
   own taxable income; a company's payments to them are dividends, with
   dividends tax withheld. Estimates only."
  (:require [com.core :as c]
            [com.greed.data.core :as data]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.company-tax :as ct]
            [com.greed.utilities.tax :as tax])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]))

(defn dividends-rate
  "Dividends tax in percent (20)."
  []
  (:dividends-tax/rate c/common-config 20))

(defn- rands [r] (utilities/whole->rands r))
(defn- fmt [^LocalDate d] (.format d (DateTimeFormatter/ofPattern "d MMMM yyyy")))

(defn business-income
  "Per business the person is in: how it reaches their personal tax.
   [{:business :kind :profit :share :pay}] where :kind is :profit (sole
   proprietor or partnership: their share of the year's profit, in rands),
   :dividends (company: what it pays them a year, in rands) or :other."
  [ctx user-id]
  (for [{:business/keys [entity-type vat-registered] :xt/keys [id] :as b} (data/get-user-businesses ctx user-id)
        :let [members (data/get-business-members ctx id)
              items   (data/get-business-budget-items ctx id)]]
    (case entity-type
      (:sole-proprietor :partnership)
      (let [share (get (data/profit-shares members) user-id 0)]
        {:business b :kind :profit :share share
         :profit (Math/round (* (/ share 100.0) (/ (ct/budget-taxable items vat-registered) 100.0)))})
      :pty-ltd
      {:business b :kind :dividends :pay (* 12 (get (data/owner-pay items members) user-id 0))}
      {:business b :kind :other})))

(defn tax-with-business
  "Personal tax for the year with and without the business profit (rands).
   A loss lowers taxable income, but not below zero."
  [salary profit age]
  (let [without (:net-tax (tax/calculate-income-tax (max 0 salary) age))
        with    (:net-tax (tax/calculate-income-tax (max 0 (+ salary profit)) age))]
    {:without without :with with :extra (- with without)}))

(def ^:private card "rounded-lg bg-white ring-1 ring-zinc-200/70 p-6")

(defn section
  "The personal Tax page's Business income section, or nil for someone with
   no business."
  [ctx user annual-salary]
  (let [rows (business-income ctx (:xt/id user))]
    (when (seq rows)
      (let [age     (or (:user/age user) 21)
            profit  (reduce + 0 (keep :profit rows))
            {:keys [extra with without]} (tax-with-business (or annual-salary 0) profit age)
            {first-due :first second-due :second third-due :third} (ct/provisional-dates (ct/financial-year 2 (LocalDate/now)))]
        [:section {:class card :aria-labelledby "biz-tax-h"}
         [:h2 {:id "biz-tax-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "Business income"]
         [:p {:class "mt-0.5 text-xs text-zinc-500"} "How your businesses reach your personal tax, from their budgets."]
         [:ul {:class "mt-4 divide-y divide-zinc-100 text-sm"}
          (for [{:keys [business kind profit share pay]} rows]
            [:li {:class "py-3"}
             [:p {:class "font-medium text-zinc-900"}
              (str (:business/name business) " · " (data/entity-types (:business/entity-type business)))]
             [:p {:class "mt-0.5 text-zinc-600"}
              (case kind
                :profit (str "Your " (if (< share 100) (str (Math/round (double share)) "% share of the ") "")
                             (if (neg? profit) "loss" "profit") " for the year: " (rands (Math/abs (long profit)))
                             ". It is your own taxable income, whether you draw it or not.")
                :dividends (if (pos? pay)
                             (str "Pays you " (rands pay) " a year as owner's drawings. From a company these are usually dividends: "
                                  (rands (Math/round (* pay (/ (dividends-rate) 100.0)))) " dividends tax (" (dividends-rate)
                                  "%) is withheld by the company, and they are not added to your taxable income. A salary from it is taxed through PAYE.")
                             "Its profit is taxed in the company (see its Tax page). Owner's drawings paid to you would usually be dividends, with dividends tax withheld.")
                (str "A " (data/entity-types (:business/entity-type business)) " is taxed under its own rules; greed does not add it to your tax."))]])]
         (when (not= 0 profit)
           [:div {:class "mt-4 border-t border-zinc-100 pt-4 text-sm"}
            [:dl {:class "space-y-2"}
             [:div {:class "flex justify-between"} [:dt {:class "text-zinc-600"} "Your tax without the business"] [:dd {:class "tabular-nums"} (rands without)]]
             [:div {:class "flex justify-between"} [:dt {:class "text-zinc-600"} "With your business profit"] [:dd {:class "tabular-nums"} (rands with)]]
             [:div {:class "flex justify-between font-semibold"} [:dt {:class "text-zinc-900"} "What the business adds"] [:dd {:class "tabular-nums"} (rands extra)]]]
            (when (pos? extra)
              [:div {:class "mt-4"}
               [:p {:class "text-zinc-700"}
                "With income that is not a salary you are usually a provisional taxpayer: two payments in the tax year, and an optional top-up after it."]
               [:ol {:class "mt-2 space-y-1 text-zinc-700"}
                [:li (str "First: about " (rands (Math/round (/ extra 2.0))) ", due " (fmt first-due))]
                [:li (str "Second: about " (rands (- extra (Math/round (/ extra 2.0)))) ", due " (fmt second-due))]
                [:li (str "Third (optional top-up): due " (fmt third-due))]]])])
         [:p {:class "mt-4 text-xs text-zinc-500"}
          (str "Estimates from the businesses' budgets, at your age "
               (if (:user/age user) "as in your profile" "taken as under 65 (add it in your profile)")
               ". Your salary's tax is covered by PAYE. Due dates skip weekends but not public holidays.")]]))))
