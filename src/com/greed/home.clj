(ns com.greed.home
  (:require [com.biffweb :as biff]
   [com.greed.middleware :as mid]
   [com.greed.ui :as ui]
   [com.greed.ui.pages.about :as p.about]
   [com.greed.ui.pages.home :as p.home]
   [com.greed.ui.pages.team :as p.team]
   [com.greed.ui.pages.signin :as p.signin]
   [com.greed.ui.pages.signup :as p.signup]
   [com.greed.ui.components.headers :as headers]))

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

(defn signin-page [{:keys [recaptcha/site-key] :as ctx}]
  (let [ctx (assoc ctx :site-key site-key
                      ::ui/recaptcha site-key)]
    (ui/page
      ctx
      (headers/pages ctx)
      (p.signin/form ctx))))

(defn signin-success-page [{:keys [biff/db session params]}]
  (let [user-id (biff/lookup-id db :user/email (:email params))]
    {:status 303
     :headers {"Location" "/app"}
     :session (assoc session :uid user-id)}))

(defn signup-page [{:keys [recaptcha/site-key] :as ctx}]
  (let [ctx (assoc ctx :site-key site-key
                      ::ui/recaptcha site-key)]
    (ui/page
      ctx
      (headers/pages ctx)
      (p.signup/form ctx))))

(defn signup-success-page [{:keys [biff/db session params]}]
  (let [user-id (biff/lookup-id db :user/email (:email params))]
    {:status 303
     :headers {"Location" "/app"}
     :session (assoc session :uid user-id)}))

(def module
  {:routes [["/"                  {:get home-page}]
            ["/about"             {:get about-page}]
            ["/team"              {:get team-page}]
            ["/signin"             {:get signin-page}]
            ["/signup"             {:get signup-page}]
            ["/authenticate" {:middleware [mid/wrap-authenticate]}
             ["/signin" {:post signin-success-page}]
             ["/signup" {:post signup-success-page}]]
            ["/logout" {:get mid/logout}]]})
