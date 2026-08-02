(ns com.greed.email
  (:require [com.greed.settings :as settings]
            [clojure.tools.logging :as log]
            [rum.core :as rum])
  (:import [jakarta.mail Message Message$RecipientType Session]
           [jakarta.mail.internet InternetAddress MimeBodyPart MimeMessage MimeMultipart]))

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

(defn template [k opts]
  ((case k
     :signin-link signin-link
     :signin-code signin-code
     :welcome welcome)
   opts))

(defn- smtp-session [{:keys [smtp/host smtp/port]}]
  (let [props (doto (java.util.Properties.)
                (.put "mail.smtp.host" host)
                (.put "mail.smtp.port" (str port))
                (.put "mail.smtp.auth" "true")
                (.put "mail.smtp.starttls.enable" "true")
                (.put "mail.smtp.connectiontimeout" "15000")
                (.put "mail.smtp.timeout" "20000")
                (.put "mail.smtp.writetimeout" "20000"))]
    (Session/getInstance props)))

(defn send-smtp [{:keys [biff/secret smtp/from smtp/username smtp/host smtp/port] :as ctx}
                 form-params]
  (try
    (let [session (smtp-session ctx)
          message (doto (MimeMessage. session)
                    (.setFrom (InternetAddress. from settings/app-name))
                    (.setRecipients Message$RecipientType/TO
                                    (into-array InternetAddress
                                                (map (fn [r] (InternetAddress. (:email r)))
                                                     (:to form-params))))
                    (.setSubject (:subject form-params)))
          multipart (MimeMultipart.)]
      (when-some [text (:text form-params)]
        (let [part (MimeBodyPart.)]
          (.setText part text "utf-8")
          (.addBodyPart multipart part)))
      (when-some [html (:html form-params)]
        (let [part (MimeBodyPart.)]
          (.setContent part html "text/html; charset=utf-8")
          (.addBodyPart multipart part)))
      (.setContent message multipart)
      (let [transport (.getTransport session) ]
        (try
          (.connect transport host (int port) username (secret :smtp/password))
          (.sendMessage transport message (.getAllRecipients message))
          (finally
            (.close transport))))
      (log/info "Email sent via SMTP" {:to (:to form-params) :subject (:subject form-params)})
      true)
    (catch Exception e
      (log/error e "Failed to send email via SMTP")
      false)))

(defn send-console [_ctx form-params]
  (println "TO:" (:to form-params))
  (println "SUBJECT:" (:subject form-params))
  (println)
  (println (:text form-params))
  (println)
  (println "To send real emails instead of printing them, add an app password for"
           "admin@mygreed.co.za to SMTP_PASSWORD in config.env.")
  true)

(defn send-email [{:keys [biff/secret] :as ctx} opts]
  (let [form-params (if-some [template-key (:template opts)]
                      (template template-key opts)
                      opts)]
    (if (and (some? secret)
             (some? (secret :smtp/password)))
      (send-smtp ctx form-params)
      (send-console ctx form-params))))
