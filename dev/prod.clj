(ns prod
  "Helpers for running queries against the PRODUCTION database via its nREPL
  server. Production doesn't share a classpath with this project, so these
  helpers send raw code strings over nREPL rather than calling into com.greed
  directly.

  Before using, open an SSH tunnel from your machine to the server:

    ssh -f -N -L 7889:localhost:7888 app@mygreed.co.za

  then connect to port 7889. Set PROD_REPL_PORT if you want a different local
  port."
  (:require [clojure.pprint :as pp]
            [nrepl.core :as nrepl]))

(def port
  (or (some-> (System/getenv "PROD_REPL_PORT") Integer/parseInt)
      7889))

(defn eval-code
  "Evaluates `code` on the production nREPL server and returns the result
  values. Prints any errors the server reports."
  [code]
  (let [conn   (nrepl/connect :host "localhost" :port port)
        client (nrepl/client conn 30000)]
    (try
      (let [msgs (nrepl/message client {:op "eval" :code code})
            errors (filter #(or (:err %) (:ex %)) msgs)]
        (doseq [{:keys [err ex]} errors]
          (println "nREPL error:" (or err ex)))
        (nrepl/response-values msgs))
      (finally
        (.close conn)))))

(defn- run-query
  "Runs an XTDB `pull` query for all docs of the given entity/anchor pair on
  prod. `anchor` is an attribute guaranteed to exist on every doc of that
  type (e.g. :user/email for users)."
  [entity anchor]
  (let [code (str "(let [{:keys [biff/db]} (repl/get-context)]"
                  "  (com.biffweb/q db"
                  "    '{:find (pull " entity " [*])"
                  "      :where [[" entity " " anchor "]]}))")]
    (eval-code code)))

(defn query-users
  "Returns all users on prod."
  []
  (run-query "user" ":user/email"))

(defn query-finances
  "Returns all finances docs on prod."
  []
  (run-query "finances" ":finances/user-id"))

(defn query-budget-items
  "Returns all budget items on prod."
  []
  (run-query "budget-item" ":budget-item/user-id"))

(defn query-goals
  "Returns all goals on prod."
  []
  (run-query "goal" ":goal/user-id"))

(defn query-events
  "Returns all events on prod."
  []
  (run-query "event" ":event/user-id"))

(defn query-tax-profiles
  "Returns all tax profiles on prod."
  []
  (run-query "tax-profile" ":tax-profile/user-id"))

(defn query-raw
  "Runs an arbitrary EDN XTDB query (as a string) on prod, pprinting the result."
  [code]
  (doseq [r (eval-code code)]
    (pp/pprint r)))

(defn query-all
  "Prints every doc of every doc-type present in the prod db."
  []
  (let [code (str "(let [{:keys [biff/db]} (repl/get-context)]"
                  "  (sort-by :db/doc-type"
                  "    (com.biffweb/q db"
                  "      '{:find [(pull e [*])]"
                  "        :where [[e :db/doc-type]]})))")
        docs (eval-code code)]
    (doseq [d (first docs)]
      (pp/pprint d))))

(comment
  ;; Make sure the tunnel is up first (see ns docstring), then:
  (query-users)
  (query-budget-items)
  (query-all)

  ;; Arbitrary query, e.g. count users:
  (query-raw
    (str "(count (com.biffweb/q (:biff/db (repl/get-context))"
         "  '{:find [u] :where [[u :user/email]]}))")))
