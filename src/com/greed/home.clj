(ns com.greed.home
  (:require [com.biffweb :as biff]
            [xtdb.api :as xt]
            [com.greed.middleware :as mid]
            [com.greed.email :as email]
            [com.greed.authentication :as auth]
            [com.greed.data.core :as data]
            [com.greed.ui :as ui]
            [com.greed.ui.pages.about :as p.about]
            [com.greed.ui.pages.contact :as p.contact]
            [com.greed.ui.pages.home :as p.home]
            [com.greed.ui.pages.legal :as p.legal]
            [com.greed.ui.pages.team :as p.team]
            [com.greed.ui.pages.signin :as p.signin]
            [com.greed.ui.pages.signup :as p.signup]
            [com.greed.ui.components.headers :as headers]
            [com.greed.password :as password]))

(defn home-page [ctx]
  (ui/page
   ctx
   (headers/pages ctx)
   (p.home/page ctx)))

(defn team-page [ctx]
  (ui/page
   ctx
   (headers/pages ctx)
   (p.team/page)))

(defn about-page [ctx]
  (ui/page
   ctx
   (headers/pages ctx)
   (p.about/page ctx)))

(defn contact-page [ctx]
  (ui/page
   ctx
   (headers/pages ctx)
   (p.contact/page ctx)))

(defn privacy-page [ctx]
  (ui/page
   ctx
   (headers/pages ctx)
   (p.legal/privacy-page ctx)))

(defn terms-page [ctx]
  (ui/page
   ctx
   (headers/pages ctx)
   (p.legal/terms-page ctx)))

(defn signin-page [{:keys [recaptcha/site-key] :as ctx}]
  (let [ctx (assoc ctx :site-key site-key
                   ::ui/recaptcha site-key)]
    (ui/page
     ctx
     (headers/pages ctx)
     (p.signin/form ctx))))

(defn signin-success-page [{:keys [biff/db session params] :as ctx}]
  (let [user-id (biff/lookup-id db :user/email (:email params))
        ;; someone who runs a business, or has an invitation waiting, picks
        ;; Personal or a business first
        choose? (or (seq (data/get-user-businesses ctx user-id))
                    (seq (data/get-user-invites ctx user-id)))]
    {:status 303
     :headers {"Location" (if choose? "/app/choose" "/app?success=signin")}
     :session (with-meta (-> session (assoc :uid user-id) (dissoc :business-id)) {:recreate true})}))

(defn signup-page [{:keys [recaptcha/site-key] :as ctx}]
  (let [ctx (assoc ctx :site-key site-key
                   ::ui/recaptcha site-key)]
    (ui/page
     ctx
     (headers/pages ctx)
     (p.signup/form ctx))))

(defn signup-success-page [{:keys [biff.xtdb/node session params biff/base-url] :as ctx}]
  (let [user-id (biff/lookup-id (xt/db node) :user/email (:email params))]
    (email/send-email ctx {:template :welcome
                           :to (:email params)
                           :firstname (:firstname params)
                           :app-url (str base-url "/app")})
    ;; a business sign-up starts in its new business
    (let [ctx         (assoc ctx :biff/db (xt/db node))
          business-id (when (auth/business-signup? params)
                        (:xt/id (first (data/get-user-businesses ctx user-id))))
          invited?    (seq (data/get-user-invites ctx user-id))]
      {:status 303
       ;; someone who signed up after being invited sees the invitation first
       :headers {"Location" (if (and invited? (not business-id)) "/app/choose" "/app?success=signup")}
       :session (with-meta (cond-> (-> session (assoc :uid user-id) (dissoc :business-id))
                             business-id (assoc :business-id business-id))
                  {:recreate true})})))

(def module
  {:routes [["/"                  {:get home-page}]
            ["/about"             {:get about-page}]
            ["/contact"           {:get contact-page}]
            ["/team"              {:get team-page}]
            ["/privacy"           {:get privacy-page}]
            ["/terms"             {:get terms-page}]
            ["/signin"             {:get signin-page}]
            ["/signup"             {:get signup-page}]
            ["/forgot-password"      {:get password/forgot-password-page
                                      :post password/forgot-password-action}]
            ["/forgot-password-sent" {:get password/forgot-password-sent-page}]
            ["/reset-password"       {:get password/reset-password-page
                                      :post password/reset-password-action}]
            ["/authenticate" {:middleware [mid/wrap-authenticate]}
             ["/signin" {:post signin-success-page}]
             ["/signup" {:post signup-success-page}]]
            ["/logout" {:get mid/logout}]]})
