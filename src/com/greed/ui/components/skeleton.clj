(ns com.greed.ui.components.skeleton)


(defn horizontal-card []
  [:div
   {:class "flex flex-col w-1/2 gap-5 p-2 bg-white shadow-lg select-none sm:p-4 sm:h-64 rounded-2xl sm:flex-row"}
   [:div
    {:class "bg-zinc-200 h-52 sm:h-full sm:w-72 rounded-xl animate-pulse"}]
   [:div
    {:class "flex flex-col flex-1 gap-5 sm:p-2"}
    [:div
     {:class "flex flex-col flex-1 gap-3"}
     [:div {:class "w-full bg-zinc-200 animate-pulse h-14 rounded-2xl"}]
     [:div {:class "w-full h-3 bg-zinc-200 animate-pulse rounded-2xl"}]
     [:div {:class "w-full h-3 bg-zinc-200 animate-pulse rounded-2xl"}]
     [:div {:class "w-full h-3 bg-zinc-200 animate-pulse rounded-2xl"}]
     [:div {:class "w-full h-3 bg-zinc-200 animate-pulse rounded-2xl"}]]
    [:div
     {:class "flex gap-3 mt-auto"}
     [:div {:class "w-20 h-8 bg-zinc-200 rounded-full animate-pulse"}]
     [:div {:class "w-20 h-8 bg-zinc-200 rounded-full animate-pulse"}]
     [:div {:class "w-20 h-8 ml-auto bg-zinc-200 rounded-full animate-pulse"}]]]])
