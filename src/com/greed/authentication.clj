(ns com.greed.authentication
  (:require [buddy.hashers :as hashers]
            [clojure.string :as str]
            [clojure.tools.logging :as logger]
            [com.greed.data.core :as data]))

(defn existing-user? [users user]
  (some? (first (filter #(= (:user/email %) (:email user)) users))))

(defn signup!? [{:keys [params] :as ctx}]
  (let [users (data/get-users ctx)
        valid? (not (existing-user? users params))]
    (when valid?
      (data/upsert-user ctx))
    valid?))

(defn hashed-password? [db-password]
  (str/starts-with? db-password "bcrypt"))

(defn validate-password? [param-password db-password]
  (let [valid-password? (if (hashed-password? db-password)
                          (hashers/check param-password db-password)
                          (= param-password db-password))]
    {:valid? valid-password?
     :message (if valid-password?
                "Password is valid"
                "Password is invalid")}))

(defn signin? [{:keys [params] :as ctx}]
  (let [user-id (data/get-user-id ctx)
        user (data/get-user ctx user-id)
        result (cond
                 (nil? user)
                 {:valid? false :error :invalid-credentials}

                 (not (data/user-active? user))
                 {:valid? false
                  :error :account-deactivated
                  :message (str "This account has been deactivated. "
                                "Please contact support to get reactivated.")}

                 :else
                 (let [{:keys [valid? message]} (validate-password? (:password params)
                                                                    (:user/password user))]
                   (if valid?
                     {:valid? true :message message}
                     {:valid? false :error :invalid-credentials :message message})))]
    (logger/info (str "Signin for " (:email params) ": " (or (:error result) :ok)))
    result))

(defn authenticate! [{:keys [uri] :as ctx}]
  (if (= "/authenticate/signup" uri)
    (if (signup!? ctx)
      {:valid? true}
      {:valid? false :error :invalid-email})
    (signin? ctx)))
