(ns com.greed.statements.core
  "Turns the text of a bank statement into transaction lines for the review
   screen. Each bank has its own reader; this picks the right one. Reading the
   PDF itself into text happens elsewhere (com.greed.statements.pdf), so
   everything here is pure and testable."
  (:require [clojure.string :as str]
            [com.greed.statements.categorise :as categorise]
            [com.greed.statements.fnb :as fnb]
            [com.greed.statements.standard-bank :as sb]
            [com.greed.statements.pdf :as pdf]))

(def adapters
  "One adapter per bank: how to recognise its statement, read its lines, and
   tell how each line's money moved. Tried in order."
  [{:id :fnb :name "FNB"
    :match? fnb/fnb-statement? :parse fnb/parse :kind fnb/kind}
   {:id :standard-bank :name "Standard Bank"
    :match? sb/standard-bank-statement? :parse sb/parse :kind sb/kind}])

(defn read-statement-text
  "Reads statement text with the matching bank's adapter. Returns
   {:bank :period :transactions :problems}, each line with :kind and
   :category, or {:error kw} when the text is empty or no adapter recognises
   the bank. With :business? true, lines are also put in business
   categories (see categorise/business-category)."
  [text & {:keys [business?]}]
  (cond
    (str/blank? text) {:error :no-text}
    :else (if-let [{:keys [parse kind]} (first (filter #((:match? %) text) adapters))]
            (update (parse text) :transactions #(mapv (fn [l] (categorise/categorise kind l business?)) %))
            {:error :unsupported-bank})))

(defn read-statement-pdf
  "Reads an uploaded statement PDF (bytes) into transaction lines. Returns the
   bank reader's result, or {:error kw} (see pdf/pdf->text and
   read-statement-text)."
  [bs & {:keys [business?]}]
  (let [{:keys [text error]} (pdf/pdf->text bs)]
    (if error {:error error} (read-statement-text text :business? business?))))
