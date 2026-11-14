(ns com.greed.schema)

(def schema
  {:user/id :uuid
   :user [:map {:closed true}
          [:xt/id          :user/id]
          [:user/email     :string]
          [:user/password  :string]
          [:user/firstname :string]
          [:user/lastname  :string]
          [:user/age       :int]
          [:user/roles     {:optional true} [:maybe [:set :keyword]]]
          [:user/active    :boolean]
          [:user/password-reset-token {:optional true} [:maybe :string]]
          [:user/password-reset-at    {:optional true} [:maybe inst?]]]

   :finances/id :uuid
   :finances [:map {:closed true}
              [:xt/id                :finances/id]
              [:finances/user-id     :user/id]
              [:finances/bank        :keyword]
              [:finances/account-type {:optional true} [:maybe :string]]
              [:finances/salary      :int]
              [:finances/payday      :int]]

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
           [:event/date     {:optional true} [:maybe :string]]
           [:event/type     {:optional true} [:maybe :keyword]]
           [:event/done     {:optional true} [:maybe :boolean]]]

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
                 [:budget-item/amount  :int]
                 ;; Text that appears on bank statement lines for this item,
                 ;; comma-separated ("discovery life, vitality"). The Spending
                 ;; tab uses it to tell planned payments from the rest.
                 [:budget-item/reference {:optional true} :string]]

   ;; Totals from one uploaded bank statement, for the Spending tab. The
   ;; statement's individual lines are never stored. Amounts are in cents.
   ;; Money in and out leave out transfers between the user's own accounts,
   ;; which are kept apart in the transfers fields.
   :statement-summary/id :uuid
   :statement-summary [:map {:closed true}
                       [:xt/id                                   :statement-summary/id]
                       [:statement-summary/user-id               :user/id]
                       [:statement-summary/bank                  :keyword]
                       [:statement-summary/month                 :string] ; "2026-10"
                       [:statement-summary/period-start          :string]
                       [:statement-summary/period-end            :string]
                       [:statement-summary/line-count            :int]
                       [:statement-summary/money-in-cents        :int]
                       [:statement-summary/money-out-cents       :int]
                       [:statement-summary/salary-cents          :int]
                       [:statement-summary/refunds-cents         :int]
                       [:statement-summary/transfers-in-cents    :int]
                       [:statement-summary/transfers-out-cents   :int]
                       [:statement-summary/categories            [:map-of :keyword :int]]
                       [:statement-summary/fees-cents            :int]
                       [:statement-summary/fee-count             :int]
                       [:statement-summary/top-merchants         [:vector [:map
                                                                           [:name :string]
                                                                           [:category :keyword]
                                                                           [:cents :int]
                                                                           [:count :int]]]]
                       [:statement-summary/budgeted-cents {:optional true} :int]
                       [:statement-summary/budget-matches {:optional true}
                        [:vector [:map [:item-id :uuid] [:title :string] [:cents :int] [:count :int]]]]
                       ;; Totals per payee (cleaned statement text, no dates or
                       ;; numbers), so the budget sync can re-match references.
                       [:statement-summary/payees {:optional true}
                        [:vector [:map [:text :string] [:category :keyword] [:spent-cents :int]
                                  [:refund-cents :int] [:count :int]]]]
                       [:statement-summary/checks-ok             :boolean]
                       [:statement-summary/uploaded-at           inst?]]})

(def module
  {:schema schema})
