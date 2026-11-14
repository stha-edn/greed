(ns com.greed.spending-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.greed.ui.app.statements :as spending]))

(def ^:private car #uuid "00000000-0000-0000-0000-0000000000c1")
(def ^:private save #uuid "00000000-0000-0000-0000-0000000000c2")

(def ^:private items-by-id
  {car  {:xt/id car :budget-item/type :expenses :budget-item/amount 4500}
   save {:xt/id save :budget-item/type :savings :budget-item/amount 1000}})

(deftest planned-against-spent
  (testing "nothing linked: all spending against all planned expenses"
    (is (= {:spent 1200000 :planned 1000000 :linked? false :budgeted 0
            :unplanned 1200000 :rest-plan 1000000 :over 200000}
           (spending/compare-with-budget [{:statement-summary/money-out-cents 1200000}] items-by-id 10000))))
  (testing "linked items are taken out of both sides; savings items have no expense plan to take out"
    (let [months [{:statement-summary/money-out-cents 900000 :statement-summary/budgeted-cents 550000
                   :statement-summary/budget-matches [{:item-id car} {:item-id save}]}
                  {:statement-summary/money-out-cents 300000 :statement-summary/budgeted-cents 0
                   :statement-summary/budget-matches [{:item-id car}]}]]
      (is (= {:spent 1200000 :planned 1000000 :linked? true :budgeted 550000
              :unplanned 650000 :rest-plan 550000 :over 100000}
             (spending/compare-with-budget months items-by-id 10000))))))
