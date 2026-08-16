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
      [:div {:class "mt-3 p-3 bg-rose-50 ring-1 ring-rose-200 rounded-lg"}
       [:p {:class "text-sm text-rose-700"}
        (when-not (= "not-signed-in" error)
          (case (tools/->keyword error)
            :recaptcha (:error/recaptcha config)
            :invalid-email (:error/invalid-email config)
            :invalid-credentials (:error/invalid-credentials config)
            :account-deactivated (:error/account-deactivated config)
            :send-failed (:error/send-failed config)
            :email-taken (:error/email-taken config)
            (:error/default config)))]])))

(defn sign-in [{:keys [site-key] :as ctx}]
  [:div {:class "w-full max-w-sm mx-auto"}
   [:div {:class "bg-white rounded-2xl shadow-card-md border border-zinc-200/70 overflow-hidden"}
    [:div {:class "px-8 py-8"}
     [:div {:class "mb-6 text-center"}
      [:a {:href "/"}
       [:span {:class "text-3xl font-giza font-bold text-zinc-900 leading-none"} "greed."]]
      [:h2 {:class "mt-4 text-lg font-semibold text-zinc-900 tracking-tight"} "Welcome back"]
      [:p {:class "mt-1 text-sm text-zinc-500"} "Sign in to your account"]]
     (biff/form
      {:action "authenticate/signin"
       :id "signin"
       :hidden {:on-error "/"}}
      (biff/recaptcha-callback "submitSignin" "signin")
      (shared/input :id "email" :type "email" :label "Email address" :required? true)
      (shared/input :id "password" :type "password" :label "Password" :required? true)
      [:div {:class "mt-5"}
       (shared/btn :variant :dark :size :md :type "submit"
                   :class (str "w-full" (when site-key " g-recaptcha"))
                   :attrs (when site-key {:data-sitekey site-key :data-callback "submitSignin"})
                   "Sign in")]
      (on-error ctx))]
    [:div {:class "px-8 py-4 bg-zinc-50 border-t border-zinc-100 text-center"}
     [:p {:class "text-sm text-zinc-500"}
      "Don't have an account? "
      [:a {:href "/signup" :class "font-medium text-emerald-600 hover:text-emerald-700 hover:underline"} "Create one"]]]]])

(defn sign-up [{:keys [site-key] :as ctx}]
  [:div {:class "w-full max-w-sm mx-auto"}
   [:div {:class "bg-white rounded-2xl shadow-card-md border border-zinc-200/70 overflow-hidden"}
    [:div {:class "px-8 py-8"}
     [:div {:class "mb-6 text-center"}
      [:a {:href "/"}
       [:span {:class "text-3xl font-giza font-bold text-zinc-900 leading-none"} "greed."]]
      [:h2 {:class "mt-4 text-lg font-semibold text-zinc-900 tracking-tight"} "Create an account"]
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
       (shared/btn :variant :primary :size :md :type "submit"
                   :class (str "w-full" (when site-key " g-recaptcha"))
                   :attrs (when site-key {:data-sitekey site-key :data-callback "submitSignup"})
                   "Create account")]
      (on-error ctx))]
    [:div {:class "px-8 py-4 bg-zinc-50 border-t border-zinc-100 text-center"}
     [:p {:class "text-sm text-zinc-500"}
      "Already have an account? "
      [:a {:href "/signin" :class "font-medium text-zinc-700 hover:text-zinc-900 hover:underline"} "Sign in"]]]]])

(defn user [ctx]
  (shared/card {:class "p-6"}
               [:h2 {:class "text-base font-semibold text-zinc-900 tracking-tight mb-5"} "Personal Information"]
               (biff/form
                {:action "/app/save-user"}
                [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
                 (shared/app-input ctx :id "firstname" :type "text" :label "First Name" :required? true)
                 (shared/app-input ctx :id "lastname" :type "text" :label "Last Name" :required? true)
                 (shared/app-input ctx :id "age" :type "number" :label "Age" :required? true)
                 (shared/app-input ctx :id "email" :type "text" :label "Email" :required? true)
                 [:div {:class "mt-4"}
                  [:label {:class "block text-sm font-medium text-zinc-700 mb-1" :for "password"} "Password"]
                  [:input {:class "block w-full px-3 py-2 text-sm text-zinc-700 bg-white border border-zinc-200 rounded-lg focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 placeholder-zinc-400 transition-colors duration-150"
                           :id "password" :name "password" :type "password"
                           :placeholder "Leave blank to keep your current password"}]
                  [:p {:class "text-xs text-zinc-400 mt-1"} "Only enter a new password if you want to change it."]]]
                [:div {:class "flex justify-end mt-5"}
                 (shared/btn :variant :dark :type "submit"
                             "Save changes")])))

(defn- account-type-options
  [bank]
  (sort (get-in c/common-config [:banking/account-types bank] #{})))

(defn account-type-field
  "htmx fragment: the account-type select (and wrapper) for the given bank.
   Returned from /app/account-type-options when the bank select changes."
  [{:keys [params]}]
  (shared/app-account-type-select :id "account-type" :label "Account Type"
                                  :hint "The type of account you hold at this bank"
                                  :options (account-type-options (some-> (:bank params) keyword))))

(defn finances [ctx]
  (let [{:keys [session]} ctx
        user-id            (:uid session)
        finances           (data/get-finances ctx user-id)
        bank-options       (sort (:banking/banks c/common-config))
        current-bank       (or (:finances/bank finances) (first bank-options))
        current-account-type (:finances/account-type finances)]
    (shared/card {:class "p-6"}
                 [:div {:class "mb-6"}
                  [:h2 {:class "text-base font-semibold text-zinc-900 tracking-tight"} "Financial Details"]
                  [:p {:class "text-sm text-zinc-400 mt-0.5"} "Personalises your dashboard, bank card, and tax estimates."]]
                 (biff/form
                  {:action "/app/save-finances"}

      ;; Banking
                  [:div
                   [:p {:class "text-xs font-semibold text-zinc-400 uppercase tracking-wider mb-3"} "Banking"]
                   [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
                    (shared/app-select ctx :id "bank" :label "Bank" :options bank-options :required? true
                                       :hint "Your primary banking institution"
                                       :attrs {"hx-get" "/app/finances/account-type-options"
                                               "hx-trigger" "change"
                                               "hx-target" "#account-type-field"
                                               "hx-swap" "outerHTML"
                                               "_" "on htmx:beforeRequest add .opacity-50 to #account-type-field
on htmx:afterRequest remove .opacity-50 from #account-type-field"})
                    (shared/app-account-type-select :id "account-type" :label "Account Type"
                                                    :hint "The type of account you hold at this bank"
                                                    :options (account-type-options current-bank)
                                                    :selected current-account-type)]]

      ;; Income
                  [:div {:class "border-t border-zinc-100 pt-5 mt-5"}
                   [:p {:class "text-xs font-semibold text-zinc-400 uppercase tracking-wider mb-3"} "Income"]
                   [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
                    (shared/app-input ctx :id "salary" :type "number" :label "Monthly Gross Salary" :required? true
                                      :prefix "R" :hint "Your salary before any deductions")
                    (shared/app-input ctx :id "payday" :type "number" :label "Pay Day" :required? true
                                      :hint "Day of the month you receive your salary (1–31)")]]

                  [:div {:class "flex justify-end pt-5 mt-5 border-t border-zinc-100"}
                   (shared/btn :variant :dark :type "submit"
                               "Save changes")]))))

(defn tax-profile [{:keys [session] :as ctx}]
  (let [user-id (:uid session)
        tp      (data/get-tax-profile ctx user-id)
        val     (fn [k] (str (or (k tp) 0)))]
    (shared/card {:class "p-6"}
                 [:div {:class "mb-6"}
                  [:h2 {:class "text-base font-semibold text-zinc-900 tracking-tight"} "Tax Assessment Profile"]
                  [:p {:class "text-sm text-zinc-400 mt-0.5"}
                   "Stored and used automatically in your tax return auto assessment."]]
                 (biff/form
                  {:action "/app/save-tax-profile"}

      ;; Medical Aid
                  [:div
                   [:p {:class "text-xs font-semibold text-zinc-400 uppercase tracking-wider mb-3"} "Medical Aid"]
                   [:p {:class "text-xs text-zinc-400 mb-4 leading-relaxed"}
                    "Leave these at 0 if you are not on medical aid. Credits are applied automatically (R364/month for you + first dependant, R246/month for each additional)."]
                   [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
                    (shared/labeled-input :id "medical-monthly" :type "number" :label "Monthly Contributions"
                                          :prefix "R" :min "0" :value (val :tax-profile/medical-monthly)
                                          :hint "Your share of the monthly medical aid premium")
                    (shared/labeled-input :id "medical-dependants" :type "number" :label "Dependants"
                                          :min "0" :value (val :tax-profile/medical-dependants)
                                          :hint "Number of registered dependants, excluding yourself")]]

      ;; Retirement
                  [:div {:class "border-t border-zinc-100 pt-5 mt-5"}
                   [:p {:class "text-xs font-semibold text-zinc-400 uppercase tracking-wider mb-3"} "Retirement Annuity"]
                   [:p {:class "text-xs text-zinc-400 mb-4 leading-relaxed"}
                    "RA contributions reduce your taxable income. The deduction is capped at 27.5% of your income or R350,000, whichever is lower."]
                   [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
                    (shared/labeled-input :id "ra-annual" :type "number" :label "Annual RA Contributions"
                                          :prefix "R" :min "0" :value (val :tax-profile/ra-annual)
                                          :hint "Total personal RA contributions for the year")]]

                  [:div {:class "flex justify-end pt-5 mt-5 border-t border-zinc-100"}
                   (shared/btn :variant :dark :type "submit"
                               "Save changes")]))))

(def ^:private budget-item-form-copy
  "Modal heading + blurb per category, so the Add dialog says exactly what it
   will create. Falls back to the generic copy when no :type is given."
  {:income   {:title "Add income"  :desc "Add an income source to your budget."}
   :expenses {:title "Add expense" :desc "Add an expense to your budget."}
   :savings  {:title "Add savings" :desc "Add a savings item to your budget."}})

(defn budget-item-form
  "Add a budget item. Pass :type to fix the category (used by each list's Add
   button); without it, the category select is shown."
  [& {:keys [type]}]
  (let [budget-item-options (:budget-item/types c/common-config)
        modal-id            (if type (str "budget-add-" (name type) "-modal") "budget-add-modal")
        {:keys [title desc]} (get budget-item-form-copy type
                                  {:title "Add Budget Item" :desc "Add a new item to your budget"})]
    [:div {:class "bg-white rounded-xl border border-zinc-200/70 shadow-card-md p-6 w-full max-w-sm"}
     [:h3 {:class "text-base font-semibold text-zinc-900 tracking-tight"} title]
     [:p {:class "mt-1 text-sm text-zinc-500 mb-4"} desc]
     (biff/form
      {:class "mt-4" :action "/app/finances/create-budget-item"}
      (if type
        [:input {:type "hidden" :name "type" :value (name type)}]
        (shared/modal-select :id "type" :label "Category" :options budget-item-options :required? true))
      (shared/modal-input :id "title" :type "text" :label "Title" :required? true)
      (shared/modal-input :id "amount" :type "number" :label "Amount (R)" :required? true)
      [:div {:class "flex gap-2 mt-5"}
       (shared/btn :variant :ghost :type "button" :class "flex-1"
                   :attrs {:_ (shared/close-actions modal-id)}
                   "Cancel")
       (shared/btn :variant :primary :type "submit" :class "flex-1"
                   "Add item")])]))

(defn budget-action-form [item]
  [:div {:class "bg-white rounded-xl border border-zinc-200/70 shadow-card-md p-6 w-full max-w-sm"}
   [:h3 {:class "text-base font-semibold text-zinc-900 tracking-tight"} "Edit Budget Item"]
   [:div {:class "mt-3 p-3 bg-zinc-50 rounded-lg text-sm text-zinc-600 mb-4"}
    [:div {:class "flex justify-between"} [:span {:class "text-zinc-400"} "Title"] (:budget-item/title item)]
    [:div {:class "flex justify-between mt-1"} [:span {:class "text-zinc-400"} "Amount"] (str "R" (:budget-item/amount item))]]
   (biff/form
    {:class "mt-2" :action (str "/app/finances/update-budget-item?budget-item-id=" (:xt/id item))}
    (shared/modal-input :id "title" :type "text" :label "New Title" :required? true)
    (shared/modal-input :id "amount" :type "number" :label "New Amount (R)" :required? true)
    [:div {:class "flex gap-2 mt-5"}
     (shared/btn :variant :ghost :type "button" :class "flex-1"
                 :attrs {:_ (shared/close-actions (str "budget-action-" (:xt/id item)))}
                 "Cancel")
     (shared/btn :variant :dark :type "submit" :class "flex-1"
                 "Update")])
   (biff/form
    {:class "mt-2" :action "/app/finances/delete-budget-item"}
    [:input {:type "hidden" :name "budget-item-id" :value (str (:xt/id item))}]
    (shared/btn :variant :danger :type "submit" :class "w-full"
                :attrs {:data-confirm "Delete this budget item?"}
                "Delete"))])
