(ns com.greed.data.core
  (:require [buddy.hashers :as hashers]
            [com.biffweb :as biff :refer [q]]
            [clojure.string :as str]
            [clojure.tools.logging :as logger]
            [com.greed.utilities.tax :as tax]
            [com.greed.utilities.core :as utilities]
            [com.greed.data.validation :as validation]
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

(defn get-budget-items [{:keys [biff/db]} user-id]
  (q db
     '{:find (pull budget-item [*])
       :in [user-id]
       :where [[budget-item :budget-item/user-id user-id]]}
     user-id))

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
                      [budget-item :budget-item/title "Salary"]]}
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

(defn upsert-user [{:keys [params] :as ctx}]
  (let [user-id (java.util.UUID/randomUUID)]
    (logger/info "Creating user...")
    (biff/submit-tx ctx
                    [{:db/doc-type :user
                      :xt/id user-id
                      :user/email (:email params)
                      :user/password (hash-password (:password params))
                      :user/firstname (:firstname params)
                      :user/lastname (:lastname params)
                      :user/age (utilities/->int (:age params))
                      :user/active true
                      :user/roles #{:user}}])))

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
                                  :user/age (utilities/->int (:age params))}]
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

(defn upsert-finances [{:keys [params] :as ctx}]
  (let [user-id (get-user-id-from-session ctx)
        {:user/keys [age]
         :or {age 21}}   (get-user ctx user-id)
        salary (utilities/->int (:salary params))
        annual-income (utilities/income->annual-income salary)
        {:keys [net-income]} (tax/calculate-income-tax annual-income age)]
    (logger/info "Creating finances...")
    (biff/submit-tx ctx
                    [{:db/doc-type :finances
                      :xt/id (java.util.UUID/randomUUID)
                      :finances/user-id user-id
                      :finances/bank (utilities/->keyword (:bank params))
                      :finances/account-type (not-empty (:account-type params))
                      :finances/salary (utilities/->int (:salary params))
                      :finances/payday (validation/->valid-payday (:payday params))}])
    (logger/info "Creating budget item...")
    (biff/submit-tx ctx
                    [{:db/doc-type :budget-item
                      :xt/id (java.util.UUID/randomUUID)
                      :budget-item/user-id user-id
                      :budget-item/title "Salary"
                      :budget-item/type :income
                      :budget-item/amount (-> net-income
                                              utilities/annual-income->monthly-income
                                              int)}])))

(defn update-finances [{:keys [params] :as ctx}]
  (let [user-id (get-user-id-from-session ctx)
        finances (get-finances ctx user-id)
        finances-id (:xt/id finances)
        salary-budget-item (get-salary-budget-item ctx user-id)
        salary-budget-item-id (:xt/id salary-budget-item)
        {:user/keys [age]
         :or {age 21}}   (get-user ctx user-id)
        salary (utilities/->int (:salary params))
        annual-income (utilities/income->annual-income salary)
        {:keys [net-income]} (tax/calculate-income-tax annual-income age)]
    (if (and finances salary-budget-item)
      (do (logger/info "Updating finances...")
          (biff/submit-tx ctx
                          [{:db/doc-type :finances
                            :xt/id finances-id
                            :db/op :update
                            :finances/bank (utilities/->keyword (:bank params))
                            :finances/account-type (not-empty (:account-type params))
                            :finances/salary (utilities/->int (:salary params))
                            :finances/payday (validation/->valid-payday (:payday params))
                            :finances/card-type :db/dissoc
                            :finances/title :db/dissoc
                            :finances/type :db/dissoc
                            :finances/amount :db/dissoc}])
          (logger/info "Updating budget item...")
          (biff/submit-tx ctx
                          [{:db/doc-type :budget-item
                            :xt/id salary-budget-item-id
                            :db/op :update
                            :budget-item/title "Salary"
                            :budget-item/type :income
                            :budget-item/amount (-> net-income
                                                    utilities/annual-income->monthly-income
                                                    int)}]))
      (logger/info "Finances not found"))))

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
                      [budget-item :budget-item/title "Medical Aid"]]}
            user-id)))

(defn get-retirement-budget-item [{:keys [biff/db]} user-id]
  (first (q db
            '{:find (pull budget-item [*])
              :in [user-id]
              :where [[budget-item :budget-item/user-id user-id]
                      [budget-item :budget-item/title "Annual RA Contributions"]]}
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

(defn get-events [{:keys [biff/db]} user-id]
  (sort-by (fn [event] (or (:event/date event) ""))
           (q db
              '{:find (pull event [*])
                :in [user-id]
                :where [[event :event/user-id user-id]]}
              user-id)))

(defn create-event [{:keys [params] :as ctx}]
  (let [user-id (get-user-id-from-session ctx)
        type    (or (some-> (:type params) utilities/->keyword) :todo)
        date    (not-empty (:date params))]
    (logger/info "Creating event...")
    (biff/submit-tx ctx
                    [{:db/doc-type :event
                      :xt/id (java.util.UUID/randomUUID)
                      :event/user-id user-id
                      :event/title (:title params)
                      :event/date date
                      :event/type type
                      :event/done false}])))

(defn get-event [{:keys [biff/db]} event-id]
  (first (q db
            '{:find (pull event [*])
              :in [event-id]
              :where [[event :xt/id event-id]]}
            event-id)))

(defn toggle-event [{:keys [params] :as ctx}]
  (let [event-id (utilities/->uuid (:event-id params))
        event    (get-event ctx event-id)]
    (if (and event (owned-by-session? ctx (:event/user-id event)))
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
    (if (and event (owned-by-session? ctx (:event/user-id event)))
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

(defn upsert-budget-item [{:keys [params] :as ctx}]
  (let [user-id (get-user-id-from-session ctx)
        budget-item-id (java.util.UUID/randomUUID)]
    (logger/info "Creating budget item...")
    (biff/submit-tx ctx
                    [(cond-> {:db/doc-type :budget-item
                              :xt/id budget-item-id
                              :budget-item/user-id user-id
                              :budget-item/title (:title params)
                              :budget-item/type (utilities/->keyword (:type params))
                              :budget-item/amount (validation/->valid-amount (:amount params))}
                       (->reference (:reference params))
                       (assoc :budget-item/reference (->reference (:reference params))))])))

(defn update-budget-item [{:keys [params] :as ctx}]
  (let [{:keys [budget-item-id]} params
        budget-item-id (utilities/->uuid budget-item-id)
        {:budget-item/keys [type user-id title amount]
         :as budget-item} (get-budget-item ctx budget-item-id)
        auto? (contains? auto-titles title)]
    (if (and budget-item (owned-by-session? ctx user-id))
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
                          :budget-item/reference (or (->reference (:reference params)) :db/dissoc)}]))
      (logger/info "Budget item not found or unauthorized"))))

(defn delete-budget-item [{:keys [params] :as ctx}]
  (let [{:keys [budget-item-id]} params
        budget-item-id (utilities/->uuid budget-item-id)
        {:budget-item/keys [user-id]
         :as budget-item} (get-budget-item ctx budget-item-id)]
    (if (and budget-item (owned-by-session? ctx user-id))
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

(defn get-statement-summaries
  "The user's monthly statement summaries, newest month first."
  [{:keys [biff/db]} user-id]
  (->> (q db
          '{:find (pull s [*])
            :in [user-id]
            :where [[s :statement-summary/user-id user-id]]}
          user-id)
       (sort-by (juxt #(str (:statement-summary/month %)) :statement-summary/period-end :statement-summary/bank)
                #(compare %2 %1))
       vec))

(defn- find-statement-summary
  "The user's existing summary for the same bank and month, if any."
  [{:keys [biff/db]} user-id {:keys [bank month]}]
  (first (q db
            '{:find (pull s [*])
              :in [user-id bank month]
              :where [[s :statement-summary/user-id user-id]
                      [s :statement-summary/bank bank]
                      [s :statement-summary/month month]]}
            user-id bank month)))

(defn- covered-days [start end]
  (inc (.between java.time.temporal.ChronoUnit/DAYS
                 (java.time.LocalDate/parse start) (java.time.LocalDate/parse end))))

(defn save-statement-summaries
  "Saves monthly summaries (see com.greed.statements.summary/summarise-months)
   for the user. Each replaces the saved one for the same bank and month,
   unless the saved one covers more days of that month (a full month is not
   replaced by the few days at the edge of a later statement). Returns the
   number of months saved."
  [ctx user-id summaries]
  (let [docs (keep (fn [{:keys [period-start period-end] :as summary}]
                     (let [old (find-statement-summary ctx user-id summary)]
                       (when (or (nil? old)
                                 (>= (covered-days period-start period-end)
                                     (covered-days (:statement-summary/period-start old)
                                                   (:statement-summary/period-end old))))
                         (into {:db/doc-type :statement-summary
                                :xt/id (or (:xt/id old) (java.util.UUID/randomUUID))
                                :statement-summary/user-id user-id
                                :statement-summary/uploaded-at (java.util.Date.)}
                               (map (fn [[k v]] [(keyword "statement-summary" (name k)) v]))
                               summary))))
                   summaries)]
    (logger/info "Saving statement summaries...")
    (when (seq docs) (biff/submit-tx ctx (vec docs)))
    (count docs)))

(defn budget-match-items
  "The user's expense and savings items that have a statement reference, in
   the shape com.greed.statements.summary/budget-totals takes."
  [ctx user-id]
  (vec (for [{:budget-item/keys [title type reference] :xt/keys [id]} (get-budget-items ctx user-id)
             :when (and reference (#{:expenses :savings} type))]
         {:id id :title title :reference reference})))

(defn sync-statement-summaries
  "Re-matches every saved month against the budget's current statement
   references, using the payee totals kept with each month. Months saved
   before payee totals were kept cannot be matched and are left as they are.
   Returns {:synced n :needs-upload n}."
  [ctx user-id]
  (let [ctx       (biff/assoc-db ctx)
        items     (budget-match-items ctx user-id)
        summaries (get-statement-summaries ctx user-id)
        [with without] ((juxt filter remove) :statement-summary/payees summaries)
        docs      (for [{:statement-summary/keys [payees] :xt/keys [id]} with
                        :let [{:keys [budget-matches budgeted-cents]} (summary/budget-totals items payees)]]
                    {:db/doc-type :statement-summary
                     :xt/id id
                     :db/op :update
                     :statement-summary/budget-matches budget-matches
                     :statement-summary/budgeted-cents budgeted-cents})]
    (when (seq docs)
      (logger/info "Syncing statement summaries with the budget...")
      (biff/submit-tx ctx (vec docs)))
    {:synced (count docs) :needs-upload (count without)}))

(defn delete-statement-summary [{:keys [params] :as ctx}]
  (let [id (utilities/->uuid (:summary-id params))
        {:statement-summary/keys [user-id] :as s}
        (when id (first (q (:biff/db ctx)
                           '{:find (pull s [*])
                             :in [id]
                             :where [[s :xt/id id] [s :statement-summary/user-id]]}
                           id)))]
    (if (and s (owned-by-session? ctx user-id))
      (do (logger/info "Deleting statement summary...")
          (biff/submit-tx ctx [{:db/doc-type :statement-summary :xt/id id :db/op :delete}]))
      (logger/info "Statement summary not found or unauthorized"))))

;; ---------------------------------------------------------------------------
;; Goals
;; ---------------------------------------------------------------------------

(defn get-goals [{:keys [biff/db]} user-id]
  (q db
     '{:find (pull goal [*])
       :in [user-id]
       :where [[goal :goal/user-id user-id]]}
     user-id))

(defn get-goal [{:keys [biff/db]} goal-id]
  (first (q db
            '{:find (pull goal [*])
              :in [goal-id]
              :where [[goal :xt/id goal-id]]}
            goal-id)))

(defn upsert-goal [{:keys [params] :as ctx}]
  (let [user-id     (get-user-id-from-session ctx)
        target-date (:target-date params)]
    (logger/info "Creating goal...")
    (biff/submit-tx ctx
                    [(cond-> {:db/doc-type :goal
                              :xt/id (java.util.UUID/randomUUID)
                              :goal/user-id user-id
                              :goal/title (:title params)
                              :goal/target (validation/->valid-amount (:target params))
                              :goal/saved (or (utilities/->int (:saved params)) 0)}
                       (not (str/blank? target-date))
                       (assoc :goal/target-date target-date))])))

(defn update-goal [{:keys [params] :as ctx}]
  (let [goal-id     (utilities/->uuid (:goal-id params))
        goal        (get-goal ctx goal-id)
        target-date (:target-date params)]
    (if (and goal (owned-by-session? ctx (:goal/user-id goal)))
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
    (if (and goal (owned-by-session? ctx (:goal/user-id goal)))
      (do (logger/info "Deleting goal...")
          (biff/submit-tx ctx
                          [{:db/doc-type :goal
                            :xt/id goal-id
                            :db/op :delete}]))
      (logger/info "Goal not found or unauthorized"))))

(defn delete-user
  "Permanently deletes the user with the given id and all of their data
  (finances, budget items, tax profile, events, goals, statement
  summaries). Returns the number
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
                                   [id :session/uid user-id])]}
                     user-id)
            owned (conj (into #{} (map first) owned) user-id)]
        (logger/info (str "Deleting user " user-id " (" (count owned) " docs)..."))
        (biff/submit-tx ctx
                        (for [id owned]
                          {:db/doc-type :user
                           :db/op :delete
                           :xt/id id}))
        (count owned)))))

(defn admin-delete-user
  "Deletes the target user unless they're the signed-in admin themselves.
  Returns :self, :not-found, or the number of docs deleted."
  [{:keys [session params] :as ctx}]
  (let [user-id (utilities/->uuid (:user-id params))]
    (cond
      (= user-id (:uid session)) :self
      (nil? (get-user ctx user-id)) :not-found
      :else (delete-user ctx user-id))))
