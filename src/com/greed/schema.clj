(ns com.greed.schema)

(def schema
  {:user/id :uuid
   :user [:map {:closed true}
          [:xt/id          :user/id]
          [:user/email     :string]
          [:user/password  :string]
          [:user/firstname :string]
          [:user/lastname  :string]
          ;; Asked of personal sign-ups only; a business sign-up has none.
          [:user/age       {:optional true} :int]
          [:user/roles     {:optional true} [:maybe [:set :keyword]]]
          [:user/active    :boolean]
          [:user/password-reset-token {:optional true} [:maybe :string]]
          [:user/password-reset-at    {:optional true} [:maybe inst?]]]

   :finances/id :uuid
   :finances [:map {:closed true}
              [:xt/id                :finances/id]
              [:finances/user-id     :user/id]
              ;; bank and account type are set on Banking; salary and payday
              ;; in Settings, so either can come first
              [:finances/bank        {:optional true} :keyword]
              [:finances/account-type {:optional true} [:maybe :string]]
              [:finances/salary      {:optional true} :int]
              [:finances/payday      {:optional true} :int]]

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
           [:event/done     {:optional true} [:maybe :boolean]]
           ;; Set on a business's events; user-id is then who added it.
           [:event/business-id {:optional true} :business/id]]

   :goal/id :uuid
   :goal [:map {:closed true}
          [:xt/id            :goal/id]
          [:goal/user-id     :user/id]
          [:goal/title       :string]
          [:goal/target      :int]
          [:goal/saved       :int]
          [:goal/target-date {:optional true} [:maybe :string]]
          ;; Set on a business's goals; user-id is then who added it.
          [:goal/business-id {:optional true} :business/id]]

   ;; A business a person runs. People are linked to it by :business-member
   ;; docs, so it can have more than one member later (co-founder,
   ;; bookkeeper).
   :business/id :uuid
   :business [:map {:closed true}
              [:xt/id                :business/id]
              [:business/name        :string]
              [:business/entity-type [:enum :sole-proprietor :pty-ltd :partnership :npc :trust]]
              [:business/created-at  inst?]
              ;; VAT: registered vendors get VAT types on budget items and a VAT page
              [:business/vat-registered {:optional true} :boolean]
              [:business/vat-number     {:optional true} :string]
              [:business/vat-category   {:optional true} [:enum :a :b :c]]
              ;; Company tax: the month the financial year ends (2 = February)
              ;; and whether the owner declares it a small business corporation.
              [:business/year-end-month {:optional true} [:int {:min 1 :max 12}]]
              [:business/sbc            {:optional true} :boolean]]

   :business-member/id :uuid
   :business-member [:map {:closed true}
                     [:xt/id                       :business-member/id]
                     [:business-member/business-id :business/id]
                     [:business-member/user-id     :user/id]
                     ;; owner: also manages members and the business's details
                     [:business-member/role        [:enum :owner :member]]
                     ;; A partnership's split of the profit, in percent. Unset
                     ;; means an equal share among the owners.
                     [:business-member/profit-share {:optional true} [:int {:min 0 :max 100}]]
                     [:business-member/created-at  inst?]]

   ;; An invitation to join a business, waiting for the person with that
   ;; email to accept or decline it. Expires after 14 days.
   :business-invite/id :uuid
   :business-invite [:map {:closed true}
                     [:xt/id                       :business-invite/id]
                     [:business-invite/business-id :business/id]
                     [:business-invite/email       :string] ; lower case
                     [:business-invite/role        [:enum :owner :member]]
                     [:business-invite/invited-by  :user/id]
                     [:business-invite/created-at  inst?]]

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
                 [:budget-item/reference {:optional true} :string]
                 ;; Set on a business's items. :budget-item/user-id is then
                 ;; the member who added it.
                 [:budget-item/business-id {:optional true} :business/id]
                 ;; Business items only: the spending category for payments
                 ;; this item's reference matches (Stock and suppliers, say).
                 [:budget-item/category {:optional true} :keyword]
                 ;; Business income and expense items of a VAT vendor; the
                 ;; amount includes VAT. Unset means income :standard,
                 ;; expenses :none (see com.greed.utilities.vat).
                 [:budget-item/vat {:optional true} [:enum :standard :zero :none]]
                 ;; A business's Owner's drawings item: the owner it is paid to.
                 [:budget-item/paid-to {:optional true} :user/id]
                 ;; A personal income item greed keeps in step with what a
                 ;; business pays this person ("From Ngunitek").
                 [:budget-item/source-business-id {:optional true} :business/id]
                 ;; Added from a bank statement on Spending ("2026-10"): the
                 ;; amount is that month's, not a planned monthly figure.
                 [:budget-item/from-statement {:optional true} :string]]

   ;; Totals from one uploaded bank statement, for the Spending tab. The
   ;; statement's individual lines are never stored. Amounts are in cents.
   ;; Money in and out leave out transfers between the user's own accounts,
   ;; which are kept apart in the transfers fields.
   :statement-summary/id :uuid
   :statement-summary [:map {:closed true}
                       [:xt/id                                   :statement-summary/id]
                       [:statement-summary/user-id               :user/id]
                       ;; Set on a business's months; user-id is then who uploaded it.
                       [:statement-summary/business-id {:optional true} :business/id]
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
                        [:vector [:map [:name {:optional true} :string] [:text :string] [:category :keyword] [:spent-cents :int]
                                  [:refund-cents :int] [:count :int]]]]
                       ;; Totals per payer of money in, so a business can add a
                       ;; regular payer to its budget's income from Spending.
                       [:statement-summary/income-payees {:optional true}
                        [:vector [:map [:name :string] [:text :string] [:cents :int] [:count :int]
                                  [:salary? {:optional true} :boolean]]]]
                       [:statement-summary/checks-ok             :boolean]
                       [:statement-summary/uploaded-at           inst?]]})

(def module
  {:schema schema})
