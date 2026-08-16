(ns com.greed.email
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [com.greed.settings :as settings]
            [clojure.tools.logging :as log]
            [rum.core :as rum]))

(defn signin-link [{:keys [to url user-exists]}]
  (let [[subject action] (if user-exists
                           [(str "Sign in to " settings/app-name) "sign in"]
                           [(str "Sign up for " settings/app-name) "sign up"])]
    {:to [{:email to}]
     :subject subject
     :html (rum/render-static-markup
            [:html
             [:body
              [:p "We received a request to " action " to " settings/app-name
               " using this email address. Click this link to " action ":"]
              [:p [:a {:href url :target "_blank"} "Click here to " action "."]]
              [:p "This link will expire in one hour. "
               "If you did not request this link, you can ignore this email."]]])
     :text (str "We received a request to " action " to " settings/app-name
                " using this email address. Click this link to " action ":\n"
                "\n"
                url "\n"
                "\n"
                "This link will expire in one hour. If you did not request this link, "
                "you can ignore this email.")}))

(defn signin-code [{:keys [to code user-exists]}]
  (let [[subject action] (if user-exists
                           [(str "Sign in to " settings/app-name) "sign in"]
                           [(str "Sign up for " settings/app-name) "sign up"])]
    {:to [{:email to}]
     :subject subject
     :html (rum/render-static-markup
            [:html
             [:body
              [:p "We received a request to " action " to " settings/app-name
               " using this email address. Enter the following code to " action ":"]
              [:p {:style {:font-size "2rem"}} code]
              [:p
               "This code will expire in three minutes. "
               "If you did not request this code, you can ignore this email."]]])
     :text (str "We received a request to " action " to " settings/app-name
                " using this email address. Enter the following code to " action ":\n"
                "\n"
                code "\n"
                "\n"
                "This code will expire in three minutes. If you did not request this code, "
                "you can ignore this email.")}))

(defn welcome [{:keys [to firstname app-url]}]
  {:to [{:email to}]
   :subject (str "Welcome to " settings/app-name)
   :html (rum/render-static-markup
          [:html
           [:body
            [:p "Hi " firstname ","]
            [:p "Welcome to " settings/app-name ". Your money is about to make a lot more sense."]
            [:p "Here's what you can do next:"]
            [:ul
             [:li "Tell us about your salary so we can show your real take-home pay and tax."]
             [:li "Set up your budget items so every rand has a job."]
             [:li "Add the goals you're building toward."]]
            [:p [:a {:href app-url :target "_blank"} "Build your plan now."]]
            [:p "If you have any questions, just reply to this email or reach us at "
             "support@mygreed.co.za."]
            [:p "— The " settings/app-name " team"]]])
   :text (str "Hi " firstname ",\n\n"
              "Welcome to " settings/app-name ". Your money is about to make a lot more sense.\n\n"
              "Here's what you can do next:\n"
              "- Tell us about your salary so we can show your real take-home pay and tax.\n"
              "- Set up your budget items so every rand has a job.\n"
              "- Add the goals you're building toward.\n\n"
              "Build your plan now: " app-url "\n\n"
              "If you have any questions, just reply to this email or reach us at "
              "support@mygreed.co.za.\n\n"
              "— The " settings/app-name " team")})

(defn password-reset [{:keys [to url]}]
  {:to [{:email to}]
   :subject (str "Reset your " settings/app-name " password")
   :html (rum/render-static-markup
          [:html
           [:body
            [:p "We received a request to reset the password for your " settings/app-name
             " account."]
            [:p [:a {:href url :target "_blank"} "Click here to reset your password."]]
            [:p "This link will expire in one hour. If you did not request this, "
             "you can ignore this email and your password will stay the same."]]])
   :text (str "We received a request to reset the password for your " settings/app-name
              " account.\n"
              "\n"
              "Click here to reset your password:\n"
              url "\n"
              "\n"
              "This link will expire in one hour. If you did not request this, "
              "you can ignore this email and your password will stay the same.")})

(defn template [k opts]
  ((case k
     :signin-link signin-link
     :signin-code signin-code
     :welcome welcome
     :password-reset password-reset)
   opts))

(defn send-mailersend [{:keys [biff/secret
                               mailersend/from
                               mailersend/from-name
                               mailersend/reply-to] :as ctx}
                       form-params]
  (let [api-key    (secret :mailersend/api-key)
        recipients (mapv (fn [r] {:email (if (string? r) r (:email r))})
                         (if (string? (:to form-params))
                           [(:to form-params)]
                           (:to form-params)))
        body (cond-> {:from {:email from :name from-name}
                      :to recipients
                      :subject (:subject form-params)}
               (:text form-params) (assoc :text (:text form-params))
               (:html form-params) (assoc :html (:html form-params))
               (some? reply-to) (assoc :reply_to {:email reply-to}))]
    (try
      (let [res (http/post "https://api.mailersend.com/v1/email"
                           {:headers {"Authorization" (str "Bearer " api-key)}
                            :content-type :json
                            :accept :json
                            :body (json/generate-string body)
                            :socket-timeout 20000
                            :connection-timeout 15000})]
        (log/info "Email sent via MailerSend" {:to (:to form-params)
                                               :subject (:subject form-params)
                                               :status (:status res)})
        true)
      (catch Exception e
        (log/error e "Failed to send email via MailerSend")
        false))))

(defn send-console [_ctx form-params]
  (println "TO:" (:to form-params))
  (println "SUBJECT:" (:subject form-params))
  (println)
  (println (:text form-params))
  (println)
  (println "To send real emails instead of printing them, create an API token at"
           "https://app.mailersend.com/integrations/api and add it as MAILERSEND_API_KEY"
           "in config.env.")
  true)

(defn send-email [{:keys [biff/secret] :as ctx} opts]
  (let [form-params (if-some [template-key (:template opts)]
                      (template template-key opts)
                      opts)]
    (if (some? (when secret (secret :mailersend/api-key)))
      (send-mailersend ctx form-params)
      (send-console ctx form-params))))
