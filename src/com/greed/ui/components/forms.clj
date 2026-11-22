(ns com.greed.ui.components.forms
  (:require [clojure.tools.logging :as log]
            [com.biffweb :as biff]
            [com.core :as c]
            [com.greed.data.core :as data]
            [com.greed.utilities.core :as tools]
            [com.greed.statements.categorise :as categorise]
            [com.greed.utilities.vat :as vat]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.svgs :as svgs]))

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
            :invalid-link (:error/invalid-link config)
            :password-blank (:error/password-blank config)
            :password-mismatch (:error/password-mismatch config)
            :business-name-blank (:error/business-name-blank config)
            :business-name-too-long (:error/business-name-too-long config)
            :entity-type-invalid (:error/entity-type-invalid config)
            (:error/default config)))]])))

(defn sign-in [{:keys [site-key] :as ctx}]
  [:div {:class "w-full max-w-sm mx-auto"}
   [:div {:class "bg-white rounded-lg border border-zinc-200 overflow-hidden"}
    [:div {:class "px-8 py-8"}
     [:div {:class "mb-6 text-center"}
      [:a {:href "/" :class "inline-block"}
       [:img {:src "/img/greed-symbol-green-tight.svg"
              :alt "greed"
              :width 192 :height 192
              :class "block h-10 w-10"}]]
      [:h2 {:class "mt-4 text-lg font-semibold text-zinc-900 tracking-tight"} "Welcome back"]
      [:p {:class "mt-1 text-sm text-zinc-500"} "Sign in to your account"]]
     (biff/form
      {:action "authenticate/signin"
       :id "signin"
       :hidden {:on-error "/"}}
      (biff/recaptcha-callback "submitSignin" "signin")
      (shared/input :id "email" :type "email" :label "Email address" :required? true)
      (shared/input :id "password" :type "password" :label "Password" :required? true)
      [:div {:class "mt-1 text-right"}
       [:a {:href "/forgot-password" :class "text-xs font-medium text-zinc-900 underline underline-offset-4 hover:text-zinc-600"} "Forgot password?"]]
      [:div {:class "mt-5"}
       (shared/btn :variant :dark :size :md :type "submit"
                   :class (str "w-full" (when site-key " g-recaptcha"))
                   :attrs (when site-key {:data-sitekey site-key :data-callback "submitSignin"})
                   "Sign in")]
      (on-error ctx))]
    [:div {:class "px-8 py-4 bg-zinc-50 border-t border-zinc-100 text-center"}
     [:p {:class "text-sm text-zinc-500"}
      "Don't have an account? "
      [:a {:href "/signup" :class "font-medium text-zinc-900 underline underline-offset-4 hover:text-zinc-600"} "Create one"]]]]])

(defn- account-type-switch
  "Personal | Business, as two links so it works without JavaScript."
  [business?]
  (let [tab (fn [href label active?]
              [:a (cond-> {:href href
                           :class (str "flex-1 rounded-md px-3 py-1.5 text-center text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 "
                                       (if active? "bg-white text-zinc-900 ring-1 ring-zinc-200" "text-zinc-600 hover:text-zinc-900"))}
                    active? (assoc :aria-current "page"))
               label])]
    [:nav {:aria-label "Account type" :class "mb-6 flex gap-1 rounded-lg bg-zinc-100 p-1"}
     (tab "/signup" "Personal" (not business?))
     (tab "/signup?as=business" "Business" business?)]))

(defn sign-up [{:keys [site-key params] :as ctx}]
  [:div {:class "w-full max-w-sm mx-auto"}
   [:div {:class "bg-white rounded-lg border border-zinc-200 overflow-hidden"}
   (let [business? (= "business" (:as params))]
    [:div {:class "px-8 py-8"}
     [:div {:class "mb-6 text-center"}
      [:a {:href "/" :class "inline-block"}
       [:img {:src "/img/greed-symbol-green-tight.svg"
              :alt "greed"
              :width 192 :height 192
              :class "block h-10 w-10"}]]
      [:h2 {:class "mt-4 text-lg font-semibold text-zinc-900 tracking-tight"} "Create an account"]
      [:p {:class "mt-1 text-sm text-zinc-500"}
       (if business? "Run your business's money next to your own" "Start managing your finances")]]
     (account-type-switch business?)
     (biff/form
      {:action "authenticate/signup"
       :id "signup"
       :hidden {:on-error "/" :account-type (if business? "business" "personal")}}
      (biff/recaptcha-callback "submitSignup" "signup")
      (when business?
        [:<>
         (shared/input :id "business-name" :type "text" :label "Business name" :required? true)
         [:div {:class "mt-4"}
          (shared/form-label "entity-type" "Type of business")
          [:select {:id "entity-type" :name "entity-type" :required true :class (shared/base-input-class)}
           [:option {:value ""} "Choose one"]
           (for [[k label] data/entity-types]
             [:option {:value (name k)} label])]]
         [:p {:class "mt-6 text-sm font-medium text-zinc-900"} "About you"]])
      [:div {:class "grid grid-cols-2 gap-3"}
       (shared/input :id "firstname" :type "text" :label "First name" :required? true)
       (shared/input :id "lastname" :type "text" :label "Last name" :required? true)]
      (when-not business?
        (shared/input :id "age" :type "number" :label "Age" :required? true))
      (shared/input :id "email" :type "email" :label "Email address" :required? true)
      (shared/input :id "password" :type "password" :label "Password" :required? true)
      [:div {:class "mt-5"}
       (shared/btn :variant :primary :size :md :type "submit"
                   :class (str "w-full" (when site-key " g-recaptcha"))
                   :attrs (when site-key {:data-sitekey site-key :data-callback "submitSignup"})
                   (if business? "Create business account" "Create account"))]
      (on-error ctx))])
    [:div {:class "px-8 py-4 bg-zinc-50 border-t border-zinc-100 text-center"}
     [:p {:class "text-sm text-zinc-500"}
      "Already have an account? "
      [:a {:href "/signin" :class "font-medium text-zinc-700 hover:text-zinc-900 hover:underline"} "Sign in"]]]]])

(defn forgot-password [{:keys [site-key] :as ctx}]
  [:div {:class "w-full max-w-sm mx-auto"}
   [:div {:class "bg-white rounded-lg border border-zinc-200 overflow-hidden"}
    [:div {:class "px-8 py-8"}
     [:div {:class "mb-6 text-center"}
      [:a {:href "/" :class "inline-block"}
       [:img {:src "/img/greed-symbol-green-tight.svg"
              :alt "greed"
              :width 192 :height 192
              :class "block h-10 w-10"}]]
      [:h2 {:class "mt-4 text-lg font-semibold text-zinc-900 tracking-tight"} "Reset your password"]
      [:p {:class "mt-1 text-sm text-zinc-500"} "Enter your email and we'll send you a reset link."]]
     (biff/form
      {:action "/forgot-password"
       :id "forgot-password"}
      (biff/recaptcha-callback "submitForgotPassword" "forgot-password")
      (shared/input :id "email" :type "email" :label "Email address" :required? true)
      [:div {:class "mt-5"}
       (shared/btn :variant :dark :size :md :type "submit"
                   :class (str "w-full" (when site-key " g-recaptcha"))
                   :attrs (when site-key {:data-sitekey site-key :data-callback "submitForgotPassword"})
                   "Send reset link")]
      (on-error ctx))]
    [:div {:class "px-8 py-4 bg-zinc-50 border-t border-zinc-100 text-center"}
     [:p {:class "text-sm text-zinc-500"}
      "Remembered your password? "
      [:a {:href "/signin" :class "font-medium text-zinc-700 hover:text-zinc-900 hover:underline"} "Sign in"]]]]])

(defn reset-password [{:keys [site-key params] :as ctx}]
  (let [token (:token params)]
    [:div {:class "w-full max-w-sm mx-auto"}
     [:div {:class "bg-white rounded-lg border border-zinc-200 overflow-hidden"}
      [:div {:class "px-8 py-8"}
       [:div {:class "mb-6 text-center"}
        [:a {:href "/" :class "inline-block"}
         [:img {:src "/img/greed-symbol-green-tight.svg"
                :alt "greed"
                :width 192 :height 192
                :class "block h-10 w-10"}]]
        [:h2 {:class "mt-4 text-lg font-semibold text-zinc-900 tracking-tight"} "Choose a new password"]
        [:p {:class "mt-1 text-sm text-zinc-500"} "Your new password will take effect immediately."]]
       (biff/form
        {:action "/reset-password"
         :id "reset-password"
         :hidden {:token token}}
        (biff/recaptcha-callback "submitResetPassword" "reset-password")
        (shared/input :id "password" :type "password" :label "New password" :required? true)
        (shared/input :id "confirm-password" :type "password" :label "Confirm new password" :required? true)
        [:div {:class "mt-5"}
         (shared/btn :variant :primary :size :md :type "submit"
                     :class (str "w-full" (when site-key " g-recaptcha"))
                     :attrs (when site-key {:data-sitekey site-key :data-callback "submitResetPassword"})
                     "Reset password")]
        (on-error ctx))]
      [:div {:class "px-8 py-4 bg-zinc-50 border-t border-zinc-100 text-center"}
       [:p {:class "text-sm text-zinc-500"}
        [:a {:href "/signin" :class "font-medium text-zinc-700 hover:text-zinc-900 hover:underline"} "Back to sign in"]]]]]))

(defn password-reset-sent [_ctx]
  [:div {:class "w-full max-w-sm mx-auto"}
   [:div {:class "bg-white rounded-lg border border-zinc-200 overflow-hidden"}
    [:div {:class "px-8 py-8 text-center"}
     [:a {:href "/" :class "inline-block"}
      [:img {:src "/img/greed-symbol-green-tight.svg"
             :alt "greed"
             :width 192 :height 192
             :class "block h-10 w-10"}]]
     [:h2 {:class "mt-4 text-lg font-semibold text-zinc-900 tracking-tight"} "Check your email"]
     [:p {:class "mt-2 text-sm text-zinc-500"}
      "If an account exists for that email, we've sent you a password reset link. "
      "It expires in one hour."]
     [:div {:class "mt-6 flex flex-col gap-2"}
      (shared/btn :variant :dark :size :md :href "/forgot-password"
                  "Send another link")
      (shared/btn :variant :ghost :size :md :href "/signin"
                  "Back to sign in")]]]])

(defn invalid-link [_ctx]
  [:div {:class "w-full max-w-sm mx-auto"}
   [:div {:class "bg-white rounded-lg border border-zinc-200 overflow-hidden"}
    [:div {:class "px-8 py-8 text-center"}
     [:a {:href "/" :class "inline-block"}
      [:img {:src "/img/greed-symbol-green-tight.svg"
             :alt "greed"
             :width 192 :height 192
             :class "block h-10 w-10"}]]
     [:h2 {:class "mt-4 text-lg font-semibold text-zinc-900 tracking-tight"} "Invalid reset link"]
     [:p {:class "mt-2 text-sm text-zinc-500"}
      "This password reset link is invalid or has expired."]
     [:div {:class "mt-6"}
      (shared/btn :variant :dark :size :md :href "/forgot-password" :class "w-full"
                  "Request a new link")]]]])

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
                  [:input {:class "block w-full px-3 py-2 text-sm text-zinc-700 bg-white border border-zinc-200 rounded-lg focus:outline-none focus:border-zinc-400 focus:ring-1 focus:ring-zinc-400 placeholder-zinc-400 transition-colors duration-150"
                           :id "password" :name "password" :type "password"
                           :placeholder "Leave blank to keep your current password"}]
                  [:p {:class "text-xs text-zinc-500 mt-1"} "Only enter a new password if you want to change it."]]]
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

(defn bank-details
  "Banking: the person's bank and account type, for the bank card."
  [ctx]
  (let [{:keys [session]} ctx
        finances             (data/get-finances ctx (:uid session))
        bank-options         (sort (:banking/banks c/common-config))
        current-bank         (or (:finances/bank finances) (first bank-options))
        current-account-type (:finances/account-type finances)]
    (shared/card {:class "p-6"}
                 [:div {:class "mb-5"}
                  [:h2 {:class "text-sm font-semibold tracking-tight text-zinc-900"} "Your bank"]
                  [:p {:class "mt-0.5 text-xs text-zinc-500"} "Shown on your bank card."]]
                 (biff/form
                  {:action "/app/banking/save-bank"}
                  [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
                   (shared/app-select ctx :id "bank" :label "Bank" :options bank-options :required? true
                                      :hint "Your main bank"
                                      :attrs {"hx-get" "/app/finances/account-type-options"
                                              "hx-trigger" "change"
                                              "hx-target" "#account-type-field"
                                              "hx-swap" "outerHTML"
                                              "_" "on htmx:beforeRequest add .opacity-50 to #account-type-field
on htmx:afterRequest remove .opacity-50 from #account-type-field"})
                   (shared/app-account-type-select :id "account-type" :label "Account Type"
                                                   :hint "The type of account you hold at this bank"
                                                   :options (account-type-options current-bank)
                                                   :selected current-account-type)]
                  [:div {:class "flex justify-end pt-5 mt-5 border-t border-zinc-100"}
                   (shared/btn :variant :dark :type "submit" "Save bank")]))))

(defn finances
  "Budget: salary and payday. The bank is on Banking."
  [ctx]
  (shared/card {:class "p-6"}
               [:div {:class "mb-6"}
                [:h2 {:class "text-sm font-semibold tracking-tight text-zinc-900"} "Salary and payday"]
                [:p {:class "mt-0.5 text-xs text-zinc-500"}
                 "For your take-home pay, tax estimates and payday. Your bank is set on "
                 [:a {:href "/app/banking" :class "font-medium text-zinc-900 underline underline-offset-2"} "Banking"] "."]]
               (biff/form
                {:action "/app/save-finances"}
                [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
                 (shared/app-input ctx :id "salary" :type "number" :label "Monthly Gross Salary" :required? true
                                   :prefix "R" :hint "Your salary before any deductions")
                 (shared/app-input ctx :id "payday" :type "number" :label "Pay Day" :required? true
                                   :hint "Day of the month you receive your salary (1-31)")]
                [:div {:class "flex justify-end pt-5 mt-5 border-t border-zinc-100"}
                 (shared/btn :variant :dark :type "submit" "Save changes")])))

(defn tax-profile [{:keys [session] :as ctx}]
  (let [user-id (:uid session)
        tp      (data/get-tax-profile ctx user-id)
        val     (fn [k] (str (or (k tp) 0)))]
    (shared/card {:class "p-6"}
                 [:div {:class "mb-6"}
                  [:h2 {:class "text-sm font-semibold tracking-tight text-zinc-900"} "Tax assessment profile"]
                  [:p {:class "mt-0.5 text-xs text-zinc-500"}
                   "Used in your annual tax estimate and in the ITR12 simulator. Saving it also adds these to your budget."]]
                 (biff/form
                  {:action "/app/save-tax-profile"}

      ;; Medical Aid
                  [:div
                   [:p {:class "text-xs font-medium text-zinc-500 mb-3"} "Medical Aid"]
                   [:p {:class "text-xs text-zinc-500 mb-4 leading-relaxed"}
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
                   [:p {:class "text-xs font-medium text-zinc-500 mb-3"} "Retirement Annuity"]
                   [:p {:class "text-xs text-zinc-500 mb-4 leading-relaxed"}
                    "RA contributions reduce your taxable income. The deduction is capped at 27.5% of your income or R350,000, whichever is lower."]
                   [:div {:class "grid grid-cols-1 gap-4 sm:grid-cols-2"}
                    (shared/labeled-input :id "ra-annual" :type "number" :label "Annual RA Contributions"
                                          :prefix "R" :min "0" :value (val :tax-profile/ra-annual)
                                          :hint "Total personal RA contributions for the year")]]

                  [:div {:class "flex justify-end pt-5 mt-5 border-t border-zinc-100"}
                   (shared/btn :variant :dark :type "submit"
                               "Save changes")]))))

(defn edit-modal
  "One of the forms above (finances, tax-profile) in a modal, opened by a
   button with (shared/open-actions id). Opening the page at #`hash` opens
   it too, so links from other pages land on the form."
  [id hash form]
  [:<>
   (shared/modal id
                 [:div {:class "relative w-[min(92vw,36rem)] max-h-[90vh] overflow-y-auto rounded-lg"}
                  [:button {:type "button" :title "Close" :aria-label "Close"
                            :class "absolute right-3 top-3 z-10 flex h-8 w-8 items-center justify-center rounded-md text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"
                            :_ (shared/close-actions id)}
                   (svgs/close {:class "h-4 w-4"})]
                  form])
   [:script (biff/unsafe (str "if(location.hash==='#" hash "'){var m=document.getElementById('" id "');if(m){m.setAttribute('open','true');}}"))]])

(defn- edit-strip
  "A slim card: what is set now, and the button that opens its modal."
  [title items modal-id label]
  [:div {:class "flex flex-col gap-3 rounded-lg bg-white px-5 py-4 ring-1 ring-zinc-200/70 sm:flex-row sm:items-center sm:justify-between"}
   [:div {:class "min-w-0"}
    [:p {:class "text-sm font-semibold tracking-tight text-zinc-900"} title]
    [:p {:class "mt-0.5 flex flex-wrap gap-x-4 gap-y-1 text-sm text-zinc-500"}
     (for [[k v] items] [:span k ": " [:span {:class "font-semibold tabular-nums text-zinc-900"} v]])]]
   (shared/btn :variant :outline :size :md :class "flex-shrink-0" :attrs {:_ (shared/open-actions modal-id)} label)])

(defn salary-strip
  "Budget: the salary and payday now, with an Edit button and its modal."
  [{:keys [session] :as ctx}]
  (let [f (data/get-finances ctx (:uid session))
        salary (:finances/salary f)
        payday (:finances/payday f)]
    [:div {:id "salary"}
     (edit-strip "Salary and payday"
                 [["Monthly salary" (if salary (tools/whole->rands salary) "not set")]
                  ["Payday" (if payday (str "the " payday (case (mod payday 10) 1 (if (= 11 payday) "th" "st") 2 (if (= 12 payday) "th" "nd") 3 (if (= 13 payday) "th" "rd") "th")) "not set")]]
                 "salary-modal" (if salary "Edit salary and payday" "Add salary and payday"))
     (edit-modal "salary-modal" "salary" (finances ctx))]))

(defn tax-profile-strip
  "Tax: medical aid and retirement annuity now, with an Edit button and its modal."
  [{:keys [session] :as ctx}]
  (let [tp  (data/get-tax-profile ctx (:uid session))
        med (or (:tax-profile/medical-monthly tp) 0)
        ra  (or (:tax-profile/ra-annual tp) 0)]
    [:div {:id "deductions"}
     (edit-strip "Medical aid and retirement annuity"
                 [["Medical aid" (if (pos? med) (str (tools/whole->rands med) " a month") "none")]
                  ["Dependants" (str (or (:tax-profile/medical-dependants tp) 0))]
                  ["Retirement annuity" (if (pos? ra) (str (tools/whole->rands ra) " a year") "none")]]
                 "tax-profile-modal" "Edit tax details")
     (edit-modal "tax-profile-modal" "deductions" (tax-profile ctx))]))

(def ^:private budget-item-form-copy
  "Modal heading + blurb per category, so the Add dialog says exactly what it
   will create. Falls back to the generic copy when no :type is given."
  {:income   {:title "Add income"  :desc "Add an income source to your budget."}
   :expenses {:title "Add expense" :desc "Add an expense to your budget."}
   :savings  {:title "Add savings" :desc "Add a savings item to your budget."}})

(defn- category-select
  "An item's spending category: payments its reference matches are counted
   there on the Spending page. Business categories are offered to a
   business's items only."
  [current business?]
  [:div {:class "mt-3"}
   (shared/form-label "category" "Spending category (optional)")
   [:select {:id "category" :name "category" :class (shared/base-input-class) :aria-describedby "category-hint"}
    [:option {:value ""} "As greed reads it"]
    (for [k categorise/spending-categories
          :when (or business? (not (categorise/business-only k)))]
      [:option (cond-> {:value (name k)} (= k current) (assoc :selected true))
       (categorise/categories k)])]
   [:p {:id "category-hint" :class "mt-1 text-xs text-zinc-500"}
    (if business?
      "Payments matched by the reference count in this category, e.g. Stock and suppliers, or Owner's drawings."
      "Payments matched by the reference count in this category, e.g. Insurance for a policy's debit order.")]])

(defn- vat-select
  "How VAT applies to a VAT vendor's income or expense item."
  [budget-type current]
  (let [current (or current (vat/default-vat-type budget-type))]
    [:div {:class "mt-3"}
     (shared/form-label "vat" "VAT")
     [:select {:id "vat" :name "vat" :class (shared/base-input-class) :aria-describedby "vat-hint"}
      (for [[k label] vat/vat-types]
        [:option (cond-> {:value (name k)} (= k current) (assoc :selected true)) label])]
     [:p {:id "vat-hint" :class "mt-1 text-xs text-zinc-500"}
      "The amount includes VAT. Salaries, SARS payments and suppliers who are not VAT registered have no VAT."]]))

(defn- paid-to-select
  "Who a business's Owner's drawings item is paid to, when it has more than
   one person to choose from. `owners` are [{:id :name}]."
  [owners current]
  (when (> (count owners) 1)
    [:div {:class "mt-3"}
     (shared/form-label "paid-to" "Paid to (for Owner's drawings)")
     [:select {:id "paid-to" :name "paid-to" :class (shared/base-input-class)}
      [:option {:value ""} "Not chosen"]
      (for [{:keys [id name]} owners]
        [:option (cond-> {:value (str id)} (= id current) (assoc :selected true)) name])]]))

(defn budget-item-form
  "Add a budget item. Pass :type to fix the category (used by each list's Add
   button); without it, the category select is shown. :business? adds the
   spending category and business wording."
  [& {:keys [type business? vat? owners]}]
  (let [budget-item-options (:budget-item/types c/common-config)
        modal-id            (if type (str "budget-add-" (name type) "-modal") "budget-add-modal")
        {:keys [title desc]} (if (and business? (= :savings type))
                               {:title "Add set-aside item" :desc "Money the business puts away: SARS, VAT, reserves or equipment."}
                               (get budget-item-form-copy type
                                    {:title "Add Budget Item" :desc "Add a new item to your budget"}))]
    [:div {:class "bg-white rounded-xl border border-zinc-200/70 shadow-xl p-6 w-full max-w-sm"}
     [:h3 {:class "text-base font-semibold text-zinc-900 tracking-tight"} title]
     [:p {:class "mt-1 text-sm text-zinc-500 mb-4"} desc]
     (biff/form
      {:class "mt-4" :action "/app/finances/create-budget-item"}
      (if type
        [:input {:type "hidden" :name "type" :value (name type)}]
        (shared/modal-select :id "type" :label "Category" :options budget-item-options :required? true))
      (shared/modal-input :id "title" :type "text" :label "Title" :required? true)
      (shared/modal-input :id "amount" :type "number" :label "Amount (R)" :required? true)
      (when (and vat? (#{:income :expenses} type))
        (vat-select type nil))
      (when (not= :income type)
        (category-select nil business?))
      (when (and business? (= :expenses type))
        (paid-to-select owners nil))
      (when (not= :income type)
        (shared/modal-input :id "reference" :type "text" :label "Statement reference (optional)"
                            :placeholder "e.g. discovery life, vitality"
                            :hint "Words from your bank statement for this item, separated by commas. Numbers are ignored, so a reference that changes each month still matches (WESBANK_FI8534...0825 matches \"wesbank\")."))
      [:div {:class "flex gap-2 mt-5"}
       (shared/btn :variant :ghost :type "button" :class "flex-1"
                   :attrs {:_ (shared/close-actions modal-id)}
                   "Cancel")
       (shared/btn :variant :dark :type "submit" :class "flex-1"
                   "Add item")])]))

(defn budget-action-form
  "Edit a budget item. An item kept in step with Settings (:auto?) can only
   change its statement reference, and cannot be deleted here."
  [item & {:keys [auto? vat? owners]}]
  [:div {:class "bg-white rounded-xl border border-zinc-200/70 shadow-xl p-6 w-full max-w-sm"}
   [:h3 {:class "text-base font-semibold text-zinc-900 tracking-tight"} (if auto? "Statement reference" "Edit Budget Item")]
   [:div {:class "mt-3 p-3 bg-zinc-50 rounded-lg text-sm text-zinc-600 mb-4"}
    [:div {:class "flex justify-between"} [:span {:class "text-zinc-500"} "Title"] (:budget-item/title item)]
    [:div {:class "flex justify-between mt-1"} [:span {:class "text-zinc-500"} "Amount"] (str "R" (:budget-item/amount item))]]
   (biff/form
    {:class "mt-2" :action (str "/app/finances/update-budget-item?budget-item-id=" (:xt/id item))}
    (when-not auto?
      [:<>
       (shared/modal-input :id "title" :type "text" :label "Title" :required? true :value (:budget-item/title item))
       (shared/modal-input :id "amount" :type "number" :label "Amount (R)" :required? true :value (str (:budget-item/amount item)))])
    (when (and vat? (:budget-item/business-id item) (#{:income :expenses} (:budget-item/type item)))
      (vat-select (:budget-item/type item) (:budget-item/vat item)))
    (when (not= :income (:budget-item/type item))
      (category-select (:budget-item/category item) (some? (:budget-item/business-id item))))
    (when (and (:budget-item/business-id item) (= :expenses (:budget-item/type item)))
      (paid-to-select owners (:budget-item/paid-to item)))
    (when (not= :income (:budget-item/type item))
      (shared/modal-input :id "reference" :type "text" :label "Statement reference (optional)"
                          :value (:budget-item/reference item)
                          :placeholder "e.g. discovery life, vitality"
                          :hint "Words from your bank statement for this item, separated by commas. Numbers are ignored, so a reference that changes each month still matches (WESBANK_FI8534...0825 matches \"wesbank\")."))
    (when auto?
      [:p {:class "mt-3 text-xs text-zinc-500"} "The title and amount follow your Settings."])
    [:div {:class "flex gap-2 mt-5"}
     (shared/btn :variant :ghost :type "button" :class "flex-1"
                 :attrs {:_ (shared/close-actions (str "budget-action-" (:xt/id item)))}
                 "Cancel")
     (shared/btn :variant :dark :type "submit" :class "flex-1"
                 "Update")])
   (when (:budget-item/from-statement item)
     (biff/form
      {:class "mt-2" :action "/app/finances/make-monthly"}
      [:input {:type "hidden" :name "budget-item-id" :value (str (:xt/id item))}]
      (shared/btn :variant :outline :type "submit" :class "w-full" "Make monthly")
      [:p {:class "mt-1 text-xs text-zinc-500"}
       "For a cost or income that repeats: it then counts in every month, not only its statement's month."]))
   (when-not auto?
     (biff/form
      {:class "mt-2" :action "/app/finances/delete-budget-item"}
      [:input {:type "hidden" :name "budget-item-id" :value (str (:xt/id item))}]
      (shared/btn :variant :danger :type "submit" :class "w-full"
                  :attrs {:data-confirm "Delete this budget item?"}
                  "Delete")))])
