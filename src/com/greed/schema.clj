(ns com.greed.schema)

(def schema
  {:user/id :uuid
   :user [:map {:closed true}
          [:xt/id          :user/id]
          [:user/email     :string]
          [:user/password  :string]
          [:user/firstname :string]
          [:user/lastname  :string]
          [:user/age       :int]]

   :finances/id :uuid
   :finances [:map {:closed true}
              [:xt/id              :finances/id]
              [:finances/user-id   :user/id]
              [:finances/bank      :keyword]
              [:finances/card-type :keyword]
              [:finances/salary    :int]
              [:finances/payday    :int]]

   :tax-profile/id :uuid
   :tax-profile [:map {:closed true}
                 [:xt/id                              :tax-profile/id]
                 [:tax-profile/user-id                :user/id]
                 [:tax-profile/medical-monthly        {:optional true} [:maybe :int]]
                 [:tax-profile/medical-dependants     {:optional true} [:maybe :int]]
                 [:tax-profile/ra-annual              {:optional true} [:maybe :int]]]

   :event/id :uuid
   :event [:map {:closed true}
           [:xt/id          :event/id]
           [:event/user-id  :user/id]
           [:event/title    :string]
           [:event/date     :string]
           [:event/type     {:optional true} [:maybe :keyword]]]

   :goal/id :uuid
   :goal [:map {:closed true}
          [:xt/id            :goal/id]
          [:goal/user-id     :user/id]
          [:goal/title       :string]
          [:goal/target      :int]
          [:goal/saved       :int]
          [:goal/target-date {:optional true} [:maybe :string]]]

   :budget-item/id :uuid
   :budget-item [:map {:closed true}
                 [:xt/id               :budget-item/id]
                 [:budget-item/user-id :user/id]
                 [:budget-item/title   :string]
                 [:budget-item/type    :keyword]
                 [:budget-item/amount  :int]]})

(def module
  {:schema schema})
