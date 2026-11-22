(ns com.greed.ui.components.alerts
  (:require [com.biffweb :as biff]
            [com.core :as c]
            [com.greed.data.core :as data]
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

(defn app-update-due?
  "True when the signed-in person hasn't acknowledged the current
   :app/version (common.edn) yet. Bumping :app/version shows the update
   notice to everyone again."
  [{:keys [session]}]
  (and (:uid session)
       (not= (:app-update-seen-version session) (:app/version c/common-config))))

;; ---------------------------------------------------------------------------
;; Toasts: every notice (saved, signed in, signed out, updated, and errors)
;; looks and behaves the same, and sits in the same place on every page.

(def ^:private kinds
  {:success {:icon (svgs/check) :badge "bg-brand-50 text-brand-700 ring-brand-600/20"}
   :error   {:icon (svgs/close) :badge "bg-rose-50 text-rose-700 ring-rose-600/20"}
   :update  {:icon (svgs/success) :badge "bg-zinc-100 text-zinc-700 ring-zinc-900/10"}})

(defn- toast
  "One notice. Success and update notices that can be dismissed by just
   closing them hide by themselves after 6 seconds; errors, and an update
   that must be acknowledged (:dismiss-href), stay until closed."
  [{:keys [id kind title message dismiss-href next]}]
  (let [{:keys [icon badge]} (kinds kind)
        sticky? (or (= :error kind) dismiss-href)
        hide    (str "add .greed-alert-out to #" id "\n  wait 200ms\n  remove #" id)]
    [:div {:id id
           :role (if (= :error kind) "alert" "status")
           :_ (when-not sticky? (str "init\n  wait 6s\n  " hide))
           :class "pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-lg bg-white p-4 shadow-card-md ring-1 ring-zinc-200/70 greed-alert-in"}
     [:span {:class (str "flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full ring-1 [&_svg]:h-4 [&_svg]:w-4 " badge)
             :aria-hidden "true"}
      icon]
     [:div {:class "min-w-0 flex-1 pt-0.5"}
      (when title [:p {:class "text-sm font-semibold text-zinc-900"} title])
      [:p {:class (str "text-sm leading-snug " (if title "mt-0.5 text-zinc-600" "text-zinc-800"))} message]]
     (let [cls "flex-shrink-0 rounded-md p-1 -m-1 text-zinc-400 transition hover:bg-zinc-100 hover:text-zinc-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 active:scale-[0.9] [&_svg]:h-4 [&_svg]:w-4"]
       (if dismiss-href
         ;; acknowledging is remembered, and you stay on the same page
         (biff/form {:action dismiss-href :hidden {:next (or next "/app")}}
                    [:button {:type "submit" :class cls :aria-label "Dismiss"} (svgs/close)])
         [:button {:type "button" :class cls :aria-label "Dismiss" :_ (str "on click\n  " hide)}
          (svgs/close)]))]))

(defn- signin-toast [kind user]
  (let [first-name (some-> (:user/firstname user) not-empty)]
    (case kind
      :signin {:kind :success
               :title "You're signed in"
               :message (if first-name (str "Welcome back, " first-name ".") "Welcome back.")}
      :signup {:kind :success
               :title "Your account is ready"
               :message (str (if first-name (str "Welcome to greed, " first-name ". ") "Welcome to greed. ")
                             "Add your salary on Budget, or upload a statement on Banking.")}
      nil)))

(defn toasts
  "All of the request's notices, in one stack at the top of the screen
   (top right on wide screens): ?success=signin|signup, ?alert=<key> from
   common.edn, and, inside the app, the app-update notice until it is
   acknowledged."
  [{:keys [params uri session] :as ctx} & {:keys [in-app?]}]
  (let [user      (when (:success params) (data/get-user ctx (:uid session)))
        alert-key (some->> (:alert params) (keyword "alert"))
        message   (get c/alert-config alert-key)
        items     (cond-> []
                    (:success params)
                    (conj (signin-toast (keyword (:success params)) user))
                    message
                    (conj {:kind (if (alert-error? alert-key) :error :success) :message message})
                    (and in-app? (app-update-due? ctx))
                    (conj {:kind :update :title "What's new"
                           :message (:alert/app-updated c/common-config)
                           :dismiss-href "/app/dismiss-app-update-banner"
                           :next uri}))
        items     (keep identity items)]
    (when (seq items)
      [:div {:aria-live "polite"
             :class (str "pointer-events-none fixed inset-x-0 z-40 flex flex-col items-center gap-2 px-4 "
                         (if in-app?
                           "top-[calc(3.5rem+env(safe-area-inset-top)+0.75rem)] md:top-6 md:left-auto md:right-6 md:items-end md:px-0"
                           "top-4 md:left-auto md:right-6 md:items-end md:px-0"))}
       (map-indexed (fn [i t] (toast (assoc t :id (str "toast-" i)))) items)])))
