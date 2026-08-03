(ns com.greed.middleware
  (:require [clojure.tools.logging :as logger]
            [com.biffweb :as biff]
            [com.biffweb.impl.auth :as biff-auth]
            [muuntaja.middleware :as muuntaja]
            [ring.middleware.anti-forgery :as csrf]
            [ring.middleware.defaults :as rd]
            [com.greed.data.core :as data]
            [com.greed.utilities.core :as utilities]
            [com.greed.authentication :as auth]))

(defonce signin-attempts (atom {}))

(defn get-client-ip [ctx]
  (or (get-in ctx [:headers "x-real-ip"])
      (:remote-addr ctx)))

(defn rate-limit-exceeded?
  "Fixed-window rate limit keyed by an arbitrary string.
  Returns true when `limit` or more attempts happen within `window-ms`.
  Stale entries are pruned so the atom stays bounded."
  [key limit window-ms]
  (let [now (System/currentTimeMillis)
        window (quot now window-ms)
        limits (swap! signin-attempts
                      (fn [m]
                        (let [pruned (if (< 1000 (count m))
                                       (into {}
                                             (filter (fn [[_ e]] (= window (:window e))))
                                             m)
                                       m)
                              entry (get pruned key)]
                          (if (and entry (= window (:window entry)))
                            (assoc pruned key (update entry :count inc))
                            (assoc pruned key {:count 1 :window window})))))]
    (> (:count (get limits key)) limit)))

(def csp-header
  (str "default-src 'self'; "
       "script-src 'self' 'unsafe-inline' 'unsafe-eval' "
       "https://unpkg.com https://cdn.jsdelivr.net "
       "https://www.googletagmanager.com https://www.google.com https://www.gstatic.com; "
       "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
       "font-src 'self' https://fonts.gstatic.com; "
       "img-src 'self' data: https://www.google.com https://www.gstatic.com "
       "https://www.googletagmanager.com; "
       "connect-src 'self' https://cdn.jsdelivr.net https://www.google.com "
       "https://www.gstatic.com https://www.google-analytics.com "
       "https://region1.google-analytics.com ws://mygreed.co.za wss://mygreed.co.za; "
       "frame-src https://www.google.com https://www.gstatic.com; "
       "base-uri 'self'; object-src 'none'; form-action 'self'; frame-ancestors 'none'"))

(defn wrap-security-headers [handler]
  (fn [ctx]
    (let [resp (handler ctx)]
      (if (map? resp)
        (update resp :headers assoc "Content-Security-Policy" csp-header)
        resp))))

(defn wrap-redirect-signed-in [handler]
  (fn [{:keys [session] :as ctx}]
    (if (some? (:uid session))
      {:status 303
       :headers {"location" "/app"}}
      (handler ctx))))

(defn wrap-signed-in [handler]
  (fn [{:keys [session] :as ctx}]
    (if (some? (:uid session))
      (handler ctx)
      {:status 303
       :headers {"location" "/signin?error=not-signed-in"}})))

(defn wrap-admin [handler]
  (fn [{:keys [session] :as ctx}]
    (if (data/admin? (data/get-user ctx (:uid session)))
      (handler ctx)
      {:status 303
       :headers {"location" "/app"}})))

(defn wrap-authenticate [handler]
  (fn [{:keys [uri] :as ctx}]
    (let [page (if (= "/authenticate/signup" uri) "signup" "signin")
          result (auth/authenticate! ctx)]
      (cond
        (and (contains? #{"/authenticate/signin" "/authenticate/signup"} uri)
             (rate-limit-exceeded? (get-client-ip ctx) 5 (* 60 1000)))
        (do (logger/info (str "Authentication rate limit exceeded for IP " (get-client-ip ctx)))
            {:status 429
             :headers {"content-type" "text/html; charset=utf-8"}
             :body "<h1>Too many attempts</h1><p>Please wait a minute and try again.</p>"})

        (not (biff-auth/passed-recaptcha? ctx))
        {:status 303
         :headers {"location" (str "/" page "?error=recaptcha")}}

        (not (:valid? result))
        {:status 303
         :headers {"location" (cond
                                (= :account-deactivated (:error result))
                                "/signin?error=account-deactivated"

                                (= page "signup")
                                "/signup?error=invalid-email"

                                :else
                                "/signin?error=invalid-credentials")}}

        :else (handler ctx)))))

(defn save-user [ctx]
  (let [user-id (data/get-user-id-from-session ctx)
        user    (data/get-user ctx user-id)]
    (cond
      (nil? user)
      {:status 303
       :headers {"location" "/signin?error=not-signed-in"}}

      (data/email-taken-by-other? ctx user-id)
      {:status 303
       :headers {"location" "/app/settings?error=email-taken"}}

      :else
      (do (data/update-user ctx)
          {:status 303
           :headers {"location" "/app/settings?alert=user-saved"}}))))

(defn save-finances [ctx]
  (let [user-id (data/get-user-id-from-session ctx)
        finances (data/get-finances ctx user-id)]
    (if finances
      (data/update-finances ctx)
      (data/upsert-finances ctx)))
  {:status 303
   :headers {"location" "/app/settings?alert=finances-saved"}})

(defn save-tax-profile [ctx]
  (let [user-id (data/get-user-id-from-session ctx)
        tp      (data/get-tax-profile ctx user-id)]
    (if tp
      (data/update-tax-profile ctx)
      (data/upsert-tax-profile ctx))
    ;; Reflect the medical aid contribution as a budget expense, like Salary -> income.
    (data/sync-medical-budget-item ctx)
    (data/sync-retirment-budget-item ctx))
  {:status 303
   :headers {"location" "/app/settings?alert=tax-profile-saved"}})

(defn create-budget-item [ctx]
  (data/upsert-budget-item ctx)
  {:status 303
   :headers {"location" "/app/finances?alert=budget-item-saved"}})

(defn update-budget-item [ctx]
  (data/update-budget-item ctx)
  {:status 303
   :headers {"location" "/app/finances?alert=budget-item-saved"}})

(defn delete-budget-item [ctx]
  (data/delete-budget-item ctx)
  {:status 303
   :headers {"location" "/app/finances?alert=budget-item-deleted"}})

(defn create-goal [ctx]
  (data/upsert-goal ctx)
  {:status 303
   :headers {"location" "/app/goals?alert=goal-saved"}})

(defn update-goal [ctx]
  (data/update-goal ctx)
  {:status 303
   :headers {"location" "/app/goals?alert=goal-saved"}})

(defn delete-goal [ctx]
  (data/delete-goal ctx)
  {:status 303
   :headers {"location" "/app/goals?alert=goal-deleted"}})

(defn admin-update-user [{:keys [session params] :as ctx}]
  (let [user-id (utilities/->uuid (:user-id params))]
    (cond
      (nil? (data/get-user ctx user-id))
      {:status 303
       :headers {"location" "/app/admin/users?alert=user-not-found"}}

      (data/email-taken-by-other? ctx user-id)
      {:status 303
       :headers {"location" "/app/admin/users?alert=email-taken"}}

      (and (= user-id (:uid session)) (not= (:role params) "admin"))
      {:status 303
       :headers {"location" "/app/admin/users?alert=cannot-remove-own-admin-role"}}

      :else
      (do (data/admin-update-user ctx)
          {:status 303
           :headers {"location" "/app/admin/users?alert=user-saved"}}))))

(defn admin-hash-user-password [ctx]
  (let [result (data/admin-hash-user-password ctx)
        alert (case result
                :hashed "password-hashed"
                :already-hashed "password-already-hashed"
                "user-not-found")]
    {:status 303
     :headers {"location" (str "/app/admin/users?alert=" alert)}}))

(defn admin-delete-user [ctx]
  (let [result (data/admin-delete-user ctx)
        alert (case result
                :self "cannot-delete-self"
                :not-found "user-not-found"
                "user-deleted")]
    {:status 303
     :headers {"location" (str "/app/admin/users?alert=" alert)}}))

(defn logout [{:keys [session]}]
  {:status 303
   :headers {"location" "/"}
   :session (dissoc session :uid)})

(defn wrap-site-defaults [handler]
  (-> handler
      biff/wrap-render-rum
      biff/wrap-anti-forgery-websockets
      csrf/wrap-anti-forgery
      biff/wrap-session
      muuntaja/wrap-params
      muuntaja/wrap-format
      (rd/wrap-defaults (-> rd/site-defaults
                            (assoc-in [:security :anti-forgery] false)
                            (assoc-in [:responses :absolute-redirects] true)
                            (assoc :session false)
                            (assoc :static false)))))

(defn wrap-api-defaults [handler]
  (-> handler
      muuntaja/wrap-params
      muuntaja/wrap-format
      (rd/wrap-defaults rd/api-defaults)))

(defn wrap-base-defaults [handler]
  (-> handler
      biff/wrap-https-scheme
      biff/wrap-resource
      biff/wrap-internal-error
      biff/wrap-ssl
      biff/wrap-log-requests
      wrap-security-headers))
