(ns com.greed.ui.components.alerts
  (:require [com.core :as c]
            [com.greed.ui.components.svgs :as svgs]))


(defn success [& {:keys [type]
                  :or {type :signin}}]
  [:div
   {:class "flex w-full max-w-sm overflow-hidden bg-white border-2 border-black rounded-lg shadow-md"}
   [:div
    {:class "flex items-center justify-center w-12 bg-emerald-800"}
    (svgs/success)]
   [:div
    {:class "px-4 py-2 -mx-3"}
    [:div
     {:class "mx-3"}
     [:span
      {:class "font-semibold text-2xl text-emerald-800"}
      "Success"]
     [:p
      {:class "text-md text-gray-600"}
      (if (= type :signin)
        "You are signed in!"
        "Your account was created!")]]]])


(defn info [& {:keys [alert]
               :or {alert nil}}]
  (let [config c/alert-config
        key (keyword "alert" alert)
        message (get config key)]
    [:div
     {:class "mx-auto w-full text-white pattern bg-blue-600 rounded-full shadow-xl"}
     [:div
      {:class "container flex justify-between px-6 py-4 mx-auto"}
      [:div
       {:class "flex items-center space-x-4"}
       (svgs/info)
       [:span
        message]]
      [:a
       {:href "/"
        :class "p-1 transition-colors duration-300 transform rounded-md hover:bg-opacity-25 hover:bg-gray-600 focus:outline-none"}
       (svgs/close)]]]))


(defn salary-prompt-modal
  "Modal prompting user to update salary. Parent must have Alpine x-data with showSalaryPrompt."
  []
  [:div
   {:class "relative flex justify-center"}
   [:div
    {:role "dialog"
     :aria-labelledby "salary-prompt-title"
     :aria-modal "true"
     :class "fixed inset-0 z-10 overflow-y-auto"
     :x-show "showSalaryPrompt"
     "x-transition:enter" "transition duration-300 ease-out"
     "x-transition:enter-start" "translate-y-4 opacity-0 sm:translate-y-0 sm:scale-95"
     "x-transition:enter-end" "translate-y-0 opacity-100 sm:scale-100"
     "x-transition:leave" "transition duration-150 ease-in"
     "x-transition:leave-start" "translate-y-0 opacity-100 sm:scale-100"
     "x-transition:leave-end" "translate-y-4 opacity-0 sm:translate-y-0 sm:scale-95"}
    [:div
     {:class "flex items-end justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:block sm:p-0"}
     [:span
      {:class "hidden sm:inline-block sm:h-screen sm:align-middle"
       :aria-hidden "true"}
      "\u00A0"]
     [:div
      {:class "relative inline-block overflow-hidden text-left align-bottom transition-all transform bg-white rounded-lg shadow-xl sm:my-8 sm:align-middle sm:max-w-lg sm:w-full"}
      [:div
       {:class "px-6 py-5 sm:p-6"}
       [:div
        {:class "flex items-start"}
        [:div
         {:class "flex items-center justify-center flex-shrink-0 w-12 h-12 rounded-full bg-amber-100"}
         (svgs/info)]
        [:div
         {:class "flex-1 mt-0 ml-4"}
         [:h3
          {:id "salary-prompt-title"
           :class "text-lg font-medium leading-6 text-gray-900"}
          "Update your salary"]
         [:p
          {:class "mt-2 text-sm text-gray-500"}
          "To get accurate budget and tax insights, please add your salary in your profile."]
         [:div
          {:class "flex justify-end gap-3 mt-6"}
          [:button
           {:type "button"
            :class "px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-amber-500"
            "@click" "showSalaryPrompt = false"}
           "Later"]
          [:a
           {:href "/app/profile"
            :class "inline-flex justify-center px-4 py-2 text-sm font-medium text-white bg-amber-600 border border-transparent rounded-md hover:bg-amber-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-amber-500"}
           "Go to profile"]]]]]]]]])
