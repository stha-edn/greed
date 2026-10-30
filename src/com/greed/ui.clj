(ns com.greed.ui
  (:require [rum.core :as rum]
            [com.biffweb :as biff]
            [clojure.java.io :as io]
            [ring.util.response :as ring-response]
            [com.greed.settings :as settings]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.footer :as footer]
            [com.greed.ui.components.headers :as headers]))

(defn static-path [path]
  (if-some [last-modified (some-> (io/resource (str "public" path))
                                  ring-response/resource-data
                                  :last-modified
                                  (.getTime))]
    (str path "?t=" last-modified)
    path))

(defn base [{:keys [::recaptcha google-analytics/id] :as ctx} & body]
  (apply
   biff/base-html
   (-> ctx
       (merge #:base{:title settings/app-name
                     :lang "en-US"
                     :description (str settings/app-name
                                       " brings your salary, tax, spending and savings into one clear place,"
                                       " so you always know what you earn, what SARS takes, and what you keep.")
                     :image (str (or (:biff/base-url ctx) "https://mygreed.co.za") "/icon-512.png")})
       (update :base/head (fn [head]
                            (concat
                             (cond-> [[:style "/* Self-hosted Inter (variable, latin subset). Kept inline rather
than in tailwind.css so it ships with the markup and never depends on a
CSS rebuild. */"
                                       "@font-face { font-family: 'Inter'; src: url('/font/InterVariable.woff2') format('woff2');"
                                       "font-weight: 100 900; font-style: normal; font-display: swap; }"
                                       "@supports (height: 100dvh) { html .min-h-screen { min-height: 100dvh; } }"]
                                      ;; Favicons and PWA manifest (from docs/brand/icons/head-snippet.html).
                                      [:link {:rel "icon" :href "/favicon.ico" :sizes "any"}]
                                      [:link {:rel "icon" :href "/favicon.svg" :type "image/svg+xml"}]
                                      [:link {:rel "apple-touch-icon" :href "/apple-touch-icon.png"}]
                                      [:link {:rel "manifest" :href "/site.webmanifest"}]
                                      [:meta {:name "theme-color" :content "#0F6B4F"}]
                                      [:link {:rel "stylesheet" :href (static-path "/css/main.css")}]
                                      [:script {:src (static-path "/js/main.js")}]
                                      [:script {:src "https://unpkg.com/htmx.org@2.0.10/dist/htmx.min.js"}]
                                      [:script {:src "https://unpkg.com/htmx-ext-ws@2.0.2/dist/ws.js"}]
                                      [:script {:src "https://unpkg.com/hyperscript.org@0.9.91"}]]
                               id (into [[:script {:async true :src (str "https://www.googletagmanager.com/gtag/js?id=" id)}]
                                         [:script (biff/unsafe
                                                   (str "window.dataLayer = window.dataLayer || [];"
                                                        "function gtag(){dataLayer.push(arguments);}"
                                                        "gtag('js', new Date());"
                                                        "gtag('config', '" id "');"))]])
                               recaptcha (conj [:script {:src "https://www.google.com/recaptcha/api.js"
                                                         :async "async" :defer "defer"}]))
                             head))))
   body))

(defn page [ctx & body]
  (base ctx [:div {:class "min-h-screen flex flex-col"}
             [:div {:class "marketing-bg flex-1"} body]
             (footer/footer)]))

(defn app [ctx & body]
  (base
   ctx
   [:div {:class "flex min-h-screen bg-zinc-50"}
    (headers/app ctx)
    [:main {:class "flex-1 min-w-0 pt-[calc(3.5rem+env(safe-area-inset-top))] md:pt-0 md:ml-64 min-h-screen flex flex-col"}
     [:div {:class "p-6 pb-[calc(5rem+env(safe-area-inset-bottom))] md:pb-6 flex-1 min-w-0"}
      body]
     (footer/app-footer)]
    (alerts/confirm-dialog)]))

(defn on-error [{:keys [status] :as ctx}]
  {:status status
   :headers {"content-type" "text/html"}
   :body (rum/render-static-markup
          (page ctx [:div {:class "flex items-center justify-center min-h-screen"}
                     [:div {:class "text-center p-8"}
                      [:img {:src "/img/greed-symbol-green-tight.svg" :alt "greed"
                             :width 192 :height 192 :class "mx-auto mb-6 block h-12 w-12"}]
                      [:h1 {:class "text-2xl font-semibold text-zinc-900 mb-2"}
                       (if (= status 404) "Page not found" "Something went wrong")]
                      [:p {:class "text-zinc-500 mb-4"}
                       (if (= status 404) "The page you are looking for does not exist." "An unexpected error occurred.")]
                      [:a {:href "/" :class "text-brand-600 hover:underline"} "Go home"]]]))})

