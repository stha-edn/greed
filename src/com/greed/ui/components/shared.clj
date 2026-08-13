(ns com.greed.ui.components.shared
  (:require [clojure.string :as str]
            [com.core :as c]
            [com.greed.data.core :as data]
            [com.greed.utilities.core :as utilities]))

(defn- split-kw-args
  "Splits `args` into [body kw-args]. Keyword options are read from the front
   in :key value pairs; everything after them is treated as element content."
  [args]
  (let [n   (count args)
        i   (loop [i 0]
              (if (and (< i n) (keyword? (nth args i)) (< (inc i) n))
                (recur (+ i 2))
                i))]
    [(subvec (vec args) i) (subvec (vec args) 0 i)]))

(defn btn
  "Button or link styled as a button. Variants:
     :primary      emerald solid
     :dark         zinc-900 solid
     :outline      white / bordered
     :ghost        zinc-100 subtle
     :danger       red solid
     :emerald-ghost transparent emerald (for dark surfaces)
   Sizes: :sm, :md, :lg. Pass :href to render an anchor instead.
   Trailing arguments are rendered as the element's content."
  [& args]
  (let [[body kw-args] (split-kw-args args)
        {:keys [variant size class href type onclick attrs]
         :or {variant :dark size :md}} (apply hash-map kw-args)
        base   "inline-flex items-center justify-center gap-2 font-medium rounded-lg transition-colors active:scale-[0.97]"
        size-c (case size
                 :sm "px-3 py-1.5 text-xs"
                 :md "px-4 py-2 text-sm"
                 :lg "px-6 py-3 text-sm font-semibold")
        variant-c (case variant
                    :primary "text-white bg-emerald-600 hover:bg-emerald-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 active:bg-emerald-800"
                    :dark "text-white bg-zinc-900 hover:bg-zinc-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-500 focus-visible:ring-offset-2 active:bg-zinc-800"
                    :outline "text-zinc-700 bg-white border border-zinc-300 hover:bg-zinc-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 active:bg-zinc-100"
                    :ghost "text-zinc-700 bg-zinc-100 hover:bg-zinc-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 active:bg-zinc-300"
                    :danger "text-white bg-red-500 hover:bg-red-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:ring-offset-2 active:bg-red-700"
                    :emerald-ghost "text-white bg-emerald-500/20 border border-transparent hover:bg-emerald-600/20 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 active:bg-emerald-700/30"
                    (throw (ex-info (str "Unknown button variant: " variant) {})))
        cls (str/join " " [base size-c variant-c class])
        attrs (merge attrs {:class cls})]
    (if href
      (into [:a (assoc attrs :href href)] body)
      (into [:button (assoc attrs :type (or type "button"))] body))))

(defn card
  "Reusable card container. Variants:
     :light   white / zinc-100 border (default)
     :soft    white-to-emerald gradient with ring (dashboard panels)
     :dark    zinc-900 panel (marketing)
   Pass children as trailing arguments."
  [opts & body]
  (let [{:keys [variant class]
         :or {variant :light}} opts]
    (into [:div
           {:class (str/join " "
              [(case variant
                 :light "bg-white border border-zinc-200/70 rounded-xl shadow-card"
                 :soft "bg-gradient-to-br from-white via-white to-emerald-50/80 border border-zinc-200/70 rounded-xl shadow-card"
                 :dark "bg-zinc-900 border border-zinc-800 rounded-2xl shadow-card-md")
                              class])}]
          body)))

(defn form-label
  "Label for form fields."
  [for & content]
  [:label {:class "block mb-1 text-sm font-medium text-zinc-700" :for for} content])

(defn section-label
  "Uppercase micro heading used to label sections and groups."
  [& content]
  [:p {:class "text-xs font-semibold text-zinc-400 uppercase tracking-wider"} content])

(defn base-input-class []
  "block w-full px-3 py-2 text-sm text-zinc-700 placeholder-zinc-400 bg-white border border-zinc-200 rounded-lg transition-colors duration-150 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500")

(defn determine-placeholder [id user profile]
  (let [config c/common-config]
    (cond
      (contains? (:user/fields config) id)
      ((keyword (str "user/" id)) user)
      (contains? (:finances/fields config) id)
      ((keyword (str "finances/" id)) profile))))

(defn input [& {:keys [id type label required?]
                :or {required? false}}]
  [:div {:class "mt-4"}
   (form-label id label)
   [:input
    {:class (base-input-class)
     :id id :name id :type type
     :autocomplete type
     :required required?}]])

(defn app-input [ctx & {:keys [id type label required? prefix hint]
                        :or {required? false}}]
  (let [{:keys [session]} ctx
        user-id     (:uid session)
        user        (data/get-user ctx user-id)
        finances    (data/get-finances ctx user-id)
        current-val (determine-placeholder id user finances)]
    [:div
     (when (seq label)
       (form-label id label))
     (if prefix
       [:div {:class "relative flex items-center"}
        [:div {:class "absolute left-3 pointer-events-none select-none text-sm font-medium text-zinc-400"} prefix]
        [:input {:class "block w-full pl-7 pr-3 py-2 text-sm text-zinc-700 placeholder-zinc-400 bg-white border border-zinc-200 rounded-lg transition-colors duration-150 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500"
                 :id id :name id :type type
                 :value (str current-val)
                 :required required?}]]
       [:input {:class (base-input-class)
                :id id :name id :type type
                :value (str current-val)
                :required required?}])
     (when hint
       [:p {:class "mt-1 text-xs text-zinc-400"} hint])]))

(defn labeled-input
  "Label + input + optional hint, with an explicit :value — unlike app-input,
   which derives its value from ctx. Pass :prefix (e.g. \"R\") for a
   currency-style field, styled identically to app-input's :prefix variant."
  [& {:keys [id label type value hint prefix required? min]
      :or {required? false type "text"}}]
  [:div
   (form-label id label)
   (if prefix
     [:div {:class "relative flex items-center"}
      [:div {:class "absolute left-3 pointer-events-none select-none text-sm font-medium text-zinc-400"} prefix]
      [:input {:class "block w-full pl-7 pr-3 py-2 text-sm text-zinc-700 placeholder-zinc-400 bg-white border border-zinc-200 rounded-lg transition-colors duration-150 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500"
               :id id :name id :type type :value value :required required? :min min}]]
     [:input {:class (base-input-class)
              :id id :name id :type type :value value :required required? :min min}])
   (when hint
     [:p {:class "mt-1 text-xs text-zinc-400"} hint])])

(defn app-select [ctx & {:keys [id label options required? hint attrs]
                         :or {required? false}}]
  (let [{:keys [session]} ctx
        user-id     (:uid session)
        finances    (data/get-finances ctx user-id)
        current-val ((keyword (str "finances/" id)) finances)]
    [:div
     (form-label id label)
     [:select (merge {:class (base-input-class)
                      :id id :name id
                      :required required?}
                     attrs)
      (for [option options]
        [:option (cond-> {:value option}
                   (= option current-val) (assoc :selected true))
         (utilities/->string option)])]
     (when hint
       [:p {:class "mt-1 text-xs text-zinc-400"} hint])]))

(defn app-account-type-select [& {:keys [id label required? hint options selected]
                                  :or {required? false}}]
  [:div {:id (str id "-field")}
   (form-label id label)
   [:select {:class (base-input-class)
             :id id :name id
             :required required?}
    [:option {:value ""} "Select an account type"]
    (for [option options]
      [:option (cond-> {:value option}
                 (= option selected) (assoc :selected true))
       option])]
   (when hint
     [:p {:class "mt-1 text-xs text-zinc-400"} hint])])

(defn- modal-exit-sequence
  "Shared exit choreography: stage the CSS `.greed-modal-out` state, wait for
   its 200ms transition, then remove `open` (which flips the element to
   `invisible`). Without the pause the exit would never be seen — `open` and
   visibility flip together. Mirrors the pattern success/info alerts use for
   their `greed-alert-out` dismissals."
  [id]
  (str "  add .greed-modal-out to #" id "\n"
       "  wait 200ms\n"
       "  remove @open from #" id "\n"
       "  remove .greed-modal-out from #" id))

(defn open-actions
  "hyperscript for an element that opens `#id` by adding an `open` attribute."
  [id]
  (str "on click\n"
       "  add @open='true' to #" id))

(defn close-actions
  "hyperscript for an element that closes `#id`. Runs the exit sequence rather
   than cutting straight to hidden, so dismissal reads as the reverse of the
   entrance — spatial consistency."
  [id]
  (str "on click\n"
       (modal-exit-sequence id)))

(defn modal
  "Alpine-free modal shell. hyperscript toggles an `open` attribute; CSS
   keyed off `.greed-modal[open]` (tailwind.css) animates the overlay and
   card separately — the same opacity-only-scrim / opacity+scale-card split
   `#confirm-dialog` already uses, so the scrim never visibly shrinks with
   the card mid-transition. The `open` attribute keeps the dialog hidden
   (visibility, pointer-events) until hyperscript adds it."
  [id & body]
  (let [close (close-actions id)]
    [:div {:id id
           :role "dialog"
           :aria-modal "true"
            :_ (str "on keydown[key == 'Escape'] from window\n"
                    "  if the @open of #" id " is 'true'\n"
                    (modal-exit-sequence id))
           :class "greed-modal fixed inset-0 z-50 flex items-center justify-center p-4 invisible pointer-events-none [&[open]]:visible [&[open]]:pointer-events-auto"}
     [:div {:class "greed-modal-overlay absolute inset-0 bg-black/50 backdrop-blur-sm" :_ close}]
     (into [:div {:class "greed-modal-card relative z-10"}] body)]))

(defn modal-input [& {:keys [id type label required?]
                      :or {required? false}}]
  [:div {:class "mt-3"}
   (form-label id label)
   [:input {:class (base-input-class)
            :type type :name id :id id :required required?}]])

(defn modal-select [& {:keys [id label options required?]
                       :or {required? false}}]
  [:div {:class "mt-3"}
   (form-label id label)
   [:select {:class (base-input-class)
             :id id :name id :required required?}
    (for [option options]
      [:option {:value option} (utilities/->string option)])]])
