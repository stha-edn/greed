(ns com.greed.data.core
  (:require [com.biffweb :as biff :refer [q]]
            [clojure.string :as str]
            [clojure.tools.logging :as logger]
            [com.greed.utilities.tax :as tax]
            [com.greed.utilities.core :as utilities]
            [com.greed.data.validation :as validation]))


(defn get-users [{:keys [biff/db]}]
  (q db
     '{:find (pull user [*])
       :where [[user :user/email]]}))

(defn get-user-id [{:keys [biff/db params]}]
  (biff/lookup-id db :user/email (:email params)))

(defn get-user-id-from-session [{:keys [session]}]
  (:uid session))

(defn get-user [{:keys [biff/db]} user-id]
  (first (q db
            '{:find (pull user [*])
              :in [user-id]
              :where [[user :xt/id user-id]]}
            user-id)))

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

(defn get-salary-budget-item [{:keys [biff/db]} user-id]
  (first (q db
            '{:find (pull budget-item [*])
              :in [user-id]
              :where [[budget-item :budget-item/user-id user-id]
                      [budget-item :budget-item/title "Salary"]]}
            user-id)))

(defn upsert-user [{:keys [params] :as ctx}]
  (let [user-id (java.util.UUID/randomUUID)]
    (logger/info "Creating user...")
    (biff/submit-tx ctx
                    [{:db/doc-type :user
                      :xt/id user-id
                      :user/email (:email params)
                      :user/password (:password params)
                      :user/firstname (:firstname params)
                      :user/lastname (:lastname params)
                      :user/age (utilities/->int (:age params))}])))

(defn update-user [{:keys [params] :as ctx}]
  (let [user-id (get-user-id ctx)
        user (get-user ctx user-id)]
    (if user
      (do
        (logger/info "Updating user...")
        (biff/submit-tx ctx
                      [{:db/doc-type :user
                        :xt/id user-id
                        :db/op :update
                        :user/email (:email params)
                        :user/password (:password params)
                        :user/firstname (:firstname params)
                        :user/lastname (:lastname params)
                        :user/age (utilities/->int (:age params))}]))
      (logger/info "User not found"))))

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
                      :finances/card-type (utilities/->keyword (:card-type params))
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
                            :finances/card-type (utilities/->keyword (:card-type params))
                            :finances/salary (utilities/->int (:salary params))
                            :finances/payday (validation/->valid-payday (:payday params))}])
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

(defn get-events [{:keys [biff/db]} user-id]
  (sort-by :event/date
           (q db
              '{:find (pull event [*])
                :in [user-id]
                :where [[event :event/user-id user-id]]}
              user-id)))

(defn create-event [{:keys [params] :as ctx}]
  (let [user-id (get-user-id-from-session ctx)
        type    (or (some-> (:type params) utilities/->keyword) :general)]
    (logger/info "Creating event...")
    (biff/submit-tx ctx
                    [{:db/doc-type :event
                      :xt/id (java.util.UUID/randomUUID)
                      :event/user-id user-id
                      :event/title (:title params)
                      :event/date (:date params)
                      :event/type type}])))

(defn delete-event [{:keys [params] :as ctx}]
  (let [event-id (utilities/->uuid (:event-id params))]
    (logger/info "Deleting event...")
    (biff/submit-tx ctx
                    [{:db/doc-type :event
                      :xt/id event-id
                      :db/op :delete}])))

(defn upsert-budget-item [{:keys [params] :as ctx}]
  (let [user-id (get-user-id-from-session ctx)
        budget-item-id (java.util.UUID/randomUUID)]
    (logger/info "Creating budget item...")
    (biff/submit-tx ctx
                    [{:db/doc-type :budget-item
                      :xt/id budget-item-id
                      :budget-item/user-id user-id
                      :budget-item/title (:title params)
                      :budget-item/type (utilities/->keyword (:type params))
                      :budget-item/amount (validation/->valid-amount (:amount params))}])))

(defn update-budget-item [{:keys [params] :as ctx}]
  (let [{:keys [budget-item-id]} params
        _ (println "Updating budget item with ID:" budget-item-id)
        budget-item-id (utilities/->uuid budget-item-id)
        {:budget-item/keys [type]
         :as budget-item} (get-budget-item ctx budget-item-id)]
    (if budget-item
      (do
        (logger/info "Updating budget item...")
        (biff/submit-tx ctx
                        [{:db/doc-type :budget-item
                          :xt/id budget-item-id
                          :db/op :update
                          :budget-item/title (:title params)
                          :budget-item/type type
                          :budget-item/amount (validation/->valid-amount (:amount params))}]))
      (logger/info "Budget item not found"))))

(defn delete-budget-item [{:keys [params] :as ctx}]
  (let [{:keys [budget-item-id]} params
        budget-item-id (utilities/->uuid budget-item-id)
        budget-item (get-budget-item ctx budget-item-id)]
    (if budget-item
      (do (logger/info "Deleting budget item...")
          (biff/submit-tx ctx
                          [{:db/doc-type :budget-item
                            :xt/id budget-item-id
                            :db/op :delete}]))
      (logger/info "Budget item not found"))))


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
    (if goal
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
      (logger/info "Goal not found"))))

(defn delete-goal [{:keys [params] :as ctx}]
  (let [goal-id (utilities/->uuid (:goal-id params))
        goal    (get-goal ctx goal-id)]
    (if goal
      (do (logger/info "Deleting goal...")
          (biff/submit-tx ctx
                          [{:db/doc-type :goal
                            :xt/id goal-id
                            :db/op :delete}]))
      (logger/info "Goal not found"))))
