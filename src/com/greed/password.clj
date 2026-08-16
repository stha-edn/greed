(ns com.greed.password
  (:require [clojure.string :as str]
            [com.biffweb :as biff]
            [com.biffweb.impl.auth :as biff-auth]
            [com.greed.data.core :as data]
            [com.greed.email :as email]
            [com.greed.ui :as ui]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.pages.password :as p.password]))

(defn- reset-link [{:keys [biff/secret biff/base-url]} email token]
  (str base-url "/reset-password?token="
       (biff/jwt-encrypt {:intent "reset-password"
                          :email email
                          :token token
                          :exp-in (* 60 60)}
                         (secret :biff/jwt-secret))))

(defn validate-reset-token
  "Decrypts and checks the reset token from the request params. Returns
   {:valid? true :user user} when the token is unexpired, belongs to a
   real user, and matches the single-use token stored on that user."
  [ctx]
  (let [{:keys [biff/secret]} ctx
        token (get-in ctx [:params :token])
        claims (some-> token (biff/jwt-decrypt (secret :biff/jwt-secret)))
        email (:email claims)
        user (when (and claims (= "reset-password" (:intent claims)))
               (data/get-user-by-email ctx email))]
    (if (and user (= (:user/password-reset-token user) (:token claims)))
      {:valid? true :user user}
      {:valid? false})))

(defn forgot-password-page [{:keys [recaptcha/site-key] :as ctx}]
  (let [ctx (assoc ctx :site-key site-key
                   ::ui/recaptcha site-key)]
    (ui/page
     ctx
     (headers/pages ctx)
     (p.password/forgot-password ctx))))

(defn forgot-password-sent-page [ctx]
  (ui/page
   ctx
   (headers/pages ctx)
   (p.password/sent ctx)))

(defn forgot-password-action [ctx]
  (if (not (biff-auth/passed-recaptcha? ctx))
    {:status 303
     :headers {"location" "/forgot-password?error=recaptcha"}}
    (let [email (biff/normalize-email (get-in ctx [:params :email]))
          user  (data/get-user-by-email ctx email)]
      ;; Always redirect to the "check your email" page so we don't reveal
      ;; whether an account exists for the given address.
      (when (and email user)
        (let [token (str (java.util.UUID/randomUUID))]
          (data/set-password-reset-token ctx (:xt/id user) token)
          (email/send-email ctx {:template :password-reset
                                 :to (:user/email user)
                                 :url (reset-link ctx (:user/email user) token)})))
      {:status 303
       :headers {"location" "/forgot-password-sent"}})))

(defn reset-password-page [{:keys [recaptcha/site-key] :as ctx}]
  (let [valid? (:valid? (validate-reset-token ctx))]
    (if valid?
      (let [ctx (assoc ctx :site-key site-key
                       ::ui/recaptcha site-key)]
        (ui/page
         ctx
         (headers/pages ctx)
         (p.password/reset-password ctx)))
      (ui/page
       ctx
       (headers/pages ctx)
       (p.password/invalid-link ctx)))))

(defn reset-password-action [{:keys [params session] :as ctx}]
  (let [token        (:token params)
        new-password (:password params)
        confirm      (:confirm-password params)
        result       (validate-reset-token ctx)]
    (cond
      (not (biff-auth/passed-recaptcha? ctx))
      {:status 303
       :headers {"location" (str "/reset-password?error=recaptcha&token=" token)}}

      (not (:valid? result))
      {:status 303
       :headers {"location" "/reset-password?error=invalid-link"}}

      (str/blank? new-password)
      {:status 303
       :headers {"location" (str "/reset-password?error=password-blank&token=" token)}}

      (not= new-password confirm)
      {:status 303
       :headers {"location" (str "/reset-password?error=password-mismatch&token=" token)}}

      :else
      (let [user (:user result)]
        (data/reset-password ctx (:xt/id user) new-password)
        {:status 303
         :headers {"location" "/app?success=signin"}
         :session (assoc session :uid (:xt/id user))}))))

(def module
  {:routes [["/forgot-password"     {:get forgot-password-page}]
            ["/forgot-password"     {:post forgot-password-action}]
            ["/forgot-password-sent" {:get forgot-password-sent-page}]
            ["/reset-password"      {:get reset-password-page}]
            ["/reset-password"      {:post reset-password-action}]]})
