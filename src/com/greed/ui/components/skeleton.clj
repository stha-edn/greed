(ns com.greed.ui.components.skeleton)


(defn horizontal-card []
  [:div
   {:class "flex flex-col gap-5 select-none w-1/2 p-2 bg-white rounded-2xl shadow-lg sm:flex-row sm:h-64 sm:p-4"}
   [:div
    {:class "h-52 bg-zinc-200 rounded-xl animate-pulse sm:h-full sm:w-72"}]
   [:div
    {:class "flex flex-col flex-1 gap-5 sm:p-2"}
    [:div
     {:class "flex flex-col flex-1 gap-3"}
     [:div {:class "w-full h-14 bg-zinc-200 rounded-2xl animate-pulse"}]
     [:div {:class "w-full h-3 bg-zinc-200 rounded-2xl animate-pulse"}]
     [:div {:class "w-full h-3 bg-zinc-200 rounded-2xl animate-pulse"}]
     [:div {:class "w-full h-3 bg-zinc-200 rounded-2xl animate-pulse"}]
     [:div {:class "w-full h-3 bg-zinc-200 rounded-2xl animate-pulse"}]]
    [:div
     {:class "flex gap-3 mt-auto"}
     [:div {:class "w-20 h-8 bg-zinc-200 rounded-full animate-pulse"}]
     [:div {:class "w-20 h-8 bg-zinc-200 rounded-full animate-pulse"}]
     [:div {:class "w-20 h-8 ml-auto bg-zinc-200 rounded-full animate-pulse"}]]]])
