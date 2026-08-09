(ns com.greed.ui.components.svgs)


(defn hamburger []
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
     :d "M3.75 9h16.5m-16.5 6.75h16.5"}]])

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

(defn search []
  [:svg
   {:class "w-5 h-5 text-zinc-400",
    :viewBox "0 0 24 24",
    :fill "none"}
   [:path
    {:d "M21 21L15 15M17 10C17 13.866 13.866 17 10 17C6.13401 17 3 13.866 3 10C3 6.13401 6.13401 3 10 3C13.866 3 17 6.13401 17 10Z",
     :stroke "currentColor",
     :stroke-width "2",
     :stroke-linecap "round",
     :stroke-linejoin "round"}]])

(defn dashboard []
  [:svg
   {:class "w-5 h-5",
    :viewBox "0 0 24 24",
    :fill "none",
    :xmlns "http://www.w3.org/2000/svg"}
   [:path
    {:d
     "M19 11H5M19 11C20.1046 11 21 11.8954 21 13V19C21 20.1046 20.1046 21 19 21H5C3.89543 21 3 20.1046 3 19V13C3 11.8954 3.89543 11 5 11M19 11V9C19 7.89543 18.1046 7 17 7M5 11V9C5 7.89543 5.89543 7 7 7M7 7V5C7 3.89543 7.89543 3 9 3H15C16.1046 3 17 3.89543 17 5V7M7 7H17",
     :stroke "currentColor",
     :stroke-width "2",
     :stroke-linecap "round",
     :stroke-linejoin "round"}]])

(defn settings []
  [:svg
   {:class "w-5 h-5",
    :viewBox "0 0 24 24",
    :fill "none",
    :xmlns "http://www.w3.org/2000/svg"}
   [:path
    {:d
     "M10.3246 4.31731C10.751 2.5609 13.249 2.5609 13.6754 4.31731C13.9508 5.45193 15.2507 5.99038 16.2478 5.38285C17.7913 4.44239 19.5576 6.2087 18.6172 7.75218C18.0096 8.74925 18.5481 10.0492 19.6827 10.3246C21.4391 10.751 21.4391 13.249 19.6827 13.6754C18.5481 13.9508 18.0096 15.2507 18.6172 16.2478C19.5576 17.7913 17.7913 19.5576 16.2478 18.6172C15.2507 18.0096 13.9508 18.5481 13.6754 19.6827C13.249 21.4391 10.751 21.4391 10.3246 19.6827C10.0492 18.5481 8.74926 18.0096 7.75219 18.6172C6.2087 19.5576 4.44239 17.7913 5.38285 16.2478C5.99038 15.2507 5.45193 13.9508 4.31731 13.6754C2.5609 13.249 2.5609 10.751 4.31731 10.3246C5.45193 10.0492 5.99037 8.74926 5.38285 7.75218C4.44239 6.2087 6.2087 4.44239 7.75219 5.38285C8.74926 5.99037 10.0492 5.45193 10.3246 4.31731Z",
     :stroke "currentColor",
     :stroke-width "2",
     :stroke-linecap "round",
     :stroke-linejoin "round"}]
   [:path
    {:d
     "M15 12C15 13.6569 13.6569 15 12 15C10.3431 15 9 13.6569 9 12C9 10.3431 10.3431 9 12 9C13.6569 9 15 10.3431 15 12Z",
     :stroke "currentColor",
     :stroke-width "2",
     :stroke-linecap "round",
     :stroke-linejoin "round"}]])

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

(defn info []
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
     :d "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"}]
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M12 16.5v-4"}]
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M12 8.25h.008v.008H12V8.25Z"}]])

(defn close []
  [:svg
   {:class "w-5 h-5",
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

(defn uptrend []
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
     :d "M2.25 18 9 11.25l4.306 4.307a11.95 11.95 0 0 1 5.814-5.519l2.74-1.22m0 0-5.94-2.281m5.94 2.28-2.28 5.941"}]])

(defn downtrend []
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
     :d "M2.25 6 9 12.75l4.286-4.286a11.948 11.948 0 0 1 4.306 6.43l.776 2.898m0 0 3.182-5.511m-3.182 5.51-5.511-3.181"}]])

(defn stable []
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
     :d "M5 12h14"}]])

(defn home []
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
     "m2.25 12 8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75"}]])

(defn ->next []
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
     :d "m8.25 4.5 7.5 7.5-7.5 7.5"}]])

(defn tools []
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
     "M11.42 15.17 17.25 21A2.652 2.652 0 0 0 21 17.25l-5.877-5.877M11.42 15.17l2.496-3.03c.317-.384.74-.626 1.208-.766M11.42 15.17l-4.655 5.653a2.548 2.548 0 1 1-3.586-3.586l6.837-5.63m5.108-.233c.55-.164 1.163-.188 1.743-.14a4.5 4.5 0 0 0 4.486-6.336l-3.276 3.277a3.004 3.004 0 0 1-2.25-2.25l3.276-3.276a4.5 4.5 0 0 0-6.336 4.486c.091 1.076-.071 2.264-.904 2.95l-.102.085m-1.745 1.437L5.909 7.5H4.5L2.25 3.75l1.5-1.5L7.5 4.5v1.409l4.26 4.26m-1.745 1.437 1.745-1.437m6.615 8.206L15.75 15.75M4.867 19.125h.008v.008h-.008v-.008Z"}]])

(defn flame []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.8",
    :stroke "currentColor",
    :class "size-8"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d
     "M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"}]])

(defn suit-case []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :aria-label "suitcase icon",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "1.8",
    :stroke "currentColor",
    :class "size-6"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M16 20V4a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"}]
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M2 8a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2Z"}]])

(defn dollar []
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
     "M12 6v12m-3-2.818.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"}]])

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

(defn arrow-> []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :class "w-6 h-6",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke "currentColor"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :stroke-width "2",
     :d
     "M13 9l3 3m0 0l-3 3m3-3H8m13 0a9 9 0 11-18 0 9 9 0 0118 0z"}]])

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

(defn x []
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
     :d "m9.75 9.75 4.5 4.5m0-4.5-4.5 4.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"}]])

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

(defn sort! []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke-width "2",
    :stroke "currentColor",
    :aria-hidden "true",
    :class "h-4 w-4"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M8.25 15L12 18.75 15.75 15m-7.5-6L12 5.25 15.75 9"}]])

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

(defn add []
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
     :d "M12 9v6m3-3H9m12 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"}]])
