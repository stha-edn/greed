(ns com.greed.statements.pdf
  "Reads the text out of an uploaded statement PDF, in memory. The file is
   never written to disk or stored: the bytes come in, text goes out."
  (:import [org.apache.pdfbox Loader]
           [org.apache.pdfbox.pdmodel PDDocument]
           [org.apache.pdfbox.pdmodel.encryption InvalidPasswordException]
           [org.apache.pdfbox.text PDFTextStripper]))

(def max-bytes
  "Largest upload accepted. Bank statements are well under 1 MB."
  (* 5 1024 1024))

(def max-pages 60)

(defn pdf?
  "True when the bytes start like a PDF (%PDF-)."
  [^bytes bs]
  (and bs (>= (alength bs) 5)
       (= "%PDF-" (String. bs 0 5 "ISO-8859-1"))))

(defn pdf->text
  "Returns {:text s} or {:error kw}: :too-large, :not-pdf, :too-many-pages,
   :password-protected, :no-text (a scanned, image-only statement) or
   :unreadable."
  [^bytes bs]
  (cond
    (or (nil? bs) (zero? (alength bs))) {:error :not-pdf}
    (> (alength bs) max-bytes)          {:error :too-large}
    (not (pdf? bs))                     {:error :not-pdf}
    :else
    (try
      ;; Statements protected only by an owner password (FNB's are) open with
      ;; the empty user password, which is what loadPDF tries.
      (with-open [^PDDocument doc (Loader/loadPDF bs)]
        (if (> (.getNumberOfPages doc) max-pages)
          {:error :too-many-pages}
          (let [text (.getText (doto (PDFTextStripper.) (.setSortByPosition true)) doc)]
            (if (< (count (.trim ^String text)) 20)
              {:error :no-text}
              {:text text}))))
      (catch InvalidPasswordException _ {:error :password-protected})
      (catch Exception _ {:error :unreadable}))))
