(ns com.greed.ui
  (:require [rum.core :as rum]
            [com.biffweb :as biff]
            [clojure.java.io :as io]
            [ring.util.response :as ring-response]
            [com.greed.settings :as settings]
            [com.greed.ui.components.headers :as headers]))

(defn static-path [path]
  (if-some [last-modified (some-> (io/resource (str "public" path))
                                  ring-response/resource-data
                                  :last-modified
                                  (.getTime))]
    (str path "?t=" last-modified)
    path))

(defn base [{:keys [::recaptcha] :as ctx} & body]
  (apply
   biff/base-html
   (-> ctx
       (merge #:base{:title settings/app-name
                     :lang "en-US"
                     :icon "/img/g.png"
                     :description (str settings/app-name " Description")
                     :image "/img/g.png"})
       (update :base/head (fn [head]
                            (concat [[:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
                                     [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin ""}]
                                     [:link {:href "https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" :rel "stylesheet"}]
                                     [:link {:rel "stylesheet" :href (static-path "/css/main.css")}]
                                     [:script {:src (static-path "/js/main.js")}]
                                     [:script {:src "https://unpkg.com/htmx.org@2.0.10/dist/htmx.min.js"}]
                                     [:script {:src "https://unpkg.com/htmx-ext-ws@2.0.2/dist/ws.js"}]
                                     [:script {:src "https://unpkg.com/hyperscript.org@0.9.91"}]
                                     [:script {:src "https://cdn.jsdelivr.net/npm/alpinejs@3.15.12/dist/cdn.min.js" :defer "defer"}]
                                     [:script {:src "https://cdn.jsdelivr.net/npm/chart.js@4.5.1/dist/chart.umd.min.js"}]
                                     (when recaptcha
                                       [:script {:src "https://www.google.com/recaptcha/api.js"
                                                 :async "async" :defer "defer"}])]
                                    head))))
   body))

(defn page [ctx & body]
  (base ctx [:.pattern.min-h-screen body]))

(defn app [ctx & body]
  (base
   ctx
   [:div {:class "flex min-h-screen bg-zinc-50"}
    (headers/app ctx)
    [:main {:class "flex-1 pt-14 md:pt-0 md:ml-64 min-h-screen"}
     [:div {:class "p-6"}
      body]]]))

(defn on-error [{:keys [status] :as ctx}]
  {:status status
   :headers {"content-type" "text/html"}
   :body (rum/render-static-markup
          (page ctx [:div {:class "flex items-center justify-center min-h-screen"}
                     [:div {:class "text-center p-8"}
                      [:h1 {:class "text-2xl font-semibold text-zinc-900 mb-2"}
                       (if (= status 404) "Page not found" "Something went wrong")]
                      [:p {:class "text-zinc-500 mb-4"}
                       (if (= status 404) "The page you are looking for does not exist." "An unexpected error occurred.")]
                      [:a {:href "/" :class "text-blue-600 hover:underline"} "Go home"]]]))})

