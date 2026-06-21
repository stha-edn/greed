(ns com.greed.ui.tools.core
  (:require [com.greed.ui.components.svgs :as svgs]))


(defn modal-tool [& {:keys [title description]
               :or   {title "Tool Title"
                      description "Lorem ipsum dolor sit amet consectetur, adipisicing elit. Nostrum quam voluptatibus"}}]
  [:div
   {:class "p-8 space-y-3 border-2 border-gray-400 rounded-xl"}
   [:span
    {:class "inline-block text-orange-500"}
    (svgs/flame)]
   [:h1
    {:class "text-xl font-semibold text-gray-700 capitalize"}
    title]
   [:p
    {:class "text-gray-500"}
    description]
   [:button
    {"@click" "isOpen = true"
     :class "inline-flex p-2 text-gray-500 capitalize transition-colors duration-300 transform bg-gray-100 rounded-full hover:underline hover:text-orange-500"}
    (svgs/arrow->)]])

(defn link-tool [& {:keys [title description link]
               :or   {title "Tool Title"
                      description "Lorem ipsum dolor sit amet consectetur, adipisicing elit. Nostrum quam voluptatibus"}}]
  [:div
   {:class "p-8 space-y-3 border-2 border-gray-400 rounded-xl"}
   [:span
    {:class "inline-block text-orange-500"}
    (svgs/flame)]
   [:h1
    {:class "text-xl font-semibold text-gray-700 capitalize"}
    title]
   [:p
    {:class "text-gray-500"}
    description]
   [:a
    {:href link
     :class "inline-flex p-2 text-gray-500 capitalize transition-colors duration-300 transform bg-gray-100 rounded-full hover:underline hover:text-orange-500"}
    (svgs/arrow->)]])

(defn tools []
  [:section
   [:div
    {:class "container px-6 py-10 mx-auto"}
    [:h1
     {:class "text-2xl font-semibold text-gray-800 capitalize lg:text-3xl"}
     "Finance "
     [:span {:class "underline decoration-blue-500"} "Tools"]]
    [:p
     {:class "mt-4 text-gray-500 xl:mt-6"}
     "Make informed decisions about your finances with our free tools."]
    [:div
     {:class "grid grid-cols-1 gap-8 mt-8 xl:mt-12 xl:gap-12 md:grid-cols-2 xl:grid-cols-3"}
     (link-tool
      :title "Income tax calculator"
      :description "Calculate your income tax in seconds"
      :link "/app/tools/income-tax-calculator")
     (link-tool
      :title "Tax returns"
      :description "File and manage your tax returns"
      :link "/app/tools/tax-returns")]]])
