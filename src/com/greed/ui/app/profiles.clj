(ns com.greed.ui.app.profiles
  "Personal or business: the Continue as page (with invitations and adding a
   business), a business's home, and its settings (details, members,
   invitations)."
  (:require [clojure.string :as str]
            [com.biffweb :as biff]
            [com.greed.ui :as ui]
            [com.greed.data.core :as data]
            [com.greed.data.plans :as plans]
            [com.greed.email :as email]
            [com.greed.ui.components.alerts :as alerts]
            [com.greed.ui.components.headers :as headers]
            [com.greed.ui.components.shared :as shared]
            [com.greed.utilities.core :as utilities]
            [com.greed.utilities.vat :as vat]))

;; ---------------------------------------------------------------------------
;; Shared bits

(def ^:private problems
  "Messages for the errors these pages redirect back with (?error=...)."
  {:business-name-blank "Enter the business's name."
   :business-name-too-long "That business name is too long. Use up to 120 characters."
   :entity-type-invalid "Choose what kind of business it is."
   :invalid-email "Enter a valid email address."
   :invalid-role "Choose Member or Owner."
   :already-member "That person is already a member of this business."
   :already-invited "That email already has an invitation waiting. Cancel it first to send a new one."
   :last-owner "A business needs at least one owner. Make someone else an owner first."
   :not-owner "Only an owner of the business can do that."
   :not-allowed "You can't do that in this business."
   :invite-gone "That invitation has expired or was cancelled. Ask for a new one."
   :vat-number-invalid "A VAT number is 10 digits and starts with 4."
   :vat-category-invalid "Choose the business's VAT period: A, B or C."
   :year-end-invalid "Choose the month the business's financial year ends."
   :shares-not-100 "The partners' shares must be whole percentages that add up to 100."})

(defn- problem [params]
  (when-let [e (:error params)]
    [:p {:role "alert" :class "mt-4 rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-700"}
     (get problems (keyword e) "Something went wrong. Try again.")]))

(defn- redirect [location & [session]]
  (cond-> {:status 303 :headers {"location" location}}
    session (assoc :session session)))

(def ^:private card "rounded-lg bg-white ring-1 ring-zinc-200/70 p-6")

(defn- business-fields
  "Name and type, prefilled when editing."
  [& {:keys [business-name entity-type]}]
  [:<>
   (shared/modal-input :id "business-name" :type "text" :label "Business name" :required? true :value business-name)
   [:div {:class "mt-3"}
    (shared/form-label "entity-type" "Type of business")
    [:select {:id "entity-type" :name "entity-type" :required true :class (shared/base-input-class)}
     [:option {:value ""} "Choose one"]
     (for [[k label] data/entity-types]
       [:option (cond-> {:value (name k)} (= k entity-type) (assoc :selected true)) label])]]])

(defn- person-name [u]
  (str/trim (str (:user/firstname u) " " (:user/lastname u))))

(defn- choice
  "One profile to continue as. `business-id` is empty for Personal."
  [{:keys [title sub business-id current?]}]
  [:li
   (biff/form {:action "/app/choose"}
              [:input {:type "hidden" :name "business-id" :value (str business-id)}]
              [:button {:type "submit"
                        :class (str "flex w-full items-center justify-between gap-4 rounded-lg border bg-white px-5 py-4 text-left transition hover:bg-zinc-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 "
                                    (if current? "border-zinc-900" "border-zinc-200"))}
               [:span {:class "min-w-0"}
                [:span {:class "block truncate text-base font-semibold text-zinc-900"} title]
                [:span {:class "block truncate text-sm text-zinc-500"} sub]]
               [:span {:aria-hidden "true" :class "flex-shrink-0 text-zinc-500"} "→"]])])

(defn- invitations [invites]
  (when (seq invites)
    [:section {:class "mb-8" :aria-labelledby "inv-h"}
     [:h2 {:id "inv-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "Invitations"]
     [:ul {:class "mt-3 space-y-3"}
      (for [{:keys [business inviter] :business-invite/keys [role] :xt/keys [id]} invites]
        [:li {:class (str card " flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between")}
         [:div {:class "min-w-0"}
          [:p {:class "truncate text-base font-semibold text-zinc-900"} (:business/name business)]
          [:p {:class "text-sm text-zinc-600"}
           (str (or (not-empty (person-name inviter)) "Someone") " invited you as "
                (if (= :owner role) "an owner" "a member") ".")]]
         [:div {:class "flex flex-shrink-0 gap-2"}
          (biff/form {:action "/app/invites/decline"}
                     [:input {:type "hidden" :name "invite-id" :value (str id)}]
                     (shared/btn :variant :ghost :size :sm :type "submit" "Decline"))
          (biff/form {:action "/app/invites/accept"}
                     [:input {:type "hidden" :name "invite-id" :value (str id)}]
                     (shared/btn :variant :dark :size :sm :type "submit" "Accept"))]])]]))

(defn- add-business-form
  "Open when asked for (?add=1, from the switcher) or when there is no
   business yet to choose. Without the Business plan it offers the trial,
   or the plans, instead of the form."
  [open? {:keys [tier trial-used?]}]
  [:details (cond-> {:class (str card " mt-8") :id "add-business"} open? (assoc :open true))
   [:summary {:class "cursor-pointer text-sm font-semibold text-zinc-900"} "Add a business"]
   [:p {:class "mt-2 text-sm text-zinc-600"} "A business you run. Its budget and spending are kept apart from your own, and you can invite others to it."]
   (if (= :business tier)
     (biff/form {:action "/app/businesses/create" :class "mt-2"}
                (business-fields)
                [:div {:class "mt-5"} (shared/btn :variant :dark :size :md :type "submit" "Add business")])
     [:div {:class "mt-4 rounded-md bg-zinc-50 p-4 ring-1 ring-zinc-200/70"}
      [:p {:class "text-sm text-zinc-700"}
       (if trial-used?
         "Businesses are part of the Business plan."
         (str "Businesses are part of the Business plan. Try it free for " (plans/trial-days) " days, no card needed."))]
      [:div {:class "mt-3 flex flex-wrap gap-2"}
       (when-not trial-used?
         (biff/form {:action "/app/plan/trial"}
                    (shared/btn :variant :dark :size :sm :type "submit" "Start free trial")))
       (shared/btn :variant :outline :size :sm :href "/app/plan" "See plans")]])])

(defn page
  "GET /app/choose: continue as Personal or as one of the person's
   businesses; accept invitations; add a business."
  [{:keys [session params] :as ctx}]
  (let [businesses (data/get-user-businesses ctx (:uid session))
        invites    (data/get-user-invites ctx (:uid session))
        current    (data/session-business ctx)
        status     (plans/status ctx (:uid session))]
    (ui/app
     ctx
     [:div {:class "mx-auto max-w-lg"}
      (headers/pages-heading ["Continue as"] "Choose whose money you are working on. You can switch at any time.")
      (problem params)
      (invitations invites)
      [:ul {:class "space-y-3"}
       (choice {:title "Personal" :sub "Your salary, tax, budget and goals" :current? (nil? current)})
       (for [{:business/keys [name entity-type] :business-member/keys [role] :xt/keys [id]} businesses]
         (choice {:title name
                  :sub (str (data/entity-types entity-type) " · " (data/roles role)
                            (when-not (plans/business-allowed? ctx (:uid session) id) " · Needs the Business plan"))
                  :business-id id :current? (= id (:xt/id current))}))]
      (add-business-form (or (= "1" (:add params)) (empty? businesses) (some? (:error params))) status)])))

(defn accept-invite
  "POST /app/invites/accept: join the business and switch to it."
  [{:keys [session params] :as ctx}]
  (if-let [business-id (data/accept-invite! ctx (:uid session) (parse-uuid (str (:invite-id params))))]
    (if (plans/business-allowed? ctx (:uid session) business-id)
      (redirect "/app?alert=invite-accepted" (assoc session :business-id business-id))
      (redirect "/app/plan?alert=need-business" (dissoc session :business-id)))
    (redirect "/app/choose?error=invite-gone")))

(defn decline-invite
  "POST /app/invites/decline."
  [{:keys [session params] :as ctx}]
  (if (data/decline-invite! ctx (:uid session) (parse-uuid (str (:invite-id params))))
    (redirect "/app/choose?alert=invite-declined")
    (redirect "/app/choose?error=invite-gone")))

(defn add-business
  "POST /app/businesses/create: add a business the person runs, and switch to it."
  [{:keys [session params] :as ctx}]
  (let [{:keys [business-id error]} (if (plans/at-least? ctx (:uid session) :business)
                                      (data/add-business! ctx (:uid session) params)
                                      {:error :needs-plan})]
    (if (= :needs-plan error)
      (redirect "/app/plan?alert=need-business")
      (if error
      (redirect (str "/app/choose?error=" (name error) "#add-business"))
      (redirect "/app?alert=business-added" (assoc session :business-id business-id))))))

(defn choose
  "POST /app/choose: switch to the chosen profile. A business the person is
   not a member of is ignored, leaving them on their personal side."
  [{:keys [session params] :as ctx}]
  (let [id       (parse-uuid (str (:business-id params)))
        business (data/get-user-business ctx (:uid session) id)]
    (if (and business (not (plans/business-allowed? ctx (:uid session) (:xt/id business))))
      (redirect "/app/plan?alert=need-business" (dissoc session :business-id))
      {:status 303
       :headers {"location" "/app"}
       :session (if business
                  (assoc session :business-id (:xt/id business))
                  (dissoc session :business-id))})))

(defn wrap-personal
  "For pages that belong to the personal side only: while a business is
   chosen they go to the business's home instead of showing personal data
   as if it were the business's."
  [handler]
  (fn [ctx]
    (if (data/session-business ctx)
      {:status 303 :headers {"location" "/app"}}
      (handler ctx))))

;; ---------------------------------------------------------------------------
;; Business settings

(defn- details-section [business owner?]
  [:section {:class card :aria-labelledby "details-h"}
   [:h2 {:id "details-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "Details"]
   (if owner?
     (biff/form {:action "/app/business/update" :class "max-w-md"}
                (business-fields :business-name (:business/name business)
                                 :entity-type (:business/entity-type business))
                [:div {:class "mt-5"} (shared/btn :variant :dark :size :md :type "submit" "Save details")])
     [:p {:class "mt-2 text-sm text-zinc-600"}
      (str (:business/name business) " · " (data/entity-types (:business/entity-type business))
           ". Only an owner can change these.")])])

(defn- vat-section [{:business/keys [vat-registered vat-number vat-category]} owner?]
  [:section {:class card :aria-labelledby "vat-h"}
   [:h2 {:id "vat-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "VAT"]
   [:p {:class "mt-0.5 max-w-[70ch] text-xs text-zinc-500"}
    (str "Registering is compulsory once taxable sales pass "
         (utilities/whole->rands (vat/compulsory-threshold)) " in 12 months, and allowed from "
         (utilities/whole->rands (vat/voluntary-threshold)) ". A VAT vendor gets a VAT type on budget items and a VAT page.")]
   (if owner?
     (biff/form {:action "/app/business/vat" :class "mt-3 max-w-md"}
                [:label {:class "flex items-center gap-2 text-sm text-zinc-800"}
                 [:input (cond-> {:type "checkbox" :name "vat-registered" :value "true" :class "rounded border-zinc-300"}
                           vat-registered (assoc :checked true))]
                 "The business is registered for VAT"]
                (shared/modal-input :id "vat-number" :type "text" :label "VAT number" :value vat-number
                                    :placeholder "4123456789" :hint "Only needed when registered.")
                [:div {:class "mt-3"}
                 (shared/form-label "vat-category" "VAT period")
                 [:select {:id "vat-category" :name "vat-category" :class (shared/base-input-class)}
                  [:option {:value ""} "Choose one"]
                  (for [[k label] vat/categories]
                    [:option (cond-> {:value (name k)} (= k vat-category) (assoc :selected true)) label])]]
                [:div {:class "mt-5"} (shared/btn :variant :dark :size :md :type "submit" "Save VAT details")])
     [:p {:class "mt-2 text-sm text-zinc-600"}
      (if vat-registered
        (str "Registered, VAT number " vat-number ". " (vat/categories vat-category) ".")
        "Not registered for VAT.")])])

(def ^:private month-names
  ["January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"])

(defn- tax-section [{:business/keys [year-end-month sbc]} owner?]
  [:section {:class card :aria-labelledby "tax-h"}
   [:h2 {:id "tax-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "Company tax"]
   [:p {:class "mt-0.5 max-w-[70ch] text-xs text-zinc-500"}
    "Used for the Tax page: the financial year sets the provisional tax dates. A small business corporation pays tax on a sliding scale instead of a flat 27%. It qualifies with gross income up to R20 million, shareholders who are all people, and when it is not a personal service provider."]
   (if owner?
     (biff/form {:action "/app/business/tax" :class "mt-3 max-w-md"}
                [:div {:class "mt-3"}
                 (shared/form-label "year-end-month" "Financial year ends in")
                 [:select {:id "year-end-month" :name "year-end-month" :class (shared/base-input-class)}
                  (for [[i m] (map-indexed vector month-names)
                        :let [n (inc i)]]
                    [:option (cond-> {:value (str n)} (= n (or year-end-month 2)) (assoc :selected true)) m])]]
                [:label {:class "mt-4 flex items-center gap-2 text-sm text-zinc-800"}
                 [:input (cond-> {:type "checkbox" :name "sbc" :value "true" :class "rounded border-zinc-300"}
                           sbc (assoc :checked true))]
                 "The business is a small business corporation"]
                [:div {:class "mt-5"} (shared/btn :variant :dark :size :md :type "submit" "Save tax details")])
     [:p {:class "mt-2 text-sm text-zinc-600"}
      (str "Financial year ends in " (month-names (dec (or year-end-month 2))) "."
           (when sbc " A small business corporation."))])])

(defn- shares-section
  "A partnership's split of the profit between its owners (partners)."
  [members owner?]
  (let [owners (filter #(= :owner (:business-member/role %)) members)
        shares (data/profit-shares members)]
    [:section {:class card :aria-labelledby "shares-h"}
     [:h2 {:id "shares-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "Partners' shares"]
     [:p {:class "mt-0.5 max-w-[70ch] text-xs text-zinc-500"}
      "Each partner's share of the profit, which is taxed in their own personal return. Owners are the partners; until shares are set, the profit is split equally."]
     (if owner?
       (biff/form {:action "/app/business/shares" :class "mt-3 max-w-md space-y-2"}
                  (for [{:keys [user] :business-member/keys [user-id] :xt/keys [id]} owners]
                    [:div {:class "flex items-center justify-between gap-4"}
                     [:label {:for (str "share-" id) :class "min-w-0 truncate text-sm text-zinc-800"} (person-name user)]
                     [:span {:class "flex items-center gap-1"}
                      [:input {:id (str "share-" id) :name (str "share-" id) :type "number" :min 0 :max 100 :step 1 :required true
                               :value (str (Math/round (double (get shares user-id 0))))
                               :class "w-20 rounded-md border border-zinc-200 px-2 py-1 text-right text-sm tabular-nums"}]
                      [:span {:class "text-sm text-zinc-500"} "%"]]])
                  [:div {:class "pt-3"} (shared/btn :variant :dark :size :md :type "submit" "Save shares")])
       [:ul {:class "mt-3 space-y-1 text-sm text-zinc-700"}
        (for [{:keys [user] :business-member/keys [user-id]} owners]
          [:li (str (person-name user) ": " (Math/round (double (get shares user-id 0))) "%")])])]))

(defn- members-section [members uid owner?]
  [:section {:class card :aria-labelledby "members-h"}
   [:h2 {:id "members-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "Members"]
   [:p {:class "mt-0.5 text-xs text-zinc-500"}
    "Members use the Budget and Spending pages. Owners also manage members and the business's details."]
   [:ul {:class "mt-4 divide-y divide-zinc-100"}
    (for [{:keys [user] :business-member/keys [role user-id] :xt/keys [id]} members
          :let [me? (= uid user-id)]]
      [:li {:class "flex flex-col gap-3 py-3 sm:flex-row sm:items-center sm:justify-between"}
       [:div {:class "min-w-0"}
        [:p {:class "truncate text-sm font-medium text-zinc-900"} (str (person-name user) (when me? " (you)"))]
        [:p {:class "truncate text-xs text-zinc-500"} (:user/email user)]]
       [:div {:class "flex flex-shrink-0 items-center gap-2"}
        (if owner?
          (biff/form {:action "/app/business/members/role" :class "flex items-center gap-2"}
                     [:input {:type "hidden" :name "member-id" :value (str id)}]
                     [:label {:for (str "role-" id) :class "sr-only"} (str "Role for " (person-name user))]
                     [:select {:id (str "role-" id) :name "role" :class "rounded-md border border-zinc-200 bg-white px-2 py-1 text-sm"}
                      (for [[k label] data/roles]
                        [:option (cond-> {:value (name k)} (= k role) (assoc :selected true)) label])]
                     (shared/btn :variant :outline :size :sm :type "submit" "Save"))
          [:span {:class "text-sm text-zinc-600"} (data/roles role)])
        (when (or owner? me?)
          (biff/form {:action "/app/business/members/remove"}
                     [:input {:type "hidden" :name "member-id" :value (str id)}]
                     (shared/btn :variant :ghost :size :sm :type "submit"
                                 :attrs {:data-confirm (if me? "Leave this business? You will lose access to its budget and spending."
                                                           (str "Remove " (person-name user) " from this business?"))}
                                 (if me? "Leave" "Remove"))))]])]])

(defn- invites-section [invites]
  [:section {:class card :aria-labelledby "invite-h"}
   [:h2 {:id "invite-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "Invite someone"]
   (biff/form {:action "/app/business/invite" :class "mt-1 grid max-w-xl grid-cols-1 gap-x-3 sm:grid-cols-[minmax(0,1fr)_9rem_auto] sm:items-end"}
              (shared/modal-input :id "email" :type "email" :label "Email address" :required? true)
              [:div {:class "mt-3"}
               (shared/form-label "role" "Role")
               [:select {:id "role" :name "role" :class (shared/base-input-class)}
                (for [[k label] data/roles] [:option {:value (name k)} label])]]
              [:div {:class "mt-3"} (shared/btn :variant :dark :size :md :type "submit" "Send invitation")])
   [:p {:class "mt-2 text-xs text-zinc-500"}
    "They get an email, and the invitation waits for them in greed for 14 days. Someone without an account signs up with that email first."]
   (when (seq invites)
     [:ul {:class "mt-5 divide-y divide-zinc-100 border-t border-zinc-100"}
      (for [{:business-invite/keys [email role] :xt/keys [id]} invites]
        [:li {:class "flex items-center justify-between gap-4 py-3"}
         [:div {:class "min-w-0"}
          [:p {:class "truncate text-sm text-zinc-900"} email]
          [:p {:class "text-xs text-zinc-500"} (str (data/roles role) " · waiting for them to accept")]]
         (biff/form {:action "/app/business/invites/cancel"}
                    [:input {:type "hidden" :name "invite-id" :value (str id)}]
                    (shared/btn :variant :ghost :size :sm :type "submit" "Cancel"))])])])

(defn- delete-section [business]
  [:section {:class "rounded-lg border border-rose-200 bg-white p-6" :aria-labelledby "delete-h"}
   [:h2 {:id "delete-h" :class "text-sm font-semibold tracking-tight text-zinc-900"} "Delete this business"]
   [:p {:class "mt-1 max-w-[60ch] text-sm text-zinc-600"}
    "Deletes the business for every member, with its budget, statement months and invitations. This cannot be undone."]
   (biff/form {:action "/app/business/delete" :class "mt-4"}
              (shared/btn :variant :danger :size :md :type "submit"
                          :attrs {:data-confirm (str "Delete " (:business/name business) " and everything in it? This cannot be undone.")}
                          "Delete business"))])

(defn settings-page
  "GET /app/settings while a business is chosen: its details, VAT, tax,
   members and invitations."
  [{:keys [session params] :as ctx}]
  (if-let [business (data/session-business ctx)]
    (let [uid    (:uid session)
          id     (:xt/id business)
          owner? (data/business-owner? ctx uid id)]
      (ui/app
       ctx
       [:div {:class "space-y-7"}
        (headers/pages-heading ["Settings"] (str (:business/name business) ": its details, VAT, tax and members."))
        (problem params)
        ;; forms side by side, as on Personal's Settings
        [:div {:class "grid grid-cols-1 items-start gap-4 lg:grid-cols-2"}
         (details-section business owner?)
         (vat-section business owner?)
         (when (= :pty-ltd (:business/entity-type business)) (tax-section business owner?))
         (when (= :partnership (:business/entity-type business))
           (shares-section (data/get-business-members ctx id) owner?))]
        (members-section (data/get-business-members ctx id) uid owner?)
        (when owner? (invites-section (data/get-business-invites ctx id)))
        (when owner? (delete-section business))]))
    (redirect "/app/choose")))

(defn- in-business
  "Runs `f` with the chosen business's id, or goes to Continue as."
  [{:keys [session] :as ctx} f]
  (if-let [business (data/session-business ctx)]
    (f (:uid session) (:xt/id business) business)
    (redirect "/app/choose")))

(defn- back [result ok-alert]
  (redirect (if (= :ok result)
              (str "/app/settings?alert=" ok-alert)
              (str "/app/settings?error=" (name result)))))

(defn update-details
  "POST /app/business/update."
  [{:keys [params] :as ctx}]
  (in-business ctx (fn [uid id _] (back (data/update-business! ctx uid id params) "business-saved"))))

(defn update-vat
  "POST /app/business/vat."
  [{:keys [params] :as ctx}]
  (in-business ctx (fn [uid id _] (back (data/update-vat-settings! ctx uid id params) "vat-saved"))))

(defn update-shares
  "POST /app/business/shares."
  [{:keys [params] :as ctx}]
  (in-business ctx (fn [uid id _] (back (data/update-profit-shares! ctx uid id params) "shares-saved"))))

(defn update-tax
  "POST /app/business/tax."
  [{:keys [params] :as ctx}]
  (in-business ctx (fn [uid id _] (back (data/update-tax-settings! ctx uid id params) "tax-details-saved"))))

(defn invite
  "POST /app/business/invite: save the invitation and email it."
  [{:keys [params biff/base-url] :as ctx}]
  (in-business
   ctx
   (fn [uid id business]
     (let [{:keys [invite error]} (data/invite-member! ctx uid id params)]
       (if error
         (back error nil)
         (do (email/send-email ctx {:template :business-invite
                                    :to (:business-invite/email invite)
                                    :business-name (:business/name business)
                                    :inviter (or (not-empty (person-name (data/get-user ctx uid))) "Someone")
                                    :role-label (data/roles (:business-invite/role invite))
                                    :url (str base-url "/app/choose")})
             (back :ok "invite-sent")))))))

(defn cancel-invite
  "POST /app/business/invites/cancel."
  [{:keys [params] :as ctx}]
  (in-business ctx (fn [uid _ _]
                     (back (if (data/cancel-invite! ctx uid (parse-uuid (str (:invite-id params)))) :ok :not-owner)
                           "invite-cancelled"))))

(defn change-role
  "POST /app/business/members/role."
  [{:keys [params] :as ctx}]
  (in-business ctx (fn [uid _ _]
                     (back (data/change-member-role! ctx uid (parse-uuid (str (:member-id params))) (:role params))
                           "member-updated"))))

(defn remove-member
  "POST /app/business/members/remove: remove someone, or leave yourself."
  [{:keys [session params] :as ctx}]
  (in-business ctx (fn [uid _ _]
                     (let [member-id (parse-uuid (str (:member-id params)))
                           self?     (some #(and (= member-id (:xt/id %)) (= uid (:business-member/user-id %)))
                                           (data/get-business-members ctx (:business-id session)))
                           result    (data/remove-member! ctx uid member-id)]
                       (if (and self? (= :ok result))
                         (redirect "/app/choose?alert=left-business" (dissoc session :business-id))
                         (back result "member-removed"))))))

(defn delete-business
  "POST /app/business/delete."
  [{:keys [session] :as ctx}]
  (in-business ctx (fn [uid id _]
                     (if (data/delete-business! ctx uid id)
                       (redirect "/app/choose?alert=business-deleted" (dissoc session :business-id))
                       (back :not-owner nil)))))
