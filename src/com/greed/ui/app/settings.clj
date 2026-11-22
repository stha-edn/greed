(ns com.greed.ui.app.settings
  "Personal Settings: only settings of the app itself. Your account (name,
   email, password, age), your subscription, and deleting your profile. Money details live where they are used: salary
   and payday on Budget, medical aid and retirement annuity on Tax, your
   bank on Banking."
  (:require [clojure.tools.logging :as log]
            [com.biffweb :as biff]
            [com.greed.authentication :as auth]
            [com.greed.data.core :as data]
            [com.greed.data.plans :as plans]
            [com.greed.payments.billing :as billing]
            [com.greed.payments.notices :as notices]
            [com.greed.ui.components.plans :as plan-cards]
            [com.greed.ui :as ui]
            [com.greed.ui.components.forms :as forms]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.stats :as stats]))

(def ^:private panel "overflow-hidden rounded-lg bg-white ring-1 ring-zinc-200/70")

(defn- pill [text tone]
  [:span {:class (str "flex-shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold ring-1 "
                      (if (= :brand tone) "bg-brand-50 text-brand-700 ring-brand-600/20" "bg-zinc-100 text-zinc-700 ring-zinc-200"))}
   text])

(defn- subscription-section
  "The paid plan: what it costs, when it renews or ends, and cancelling it
   (or undoing a cancellation) right here. Card changes are on Paystack."
  [{:keys [session] :as ctx}]
  (let [{:keys [subscription trial? days-left admin?]} (plans/status ctx (:uid session))
        sub     subscription
        status  (:subscription/status sub)
        tier    (:subscription/plan sub)
        interval (or (:subscription/interval sub) :monthly)
        ends    (some-> (:subscription/period-ends sub) notices/date-text)
        price   (when (plans/paid? sub) (plans/price tier interval))]
    [:div {:id "subscription" :class (str panel " scroll-mt-20")}
     (if (plans/paid? sub)
       [:div
        [:div {:class "flex flex-col gap-4 px-5 py-5 sm:flex-row sm:items-start sm:justify-between"}
         [:div {:class "min-w-0"}
          [:div {:class "flex flex-wrap items-center gap-2"}
           [:p {:class "text-sm font-semibold tracking-tight text-zinc-900"} (str (plans/tier-name tier) ", " (if (= :yearly interval) "yearly" "monthly"))]
           (case status
             :non-renewing (pill "Cancelled" :zinc)
             :attention [:span {:class "rounded-full bg-rose-50 px-2.5 py-1 text-xs font-semibold text-rose-700 ring-1 ring-rose-600/20"} "Payment failed"]
             (pill "Active" :brand))]
          [:p {:class "mt-1 text-sm text-zinc-500"}
           (str (plan-cards/rands price) (if (= :yearly interval) " a year" " a month") ", by card through Paystack. "
                (case status
                  :non-renewing (str "It ends on " (or ends "the last day you paid for") " and won't renew.")
                  :attention "The last renewal didn't go through. Update your card to keep it."
                  (str "Renews on " (or ends "your next billing date") ".")))]]
         [:div {:class "flex flex-shrink-0 flex-wrap gap-2"}
          (shared/btn :variant :outline :size :md :href "/app/plan/manage" "Update card")
          (if (= :non-renewing status)
            (biff/form {:action "/app/settings/resume-subscription"}
                       (shared/btn :variant :dark :size :md :type "submit" "Keep my plan"))
            (biff/form {:action "/app/settings/cancel-subscription"}
                       [:button {:type "submit"
                                 :data-confirm (str "Cancel your " (plans/tier-name tier) " subscription? You keep it until "
                                                    (or ends "the end of the period you paid for")
                                                    ", then move to Free. You won't be charged again.")
                                 :class "inline-flex items-center justify-center gap-2 rounded-lg border border-rose-200 bg-white px-4 py-2 text-sm font-medium text-rose-700 transition hover:bg-rose-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-400 active:scale-[0.97]"}
                        "Cancel subscription"]))]]]
       [:div {:class "flex flex-col gap-4 px-5 py-5 sm:flex-row sm:items-center sm:justify-between"}
        [:div {:class "min-w-0"}
         [:p {:class "text-sm font-semibold tracking-tight text-zinc-900"} "No paid subscription"]
         [:p {:class "mt-0.5 text-sm text-zinc-500"}
          (cond admin? "Admins have every feature without one."
                trial? (str "You are on the free trial, with " days-left " day" (when (not= 1 days-left) "s") " left. Nothing is charged unless you subscribe.")
                :else "You are on Free. Nothing is charged.")]]
        (when-not admin?
          (shared/btn :variant :dark :size :md :class "flex-shrink-0" :href "/app/plan" "See plans"))])]))

(def ^:private delete-modal-id "delete-profile-modal")

(defn- delete-section [{:keys [params]}]
  [:div {:id "delete" :class (str panel " ring-rose-200")}
   [:div {:class "flex flex-col gap-4 px-5 py-5 sm:flex-row sm:items-center sm:justify-between"}
    [:div {:class "min-w-0"}
     [:p {:class "text-sm font-semibold tracking-tight text-zinc-900"} "Delete your profile"]
     [:p {:class "mt-0.5 max-w-[60ch] text-sm text-zinc-500"}
      "Deletes your account and everything in it: your budget, statements, goals, calendar and tax details, and businesses where you are the only member. A paid plan stops renewing. This cannot be undone."]]
    (shared/btn :variant :danger :size :md :class "flex-shrink-0" :attrs {:_ (shared/open-actions delete-modal-id)} "Delete profile")]
   (when (= "wrong-password" (:error params))
     [:p {:role "alert" :class "border-t border-rose-100 bg-rose-50 px-5 py-3 text-sm text-rose-700"}
      "That password was not right, so nothing was deleted."])
   (shared/modal delete-modal-id
                 [:div {:class "w-[min(92vw,28rem)] rounded-lg bg-white p-6"}
                  [:h3 {:class "text-base font-semibold text-zinc-900"} "Delete your profile?"]
                  [:p {:class "mt-2 text-sm text-zinc-600"}
                   "Everything in your account is deleted for good, and you are signed out. Enter your password to confirm."]
                  (biff/form {:action "/app/settings/delete-profile" :class "mt-4"}
                             (shared/form-label "delete-password" "Password")
                             [:input {:id "delete-password" :name "password" :type "password" :required true
                                      :autocomplete "current-password" :class (shared/base-input-class)}]
                             [:div {:class "mt-6 flex justify-end gap-3"}
                              (shared/btn :variant :ghost :size :md :attrs {:_ (shared/close-actions delete-modal-id)} "Cancel")
                              (shared/btn :variant :danger :size :md :type "submit" "Delete for good")])])
   (when (= "wrong-password" (:error params))
     [:script (biff/unsafe (str "document.getElementById('" delete-modal-id "').setAttribute('open','true');"))])])

(defn page [ctx]
  (ui/app
   ctx
   [:div {:class "space-y-7"}
    (headers/pages-heading ["Settings"] "Your account and your subscription.")
    [:div
     (stats/section-header "Account")
     [:div {:class "max-w-3xl"} (forms/user ctx)]]
    [:div
     (stats/section-header "Subscription")
     [:div {:class "max-w-3xl"} (subscription-section ctx)]]
    [:div
     (stats/section-header "Danger zone")
     [:div {:class "max-w-3xl"} (delete-section ctx)]]]))

(defn delete-profile
  "POST /app/settings/delete-profile: the person deletes their own profile,
   after confirming their password. A paid plan is stopped with Paystack
   first; if that fails, nothing is deleted."
  [{:keys [session params] :as ctx}]
  (let [uid  (:uid session)
        user (data/get-user ctx uid)]
    (cond
      (nil? user)
      {:status 303 :headers {"location" "/"} :session (dissoc session :uid :business-id)}

      (not (:valid? (auth/validate-password? (str (:password params)) (:user/password user))))
      {:status 303 :headers {"location" "/app/settings?error=wrong-password#delete"}}

      (not (billing/cancel-for-deletion! ctx uid))
      {:status 303 :headers {"location" "/app/settings?alert=delete-billing-failed#delete"}}

      :else
      (do (log/info "A person deleted their own profile" uid)
          (data/delete-user ctx uid)
          {:status 303
           :headers {"location" "/?alert=profile-deleted"}
           :session (dissoc session :uid :business-id)}))))
