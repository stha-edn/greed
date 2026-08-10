(ns tasks
  (:require [clojure.string :as str]
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

;; Tasks should be vars (#'hello instead of hello) so that `clj -M:dev help` can
;; print their docstrings.
(def custom-tasks
  {"hello" #'hello
   "bump-version" #'bump-version})

(def tasks (merge tasks/tasks custom-tasks))
