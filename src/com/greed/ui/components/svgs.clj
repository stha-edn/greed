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
   {:class "w-6 h-6 text-white fill-current",
    :viewBox "0 0 40 40",
    :xmlns "http://www.w3.org/2000/svg"}
   [:path
    {:d "M20 3.33331C10.8 3.33331 3.33337 10.8 3.33337 20C3.33337 29.2 10.8 36.6666 20 36.6666C29.2 36.6666 36.6667 29.2 36.6667 20C36.6667 10.8 29.2 3.33331 20 3.33331ZM16.6667 28.3333L8.33337 20L10.6834 17.65L16.6667 23.6166L29.3167 10.9666L31.6667 13.3333L16.6667 28.3333Z"}]])

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

(defn visa []
  [:img {:src "/img/visa.svg"}])

(defn mastercard []
  [:img {:src "/img/mastercard.svg"}])

(defn info []
  [:svg
   {:viewBox "0 0 40 40",
    :class "w-6 h-6 fill-current"}
   [:path
    {:d "M20 3.33331C10.8 3.33331 3.33337 10.8 3.33337 20C3.33337 29.2 10.8 36.6666 20 36.6666C29.2 36.6666 36.6667 29.2 36.6667 20C36.6667 10.8 29.2 3.33331 20 3.33331ZM21.6667 28.3333H18.3334V25H21.6667V28.3333ZM21.6667 21.6666H18.3334V11.6666H21.6667V21.6666Z"}]])

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
    :class "w-5 h-5 fill-current",
    :viewBox "0 0 24 24"}
   [:path
    {:class "heroicon-ui",
     :d
     "M20 15a1 1 0 002 0V7a1 1 0 00-1-1h-8a1 1 0 000 2h5.59L13 13.59l-3.3-3.3a1 1 0 00-1.4 0l-6 6a1 1 0 001.4 1.42L9 12.4l3.3 3.3a1 1 0 001.4 0L20 9.4V15z"}]])

(defn downtrend []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :class "w-5 h-5 fill-current",
    :viewBox "0 0 24 24"}
   [:path
    {:class "heroicon-ui",
     :d
     "M20 9a1 1 0 012 0v8a1 1 0 01-1 1h-8a1 1 0 010-2h5.59L13 10.41l-3.3 3.3a1 1 0 01-1.4 0l-6-6a1 1 0 011.4-1.42L9 11.6l3.3-3.3a1 1 0 011.4 0l6.3 6.3V9z"}]])

(defn stable []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :class "w-5 h-5 fill-current",
    :viewBox "0 0 24 24"}
   [:path
    {:class "heroicon-ui",
     :d "M17 11a1 1 0 010 2H7a1 1 0 010-2h10z"}]])

(defn home []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :class "w-5 h-5",
    :viewBox "0 0 20 20",
    :fill "currentColor"}
   [:path
    {:d
     "M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"}]])

(defn ->next []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :class "w-5 h-5",
    :viewBox "0 0 20 20",
    :fill "currentColor"}
   [:path
    {:fill-rule "evenodd",
     :d
     "M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z",
     :clip-rule "evenodd"}]])

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
    :class "w-8 h-8",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke "currentColor"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :stroke-width "2",
     :d
     "M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z"}]
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :stroke-width "2",
     :d
     "M9.879 16.121A3 3 0 1012.015 11L11 14H9c0 .768.293 1.536.879 2.121z"}]])

(defn suit-case []
  [:svg
   {:aria-label "suitcase icon",
    :class "w-6 h-6 fill-current",
    :viewBox "0 0 24 24",
    :fill "none",
    :xmlns "http://www.w3.org/2000/svg"}
   [:path {:d "M14 11H10V13H14V11Z"}]
   [:path
    {:fill-rule "evenodd",
     :clip-rule "evenodd",
     :d
     "M7 5V4C7 2.89545 7.89539 2 9 2H15C16.1046 2 17 2.89545 17 4V5H20C21.6569 5 23 6.34314 23 8V18C23 19.6569 21.6569 21 20 21H4C2.34314 21 1 19.6569 1 18V8C1 6.34314 2.34314 5 4 5H7ZM9 4H15V5H9V4ZM4 7C3.44775 7 3 7.44769 3 8V14H21V8C21 7.44769 20.5522 7 20 7H4ZM3 18V16H21V18C21 18.5523 20.5522 19 20 19H4C3.44775 19 3 18.5523 3 18Z"}]])

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
     "M2.25 18.75a60.07 60.07 0 0 1 15.797 2.101c.727.198 1.453-.342 1.453-1.096V18.75M3.75 4.5v.75A.75.75 0 0 1 3 6h-.75m0 0v-.375c0-.621.504-1.125 1.125-1.125H20.25M2.25 6v9m18-10.5v.75c0 .414.336.75.75.75h.75m-1.5-1.5h.375c.621 0 1.125.504 1.125 1.125v9.75c0 .621-.504 1.125-1.125 1.125h-.375m1.5-1.5H21a.75.75 0 0 0-.75.75v.75m0 0H3.75m0 0h-.375a1.125 1.125 0 0 1-1.125-1.125V15m1.5 1.5v-.75A.75.75 0 0 0 3 15h-.75M15 10.5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Zm3 0h.008v.008H18V10.5Zm-12 0h.008v.008H6V10.5Z"}]])

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

(defn money []
  [:svg
   {:xmlns "http://www.w3.org/2000/svg",
    :class "size-8",
    :fill "none",
    :viewBox "0 0 24 24",
    :stroke "currentColor",
    :stroke-width "2"}
   [:path
    {:stroke-linecap "round",
     :stroke-linejoin "round",
     :d "M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z"}]])

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
    :viewBox "0 0 24 24",
    :fill "currentColor",
    :aria-hidden "true",
    :class "h-4 w-4"}
   [:path
    {:d "M21.731 2.269a2.625 2.625 0 00-3.712 0l-1.157 1.157 3.712 3.712 1.157-1.157a2.625 2.625 0 000-3.712zM19.513 8.199l-3.712-3.712-12.15 12.15a5.25 5.25 0 00-1.32 2.214l-.8 2.685a.75.75 0 00.933.933l2.685-.8a5.25 5.25 0 002.214-1.32L19.513 8.2z"}]])

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
