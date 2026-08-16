(ns com.greed.ui.pages.password
  (:require [com.greed.ui.components.forms :as forms]))

(defn forgot-password [ctx]
  [:div {:class "flex items-center justify-center min-h-[80vh] px-4"}
   (forms/forgot-password ctx)])

(defn reset-password [ctx]
  [:div {:class "flex items-center justify-center min-h-[80vh] px-4 py-8"}
   (forms/reset-password ctx)])

(defn sent [ctx]
  [:div {:class "flex items-center justify-center min-h-[80vh] px-4"}
   (forms/password-reset-sent ctx)])

(defn invalid-link [ctx]
  [:div {:class "flex items-center justify-center min-h-[80vh] px-4"}
   (forms/invalid-link ctx)])
