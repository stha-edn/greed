(ns com.greed.ui.components.alerts
  (:require [com.biffweb :as biff]
            [com.core :as c]
            [com.greed.ui.components.svgs :as svgs]))

(defn- alert-error?
  "True when the alert key represents an error/blocking notice rather than a success."
  [alert-key]
  (contains? (:alert/errors c/common-config) alert-key))

(defn success [& {:keys [type]
                  :or {type :signin}}]
  [:div
   {:class "flex w-full max-w-sm overflow-hidden rounded-xl bg-white ring-1 ring-emerald-200 shadow-card"}
   [:div
    {:class "flex items-center justify-center w-12 bg-emerald-600 text-white"}
    (svgs/success)]
   [:div
    {:class "px-4 py-3"}
    [:div
     {:class "mx-3"}
     [:p {:class "font-semibold text-lg text-zinc-900"} "Success"]
     [:p {:class "text-sm text-zinc-500"}
      (if (= type :signin)
        "You are signed in!"
        "Your account was created!")]]]])

(defn info [params]
  (let [alert     (:alert params)
        alert-key (when alert (keyword "alert" alert))
        message   (get c/alert-config alert-key)
        error?    (alert-error? alert-key)]
    (when message
      [:div
       {:x-data (str "{ show: true, autoDismiss: " (not error?)
                     ", init() { if (this.autoDismiss) setTimeout(() => { this.show = false }, 6000) } }")
        :x-show "show"
        :x-cloak ""
        "x-transition:enter" "transition duration-300 ease-out"
        "x-transition:enter-start" "-translate-y-3 opacity-0"
        "x-transition:enter-end" "translate-y-0 opacity-100"
        "x-transition:leave" "transition duration-200 ease-in"
        "x-transition:leave-start" "translate-y-0 opacity-100"
        "x-transition:leave-end" "-translate-y-2 opacity-0"
        :class (str "flex items-start gap-3 w-full rounded-xl ring-1 shadow-card p-4 "
                    (if error? "bg-rose-50 ring-rose-200" "bg-emerald-50 ring-emerald-200"))}
       [:div
        {:class (str "flex-shrink-0 w-9 h-9 rounded-lg flex items-center justify-center "
                     (if error? "bg-rose-100 text-rose-600" "bg-emerald-100 text-emerald-600"))}
        (if error? (svgs/close) (svgs/check))]
       [:div {:class "flex-1 min-w-0"}
        [:p {:class (str "text-sm font-medium leading-snug "
                         (if error? "text-rose-900" "text-emerald-900"))}
         message]]
       [:button
        {:type "button"
         :x-on:click "show = false"
         :class (str "flex-shrink-0 self-start p-1 -m-1 rounded-md transition-colors "
                     (if error? "text-rose-400 hover:text-rose-700 hover:bg-rose-100"
                        "text-emerald-500 hover:text-emerald-700 hover:bg-emerald-100"))}
        (svgs/close)]])))

(defn finance-tax-prompt-modal
  "Modal prompting the user to complete their finance & tax profile.
   Parent must have Alpine x-data with showFinanceTaxPrompt."
  []
  [:div
   {:class "relative flex justify-center"}
   [:div
    {:role "dialog"
     :aria-labelledby "finance-tax-prompt-title"
     :aria-modal "true"
     :class "fixed inset-0 z-10 overflow-y-auto"
     :x-cloak ""
     :x-show "showFinanceTaxPrompt"
     "x-transition:enter" "transition duration-300 ease-out"
     "x-transition:enter-start" "translate-y-4 opacity-0 sm:translate-y-0 sm:scale-95"
     "x-transition:enter-end" "translate-y-0 opacity-100 sm:scale-100"
     "x-transition:leave" "transition duration-150 ease-in"
     "x-transition:leave-start" "translate-y-0 opacity-100 sm:scale-100"
     "x-transition:leave-end" "translate-y-4 opacity-0 sm:translate-y-0 sm:scale-95"}
    [:div
     {:class "flex items-end justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:block sm:p-0"}
     [:span
      {:class "hidden sm:inline-block sm:h-screen sm:align-middle"
       :aria-hidden "true"}
      "\u00A0"]
     [:div
      {:class "relative inline-block overflow-hidden text-left align-bottom transition-all transform bg-gradient-to-br from-zinc-800 via-zinc-900 to-black rounded-lg shadow-xl sm:my-8 sm:align-middle sm:max-w-lg sm:w-full"}
      [:div
       {:class "px-6 py-5 sm:p-6"}
       [:div
        {:class "flex items-start"}
        [:div
         {:class "flex items-center justify-center flex-shrink-0 w-12 h-12"}
         (svgs/chart-bar)]
        [:div
         {:class "flex-1 mt-0 ml-4"}
         [:h3
          {:id "finance-tax-prompt-title"
           :class "text-lg font-medium leading-6 text-zinc-300"}
          "Complete your finance & tax profile"]
         [:p
          {:class "mt-2 text-sm text-zinc-500"}
          "A few details help us personalise your dashboard, bank card, and tax estimates."]]]
       [:div
        {:class "mt-5 space-y-3"}
        [:div
         {:class "flex items-center gap-3"}
         [:div
          {:class "flex items-center justify-center flex-shrink-0 w-8 h-8 rounded-full bg-emerald-500/10 text-emerald-300"}
          (svgs/banknotes)]
         [:div
          {:class "min-w-0"}
          [:p {:class "text-sm font-medium text-zinc-200"} "Banking"]
          [:p {:class "text-xs text-zinc-500"} "Bank and account type"]]]
        [:div
         {:class "flex items-center gap-3"}
         [:div
          {:class "flex items-center justify-center flex-shrink-0 w-8 h-8 rounded-full bg-emerald-500/10 text-emerald-300"}
          (svgs/money)]
         [:div
          {:class "min-w-0"}
          [:p {:class "text-sm font-medium text-zinc-200"} "Income"]
          [:p {:class "text-xs text-zinc-500"} "Monthly gross salary and pay day"]]]
        [:div
         {:class "flex items-center gap-3"}
         [:div
          {:class "flex items-center justify-center flex-shrink-0 w-8 h-8 rounded-full bg-emerald-500/10 text-emerald-300"}
          (svgs/suit-case)]
         [:div
          {:class "min-w-0"}
          [:p {:class "text-sm font-medium text-zinc-200"} "Medical aid"]
          [:p {:class "text-xs text-zinc-500"} "Monthly contributions and dependants"]]]
        [:div
         {:class "flex items-center gap-3"}
         [:div
          {:class "flex items-center justify-center flex-shrink-0 w-8 h-8 rounded-full bg-emerald-500/10 text-emerald-300"}
          (svgs/target)]
         [:div
          {:class "min-w-0"}
          [:p {:class "text-sm font-medium text-zinc-200"} "Retirement annuity"]
          [:p {:class "text-xs text-zinc-500"} "Annual RA contributions"]]]]
        [:div
         {:class "flex justify-end gap-3 mt-6"}
         (biff/form {:action "/app/dismiss-finance-tax-prompt"}
           [:button
            {:type "submit"
             :class "px-4 py-2 text-sm font-medium text-zinc-700 bg-white border border-zinc-300 rounded-md hover:bg-zinc-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-amber-500"}
            "Later"])
         [:a
         {:href "/app/settings"
          :class "inline-flex justify-center px-4 py-2 text-sm font-medium text-white bg-emerald-500/20 border border-transparent rounded-md hover:bg-emerald-600/20 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500"}
         "Update"]]]]]]])
