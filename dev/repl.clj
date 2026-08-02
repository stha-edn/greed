(ns repl
  (:require [com.greed :as main]
   [com.biffweb :as biff :refer [q]]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [nrepl.core :as nrepl]))

;; REPL-driven development
;; ----------------------------------------------------------------------------------------
;; If you're new to REPL-driven development, Biff makes it easy to get started: whenever
;; you save a file, your changes will be evaluated. Biff is structured so that in most
;; cases, that's all you'll need to do for your changes to take effect. (See main/refresh
;; below for more details.)
;;
;; The `clj -M:dev dev` command also starts an nREPL server on port 7888, so if you're
;; already familiar with REPL-driven development, you can connect to that with your editor.
;;
;; If you're used to jacking in with your editor first and then starting your app via the
;; REPL, you will need to instead connect your editor to the nREPL server that `clj -M:dev
;; dev` starts. e.g. if you use emacs, instead of running `cider-jack-in`, you would run
;; `cider-connect`. See "Connecting to a Running nREPL Server:"
;; https://docs.cider.mx/cider/basics/up_and_running.html#connect-to-a-running-nrepl-server
;; ----------------------------------------------------------------------------------------

;; This function should only be used from the REPL. Regular application code
;; should receive the system map from the parent Biff component. For example,
;; the use-jetty component merges the system map into incoming Ring requests.
(defn get-context []
  (biff/merge-context @main/system))

;; ----------------------------------------------------------------------------
;; Environment dispatch
;; ----------------------------------------------------------------------------
;;
;; Every helper below takes an `env` as its first argument: :dev runs in-process
;; against the local dev database, and :prod runs the same operation on the prod
;; database by sending the code over an SSH tunnel to the prod nREPL server.
;;
;; Open the tunnel with:
;;
;;   ssh -f -N -L 7889:localhost:7888 app@mygreed.co.za
;;
;; If you map a local port other than 7889, set PROD_REPL_PORT to it. Prod runs
;; a separate process that doesn't share this project's classpath, so operations
;; are sent to it as code strings.

(def prod-port
  (or (some-> (System/getenv "PROD_REPL_PORT") Integer/parseInt)
    7889))

(defn- prod-eval-code
  "Evaluates `code` on the prod nREPL server and returns the result. Prints any
  errors the server reports. Requires the tunnel."
  [code]
  (let [conn   (nrepl/connect :host "localhost" :port prod-port)
        client (nrepl/client conn 30000)]
    (try
      (let [msgs (nrepl/message client {:op "eval" :code code})
            errors (filter #(or (:err %) (:ex %)) msgs)]
        (doseq [{:keys [err ex]} errors]
          (println "nREPL error:" (or err ex)))
        (first (nrepl/response-values msgs)))
      (finally
        (.close conn)))))

(defn- eval-on
  "Evaluates `code` on :dev (in-process) or :prod (over the tunnel)."
  [env code]
  (if (= env :prod)
    (prod-eval-code code)
    (binding [*ns* (the-ns 'repl)]
      (eval (read-string code)))))

;; ----------------------------------------------------------------------------
;; Queries
;; ----------------------------------------------------------------------------

(defn- entity-str
  "Returns the query variable name for an entity, e.g. :user -> \"user\"."
  [entity]
  (str (if (keyword? entity) (name entity) entity)))

(defn docs-of
  "Returns all docs of the given entity/anchor pair, e.g.
  (docs-of :dev :user :user/email). Pass :prod to query production instead."
  [env entity anchor]
  (eval-on env
    (str "(let [{:keys [biff/db]} (repl/get-context)]"
      "  (com.biffweb/q db"
      "    '{:find (pull " (entity-str entity) " [*])"
      "      :where [[" (entity-str entity) " " (str anchor) "]]}))")))

(defn query-raw
  "Runs an arbitrary XTDB query (as a string) on :dev or :prod, printing the
  result."
  [env code]
  (pp/pprint (eval-on env code)))

;; ----------------------------------------------------------------------------
;; User management
;; ----------------------------------------------------------------------------
;;
;; All of these take an `env` as their first argument (:dev or :prod) and return
;; nil if no user has the given email.

(defn get-user
  "Returns the user with the given email, from :dev or :prod, or nil."
  [env email]
  (eval-on env
    (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)]"
      "  (com.greed.data.core/get-user ctx"
      "    (com.biffweb/lookup-id db :user/email " (pr-str email) ")))")))

(defn get-user-roles
  "Returns the set of roles for the user with the given email, from :dev or
  :prod."
  [env email]
  (eval-on env
    (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
      "      user (com.greed.data.core/get-user ctx"
      "            (com.biffweb/lookup-id db :user/email " (pr-str email) "))]"
      "  (:user/roles user))")))

(defn add-user-role
  "Adds `role` to the user's :user/roles set (idempotent), on :dev or :prod.
  Returns the user-id, or nil if no user has that email."
  [env email role]
  (eval-on env
    (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
      "        user-id (com.biffweb/lookup-id db :user/email " (pr-str email) ")"
      "        user (when user-id (com.greed.data.core/get-user ctx user-id))]"
      "  (when user"
      "    (com.biffweb/submit-tx ctx"
      "      [{:db/doc-type :user"
      "        :xt/id user-id"
      "        :db/op :update"
      "        :user/roles (conj (set (:user/roles user)) " (pr-str role) ")}]))"
      "  user-id)")))

(defn remove-user-role
  "Removes `role` from the user's :user/roles set (idempotent), on :dev or
  :prod. Returns the user-id, or nil if no user has that email."
  [env email role]
  (eval-on env
    (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
      "        user-id (com.biffweb/lookup-id db :user/email " (pr-str email) ")"
      "        user (when user-id (com.greed.data.core/get-user ctx user-id))]"
      "  (when user"
      "    (com.biffweb/submit-tx ctx"
      "      [{:db/doc-type :user"
      "        :xt/id user-id"
      "        :db/op :update"
      "        :user/roles (disj (set (:user/roles user)) " (pr-str role) ")}]))"
      "  user-id)")))

(defn make-admin
  "Gives the user with the given email the :admin role, on :dev or :prod.
  Returns the user-id, or nil if no user has that email."
  [env email]
  (add-user-role env email :admin))

(defn user-active?
  "Returns whether the user with the given email is active, on :dev or :prod.
  Only an explicit :user/active true counts as active; a missing or nil value
  counts as deactivated. Returns nil if no user has that email."
  [env email]
  (eval-on env
    (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
      "      user (com.greed.data.core/get-user ctx"
      "            (com.biffweb/lookup-id db :user/email " (pr-str email) "))]"
      "  (when user (com.greed.data.core/user-active? user)))")))

(defn set-user-active
  "Sets whether the user with the given email is active, on :dev or :prod.
  Returns the user-id, or nil if no user has that email."
  [env email active?]
  (eval-on env
    (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
      "        user-id (com.biffweb/lookup-id db :user/email " (pr-str email) ")]"
      "  (when user-id"
      "    (com.biffweb/submit-tx ctx"
      "      [{:db/doc-type :user"
      "        :xt/id user-id"
      "        :db/op :update"
      "        :user/active " (if active? "true" "false") "}]))"
      "  user-id)")))

(defn activate-user
  "Reactivates the user with the given email, on :dev or :prod. Deactivated
  users can't sign in. Returns the user-id, or nil if no user has that email."
  [env email]
  (set-user-active env email true))

(defn deactivate-user
  "Deactivates the user with the given email, on :dev or :prod. Deactivated
  users can't sign in. Returns the user-id, or nil if no user has that email."
  [env email]
  (set-user-active env email false))

(defn delete-user
  "Completely removes the user with the given email and all of their data
  (finances, budget items, tax profile, events, goals), on :dev or :prod.
  Returns the number of docs deleted, or nil if no user has that email."
  [env email]
  (eval-on env
    (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
      "      user-id (com.biffweb/lookup-id db :user/email " (pr-str email) ")]"
      "  (when user-id (com.greed.data.core/delete-user ctx user-id)))")))

;; ----------------------------------------------------------------------------
;; Passwords
;; ----------------------------------------------------------------------------

(defn bcrypt-user-password
  "Re-hashes the stored password of the user with the given email to bcrypt,
  on :dev or :prod. Useful for upgrading users who still have legacy plaintext
  passwords. Returns the user-id, or nil if no user has that email."
  [env email]
  (eval-on env
    (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
      "        user-id (com.biffweb/lookup-id db :user/email " (pr-str email) ")"
      "        user (when user-id (com.greed.data.core/get-user ctx user-id))]"
      "  (when user"
      "    (com.biffweb/submit-tx ctx"
      "      [{:db/doc-type :user"
      "        :xt/id user-id"
      "        :db/op :update"
      "        :user/password (com.greed.data.core/hash-password (:user/password user))}]))"
      "  user-id)")))

(defn bcrypt-all-plaintext-passwords
  "Sweeps the whole db and re-hashes every user whose stored password isn't
  already bcrypt, on :dev or :prod. Returns the list of upgraded emails."
  [env]
  (eval-on env
    (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
      "        users (com.biffweb/q db"
      "                 '{:find (pull user [*])"
      "                   :where [[user :user/email]]})"
      "        plaintext (filter #(not (clojure.string/starts-with? (:user/password %) \"bcrypt\")) users)]"
      "  (doseq [user plaintext]"
      "    (com.biffweb/submit-tx ctx"
      "      [{:db/doc-type :user"
      "        :xt/id (:xt/id user)"
      "        :db/op :update"
      "        :user/password (com.greed.data.core/hash-password (:user/password user))}]))"
      "  (mapv :user/email plaintext))")))

;; ----------------------------------------------------------------------------
;; Maintenance
;; ----------------------------------------------------------------------------

(defn add-fixtures []
  (biff/submit-tx (get-context)
    (-> (io/resource "fixtures.edn")
      slurp
      edn/read-string)))

(defn check-config []
  (let [prod-config (biff/use-aero-config {:biff.config/profile "prod"})
        dev-config  (biff/use-aero-config {:biff.config/profile "dev"})
        ;; Add keys for any other secrets you've added to resources/config.edn
        secret-keys [:biff.middleware/cookie-secret
                     :biff/jwt-secret
                     :mailersend/api-key
                     :recaptcha/secret-key
                     ; ...
                     ]
        get-secrets (fn [{:keys [biff/secret] :as config}]
                      (into {}
                        (map (fn [k]
                               [k (secret k)]))
                        secret-keys))]
    {:prod-config prod-config
     :dev-config dev-config
     :prod-secrets (get-secrets prod-config)
     :dev-secrets (get-secrets dev-config)}))

(comment
  ;; --------------------------------------------------------------------------
  ;; Setup
  ;; --------------------------------------------------------------------------
  ;;
  ;; `clj -M:dev dev` starts an nREPL server on port 7888. Connect your editor
  ;; to nrepl://localhost:7888 (Calva picks up .nrepl-port automatically).
  ;;
  ;; Every helper below takes an `env` as its first argument: :dev hits the
  ;; local dev database, :prod runs the same operation on production via the
  ;; SSH tunnel (open it with `ssh -f -N -L 7889:localhost:7888
  ;; app@mygreed.co.za`; override the local port with PROD_REPL_PORT).

  ;; --------------------------------------------------------------------------
  ;; Queries
  ;; --------------------------------------------------------------------------

  ;; All users:
  (docs-of :dev :user :user/email)
  (docs-of :prod :user :user/email)

  ;; A user's finances — swap the attr for other doc types: budget items
  ;; (:budget-item/user-id), goals (:goal/user-id), events (:event/user-id),
  ;; tax profiles (:tax-profile/user-id):
  (docs-of :dev :finances :finances/user-id)

  ;; A single user by email:
  (get-user :dev "you@example.com")

  ;; Run an arbitrary query, printing the result:
  (query-raw :dev "(count (com.biffweb/q (:biff/db (repl/get-context)) '{:find [u] :where [[u :user/email]]}))")

  ;; --------------------------------------------------------------------------
  ;; User management
  ;; --------------------------------------------------------------------------
  ;;
  ;; Each helper takes :dev or :prod and returns nil if the email doesn't match
  ;; a user.

  ;; Roles: users carry a set of roles (e.g. #{:admin}). There's no UI for
  ;; this yet, so assign them from the REPL:
  (make-admin :dev "you@example.com")
  (add-user-role :dev "you@example.com" :moderator)
  (remove-user-role :dev "you@example.com" :moderator)
  (get-user-roles :dev "you@example.com")

  ;; Activation: new signups are active, but a user counts as active only with
  ;; an explicit :user/active true — missing or nil means deactivated.
  ;; Deactivated users can't sign in (the signin form tells them to contact
  ;; support):
  (activate-user :dev "you@example.com")
  (deactivate-user :dev "you@example.com")
  (user-active? :dev "you@example.com")

  ;; Deletion: removes the user and all of their data (finances, budget items,
  ;; tax profile, events, goals). Returns the number of docs deleted:
  (delete-user :dev "you@example.com")

  ;; To run any of the above against prod, pass :prod instead (tunnel
  ;; required):
  ;; (user-active? :prod "you@example.com")
  ;; (activate-user :prod "you@example.com")

  ;; --------------------------------------------------------------------------
  ;; Passwords
  ;; --------------------------------------------------------------------------
  ;;
  ;; Accounts created before bcrypt hashing store plaintext passwords. They
  ;; still verify, but re-hash them so they're stored as bcrypt:
  (bcrypt-user-password :dev "you@example.com")
  (bcrypt-all-plaintext-passwords :dev)

  ;; --------------------------------------------------------------------------
  ;; Maintenance
  ;; --------------------------------------------------------------------------
  ;;
  ;; After changing main/initial-system, main/components, :tasks, :queues,
  ;; config.env, or deps.edn:
  (main/refresh)

  ;; Add fixture/seed data (dev only). After editing resources/fixtures.edn,
  ;; reset the db with `rm -r storage/xtdb` (never in prod), restart the app,
  ;; then call this again:
  (add-fixtures)

  ;; Update an existing user's email address:
  (let [{:keys [biff/db] :as ctx} (get-context)
        user-id (biff/lookup-id db :user/email "hello@example.com")]
    (biff/submit-tx ctx
      [{:db/doc-type :user
        :xt/id user-id
        :db/op :update
        :user/email "new.address@example.com"}]))

  ;; Test the job system (check the terminal for output):
  (biff/submit-job (get-context) :echo {:foo "bar"})
  (deref (biff/submit-job-for-result (get-context) :echo {:foo "bar"})))
