(ns com.greed.ui.components.svgs)


(defn success []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.8",
    :stroke "currentColor",
    :class "size-6"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"}]])

(defn squares-2x2 []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.5",
    :stroke "currentColor",
    :class "size-6"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M3.75 6A2.25 2.25 0 0 1 6 3.75h2.25A2.25 2.25 0 0 1 10.5 6v2.25a2.25 2.25 0 0 1-2.25 2.25H6a2.25 2.25 0 0 1-2.25-2.25V6ZM3.75 15.75A2.25 2.25 0 0 1 6 13.5h2.25a2.25 2.25 0 0 1 2.25 2.25V18a2.25 2.25 0 0 1-2.25 2.25H6A2.25 2.25 0 0 1 3.75 18v-2.25ZM13.5 6a2.25 2.25 0 0 1 2.25-2.25H18A2.25 2.25 0 0 1 20.25 6v2.25A2.25 2.25 0 0 1 18 10.5h-2.25a2.25 2.25 0 0 1-2.25-2.25V6ZM13.5 15.75a2.25 2.25 0 0 1 2.25-2.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-2.25A2.25 2.25 0 0 1 13.5 18v-2.25Z"}]])

(defn logout []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.5",
    :stroke "currentColor",
    :class "w-5 h-5"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M15.75 9V5.25A2.25 2.25 0 0 0 13.5 3h-6a2.25 2.25 0 0 0-2.25 2.25v13.5A2.25 2.25 0 0 0 7.5 21h6a2.25 2.25 0 0 0 2.25-2.25V15m3 0 3-3m0 0-3-3m3 3H9"}]])

(defn users []
  [:svg
   {:class "w-5 h-5",
    :viewBox "0 0 24 24",
    :fill "none",
    :xmlns "http://www.w3.org/2000/svg"}
   [:path
    {:d
     "M17 20V18C17 15.7909 15.2091 14 13 14H6C3.79086 14 2 15.7909 2 18V20M22 20V18C22 16.1362 20.7252 14.5722 19 14.126M15 6.87398C16.7252 7.32028 18 8.88418 18 10.75C18 12.6158 16.7252 14.1797 15 14.626M13.5 6.5C13.5 8.70914 11.7091 10.5 9.5 10.5C7.29086 10.5 5.5 8.70914 5.5 6.5C5.5 4.29086 7.29086 2.5 9.5 2.5C11.7091 2.5 13.5 4.29086 13.5 6.5Z",
     :stroke "currentColor",
     :stroke-width "2",
     :stroke-linecap "round",
     :stroke-linejoin "round"}]])

(defn card-chip []
  [:svg
   {:viewBox "0 0 24 18",
    :class "w-7 h-5 fill-current"}
   [:rect {:x "0.5" :y "0.5" :width "23" :height "17" :rx "3" :stroke "currentColor" :stroke-width "1" :fill "none" :fill-opacity "0.15"}]
   [:rect {:x "4" :y "4" :width "16" :height "10" :rx "1.5" :fill "currentColor" :fill-opacity "0.35"}]
   [:line {:x1 "9" :y1 "4" :x2 "9" :y2 "14" :stroke "currentColor" :stroke-width "1"}]
   [:line {:x1 "15" :y1 "4" :x2 "15" :y2 "14" :stroke "currentColor" :stroke-width "1"}]
   [:line {:x1 "4" :y1 "9" :x2 "20" :y2 "9" :stroke "currentColor" :stroke-width "1"}]])

(defn contactless []
  [:svg
   {:viewBox "0 0 24 24",
    :fill "none",
    :stroke "currentColor",
    :stroke-width "1.75",
    :stroke-linecap "round",
    :class "w-5 h-5"}
   [:path {:d "M8 16.5a6 6 0 0 1 0-9"}]
   [:path {:d "M12 19a10 10 0 0 1 0-14"}]
   [:path {:d "M16 21a14 14 0 0 1 0-18"}]])

(defn close [& [{:keys [class]}]]
  [:svg
   {:class (or class "w-5 h-5"),
    :viewBox "0 0 24 24",
    :fill "none",
    :xmlns "http://www.w3.org/2000/svg"}
   [:path
    {:d "M6 18L18 6M6 6L18 18",
     :stroke "currentColor",
     :stroke-width "2",
     :stroke-linecap "round",
     :stroke-linejoin "round"}]])

(defn check []
  [:svg
   {:class "w-6 h-6",
    :viewBox "0 0 24 24",
    :fill "none",
    :xmlns "http://www.w3.org/2000/svg"}
   [:path
    {:d "M5 13l4 4L19 7",
     :stroke "currentColor",
     :stroke-width "2",
     :stroke-linecap "round",
     :stroke-linejoin "round"}]])

(defn cog []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.5",
    :stroke "currentColor",
    :class "size-6"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M4.5 12a7.5 7.5 0 0 0 15 0m-15 0a7.5 7.5 0 1 1 15 0m-15 0H3m16.5 0H21m-1.5 0H12m-8.457 3.077 1.41-.513m14.095-5.13 1.41-.513M5.106 17.785l1.15-.964m11.49-9.642 1.149-.964M7.501 19.795l.75-1.3m7.5-12.99.75-1.3m-6.063 16.658.26-1.477m2.605-14.772.26-1.477m0 17.726-.26-1.477M10.698 4.614l-.26-1.477M16.5 19.794l-.75-1.299M7.5 4.205 12 12m6.894 5.785-1.149-.964M6.256 7.178l-1.15-.964m15.352 8.864-1.41-.513M4.954 9.435l-1.41-.514M12.002 12l-3.75 6.495"}]])

(defn calendar []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.5",
    :stroke "currentColor",
    :class "size-6"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d
     "M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5"}]])

(defn home [{:keys [class]}]
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.8",
    :stroke "currentColor",
    :class (or class "size-5")}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d
     "m2.25 12 8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75"}]])

(defn ->next [{:keys [class]}]
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.8",
    :stroke "currentColor",
    :class (or class "size-5")}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "m8.25 4.5 7.5 7.5-7.5 7.5"}]])

(defn percent-badge []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.5",
    :stroke "currentColor",
    :class "size-6"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d
     "m8.99 14.993 6-6m6 3.001c0 1.268-.63 2.39-1.593 3.069a3.746 3.746 0 0 1-1.043 3.296 3.745 3.745 0 0 1-3.296 1.043 3.745 3.745 0 0 1-3.068 1.593c-1.268 0-2.39-.63-3.068-1.593a3.745 3.745 0 0 1-3.296-1.043 3.746 3.746 0 0 1-1.043-3.297 3.746 3.746 0 0 1-1.593-3.068c0-1.268.63-2.39 1.593-3.068a3.746 3.746 0 0 1 1.043-3.297 3.745 3.745 0 0 1 3.296-1.042 3.745 3.745 0 0 1 3.068-1.594c1.268 0 2.39.63 3.068 1.593a3.745 3.745 0 0 1 3.296 1.043 3.746 3.746 0 0 1 1.043 3.297 3.746 3.746 0 0 1 1.593 3.068ZM9.74 9.743h.008v.007H9.74v-.007Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm4.125 4.5h.008v.008h-.008v-.008Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z"}]])

(defn document-text []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.5",
    :stroke "currentColor",
    :class "size-6"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z"}]])

(defn gift []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.5",
    :stroke "currentColor",
    :class "size-6"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M21 11.25v8.25a1.5 1.5 0 0 1-1.5 1.5H5.25a1.5 1.5 0 0 1-1.5-1.5v-8.25M12 4.875A2.625 2.625 0 1 0 9.375 7.5H12m0-2.625V7.5m0-2.625A2.625 2.625 0 1 1 14.625 7.5H12m0 0V21m-8.625-9.75h18c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125h-18c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125Z"}]])

(defn credit-card []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.5",
    :stroke "currentColor",
    :class "size-6"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d
     "M2.25 8.25h19.5M2.25 9h19.5m-16.5 5.25h6m-6 2.25h3m-3.75 3h15a2.25 2.25 0 0 0 2.25-2.25V6.75A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25v10.5A2.25 2.25 0 0 0 4.5 19.5Z"}]])

(defn banknotes []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.5",
    :stroke "currentColor",
    :class "size-6"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d
     "M12 21V12.75M15.75 21V12.75M8.25 21V12.75M3 9L12 3L21 9M19.5 21V10.3325C17.0563 9.94906 14.5514 9.75 12 9.75C9.44861 9.75 6.94372 9.94906 4.5 10.3325V21M3 21H21M12 6.75H12.0075V6.7575H12V6.75Z"}]])

(defn target []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.8",
    :stroke "currentColor",
    :class "size-5"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d
     "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Zm0-4.5a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9Zm0-3a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z"}]])

(defn chart-bar []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.8",
    :stroke "currentColor",
    :class "size-5"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d
     "M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z"}]])

(defn trending-up []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.8",
    :stroke "currentColor",
    :class "size-5"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d
     "M2.25 18 9 11.25l4.306 4.307a11.95 11.95 0 0 1 5.814-5.519l2.74-1.22m0 0-5.94-2.281m5.94 2.28-2.28 5.941"}]])

(defn trending-down []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.8",
    :stroke "currentColor",
    :class "size-5"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d
     "M2.25 6 9 12.75l4.286-4.286a11.948 11.948 0 0 1 4.306 6.43l.776 2.898m0 0 3.182-5.511m-3.182 5.51-5.511-3.181"}]])

(defn wallet []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.8",
    :stroke "currentColor",
    :class "size-5"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d
     "M21 12a2.25 2.25 0 0 0-2.25-2.25H15a3 3 0 1 1-6 0H5.25A2.25 2.25 0 0 0 3 12m18 0v6a2.25 2.25 0 0 1-2.25 2.25H5.25A2.25 2.25 0 0 1 3 18v-6m18 0V9M3 12V9m18 0a2.25 2.25 0 0 0-2.25-2.25H5.25A2.25 2.25 0 0 0 3 9m18 0V6a2.25 2.25 0 0 0-2.25-2.25H5.25A2.25 2.25 0 0 0 3 6v3"}]])

(defn money []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :class "size-8",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke "currentColor",
    :stroke-width "1.5"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d
     "M12 6V18M9 15.1818L9.87887 15.841C11.0504 16.7197 12.9498 16.7197 14.1214 15.841C15.2929 14.9623 15.2929 13.5377 14.1214 12.659C13.5355 12.2196 12.7677 12 11.9999 12C11.275 12 10.5502 11.7804 9.99709 11.341C8.891 10.4623 8.891 9.03772 9.9971 8.15904C11.1032 7.28036 12.8965 7.28036 14.0026 8.15904L14.4175 8.48863M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z"}]])

(defn heart []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.5",
    :stroke "currentColor",
    :class "size-6"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z"}]])

(defn umbrella []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.8",
    :stroke "currentColor",
    :class "size-6"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M23 12a11.05 11.05 0 0 0-22 0zm-5 7a3 3 0 0 1-6 0v-7"}]])

(defn action []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.8",
    :stroke "currentColor",
    :aria-hidden "true",
    :class "size-4"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d
     "M16.862 4.487l1.687-1.688a1.875 1.875 0 1 1 2.652 2.652L6.832 19.82a4.5 4.5 0 0 1-1.897 1.13l-2.685.8.8-2.685a4.5 4.5 0 0 1 1.13-1.897L16.862 4.487zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0 1 15.75 21H5.25A2.25 2.25 0 0 1 3 18.75V8.25A2.25 2.25 0 0 1 5.25 6H10"}]])

(defn plus [{:keys [class]}]
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "2",
    :stroke "currentColor",
    :class class}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :stroke-width "2",
     :d "M12 4v16m8-8H4"}]])

(defn shield-check []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.5",
    :stroke "currentColor",
    :class "size-6"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M9 12.75 11.25 15 15 9.75m-3-7.036A11.959 11.959 0 0 1 3.598 6 11.99 11.99 0 0 0 3 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285Z"}]])

(defn envelope []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.5",
    :stroke "currentColor",
    :class "size-6"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M21.75 6.75v10.5a2.25 2.25 0 0 1-2.25 2.25h-15a2.25 2.25 0 0 1-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25m19.5 0v.243a2.25 2.25 0 0 1-1.07 1.916l-7.5 4.615a2.25 2.25 0 0 1-2.36 0L3.32 8.91a2.25 2.25 0 0 1-1.07-1.916V6.75"}]])

(defn instagram []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :viewBox "0 0 512 512",
    :class "size-6"}
   [:path
    {:fill "currentColor"
     :d "M349.33,69.33a93.62,93.62,0,0,1,93.34,93.34V349.33a93.62,93.62,0,0,1-93.34,93.34H162.67a93.62,93.62,0,0,1-93.34-93.34V162.67a93.62,93.62,0,0,1,93.34-93.34H349.33m0-37.33H162.67C90.8,32,32,90.8,32,162.67V349.33C32,421.2,90.8,480,162.67,480H349.33C421.2,480,480,421.2,480,349.33V162.67C480,90.8,421.2,32,349.33,32Z M377.33,162.67a28,28,0,1,1,28-28A27.94,27.94,0,0,1,377.33,162.67Z M256,181.33A74.67,74.67,0,1,1,181.33,256,74.75,74.75,0,0,1,256,181.33M256,144A112,112,0,1,0,368,256,112,112,0,0,0,256,144Z"}]])
