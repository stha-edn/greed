(ns com.greed.ui.components.illustrations
  "Drawings of greed's own world, in one flat language: white paper, ink
   lines, the brand green for the one thing that matters on each object.
   Numbers match the home page sample (R25,000 a month).")

(def ^:private ink "#18181b")
(def ^:private muted "#52525b")
(def ^:private rule "#d4d4d8")
(def ^:private green "#0F6B4F")

(defn- t [x y s & {:keys [size weight fill anchor]}]
  [:text {:x x :y y :font-family "Inter, sans-serif" :font-size (or size 14)
          :font-weight (or weight 400) :fill (or fill ink) :text-anchor (or anchor "start")} s])

(defn payslip
  "A payslip for the sample salary: the artefact every salaried person
   already reads once a month."
  [& {:keys [class]}]
  [:svg {:viewBox "0 0 360 470" :class class :role "img"
         :aria-label "Drawing of a November payslip: basic salary R25,000, PAYE R3,381, net pay R21,619."}
   ;; paper with a torn top edge
   [:path {:fill "#ffffff" :stroke rule :stroke-width "1"
           :d (str "M10 22 "
                   (apply str (for [i (range 17)] (str "L" (+ 20 (* i 20)) " " (if (even? i) 10 22) " ")))
                   "L350 22 L350 460 L10 460 Z")}]
   (t 34 70 "Payslip" :size 22 :weight 600)
   (t 326 70 "November 2026" :size 14 :fill muted :anchor "end")
   [:line {:x1 34 :y1 92 :x2 326 :y2 92 :stroke rule}]
   (t 34 124 "Basic salary" :fill muted)
   (t 326 124 "R25,000.00" :weight 600 :anchor "end")
   (t 34 158 "PAYE (SARS)" :fill muted)
   (t 326 158 "−R3,381.00" :weight 600 :anchor "end")
   (t 34 192 "Pay date" :fill muted)
   (t 326 192 "25 Nov 2026" :weight 600 :anchor "end")
   [:line {:x1 34 :y1 216 :x2 326 :y2 216 :stroke ink :stroke-width "1.5"}]
   (t 34 252 "Net pay" :size 16 :weight 600)
   (t 326 254 "R21,619.00" :size 24 :weight 700 :fill green :anchor "end")
   ;; what greed adds: the plan for the net pay
   [:rect {:x 34 :y 290 :width 292 :height 10 :rx 5 :fill "#f4f4f5"}]
   [:rect {:x 34 :y 290 :width 142 :height 10 :rx 5 :fill ink}]
   [:rect {:x 176 :y 290 :width 20 :height 10 :fill "#a1a1aa"}]
   [:rect {:x 196 :y 290 :width 130 :height 10 :rx 5 :fill green}]
   (t 34 330 "Expenses R10,500" :size 13 :fill muted)
   (t 34 352 "Savings R1,500" :size 13 :fill muted)
   (t 326 330 "Left to plan" :size 13 :fill muted :anchor "end")
   (t 326 352 "R9,619" :size 13 :weight 600 :anchor "end")
   ;; paid stamp
   [:g {:transform "rotate(-12 262 410)"}
    [:circle {:cx 262 :cy 410 :r 38 :fill "none" :stroke green :stroke-width "2.5"}]
    [:circle {:cx 262 :cy 410 :r 31 :fill "none" :stroke green :stroke-width "1"}]
    (t 262 406 "PAID" :size 14 :weight 700 :fill green :anchor "middle")
    (t 262 423 "25 NOV" :size 11 :weight 600 :fill green :anchor "middle")]])

(defn envelope
  "An envelope addressed to the support inbox, stamped with the greed mark."
  [& {:keys [class]}]
  [:svg {:viewBox "0 0 440 290" :class class :role "img"
         :aria-label "Drawing of an envelope addressed to greed support at support@mygreed.co.za."}
   [:rect {:x 6 :y 6 :width 428 :height 278 :rx 6 :fill "#ffffff" :stroke rule}]
   [:path {:d "M6 12 L220 160 L434 12" :fill "none" :stroke rule :stroke-width "1.5"}]
   ;; stamp: the greed symbol
   [:rect {:x 352 :y 28 :width 58 :height 68 :rx 4 :fill "#ffffff" :stroke green :stroke-width "1.5" :stroke-dasharray "3 3"}]
   [:rect {:x 362 :y 40 :width 38 :height 38 :rx 11 :fill green}]
   [:rect {:x 371 :y 53.5 :width 29 :height 4 :rx 2 :fill "#ffffff"}]
   ;; address
   (t 150 206 "greed support" :size 18 :weight 600)
   (t 150 232 "support@mygreed.co.za" :size 15 :fill muted)
   (t 150 256 "Read by a person, in South Africa" :size 13 :fill muted)])

(defn calendar-sheet
  "A day-to-a-page calendar torn at payday."
  [& {:keys [class]}]
  [:svg {:viewBox "0 0 260 300" :class class :role "img" :aria-label "Drawing of a calendar page showing 25 November, payday."}
   [:rect {:x 10 :y 30 :width 240 :height 260 :rx 8 :fill "#ffffff" :stroke rule}]
   [:path {:d "M10 38 a8 8 0 0 1 8 -8 h224 a8 8 0 0 1 8 8 v52 h-240 z" :fill green}]
   (for [x [70 130 190]]
     [:g
      [:rect {:x (- x 5) :y 12 :width 10 :height 34 :rx 5 :fill ink}]])
   (t 130 74 "November" :size 20 :weight 600 :fill "#ffffff" :anchor "middle")
   (t 130 200 "25" :size 96 :weight 700 :anchor "middle")
   (t 130 246 "Payday" :size 18 :weight 600 :fill green :anchor "middle")])
