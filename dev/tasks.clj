(ns tasks
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :as t]
            [com.biffweb.tasks :as tasks]))

(defn hello
  "Says 'Hello'"
  []
  (println "Hello"))

(def ^:private version-config-path "resources/config/common.edn")
(def ^:private version-pattern #":app/version\s+\"(\d+)\"")

(defn bump-version
  "Bumps :app/version in resources/config/common.edn by 1, so the \"app has
   been updated\" dashboard banner (dashboard/app-update-banner-due?) shows
   again for every user — including ones who already dismissed the
   previous version. Run this after shipping something worth announcing."
  []
  (let [content (slurp version-config-path)
        [_ current] (re-find version-pattern content)]
    (if-not current
      (println "Could not find :app/version in" version-config-path)
      (let [next    (str (inc (parse-long current)))
            updated (str/replace-first content
                                        (re-pattern (str ":app/version\\s+\"" current "\""))
                                        (str ":app/version \"" next "\""))]
        (spit version-config-path updated)
        (println (str ":app/version " current " -> " next))))))

(defn- test-namespaces
  "Every namespace under test/ whose name ends in -test, from its file path:
   test/com/greed/summary_test.clj -> com.greed.summary-test."
  []
  (->> (file-seq (io/file "test"))
       (map #(.getPath ^java.io.File %))
       (filter #(str/ends-with? % "_test.clj"))
       (map #(-> %
                 (subs (count "test/") (- (count %) (count ".clj")))
                 (str/replace "/" ".")
                 (str/replace "_" "-")
                 symbol))
       sort))

(defn run-tests
  "Runs the tests: every namespace under test/ ending in -test, or only the
   ones named, e.g. `clj -M:dev test com.greed.summary-test`. Exits with 1
   when a test fails, so it can be used in a script or CI."
  [& namespaces]
  (let [nss (if (seq namespaces) (map symbol namespaces) (test-namespaces))]
    (apply require nss)
    (let [{:keys [fail error]} (apply t/run-tests nss)]
      (shutdown-agents)
      (System/exit (if (zero? (+ fail error)) 0 1)))))

;; Tasks should be vars (#'hello instead of hello) so that `clj -M:dev help` can
;; print their docstrings.
(def custom-tasks
  {"hello" #'hello
   "bump-version" #'bump-version
   "test" #'run-tests})

(def tasks (merge tasks/tasks custom-tasks))
