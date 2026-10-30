(ns com.greed.ui.pages.signin
  (:require [com.greed.ui.components.forms :as forms]))

(defn form [ctx]
  [:div {:class "flex items-center justify-center min-h-[80svh] px-4"}
   (forms/sign-in ctx)])
