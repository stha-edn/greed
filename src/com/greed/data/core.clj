(ns com.greed.data.core
  (:require [buddy.hashers :as hashers]
            [com.biffweb :as biff :refer [q]]
            [xtdb.api :as xt]
            [clojure.string :as str]
            [clojure.tools.logging :as logger]
            [com.greed.utilities.tax :as tax]
            [com.greed.utilities.core :as utilities]
            [com.greed.data.validation :as validation]
            [com.greed.statements.categorise :as categorise]
            [com.greed.statements.summary :as summary]))

(defn get-users [{:keys [biff/db]}]
  (q db
     '{:find (pull user [*])
       :where [[user :user/email]]}))

(defn get-user-id [{:keys [biff/db params]}]
  (biff/lookup-id db :user/email (:email params)))

(defn get-user-id-from-session [{:keys [session]}]
  (:uid session))

(defn owned-by-session?
  "True when the session's uid matches the owner of the given record."
  [{:keys [session]} owner-id]
  (= (:uid session) owner-id))

(declare session-business get-user-business sync-owner-pay! can-change-budget-item?)

(defn owner
  "Whose money the signed-in person is working on: {:user-id uid
   :business-id id}, with :business-id nil on their personal side."
  [{:keys [session] :as ctx}]
  {:user-id (:uid session) :business-id (:xt/id (session-business ctx))})

(defn- ->owner
  "An owner map from an owner map or a bare user id (personal)."
  [o]
  (if (map? o) o {:user-id o}))

(defn email-taken-by-other?
  "True when the submitted email belongs to a user other than the given user-id."
  [{:keys [biff/db params]} user-id]
  (let [email       (:email params)
        existing-id (biff/lookup-id db :user/email email)]
    (and (some? existing-id) (not= existing-id user-id))))

(defn get-user [{:keys [biff/db]} user-id]
  (first (q db
            '{:find (pull user [*])
              :in [user-id]
              :where [[user :xt/id user-id]]}
            user-id)))

(defn admin? [user]
  (contains? (:user/roles user) :admin))

(defn get-finances [{:keys [biff/db]} user-id]
  (first (q db
            '{:find (pull finances [*])
              :in [user-id]
              :where [[finances :finances/user-id user-id]]}
            user-id)))

(defn get-budget-item [{:keys [biff/db]} budget-item-id]
  (first (q db
            '{:find (pull budget-item [*])
              :in [budget-item-id]
              :where [[budget-item :xt/id budget-item-id]]}
            budget-item-id)))

(defn get-budget-items
  "The user's personal budget items (never a business's)."
  [{:keys [biff/db]} user-id]
  (q db
     '{:find (pull budget-item [*])
       :in [user-id]
       :where [[budget-item :budget-item/user-id user-id]
               (not [budget-item :budget-item/business-id])]}
     user-id))

(defn get-business-budget-items
  "A business's budget items, whoever of its members added them."
  [{:keys [biff/db]} business-id]
  (q db
     '{:find (pull budget-item [*])
       :in [business-id]
       :where [[budget-item :budget-item/business-id business-id]]}
     business-id))

(defn get-owner-budget-items
  "Budget items for an owner (see `owner`): a business's, or personal."
  [ctx o]
  (let [{:keys [user-id business-id]} (->owner o)]
    (if business-id
      (get-business-budget-items ctx business-id)
      (get-budget-items ctx user-id))))

(defn current-month
  "This month as \"yyyy-MM\"."
  []
  (str (java.time.YearMonth/now)))

(defn monthly-items
  "Items planned for every month: not the ones added from a statement, which
   are that month's amounts only."
  [items]
  (remove :budget-item/from-statement items))

(defn items-for-month
  "The items that count in `month` (\"yyyy-MM\"): every monthly item, and
   the items added from that month's statement."
  [items month]
  (filter #(let [f (:budget-item/from-statement %)] (or (nil? f) (= f month))) items))

(defn make-monthly!
  "Turns an item added from a statement into a monthly item. Returns true if
   the person could change it."
  [{:keys [params] :as ctx}]
  (let [id   (utilities/->uuid (:budget-item-id params))
        item (when id (get-budget-item ctx id))]
    (when (and item (:budget-item/from-statement item) (can-change-budget-item? ctx item))
      (biff/submit-tx ctx [{:db/doc-type :budget-item :xt/id id :db/op :update
                            :budget-item/from-statement :db/dissoc}])
      true)))

(defn get-budget-data
  "Totals the month's budget items grouped by type."
  [budget-items]
  (let [income-items (filterv #(= (:budget-item/type %) :income) budget-items)
        expenses-items (filterv #(= (:budget-item/type %) :expenses) budget-items)
        savings-items (filterv #(= (:budget-item/type %) :savings) budget-items)

        total-income (reduce + (map :budget-item/amount income-items))
        total-expenses (reduce + (map :budget-item/amount expenses-items))
        total-savings (reduce + (map :budget-item/amount savings-items))]
    {:total-income total-income
     :total-expenses total-expenses
     :total-savings total-savings}))

(defn get-salary-budget-item [{:keys [biff/db]} user-id]
  (first (q db
            '{:find (pull budget-item [*])
              :in [user-id]
              :where [[budget-item :budget-item/user-id user-id]
                      [budget-item :budget-item/title "Salary"]
                      (not [budget-item :budget-item/business-id])]}
            user-id)))

(defn hash-password [password]
  (hashers/derive password))

(defn hashed-password?
  "True when `password` is already a properly hashed buddy.hashers value,
  as opposed to e.g. legacy plaintext that slipped in via a script or import."
  [password]
  (try
    (boolean (hashers/parse-password password))
    (catch Exception _ false)))

(defn user-active?
  "A user is active only when explicitly marked active (:user/active true).
  A missing or nil :user/active counts as deactivated."
  [user]
  (true? (:user/active user)))

(defn get-user-by-email [ctx email]
  (let [target (biff/normalize-email email)]
    (first (filter (fn [user]
                     (= target (biff/normalize-email (:user/email user))))
                   (get-users ctx)))))

(defn set-password-reset-token
  "Stores a single-use token on the user so password-reset links can only be
  redeemed once. Requesting a new reset overwrites any previous token."
  [ctx user-id token]
  (biff/submit-tx ctx
                  [{:db/doc-type :user
                    :xt/id user-id
                    :db/op :update
                    :user/password-reset-token token
                    :user/password-reset-at :db/now}]))

(defn reset-password
  "Sets a new password hash and clears the single-use reset token."
  [ctx user-id new-password]
  (biff/submit-tx ctx
                  [{:db/doc-type :user
                    :xt/id user-id
                    :db/op :update
                    :user/password (hash-password new-password)
                    :user/password-reset-token :db/dissoc
                    :user/password-reset-at :db/dissoc}]))

(defn upsert-user
  "Creates a user from sign-up params. Returns the user's id."
  [{:keys [params] :as ctx}]
  (let [user-id (java.util.UUID/randomUUID)
        age     (utilities/->int (:age params))]
    (logger/info "Creating user...")
    (biff/submit-tx ctx
                    [(cond-> {:db/doc-type :user
                              :xt/id user-id
                              :user/email (:email params)
                              :user/password (hash-password (:password params))
                              :user/firstname (:firstname params)
                              :user/lastname (:lastname params)
                              :user/active true
                              :user/roles #{:user}}
                       ;; a business sign-up gives no age
                       age (assoc :user/age age))])
    user-id))

(defn update-user-fields [ctx user-id params]
  (let [user (get-user ctx user-id)]
    (if user
      (do
        (logger/info "Updating user...")
        (biff/submit-tx ctx
                        (cond-> [{:db/doc-type :user
                                  :xt/id user-id
                                  :db/op :update
                                  :user/email (:email params)
                                  :user/firstname (:firstname params)
                                  :user/lastname (:lastname params)
                                  ;; left as it is when the form gives none
                                  :user/age (or (utilities/->int (:age params)) (:user/age user) :db/dissoc)}]
                          (not (str/blank? (:password params)))
                          (assoc-in [0 :user/password] (hash-password (:password params))))))
      (logger/info "User not found"))))

(defn update-user [{:keys [params] :as ctx}]
  (update-user-fields ctx (get-user-id-from-session ctx) params))

(defn set-user-role
  "Admin-only: replaces the target user's roles with the single given role."
  [ctx user-id role]
  (biff/submit-tx ctx
                  [{:db/doc-type :user
                    :xt/id user-id
                    :db/op :update
                    :user/roles #{role}}]))

(defn admin-update-user [{:keys [params] :as ctx}]
  (let [user-id (utilities/->uuid (:user-id params))]
    (update-user-fields ctx user-id params)
    (set-user-role ctx user-id (utilities/->keyword (:role params)))))

(defn admin-hash-user-password
  "If the target user's stored password isn't a proper hash (e.g. legacy
  plaintext that slipped in via a script or import), re-hashes it in place.
  Returns :hashed, :already-hashed, or :not-found."
  [{:keys [params] :as ctx}]
  (let [user-id (utilities/->uuid (:user-id params))
        user (get-user ctx user-id)]
    (cond
      (nil? user) :not-found
      (hashed-password? (:user/password user)) :already-hashed
      :else
      (do (logger/info (str "Hashing plaintext password for user " user-id))
          (biff/submit-tx ctx
                          [{:db/doc-type :user
                            :xt/id user-id
                            :db/op :update
                            :user/password (hash-password (:user/password user))}])
          :hashed))))

(defn- salary-budget-amount
  "The monthly take-home for the Salary budget item, from a gross monthly salary."
  [ctx user-id salary]
  (let [{:user/keys [age] :or {age 21}} (get-user ctx user-id)
        {:keys [net-income]} (tax/calculate-income-tax (utilities/income->annual-income (or salary 0)) (or age 21))]
    (-> net-income utilities/annual-income->monthly-income int)))

(defn- sync-salary-item!
  "Keeps the Salary budget item in step with the salary in Settings."
  [ctx user-id salary]
  (let [item (get-salary-budget-item ctx user-id)]
    (logger/info "Syncing the Salary budget item...")
    (biff/submit-tx ctx
                    [(cond-> {:db/doc-type :budget-item
                              :xt/id (or (:xt/id item) (java.util.UUID/randomUUID))
                              :budget-item/user-id user-id
                              :budget-item/title "Salary"
                              :budget-item/type :income
                              :budget-item/amount (salary-budget-amount ctx user-id salary)}
                       item (assoc :db/op :update))])))

(defn upsert-finances
  "Settings: saves salary and payday (creating the finances record if there
   is none yet), and keeps the Salary budget item in step."
  [{:keys [params] :as ctx}]
  (let [user-id  (get-user-id-from-session ctx)
        finances (get-finances ctx user-id)
        salary   (utilities/->int (:salary params))]
    (logger/info "Saving salary and payday...")
    (biff/submit-tx ctx
                    [(cond-> {:db/doc-type :finances
                              :xt/id (or (:xt/id finances) (java.util.UUID/randomUUID))
                              :finances/user-id user-id
                              :finances/salary salary
                              :finances/payday (validation/->valid-payday (:payday params))}
                       finances (assoc :db/op :update
                                       :finances/card-type :db/dissoc
                                       :finances/title :db/dissoc
                                       :finances/type :db/dissoc
                                       :finances/amount :db/dissoc))])
    (sync-salary-item! ctx user-id salary)))

(defn update-finances
  "Same as upsert-finances: salary and payday from Settings."
  [ctx]
  (upsert-finances ctx))

(defn save-bank-details!
  "Banking: saves the person's bank and account type (creating the finances
   record if there is none yet). Returns :ok or :bank-invalid."
  [{:keys [params] :as ctx} banks]
  (let [user-id  (get-user-id-from-session ctx)
        finances (get-finances ctx user-id)
        bank     (utilities/->keyword (:bank params))]
    (if-not (some #{bank} banks)
      :bank-invalid
      (do (logger/info "Saving bank details...")
          (biff/submit-tx ctx
                          [(cond-> {:db/doc-type :finances
                                    :xt/id (or (:xt/id finances) (java.util.UUID/randomUUID))
                                    :finances/user-id user-id
                                    :finances/bank bank
                                    :finances/account-type (or (not-empty (:account-type params)) :db/dissoc)}
                             finances (assoc :db/op :update)
                             (not finances) (update :finances/account-type #(when-not (= :db/dissoc %) %)))])
          :ok))))

(defn get-tax-profile [{:keys [biff/db]} user-id]
  (first (q db
            '{:find (pull tp [*])
              :in [user-id]
              :where [[tp :tax-profile/user-id user-id]]}
            user-id)))

(defn upsert-tax-profile [{:keys [params] :as ctx}]
  (let [user-id (get-user-id-from-session ctx)]
    (logger/info "Creating tax profile...")
    (biff/submit-tx ctx
                    [{:db/doc-type :tax-profile
                      :xt/id (java.util.UUID/randomUUID)
                      :tax-profile/user-id user-id
                      :tax-profile/medical-monthly    (or (utilities/->int (:medical-monthly params)) 0)
                      :tax-profile/medical-dependants (or (utilities/->int (:medical-dependants params)) 0)
                      :tax-profile/ra-annual          (or (utilities/->int (:ra-annual params)) 0)}])))

(defn update-tax-profile [{:keys [params] :as ctx}]
  (let [user-id (get-user-id-from-session ctx)
        tp      (get-tax-profile ctx user-id)
        tp-id   (:xt/id tp)]
    (if tp
      (do (logger/info "Updating tax profile...")
          (biff/submit-tx ctx
                          [{:db/doc-type :tax-profile
                            :xt/id tp-id
                            :db/op :update
                            :tax-profile/medical-monthly    (or (utilities/->int (:medical-monthly params)) 0)
                            :tax-profile/medical-dependants (or (utilities/->int (:medical-dependants params)) 0)
                            :tax-profile/ra-annual          (or (utilities/->int (:ra-annual params)) 0)}]))
      (logger/info "Tax profile not found"))))

(defn get-medical-budget-item [{:keys [biff/db]} user-id]
  (first (q db
            '{:find (pull budget-item [*])
              :in [user-id]
              :where [[budget-item :budget-item/user-id user-id]
                      [budget-item :budget-item/title "Medical Aid"]
                      (not [budget-item :budget-item/business-id])]}
            user-id)))

(defn get-retirement-budget-item [{:keys [biff/db]} user-id]
  (first (q db
            '{:find (pull budget-item [*])
              :in [user-id]
              :where [[budget-item :budget-item/user-id user-id]
                      [budget-item :budget-item/title "Annual RA Contributions"]
                      (not [budget-item :budget-item/business-id])]}
            user-id)))

(defn sync-medical-budget-item
  "Keeps a 'Medical Aid' expense budget item in sync with the tax profile's
   monthly medical contribution — mirroring how 'Salary' tracks income.
   Creates it when a contribution is entered, updates it when it changes,
   and removes it when set back to 0."
  [{:keys [params] :as ctx}]
  (let [user-id  (get-user-id-from-session ctx)
        medical  (or (utilities/->int (:medical-monthly params)) 0)
        existing (get-medical-budget-item ctx user-id)]
    (cond
      (and (pos? medical) existing)
      (do (logger/info "Updating Medical Aid budget item...")
          (biff/submit-tx ctx
                          [{:db/doc-type :budget-item
                            :xt/id (:xt/id existing)
                            :db/op :update
                            :budget-item/title "Medical Aid"
                            :budget-item/type :expenses
                            :budget-item/amount medical}]))

      (pos? medical)
      (do (logger/info "Creating Medical Aid budget item...")
          (biff/submit-tx ctx
                          [{:db/doc-type :budget-item
                            :xt/id (java.util.UUID/randomUUID)
                            :budget-item/user-id user-id
                            :budget-item/title "Medical Aid"
                            :budget-item/type :expenses
                            :budget-item/amount medical}]))

      existing
      (do (logger/info "Removing Medical Aid budget item (contribution set to 0)...")
          (biff/submit-tx ctx
                          [{:db/doc-type :budget-item
                            :xt/id (:xt/id existing)
                            :db/op :delete}])))))

(defn sync-retirment-budget-item
  ""
  [{:keys [params] :as ctx}]
  (let [user-id (get-user-id-from-session ctx)
        ra-annual (or (utilities/->int (:ra-annual params)) 0)
        existing (get-retirement-budget-item ctx user-id)]
    (cond
      (and (pos? ra-annual) existing)
      (do (logger/info "Updating Annual RA Contributions budget item...")
          (biff/submit-tx ctx
                          [{:db/doc-type :budget-item
                            :xt/id (:xt/id existing)
                            :db/op :update
                            :budget-item/title "Annual RA Contributions"
                            :budget-item/type :expenses
                            :budget-item/amount ra-annual}]))

      (pos? ra-annual)
      (do (logger/info "Creating Annual RA Contributions budget item...")
          (biff/submit-tx ctx
                          [{:db/doc-type :budget-item
                            :xt/id (java.util.UUID/randomUUID)
                            :budget-item/user-id user-id
                            :budget-item/title "Annual RA Contributions"
                            :budget-item/type :expenses
                            :budget-item/amount ra-annual}]))

      existing
      (do (logger/info "Removing Annual RA Contributions budget item (contribution set to 0)...")
          (biff/submit-tx ctx
                          [{:db/doc-type :budget-item
                            :xt/id (:xt/id existing)
                            :db/op :delete}])))))

(defn- can-change-owned?
  "A business's record can be changed by any member of the business; a
   personal one only by its owner."
  [{:keys [session] :as ctx} user-id business-id]
  (if business-id
    (some? (get-user-business ctx (:uid session) business-id))
    (owned-by-session? ctx user-id)))

(defn get-events
  "Calendar events for an owner (a user id for their personal side, or an
   owner map, see `owner`), by date."
  [{:keys [biff/db]} o]
  (let [{:keys [user-id business-id]} (->owner o)]
    (sort-by (fn [event] (or (:event/date event) ""))
             (if business-id
               (q db
                  '{:find (pull event [*])
                    :in [business-id]
                    :where [[event :event/business-id business-id]]}
                  business-id)
               (q db
                  '{:find (pull event [*])
                    :in [user-id]
                    :where [[event :event/user-id user-id]
                            (not [event :event/business-id])]}
                  user-id)))))

(defn create-event
  "Adds an event for whoever the person is working on: their business while
   one is chosen, else themselves."
  [{:keys [params] :as ctx}]
  (let [{:keys [user-id business-id]} (owner ctx)
        type    (or (some-> (:type params) utilities/->keyword) :todo)
        date    (not-empty (:date params))]
    (logger/info "Creating event...")
    (biff/submit-tx ctx
                    [(cond-> {:db/doc-type :event
                              :xt/id (java.util.UUID/randomUUID)
                              :event/user-id user-id
                              :event/title (:title params)
                              :event/date date
                              :event/type type
                              :event/done false}
                       business-id (assoc :event/business-id business-id))])))

(defn get-event [{:keys [biff/db]} event-id]
  (first (q db
            '{:find (pull event [*])
              :in [event-id]
              :where [[event :xt/id event-id]]}
            event-id)))

(defn toggle-event [{:keys [params] :as ctx}]
  (let [event-id (utilities/->uuid (:event-id params))
        event    (get-event ctx event-id)]
    (if (and event (can-change-owned? ctx (:event/user-id event) (:event/business-id event)))
      (do (logger/info "Toggling event...")
          (biff/submit-tx ctx
                          [{:db/doc-type :event
                            :xt/id event-id
                            :db/op :update
                            :event/done (not (:event/done event))}]))
      (logger/info "Event not found or unauthorized"))))

(defn delete-event [{:keys [params] :as ctx}]
  (let [event-id (utilities/->uuid (:event-id params))
        event    (get-event ctx event-id)]
    (if (and event (can-change-owned? ctx (:event/user-id event) (:event/business-id event)))
      (do (logger/info "Deleting event...")
          (biff/submit-tx ctx
                          [{:db/doc-type :event
                            :xt/id event-id
                            :db/op :delete}]))
      (logger/info "Event not found or unauthorized"))))

(def auto-titles
  "Budget items greed keeps in step with Settings. Only their statement
   reference can be changed from Finances."
  #{"Salary" "Medical Aid" "Annual RA Contributions"})

(defn ->reference
  "A statement reference from a form: trimmed, at most 200 characters, nil
   when blank."
  [s]
  (let [s (str/trim (str s))]
    (when-not (str/blank? s) (subs s 0 (min 200 (count s))))))

(defn ->vat-type
  "A VAT type from a form (standard, zero, none), or nil."
  [s]
  (#{:standard :zero :none} (keyword (str s))))

(declare get-business-members)

(defn ->member-id
  "A member of the business's user id from a form, or nil."
  [ctx business-id s]
  (when-let [id (parse-uuid (str s))]
    (when (some #(= id (:business-member/user-id %)) (get-business-members ctx business-id))
      id)))

(defn ->category
  "A spending category from a form, or nil when blank or unknown. Business
   categories (SARS, salaries, stock...) are only for a business's items."
  [s business?]
  (let [k (keyword (str s))]
    (when (and (some #{k} categorise/spending-categories)
               (or business? (not (categorise/business-only k))))
      k)))

(defn can-change-budget-item?
  "A business's item can be changed by any member of the business; a
   personal item only by its owner."
  [{:keys [session] :as ctx} {:budget-item/keys [user-id business-id]}]
  (if business-id
    (some? (get-user-business ctx (:uid session) business-id))
    (owned-by-session? ctx user-id)))

(defn upsert-budget-item
  "Adds a budget item for whoever the person is working on: their business
   while one is chosen, else themselves."
  [{:keys [params] :as ctx}]
  (let [{:keys [user-id business-id]} (owner ctx)
        budget-item-id (java.util.UUID/randomUUID)]
    (logger/info "Creating budget item...")
    (biff/submit-tx ctx
                    [(cond-> {:db/doc-type :budget-item
                              :xt/id budget-item-id
                              :budget-item/user-id user-id
                              :budget-item/title (:title params)
                              :budget-item/type (utilities/->keyword (:type params))
                              :budget-item/amount (validation/->valid-amount (:amount params))}
                       business-id (assoc :budget-item/business-id business-id)
                       (->category (:category params) business-id)
                       (assoc :budget-item/category (->category (:category params) business-id))
                       (and business-id (->vat-type (:vat params)))
                       (assoc :budget-item/vat (->vat-type (:vat params)))
                       (and business-id (->member-id ctx business-id (:paid-to params)))
                       (assoc :budget-item/paid-to (->member-id ctx business-id (:paid-to params)))
                       (->reference (:reference params))
                       (assoc :budget-item/reference (->reference (:reference params)))
                       ;; set by Spending's Add to income / Add to expenses
                       (re-matches #"\d{4}-\d{2}" (str (:from-statement params)))
                       (assoc :budget-item/from-statement (:from-statement params)))])))

(defn update-budget-item [{:keys [params] :as ctx}]
  (let [{:keys [budget-item-id]} params
        budget-item-id (utilities/->uuid budget-item-id)
        {:budget-item/keys [type title amount business-id]
         :as budget-item} (get-budget-item ctx budget-item-id)
        ;; only personal items are kept in step with Settings, or with a business
        auto? (and (not business-id)
                   (or (contains? auto-titles title) (:budget-item/source-business-id budget-item)))]
    (if (and budget-item (can-change-budget-item? ctx budget-item))
      (do
        (logger/info "Updating budget item...")
        (biff/submit-tx ctx
                        [{:db/doc-type :budget-item
                          :xt/id budget-item-id
                          :db/op :update
                          ;; Items kept in step with Settings keep their title and amount.
                          :budget-item/title (if (or auto? (str/blank? (:title params))) title (:title params))
                          :budget-item/type type
                          :budget-item/amount (if (or auto? (str/blank? (str (:amount params))))
                                                amount
                                                (validation/->valid-amount (:amount params)))
                          :budget-item/reference (or (->reference (:reference params)) :db/dissoc)
                          :budget-item/category (or (->category (:category params) business-id) :db/dissoc)
                          :budget-item/vat (or (when business-id (->vat-type (:vat params))) :db/dissoc)
                          :budget-item/paid-to (or (when business-id (->member-id ctx business-id (:paid-to params))) :db/dissoc)}]))
      (logger/info "Budget item not found or unauthorized"))))

(defn delete-budget-item [{:keys [params] :as ctx}]
  (let [{:keys [budget-item-id]} params
        budget-item-id (utilities/->uuid budget-item-id)
        budget-item (get-budget-item ctx budget-item-id)]
    ;; an item kept in step with a business is removed from the business side
    (if (and budget-item (can-change-budget-item? ctx budget-item)
             (not (:budget-item/source-business-id budget-item)))
      (do (logger/info "Deleting budget item...")
          (biff/submit-tx ctx
                          [{:db/doc-type :budget-item
                            :xt/id budget-item-id
                            :db/op :delete}]))
      (logger/info "Budget item not found or unauthorized"))))

;; ---------------------------------------------------------------------------
;; Statement summaries (Spending tab). Totals per uploaded statement; the
;; statement's individual lines are never stored.
;; ---------------------------------------------------------------------------

(defn- newest-first [summaries]
  (vec (sort-by (juxt #(str (:statement-summary/month %)) :statement-summary/period-end :statement-summary/bank)
                #(compare %2 %1)
                summaries)))

(defn get-statement-summaries
  "Monthly statement summaries for an owner (a user id for their personal
   side, or an owner map, see `owner`), newest month first."
  [{:keys [biff/db]} o]
  (let [{:keys [user-id business-id]} (->owner o)]
    (newest-first
     (if business-id
       (q db
          '{:find (pull s [*])
            :in [business-id]
            :where [[s :statement-summary/business-id business-id]]}
          business-id)
       (q db
          '{:find (pull s [*])
            :in [user-id]
            :where [[s :statement-summary/user-id user-id]
                    (not [s :statement-summary/business-id])]}
          user-id)))))

(defn- find-statement-summary
  "The owner's existing summary for the same bank and month, if any."
  [ctx o {:keys [bank month]}]
  (some #(when (and (= bank (:statement-summary/bank %)) (= month (:statement-summary/month %))) %)
        (get-statement-summaries ctx o)))

(defn- covered-days [start end]
  (inc (.between java.time.temporal.ChronoUnit/DAYS
                 (java.time.LocalDate/parse start) (java.time.LocalDate/parse end))))

(defn save-statement-summaries
  "Saves monthly summaries (see com.greed.statements.summary/summarise-months)
   for the user. Each replaces the saved one for the same bank and month,
   unless the saved one covers more days of that month (a full month is not
   replaced by the few days at the edge of a later statement). Returns the
   number of months saved."
  [ctx o summaries]
  (let [{:keys [user-id business-id]} (->owner o)
        docs (keep (fn [{:keys [period-start period-end] :as summary}]
                     (let [old (find-statement-summary ctx o summary)]
                       (when (or (nil? old)
                                 (>= (covered-days period-start period-end)
                                     (covered-days (:statement-summary/period-start old)
                                                   (:statement-summary/period-end old))))
                         (into (cond-> {:db/doc-type :statement-summary
                                        :xt/id (or (:xt/id old) (java.util.UUID/randomUUID))
                                        :statement-summary/user-id user-id
                                        :statement-summary/uploaded-at (java.util.Date.)}
                                 business-id (assoc :statement-summary/business-id business-id))
                               (map (fn [[k v]] [(keyword "statement-summary" (name k)) v]))
                               summary))))
                   summaries)]
    (logger/info "Saving statement summaries...")
    (when (seq docs) (biff/submit-tx ctx (vec docs)))
    (count docs)))

(defn budget-match-items
  "The owner's expense and savings (set aside) items that have a statement
   reference, in the shape com.greed.statements.summary/budget-totals takes."
  [ctx o]
  (vec (for [{:budget-item/keys [title type reference category] :xt/keys [id]} (get-owner-budget-items ctx o)
             :when (and reference (#{:expenses :savings} type))]
         (cond-> {:id id :title title :reference reference}
           category (assoc :category category)))))

(defn statement-origins
  "Budget items that were added from a bank statement before items recorded
   it, worked out from the saved months: an item counts when its title is a
   payee's (or payer's) name in a month, its amount is that month's amount,
   and, for an expense, its statement reference is its own title (Add to
   expenses set both). Returns {item-id \"yyyy-MM\"}, the newest month that
   fits. Planned items, auto items and items already tagged are left out."
  [items summaries]
  (let [norm      (comp str/lower-case str/trim str)
        candidate (fn [{:budget-item/keys [type title reference from-statement source-business-id]}]
                    (and (nil? from-statement) (nil? source-business-id)
                         (not (contains? auto-titles title))
                         (case type
                           :expenses (= (norm title) (norm reference))
                           :income true
                           false)))
        ;; [month name rands] for every payee and payer of every saved month
        seen (for [{:statement-summary/keys [month payees income-payees]} summaries
                   :when month
                   row (concat
                        (for [{:keys [name text spent-cents refund-cents]} payees
                              nm (distinct (remove str/blank? [name (summary/merchant-name text)]))]
                          [:expenses month nm (Math/round (/ (- spent-cents refund-cents) 100.0))])
                        (for [{:keys [name cents]} income-payees]
                          [:income month name (Math/round (/ cents 100.0))]))]
               row)]
    (into {}
          (for [{:budget-item/keys [type title amount] :xt/keys [id] :as item} items
                :when (candidate item)
                :let [months (for [[t month nm rands] seen
                                   :when (and (= t type) (= (norm nm) (norm title)) (= rands amount))]
                               month)]
                :when (seq months)]
            [id (last (sort months))]))))

(defn tag-statement-origins!
  "Tags the owner's budget items that came from a bank statement before
   items recorded it (see statement-origins). Returns how many were tagged."
  [ctx o]
  (let [ctx  (biff/assoc-db ctx)
        tags (statement-origins (get-owner-budget-items ctx o) (get-statement-summaries ctx o))]
    (when (seq tags)
      (logger/info (str "Tagging " (count tags) " budget items added from statements..."))
      (biff/submit-tx ctx (for [[id month] tags]
                            {:db/doc-type :budget-item :xt/id id :db/op :update
                             :budget-item/from-statement month})))
    (count tags)))

(defn sync-statement-summaries
  "Re-matches every saved month against the budget's current statement
   references, using the payee totals kept with each month. Months saved
   before payee totals were kept cannot be matched and are left as they are.
   Returns {:synced n :needs-upload n}."
  [ctx o]
  (let [ctx       (biff/assoc-db ctx)
        items     (budget-match-items ctx o)
        summaries (get-statement-summaries ctx o)
        [with without] ((juxt filter remove) :statement-summary/payees summaries)
        docs      (for [{:statement-summary/keys [payees] :xt/keys [id]} with
                        :let [{:keys [budget-matches budgeted-cents categories]} (summary/budget-totals items payees)]]
                    {:db/doc-type :statement-summary
                     :xt/id id
                     :db/op :update
                     :statement-summary/budget-matches budget-matches
                     :statement-summary/budgeted-cents budgeted-cents
                     :statement-summary/categories categories})]
    (when (seq docs)
      (logger/info "Syncing statement summaries with the budget...")
      (biff/submit-tx ctx (vec docs)))
    ;; items added from statements before they were tagged get their tag
    (tag-statement-origins! ctx o)
    {:synced (count docs) :needs-upload (count without)}))

(defn delete-statement-summary [{:keys [params] :as ctx}]
  (let [id (utilities/->uuid (:summary-id params))
        {:statement-summary/keys [user-id business-id] :as s}
        (when id (first (q (:biff/db ctx)
                           '{:find (pull s [*])
                             :in [id]
                             :where [[s :xt/id id] [s :statement-summary/user-id]]}
                           id)))]
    (if (and s (if business-id
                 (some? (get-user-business ctx (get-in ctx [:session :uid]) business-id))
                 (owned-by-session? ctx user-id)))
      (do (logger/info "Deleting statement summary...")
          (biff/submit-tx ctx [{:db/doc-type :statement-summary :xt/id id :db/op :delete}]))
      (logger/info "Statement summary not found or unauthorized"))))

;; ---------------------------------------------------------------------------
;; Goals
;; ---------------------------------------------------------------------------

(defn get-goals
  "Goals for an owner (a user id for their personal side, or an owner map)."
  [{:keys [biff/db]} o]
  (let [{:keys [user-id business-id]} (->owner o)]
    (if business-id
      (q db
         '{:find (pull goal [*])
           :in [business-id]
           :where [[goal :goal/business-id business-id]]}
         business-id)
      (q db
         '{:find (pull goal [*])
           :in [user-id]
           :where [[goal :goal/user-id user-id]
                   (not [goal :goal/business-id])]}
         user-id))))

(defn get-goal [{:keys [biff/db]} goal-id]
  (first (q db
            '{:find (pull goal [*])
              :in [goal-id]
              :where [[goal :xt/id goal-id]]}
            goal-id)))

(defn upsert-goal
  "Adds a goal for whoever the person is working on: their business while
   one is chosen, else themselves."
  [{:keys [params] :as ctx}]
  (let [{:keys [user-id business-id]} (owner ctx)
        target-date (:target-date params)]
    (logger/info "Creating goal...")
    (biff/submit-tx ctx
                    [(cond-> {:db/doc-type :goal
                              :xt/id (java.util.UUID/randomUUID)
                              :goal/user-id user-id
                              :goal/title (:title params)
                              :goal/target (validation/->valid-amount (:target params))
                              :goal/saved (or (utilities/->int (:saved params)) 0)}
                       business-id (assoc :goal/business-id business-id)
                       (not (str/blank? target-date))
                       (assoc :goal/target-date target-date))])))

(defn update-goal [{:keys [params] :as ctx}]
  (let [goal-id     (utilities/->uuid (:goal-id params))
        goal        (get-goal ctx goal-id)
        target-date (:target-date params)]
    (if (and goal (can-change-owned? ctx (:goal/user-id goal) (:goal/business-id goal)))
      (do (logger/info "Updating goal...")
          (biff/submit-tx ctx
                          [(cond-> {:db/doc-type :goal
                                    :xt/id goal-id
                                    :db/op :update
                                    :goal/title (:title params)
                                    :goal/target (validation/->valid-amount (:target params))
                                    :goal/saved (or (utilities/->int (:saved params)) 0)}
                             (not (str/blank? target-date))
                             (assoc :goal/target-date target-date))]))
      (logger/info "Goal not found or unauthorized"))))

(defn delete-goal [{:keys [params] :as ctx}]
  (let [goal-id (utilities/->uuid (:goal-id params))
        goal    (get-goal ctx goal-id)]
    (if (and goal (can-change-owned? ctx (:goal/user-id goal) (:goal/business-id goal)))
      (do (logger/info "Deleting goal...")
          (biff/submit-tx ctx
                          [{:db/doc-type :goal
                            :xt/id goal-id
                            :db/op :delete}]))
      (logger/info "Goal not found or unauthorized"))))

(defn delete-user
  "Permanently deletes the user with the given id and all of their data
  (finances, budget items, tax profile, events, goals, statement
  summaries, business memberships, and businesses they were the only member
  of). Returns the number
  of docs deleted (including the user itself), or nil if no user exists
  with that id."
  [ctx user-id]
  (let [{:keys [biff/db]} ctx
        user-doc (q db '{:find [id]
                         :in [user-id]
                         :where [[id :xt/id user-id]]}
                    user-id)]
    (when (seq user-doc)
      (let [owned (q db
                     '{:find [id]
                       :in [user-id]
                       :where [(or [id :finances/user-id user-id]
                                   [id :budget-item/user-id user-id]
                                   [id :tax-profile/user-id user-id]
                                   [id :event/user-id user-id]
                                   [id :goal/user-id user-id]
                                   [id :statement-summary/user-id user-id]
                                   [id :business-member/user-id user-id]
                                   [id :session/uid user-id])]}
                     user-id)
            ;; businesses with no other member go too
            sole (q db
                    '{:find [b]
                      :in [user-id]
                      :where [[m :business-member/user-id user-id]
                              [m :business-member/business-id b]
                              (not-join [b user-id]
                                        [m2 :business-member/business-id b]
                                        [m2 :business-member/user-id other]
                                        [(not= other user-id)])]}
                    user-id)
            sole  (into #{} (map first) sole)
            ;; and the budget items and statement months of those businesses
            sole-data (q db
                         '{:find [id]
                           :in [[b ...]]
                           :where [(or [id :budget-item/business-id b]
                                       [id :statement-summary/business-id b]
                                       [id :business-member/business-id b]
                                       [id :business-invite/business-id b]
                                       [id :goal/business-id b]
                                       [id :event/business-id b])]}
                         (vec sole))
            ;; what they added to a business someone else is still in stays
            shared (q db
                      '{:find [id b]
                        :in [user-id]
                        :where [(or (and [id :budget-item/user-id user-id] [id :budget-item/business-id b])
                                    (and [id :statement-summary/user-id user-id] [id :statement-summary/business-id b])
                                    (and [id :goal/user-id user-id] [id :goal/business-id b])
                                    (and [id :event/user-id user-id] [id :event/business-id b]))]}
                      user-id)
            keep-ids (into #{} (keep (fn [[id b]] (when-not (sole b) id))) shared)
            owned (-> (into #{} (map first) owned)
                      (into sole)
                      (into (map first) sole-data)
                      (conj user-id)
                      (->> (remove keep-ids) set))]
        (logger/info (str "Deleting user " user-id " (" (count owned) " docs)..."))
        (biff/submit-tx ctx
                        (for [id owned]
                          {:db/doc-type :user
                           :db/op :delete
                           :xt/id id}))
        (count owned)))))

(def entity-types
  "Business entity types, in the order the sign-up form lists them."
  (array-map :sole-proprietor "Sole proprietor"
             :pty-ltd         "(Pty) Ltd"
             :partnership     "Partnership"
             :npc             "Non-profit company (NPC)"
             :trust           "Trust"))

(defn business-problem
  "Why the business details in a sign-up are not usable, or nil."
  [{:keys [business-name entity-type]}]
  (cond
    (str/blank? business-name) :business-name-blank
    (> (count (str/trim business-name)) 120) :business-name-too-long
    (not (contains? entity-types (keyword entity-type))) :entity-type-invalid))

(defn create-business!
  "Creates a business with `user-id` as its owner. Returns the business id."
  [ctx user-id {:keys [business-name entity-type]}]
  (let [business-id (java.util.UUID/randomUUID)
        now (java.util.Date.)]
    (logger/info "Creating business...")
    (biff/submit-tx ctx
                    [{:db/doc-type :business
                      :xt/id business-id
                      :business/name (str/trim business-name)
                      :business/entity-type (keyword entity-type)
                      :business/created-at now}
                     {:db/doc-type :business-member
                      :xt/id (java.util.UUID/randomUUID)
                      :business-member/business-id business-id
                      :business-member/user-id user-id
                      :business-member/role :owner
                      :business-member/created-at now}])
    business-id))

(defn get-user-businesses
  "The businesses the user is a member of, by name, each with :business-member/role."
  [{:keys [biff/db]} user-id]
  (->> (q db
          '{:find [(pull b [*]) role]
            :in [user-id]
            :where [[m :business-member/user-id user-id]
                    [m :business-member/business-id b]
                    [m :business-member/role role]]}
          user-id)
       (map (fn [[b role]] (assoc b :business-member/role role)))
       (sort-by (comp str/lower-case :business/name))
       vec))

(defn get-user-business
  "The business with `business-id` if the user is a member of it, else nil."
  [ctx user-id business-id]
  (when business-id
    (some #(when (= business-id (:xt/id %)) %) (get-user-businesses ctx user-id))))

(defn session-business
  "The business the signed-in person is working in (chosen after sign-in),
   or nil for their personal side. A business they are no longer a member
   of counts as nil."
  [{:keys [session] :as ctx}]
  (get-user-business ctx (:uid session) (:business-id session)))

;; ---------------------------------------------------------------------------
;; Business members and invitations
;; ---------------------------------------------------------------------------

(def roles
  "Member roles, with what each can do."
  (array-map :member "Member" :owner "Owner"))

(def invite-days
  "How long an invitation can be accepted for."
  14)

(defn member-role
  "The user's role in the business (:owner or :member), or nil."
  [ctx user-id business-id]
  (:business-member/role (get-user-business ctx user-id business-id)))

(defn business-owner? [ctx user-id business-id]
  (= :owner (member-role ctx user-id business-id)))

(defn get-business-members
  "A business's members with their names and emails, owners first."
  [{:keys [biff/db]} business-id]
  (->> (q db
          '{:find [(pull m [*]) (pull u [:xt/id :user/firstname :user/lastname :user/email])]
            :in [business-id]
            :where [[m :business-member/business-id business-id]
                    [m :business-member/user-id u]]}
          business-id)
       (map (fn [[m u]] (assoc m :user u)))
       (sort-by (juxt #(if (= :owner (:business-member/role %)) 0 1)
                      #(str/lower-case (str (get-in % [:user :user/firstname])))))
       vec))

(defn- owner-count [ctx business-id]
  (count (filter #(= :owner (:business-member/role %)) (get-business-members ctx business-id))))

(defn- invite-live?
  "Not yet expired."
  [{:business-invite/keys [created-at]}]
  (.isAfter (.plus (.toInstant ^java.util.Date created-at) (java.time.Duration/ofDays invite-days))
            (java.time.Instant/now)))

(defn get-business-invites
  "A business's invitations that can still be accepted, newest first."
  [{:keys [biff/db]} business-id]
  (->> (q db
          '{:find (pull i [*])
            :in [business-id]
            :where [[i :business-invite/business-id business-id]]}
          business-id)
       (filter invite-live?)
       (sort-by :business-invite/created-at #(compare %2 %1))
       vec))

(defn get-user-invites
  "Invitations waiting for the user (matched on their email), each with the
   business and who sent it, for businesses they are not in yet."
  [{:keys [biff/db] :as ctx} user-id]
  (let [email (some-> (get-user ctx user-id) :user/email str/lower-case)]
    (->> (when email
           (q db
              '{:find [(pull i [*]) (pull b [*]) (pull u [:user/firstname :user/lastname])]
                :in [email]
                :where [[i :business-invite/email email]
                        [i :business-invite/business-id b]
                        [i :business-invite/invited-by u]]}
              email))
         (filter (fn [[i b]] (and (invite-live? i) (:business/name b)
                                  (nil? (get-user-business ctx user-id (:xt/id b))))))
         (map (fn [[i b u]] (assoc i :business b :inviter u)))
         (sort-by :business-invite/created-at #(compare %2 %1))
         vec)))

(defn- email? [s]
  (boolean (re-matches #"[^\s@]+@[^\s@]+\.[^\s@]+" (str s))))

(defn invite-member!
  "An owner invites someone by email. Returns {:invite doc} or {:error kw}:
   :not-owner, :invalid-email, :invalid-role, :already-member,
   :already-invited."
  [ctx user-id business-id {:keys [email role]}]
  (let [email (str/lower-case (str/trim (str email)))
        role  (keyword (str role))
        members (get-business-members ctx business-id)]
    (cond
      (not (business-owner? ctx user-id business-id)) {:error :not-owner}
      (not (email? email)) {:error :invalid-email}
      (not (contains? roles role)) {:error :invalid-role}
      (some #(= email (some-> % :user :user/email str/lower-case)) members) {:error :already-member}
      (some #(= email (:business-invite/email %)) (get-business-invites ctx business-id)) {:error :already-invited}
      :else
      (let [invite {:db/doc-type :business-invite
                    :xt/id (java.util.UUID/randomUUID)
                    :business-invite/business-id business-id
                    :business-invite/email email
                    :business-invite/role role
                    :business-invite/invited-by user-id
                    :business-invite/created-at (java.util.Date.)}]
        (logger/info "Inviting a business member...")
        (biff/submit-tx ctx [invite])
        {:invite invite}))))

(defn- get-invite [{:keys [biff/db]} invite-id]
  (when invite-id
    (let [i (xt/entity db invite-id)]
      (when (:business-invite/business-id i) i))))

(defn- invite-for-user
  "The invitation if it is addressed to this user's email and still live."
  [ctx user-id invite-id]
  (let [invite (get-invite ctx invite-id)
        email  (some-> (get-user ctx user-id) :user/email str/lower-case)]
    (when (and invite email (= email (:business-invite/email invite)) (invite-live? invite))
      invite)))

(defn accept-invite!
  "The invited person joins the business. Only the person signed in with the
   invited email can accept. Returns the business id, or nil."
  [ctx user-id invite-id]
  (when-let [{:business-invite/keys [business-id role] :as invite} (invite-for-user ctx user-id invite-id)]
    (logger/info "Accepting a business invitation...")
    (biff/submit-tx ctx
                    (cond-> [{:db/doc-type :business-invite :xt/id (:xt/id invite) :db/op :delete}]
                      (nil? (get-user-business ctx user-id business-id))
                      (conj {:db/doc-type :business-member
                             :xt/id (java.util.UUID/randomUUID)
                             :business-member/business-id business-id
                             :business-member/user-id user-id
                             :business-member/role role
                             :business-member/created-at (java.util.Date.)})))
    (sync-owner-pay! ctx business-id)
    business-id))

(defn decline-invite!
  "The invited person turns the invitation down. Returns true if it was theirs."
  [ctx user-id invite-id]
  (when-let [invite (invite-for-user ctx user-id invite-id)]
    (biff/submit-tx ctx [{:db/doc-type :business-invite :xt/id (:xt/id invite) :db/op :delete}])
    true))

(defn cancel-invite!
  "An owner withdraws an invitation. Returns true if they could."
  [ctx user-id invite-id]
  (when-let [{:business-invite/keys [business-id] :as invite} (get-invite ctx invite-id)]
    (when (business-owner? ctx user-id business-id)
      (biff/submit-tx ctx [{:db/doc-type :business-invite :xt/id (:xt/id invite) :db/op :delete}])
      true)))

(defn- get-member [{:keys [biff/db]} member-id]
  (when member-id
    (let [m (xt/entity db member-id)]
      (when (:business-member/business-id m) m))))

(defn change-member-role!
  "An owner changes a member's role. The last owner cannot stop being one.
   Returns :ok or an error keyword (:not-owner, :invalid-role, :last-owner)."
  [ctx user-id member-id role]
  (let [{:business-member/keys [business-id] :as m} (get-member ctx member-id)
        role (keyword (str role))]
    (cond
      (or (nil? m) (not (business-owner? ctx user-id business-id))) :not-owner
      (not (contains? roles role)) :invalid-role
      (and (= :owner (:business-member/role m)) (not= :owner role) (= 1 (owner-count ctx business-id))) :last-owner
      :else (do (biff/submit-tx ctx [{:db/doc-type :business-member :xt/id member-id :db/op :update
                                      :business-member/role role}])
                (sync-owner-pay! ctx business-id)
                :ok))))

(defn remove-member!
  "An owner removes a member, or a member leaves (removes themselves). The
   last owner cannot go. Returns :ok or :not-allowed or :last-owner."
  [ctx user-id member-id]
  (let [{:business-member/keys [business-id] :as m} (get-member ctx member-id)
        self? (= user-id (:business-member/user-id m))]
    (cond
      (or (nil? m) (not (or self? (business-owner? ctx user-id business-id)))) :not-allowed
      (and (= :owner (:business-member/role m)) (= 1 (owner-count ctx business-id))) :last-owner
      :else (do (biff/submit-tx ctx [{:db/doc-type :business-member :xt/id member-id :db/op :delete}])
                (sync-owner-pay! ctx business-id)
                :ok))))

(defn update-business!
  "An owner renames the business or changes its type. Returns :ok, :not-owner
   or a problem from `business-problem`."
  [ctx user-id business-id {:keys [business-name entity-type] :as params}]
  (cond
    (not (business-owner? ctx user-id business-id)) :not-owner
    (business-problem params) (business-problem params)
    :else (do (biff/submit-tx ctx [{:db/doc-type :business :xt/id business-id :db/op :update
                                    :business/name (str/trim business-name)
                                    :business/entity-type (keyword entity-type)}])
              (sync-owner-pay! ctx business-id)
              :ok)))

(defn update-vat-settings!
  "An owner sets whether the business is a VAT vendor, its VAT number and
   tax period category. Returns :ok, :not-owner, :vat-number-invalid or
   :vat-category-invalid."
  [ctx user-id business-id {:keys [vat-registered vat-number vat-category]}]
  (let [registered? (= "true" (str vat-registered))
        number      (str/replace (str vat-number) #"\s" "")
        cat         (#{:a :b :c} (keyword (str vat-category)))]
    (cond
      (not (business-owner? ctx user-id business-id)) :not-owner
      (and registered? (not (re-matches #"4\d{9}" number))) :vat-number-invalid
      (and registered? (nil? cat)) :vat-category-invalid
      :else (do (biff/submit-tx ctx [(if registered?
                                       {:db/doc-type :business :xt/id business-id :db/op :update
                                        :business/vat-registered true
                                        :business/vat-number number
                                        :business/vat-category cat}
                                       {:db/doc-type :business :xt/id business-id :db/op :update
                                        :business/vat-registered false
                                        :business/vat-number :db/dissoc
                                        :business/vat-category :db/dissoc})])
                :ok))))

(defn update-tax-settings!
  "An owner sets the month the financial year ends and whether the business
   is a small business corporation. Returns :ok, :not-owner or
   :year-end-invalid."
  [ctx user-id business-id {:keys [year-end-month sbc]}]
  (let [m (utilities/->int (str year-end-month))]
    (cond
      (not (business-owner? ctx user-id business-id)) :not-owner
      (not (and m (<= 1 m 12))) :year-end-invalid
      :else (do (biff/submit-tx ctx [{:db/doc-type :business :xt/id business-id :db/op :update
                                      :business/year-end-month m
                                      :business/sbc (= "true" (str sbc))}])
                :ok))))

(defn owner-pay
  "What a business's budget pays each person, in rands a month: its expense
   items in the Owner's drawings category, by who they are paid to. An item
   with no one chosen goes to the owner when there is only one.
   `members` are business-member docs."
  [items members]
  (let [ids    (set (map :business-member/user-id members))
        owners (filter #(= :owner (:business-member/role %)) members)
        single (when (= 1 (count owners)) (:business-member/user-id (first owners)))]
    (reduce (fn [m {:budget-item/keys [type category paid-to amount]}]
              (if-let [to (when (and (= :expenses type) (= :drawings category))
                            (or (ids paid-to) single))]
                (update m to (fnil + 0) amount)
                m))
            {}
            ;; a month's once-off from a statement is not monthly pay
            (monthly-items items))))

(defn profit-shares
  "Each owner's share of a business's profit, in percent: the shares set on
   their memberships, or an equal split. `members` are business-member docs."
  [members]
  (let [owners (filter #(= :owner (:business-member/role %)) members)]
    (if (some :business-member/profit-share owners)
      (into {} (map (juxt :business-member/user-id #(or (:business-member/profit-share %) 0))) owners)
      (into {} (map (fn [o] [(:business-member/user-id o) (/ 100.0 (count owners))])) owners))))

(defn sync-owner-pay!
  "Keeps each person's \"From <business>\" personal income item in step with
   what the business's budget pays them: created, changed, or removed when
   it pays them nothing (or the business is gone)."
  [ctx business-id]
  (let [{:keys [biff/db] :as ctx} (biff/assoc-db ctx)
        business (xt/entity db business-id)
        members  (when (:business/name business) (get-business-members ctx business-id))
        pay      (if members (owner-pay (get-business-budget-items ctx business-id) members) {})
        existing (into {} (map (juxt :budget-item/user-id identity))
                       (q db '{:find (pull i [*])
                               :in [b]
                               :where [[i :budget-item/source-business-id b]]}
                          business-id))
        docs     (for [uid (distinct (concat (keys pay) (keys existing)))
                       :let [amount (get pay uid 0)
                             old    (existing uid)]
                       :when (or (pos? amount) old)]
                   (if (pos? amount)
                     {:db/doc-type :budget-item
                      :xt/id (or (:xt/id old) (java.util.UUID/randomUUID))
                      :budget-item/user-id uid
                      :budget-item/title (str "From " (:business/name business))
                      :budget-item/type :income
                      :budget-item/amount amount
                      :budget-item/source-business-id business-id}
                     {:db/doc-type :budget-item :xt/id (:xt/id old) :db/op :delete}))]
    (when (seq docs)
      (logger/info "Syncing owner's pay to personal budgets...")
      (biff/submit-tx ctx (vec docs)))
    (count docs)))

(defn update-profit-shares!
  "An owner sets the owners' shares of the profit, in whole percent adding up
   to 100. `params` has :share-<member id> for each owner. Returns :ok,
   :not-owner or :shares-not-100."
  [ctx user-id business-id params]
  (let [owners (filter #(= :owner (:business-member/role %)) (get-business-members ctx business-id))
        shares (for [o owners]
                 [o (utilities/->int (str (get params (keyword (str "share-" (:xt/id o))))))])]
    (cond
      (not (business-owner? ctx user-id business-id)) :not-owner
      (or (some (fn [[_ n]] (not (and n (<= 0 n 100)))) shares)
          (not= 100 (reduce + 0 (map second shares)))) :shares-not-100
      :else (do (biff/submit-tx ctx (for [[o n] shares]
                                      {:db/doc-type :business-member :xt/id (:xt/id o) :db/op :update
                                       :business-member/profit-share n}))
                :ok))))

(defn delete-business!
  "An owner deletes the business with its members, invitations, budget items
   and statement months. Returns true if they could."
  [{:keys [biff/db] :as ctx} user-id business-id]
  (when (business-owner? ctx user-id business-id)
    (let [ids (map first (q db
                            '{:find [id]
                              :in [b]
                              :where [(or [id :business-member/business-id b]
                                          [id :business-invite/business-id b]
                                          [id :budget-item/business-id b]
                                          [id :budget-item/source-business-id b]
                                          [id :statement-summary/business-id b]
                                          [id :goal/business-id b]
                                          [id :event/business-id b])]}
                            business-id))]
      (logger/info "Deleting a business...")
      (biff/submit-tx ctx (for [id (cons business-id ids)]
                            {:db/doc-type :business :xt/id id :db/op :delete}))
      true)))

(defn add-business!
  "A signed-in person adds a business they run. Returns {:business-id id} or
   {:error problem}."
  [ctx user-id params]
  (if-let [problem (business-problem params)]
    {:error problem}
    {:business-id (create-business! ctx user-id params)}))

(defn admin-delete-user
  "Deletes the target user unless they're the signed-in admin themselves.
  Returns :self, :not-found, or the number of docs deleted."
  [{:keys [session params] :as ctx}]
  (let [user-id (utilities/->uuid (:user-id params))]
    (cond
      (= user-id (:uid session)) :self
      (nil? (get-user ctx user-id)) :not-found
      :else (delete-user ctx user-id))))
