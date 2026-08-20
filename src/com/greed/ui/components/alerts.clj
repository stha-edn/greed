(ns com.greed.ui.components.alerts
  (:require [com.biffweb :as biff]
            [com.core :as c]
            [com.greed.ui.components.shared :as shared]
            [com.greed.ui.components.svgs :as svgs]))

(defn confirm-dialog
  "Styled replacement for window.confirm(). Driven by the vanilla-JS
   GreedConfirm module in main.js — no Alpine dependency. The markup is
   static; JS toggles the `data-open` attribute (display) and the
   `confirm-visible` class (opacity/scale transition). Any `data-confirm`
   submit button is intercepted by a delegated listener and, on accept, its
   form is submitted (native or htmx). Render once per app page."
  []
  [:div {:id "confirm-dialog"
         :role "dialog"
         :aria-modal "true"
         :aria-labelledby "confirm-dialog-title"
         :aria-describedby "confirm-dialog-message"
         :class "fixed inset-0 z-50 flex items-center justify-center p-4"}
   [:div {:data-cf-overlay "true"
          :class "absolute inset-0 bg-black/50 backdrop-blur-sm confirm-overlay"}]
   [:div {:data-cf-card "true"
          :class "relative z-10 w-full max-w-sm rounded-2xl bg-white p-6 shadow-card-md confirm-card"}
    [:h3 {:data-cf-title "true" :class "text-base font-semibold text-zinc-900"}
     "Are you sure?"]
    [:p {:data-cf-message "true" :class "mt-2 text-sm text-zinc-500"}]
    [:div {:class "mt-6 flex justify-end gap-3"}
     (shared/btn :variant :outline :size :md :attrs {:data-cf-cancel "true"} "Cancel")
     (shared/btn :variant :danger :size :md :attrs {:data-cf-accept "true"} "Delete")]]])

(defn- alert-error?
  "True when the alert key represents an error/blocking notice rather than a success."
  [alert-key]
  (contains? (:alert/errors c/common-config) alert-key))

(defn ^:deprecated success
  "Celebratory confirmation card for a just-completed signin/signup — same
   markup, entrance/exit, and dismiss treatment as `info`'s non-error style
   (greed-alert-in/-out), shown once right after the redirect to /app via
   ?success=signin|signup."
  [& {:keys [type]
      :or {type :signin}}]
  [:div
   {:id "success-alert"
    :_ (str "init\n"
            "  wait 6s\n"
            "  add .greed-alert-out\n"
            "  wait 200ms\n"
            "  hide #success-alert")
    :class "flex items-center gap-3 w-full rounded-xl bg-emerald-50 ring-1 ring-emerald-200 shadow-card p-4 greed-alert-in"}
   [:div
    {:class "flex-shrink-0 w-9 h-9 rounded-lg flex items-center justify-center bg-emerald-100 text-emerald-600"}
    (svgs/check)]
   [:div {:class "flex-1 min-w-0"}
    [:p {:class "text-sm font-medium leading-snug text-emerald-900"}
     (if (= type :signin)
       "You are signed in!"
       "Your account was created!")]]
   [:button
    {:type "button"
     :_ (str "on click\n"
             "  add .greed-alert-out to #success-alert\n"
             "  wait 200ms\n"
             "  hide #success-alert")
     :class "flex-shrink-0 self-start p-1 -m-1 rounded-md transition active:scale-[0.9] text-emerald-500 hover:text-emerald-700 hover:bg-emerald-100"}
    (svgs/close)]])

(defn info
  "Dismissible toast, driven by an `:alert` key in `params` (usually the
   request's query params, e.g. `?alert=goal-saved`, but a synthetic
   `{:alert \"key\"}` map works too). Non-error alerts auto-hide after 6s —
   purely visual, the session is never touched by the timeout.

   Pass `:dismiss-href` for an alert that shouldn't reappear once seen (e.g.
   dashboard.clj's app-update-banner-due?): the close button then POSTs
   there instead of just hiding client-side, so dismissal persists. The 6s
   auto-hide still only hides — acknowledging is an explicit click, not an
   idle timeout, so an unread banner still greets the user next visit."
  [params & {:keys [dismiss-href]}]
  (let [alert     (:alert params)
        alert-key (when alert (keyword "alert" alert))
        message   (get c/alert-config alert-key)
        error?    (alert-error? alert-key)
        close-btn-class (str "flex-shrink-0 self-start p-1 -m-1 rounded-md transition active:scale-[0.9] "
                             (if error? "text-rose-400 hover:text-rose-700 hover:bg-rose-100"
                                 "text-emerald-500 hover:text-emerald-700 hover:bg-emerald-100"))]
    (when message
      [:div
       {:id "info-alert"
        :_ (when-not error?
             (str "init\n"
                  "  wait 6s\n"
                  "  add .greed-alert-out\n"
                  "  wait 200ms\n"
                  "  hide #info-alert"))
        :class (str "flex items-center gap-3 w-full rounded-xl ring-1 shadow-card p-4 greed-alert-in "
                    (if error? "bg-rose-50 ring-rose-200" "bg-emerald-50 ring-emerald-200"))}
       [:div
        {:class (str "flex-shrink-0 w-9 h-9 rounded-lg flex items-center justify-center "
                     (if error? "bg-rose-100 text-rose-600" "bg-emerald-100 text-emerald-600"))}
        (if error? (svgs/close) (svgs/check))]
       [:div {:class "flex-1 min-w-0"}
        [:p {:class (str "text-sm font-medium leading-snug "
                         (if error? "text-rose-900" "text-emerald-900"))}
         message]]
       (if dismiss-href
         (biff/form {:action dismiss-href}
                    [:button {:type "submit" :class close-btn-class} (svgs/close)])
         [:button
          {:type "button"
           :_ (str "on click\n"
                   "  add .greed-alert-out to #info-alert\n"
                   "  wait 200ms\n"
                   "  hide #info-alert")
           :class close-btn-class}
          (svgs/close)])])))

(defn finance-tax-prompt-modal
  "Modal prompting the user to complete their finance & tax profile. Rendered
   only when the prompt is due (see dashboard/finance-tax-prompt-due?), so it
   is shown directly by the server — no client-side visibility state needed.
   Escape and a backdrop click both defer it (same effect as \"Later\") —
   every other dialog in the app (shared/modal, confirm-dialog) offers that
   same way out, and there's no lighter-weight \"just close it\" here since
   it would only reappear on the next page load anyway."
  []
  [:div
   {:class "relative flex justify-center"}
   [:div
    {:role "dialog"
     :aria-labelledby "finance-tax-prompt-title"
     :aria-modal "true"
     :_ "on keydown[key=='Escape'] from window trigger click on #finance-tax-prompt-later-submit"
     :class "fixed inset-0 z-10 overflow-y-auto"}
    [:div {:class "absolute inset-0 z-0 bg-zinc-950/60 backdrop-blur-sm greed-scrim-in"
           :_ "on click trigger click on #finance-tax-prompt-later-submit"
           :aria-hidden "true"}]
    [:div
     {:class "relative z-10 flex items-end justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:block sm:p-0"}
     [:span
      {:class "hidden sm:inline-block sm:h-screen sm:align-middle"
       :aria-hidden "true"}
      "\u00A0"]
     [:div
      {:class "relative inline-block overflow-hidden text-left align-bottom bg-gradient-to-br from-zinc-800 via-zinc-900 to-black ring-1 ring-white/10 rounded-2xl shadow-card-md sm:my-8 sm:align-middle sm:max-w-lg sm:w-full greed-modal-in"}
      [:div {:class "absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-emerald-500/40 to-transparent"}]
      [:div {:class "absolute -top-24 -right-24 h-64 w-64 rounded-full bg-emerald-500/10 blur-3xl"}]
      [:div
       {:class "relative px-6 py-5 sm:p-6"}
       [:div
        {:class "flex items-start"}
        [:div
         {:class "flex items-center justify-center flex-shrink-0 w-12 h-12"}
         (svgs/percent-badge)]
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
          (svgs/heart)]
         [:div
          {:class "min-w-0"}
          [:p {:class "text-sm font-medium text-zinc-200"} "Medical aid"]
          [:p {:class "text-xs text-zinc-500"} "Monthly contributions and dependants"]]]
        [:div
         {:class "flex items-center gap-3"}
         [:div
          {:class "flex items-center justify-center flex-shrink-0 w-8 h-8 rounded-full bg-emerald-500/10 text-emerald-300"}
          (svgs/umbrella)]
         [:div
          {:class "min-w-0"}
          [:p {:class "text-sm font-medium text-zinc-200"} "Retirement annuity"]
          [:p {:class "text-xs text-zinc-500"} "Annual RA contributions"]]]]
       [:div
        {:class "flex justify-end gap-3 mt-6"}
        (biff/form {:action "/app/dismiss-finance-tax-prompt"}
                   [:button
                    {:id "finance-tax-prompt-later-submit"
                     :type "submit"
                     :class "px-4 py-2 text-sm font-medium text-zinc-700 bg-white border border-zinc-300 rounded-md transition hover:bg-zinc-50 active:scale-[0.97] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500"}
                    "Later"])
        [:a
         {:href "/app/settings"
          :class "inline-flex justify-center px-4 py-2 text-sm font-medium text-white bg-emerald-500/20 border border-transparent rounded-md transition hover:bg-emerald-600/20 active:scale-[0.97] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500"}
         "Update"]]]]]]])
