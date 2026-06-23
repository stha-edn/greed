(ns com.greed.ui.components.forms
  (:require [clojure.tools.logging :as log]
            [com.biffweb :as biff]
            [com.core :as c]
            [com.greed.data.core :as data]
            [com.greed.utilities.core :as tools]
            [com.greed.ui.components.shared :as shared]))

(defn on-error [{:keys [params]}]
  (let [config c/common-config]
    (when-some [error (:error params)]
      (log/error "Error during form submission:" error)
      [:div {:class "mt-3 p-3 bg-red-50 border border-red-200 rounded-lg"}
       [:p {:class "text-sm text-red-600"}
        (when-not (= "not-signed-in" error)
          (case (tools/->keyword error)
            :recaptcha (:error/recaptcha config)
            :invalid-email (:error/invalid-email config)
            :invalid-credentials (:error/invalid-credentials config)
            :send-failed (:error/send-failed config)
            (:error/default config)))]])))

(defn sign-in [{:keys [site-key] :as ctx}]
  [:div {:class "w-full max-w-sm mx-auto"}
   [:div {:class "bg-white rounded-2xl shadow-card-md border border-zinc-100 overflow-hidden"}
    [:div {:class "px-8 py-8"}
     [:div {:class "mb-6 text-center"}
      [:a {:href "/"}
       [:span {:class "text-3xl font-giza font-bold text-zinc-900"} "greed."]]
      [:h2 {:class "mt-4 text-lg font-semibold text-zinc-900"} "Welcome back"]
      [:p {:class "mt-1 text-sm text-zinc-500"} "Sign in to your account"]]
     (biff/form
      {:action "authenticate/signin"
       :id "signin"
       :hidden {:on-error "/"}}
      (biff/recaptcha-callback "submitSignin" "signin")
      (shared/input :id "email" :type "email" :label "Email address" :required? true)
      (shared/input :id "password" :type "password" :label "Password" :required? true)
      [:div {:class "mt-5"}
       [:button
        (merge (when site-key {:data-sitekey site-key :data-callback "submitSignin"})
               {:class "w-full px-4 py-2.5 text-sm font-medium text-white bg-zinc-900 rounded-lg hover:bg-zinc-700 transition-colors"
                :type "submit"})
        "Sign in"]]
      (on-error ctx))]
    [:div {:class "px-8 py-4 bg-zinc-50 border-t border-zinc-100 text-center"}
     [:p {:class "text-sm text-zinc-500"}
      "Don't have an account? "
      [:a {:href "/signup" :class "font-medium text-emerald-600 hover:text-emerald-700 hover:underline"} "Create one"]]]]])

(defn sign-up [{:keys [site-key] :as ctx}]
  [:div {:class "w-full max-w-sm mx-auto"}
   [:div {:class "bg-white rounded-2xl shadow-card-md border border-zinc-100 overflow-hidden"}
    [:div {:class "px-8 py-8"}
     [:div {:class "mb-6 text-center"}
      [:a {:href "/"}
       [:span {:class "text-3xl font-giza font-bold text-zinc-900"} "greed."]]
      [:h2 {:class "mt-4 text-lg font-semibold text-zinc-900"} "Create an account"]
      [:p {:class "mt-1 text-sm text-zinc-500"} "Start managing your finances"]]
     (biff/form
      {:action "authenticate/signup"
       :id "signup"
       :hidden {:on-error "/"}}
      (biff/recaptcha-callback "submitSignup" "signup")
      [:div {:class "grid grid-cols-2 gap-3"}
       (shared/input :id "firstname" :type "text" :label "First name" :required? true)
       (shared/input :id "lastname" :type "text" :label "Last name" :required? true)]
      (shared/input :id "age" :type "number" :label "Age" :required? true)
      (shared/input :id "email" :type "email" :label "Email address" :required? true)
      (shared/input :id "password" :type "password" :label "Password" :required? true)
      [:div {:class "mt-5"}
       [:button
        (merge (when site-key {:data-sitekey site-key :data-callback "submitSignup"})
               {:class "w-full px-4 py-2.5 text-sm font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors"
                :type "submit"})
        "Create account"]]
      (on-error ctx))]
    [:div {:class "px-8 py-4 bg-zinc-50 border-t border-zinc-100 text-center"}
     [:p {:class "text-sm text-zinc-500"}
      "Already have an account? "
      [:a {:href "/signin" :class "font-medium text-zinc-700 hover:text-zinc-900 hover:underline"} "Sign in"]]]]])

(defn user [ctx]
  [:div {:class "bg-white rounded-xl border border-zinc-100 shadow-card p-6"}
   [:h2 {:class "text-base font-semibold text-zinc-900 mb-5"} "Personal Information"]
   (biff/form
    {:action "/app/save-user"}
    [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
     (shared/app-input ctx :id "firstname" :type "text" :label "First Name" :required? true)
     (shared/app-input ctx :id "lastname" :type "text" :label "Last Name" :required? true)
     (shared/app-input ctx :id "age" :type "number" :label "Age" :required? true)
     (shared/app-input ctx :id "email" :type "text" :label "Email" :required? true)
     (shared/app-input ctx :id "password" :type "password" :label "Password" :required? true)]
    [:div {:class "flex justify-end mt-5"}
     [:button {:class "px-6 py-2 text-sm font-medium text-white bg-zinc-900 rounded-lg hover:bg-zinc-700 transition-colors"
               :type "submit"}
      "Save changes"]])])

(defn finances [ctx]
  (let [bank-options      (sort (:banking/banks c/common-config))
        card-type-options (sort (:banking/card-types c/common-config))]
    [:div {:class "bg-white rounded-xl border border-zinc-100 shadow-card p-6"}
     [:div {:class "mb-6"}
      [:h2 {:class "text-base font-semibold text-zinc-900"} "Financial Details"]
      [:p {:class "text-sm text-zinc-400 mt-0.5"} "Personalises your dashboard, bank card, and tax estimates."]]
     (biff/form
      {:action "/app/save-finances"}

      ;; Banking
      [:div {:class "mb-6"}
       [:p {:class "text-xs font-semibold text-zinc-400 uppercase tracking-wider mb-3"} "Banking"]
       [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
        (shared/app-select ctx :id "bank" :label "Bank" :options bank-options :required? true
                           :hint "Your primary banking institution")
        (shared/app-select ctx :id "card-type" :label "Card Type" :options card-type-options :required? true
                           :hint "The network on your debit card")]]

      ;; Income
      [:div {:class "mb-6"}
       [:p {:class "text-xs font-semibold text-zinc-400 uppercase tracking-wider mb-3"} "Income"]
       [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
        (shared/app-input ctx :id "salary" :type "number" :label "Monthly Gross Salary" :required? true
                          :prefix "R" :hint "Your salary before any deductions")
        (shared/app-input ctx :id "payday" :type "number" :label "Pay Day" :required? true
                          :hint "Day of the month you receive your salary (1–31)")]]

      [:div {:class "flex justify-end pt-2 border-t border-zinc-50"}
       [:button {:class "px-6 py-2 text-sm font-medium text-white bg-zinc-900 rounded-lg hover:bg-zinc-700 transition-colors"
                 :type "submit"}
        "Save changes"]])]))

(defn tax-profile [{:keys [session] :as ctx}]
  (let [user-id (:uid session)
        tp      (data/get-tax-profile ctx user-id)
        val     (fn [k] (str (or (k tp) 0)))]
    [:div {:class "bg-white rounded-xl border border-zinc-100 shadow-card p-6"}
     [:div {:class "mb-6"}
      [:h2 {:class "text-base font-semibold text-zinc-900"} "Tax Assessment Profile"]
      [:p {:class "text-sm text-zinc-400 mt-0.5"}
       "Stored and used automatically in your tax return auto assessment."]]
     (biff/form
      {:action "/app/save-tax-profile"}

      ;; Medical Aid
      [:div {:class "mb-6"}
       [:p {:class "text-xs font-semibold text-zinc-400 uppercase tracking-wider mb-3"} "Medical Aid"]
       [:p {:class "text-xs text-zinc-400 mb-4 leading-relaxed"}
        "Leave these at 0 if you are not on medical aid. Credits are applied automatically (R364/month for you + first dependant, R246/month for each additional)."]
       [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
        [:div
         [:label {:class "block text-sm font-medium text-zinc-700 mb-1" :for "medical-monthly"} "Monthly Contributions"]
         [:div {:class "relative flex items-center"}
          [:div {:class "absolute left-3 text-zinc-400 text-sm font-medium pointer-events-none select-none"} "R"]
          [:input {:class "block w-full pl-7 pr-3 py-2 text-sm text-zinc-700 bg-white border border-zinc-200 rounded-lg focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-colors"
                   :id "medical-monthly" :name "medical-monthly" :type "number" :min "0"
                   :value (val :tax-profile/medical-monthly)}]]
         [:p {:class "text-xs text-zinc-400 mt-1"} "Your share of the monthly medical aid premium"]]
        [:div
         [:label {:class "block text-sm font-medium text-zinc-700 mb-1" :for "medical-dependants"} "Dependants"]
         [:input {:class "block w-full px-3 py-2 text-sm text-zinc-700 bg-white border border-zinc-200 rounded-lg focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-colors"
                  :id "medical-dependants" :name "medical-dependants" :type "number" :min "0"
                  :value (val :tax-profile/medical-dependants)}]
         [:p {:class "text-xs text-zinc-400 mt-1"} "Number of registered dependants, excluding yourself"]]]]

      ;; Retirement
      [:div {:class "mb-6"}
       [:p {:class "text-xs font-semibold text-zinc-400 uppercase tracking-wider mb-3"} "Retirement Annuity"]
       [:p {:class "text-xs text-zinc-400 mb-4 leading-relaxed"}
        "RA contributions reduce your taxable income. The deduction is capped at 27.5% of your income or R350,000, whichever is lower."]
       [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
        [:div
         [:label {:class "block text-sm font-medium text-zinc-700 mb-1" :for "ra-annual"} "Annual RA Contributions"]
         [:div {:class "relative flex items-center"}
          [:div {:class "absolute left-3 text-zinc-400 text-sm font-medium pointer-events-none select-none"} "R"]
          [:input {:class "block w-full pl-7 pr-3 py-2 text-sm text-zinc-700 bg-white border border-zinc-200 rounded-lg focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-colors"
                   :id "ra-annual" :name "ra-annual" :type "number" :min "0"
                   :value (val :tax-profile/ra-annual)}]]
         [:p {:class "text-xs text-zinc-400 mt-1"} "Total personal RA contributions for the year"]]]]

      [:div {:class "flex justify-end pt-2 border-t border-zinc-50"}
       [:button {:class "px-6 py-2 text-sm font-medium text-white bg-zinc-900 rounded-lg hover:bg-zinc-700 transition-colors"
                 :type "submit"}
        "Save changes"]])]))

(defn income-tax-form []
  [:div {:class "bg-white rounded-xl border border-zinc-100 shadow-card-md p-6 w-full max-w-sm"}
   [:h3 {:class "text-base font-semibold text-zinc-900"} "Income Tax Calculator"]
   [:p {:class "mt-1 text-sm text-zinc-500 mb-4"} "Calculate your income tax in seconds"]
   (biff/form
    {:class "mt-4" :action "/app/tax/income-tax-calculator"}
    (shared/modal-input :id "income" :type "number" :label "Monthly Income (R)" :required? true)
    (shared/modal-input :id "age" :type "number" :label "Age" :required? true)
    [:div {:class "flex gap-2 mt-5"}
     [:button {:type "button" "@click" "isOpen = false"
               :class "flex-1 px-4 py-2 text-sm font-medium text-zinc-700 bg-zinc-100 rounded-lg hover:bg-zinc-200 transition-colors"}
      "Cancel"]
     [:button {:type "submit"
               :class "flex-1 px-4 py-2 text-sm font-medium text-white bg-zinc-900 rounded-lg hover:bg-zinc-700 transition-colors"}
      "Calculate"]])])

(defn budget-item-form []
  (let [budget-item-options (:budget-item/types c/common-config)]
    [:div {:class "bg-white rounded-xl border border-zinc-100 shadow-card-md p-6 w-full max-w-sm"}
     [:h3 {:class "text-base font-semibold text-zinc-900"} "Add Budget Item"]
     [:p {:class "mt-1 text-sm text-zinc-500 mb-4"} "Add a new item to your budget"]
     (biff/form
      {:class "mt-4" :action "/app/finances/create-budget-item"}
      (shared/modal-select :id "type" :label "Category" :options budget-item-options :required? true)
      (shared/modal-input :id "title" :type "text" :label "Title" :required? true)
      (shared/modal-input :id "amount" :type "number" :label "Amount (R)" :required? true)
      [:div {:class "flex gap-2 mt-5"}
       [:button {:type "button" "@click" "isAddButtonOpen = false"
                 :class "flex-1 px-4 py-2 text-sm font-medium text-zinc-700 bg-zinc-100 rounded-lg hover:bg-zinc-200 transition-colors"}
        "Cancel"]
       [:button {:type "submit"
                 :class "flex-1 px-4 py-2 text-sm font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors"}
        "Add item"]])]))

(defn budget-action-form [item]
  [:div {:class "bg-white rounded-xl border border-zinc-100 shadow-card-md p-6 w-full max-w-sm"}
   [:h3 {:class "text-base font-semibold text-zinc-900"} "Edit Budget Item"]
   [:div {:class "mt-3 p-3 bg-zinc-50 rounded-lg text-sm text-zinc-600 mb-4"}
    [:div {:class "flex justify-between"} [:span {:class "text-zinc-400"} "Title"] (:budget-item/title item)]
    [:div {:class "flex justify-between mt-1"} [:span {:class "text-zinc-400"} "Amount"] (str "R" (:budget-item/amount item))]]
   (biff/form
    {:class "mt-2" :action (str "/app/finances/update-budget-item?budget-item-id=" (:xt/id item))}
    (shared/modal-input :id "title" :type "text" :label "New Title" :required? true)
    (shared/modal-input :id "amount" :type "number" :label "New Amount (R)" :required? true)
    [:div {:class "flex gap-2 mt-5"}
     [:button {:type "submit"
               :class "flex-1 px-4 py-2 text-sm font-medium text-white bg-zinc-900 rounded-lg hover:bg-zinc-700 transition-colors"}
      "Update"]
     [:a {:href (str "/app/finances/delete-budget-item?budget-item-id=" (:xt/id item))
          :class "flex-1 px-4 py-2 text-sm font-medium text-center text-white bg-red-500 rounded-lg hover:bg-red-600 transition-colors"}
      "Delete"]]
    [:div {:class "mt-2"}
     [:button {:type "button" "@click" "isActionModalOpen = false"
               :class "w-full px-4 py-2 text-sm font-medium text-zinc-600 hover:bg-zinc-100 rounded-lg transition-colors"}
      "Cancel"]])])
