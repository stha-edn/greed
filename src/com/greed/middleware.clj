(ns com.greed.middleware
  (:require [com.biffweb :as biff]
            [muuntaja.middleware :as muuntaja]
            [ring.middleware.anti-forgery :as csrf]
            [ring.middleware.defaults :as rd]
            [com.greed.data.core :as data]
            [com.greed.authentication :as auth]))

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

(defn wrap-authenticate [handler]
  (fn [{:keys [uri] :as ctx}]
    (let [error-location (if (= "/authenticate/signup" uri)
                           "/signup?error=invalid-email"
                           "/signin?error=invalid-credentials")]
      (if (auth/authenticate! ctx)
        (handler ctx)
        {:status 303
         :headers {"location" error-location}}))))

(defn save-user [ctx]
  (let [user-id (data/get-user-id ctx)
        user (data/get-user ctx user-id)]
    (if user
      (data/update-user ctx)
      (data/upsert-user ctx)))
  {:status 303
   :headers {"location" "/app/settings?alert=user-saved"}})

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
      (data/upsert-tax-profile ctx)))
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
      biff/wrap-log-requests))
