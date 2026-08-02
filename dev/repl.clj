(ns repl
  (:require [com.greed :as main]
            [com.biffweb :as biff :refer [q]]
            [com.greed.data.core :as data]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]
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

(defn bcrypt-user-password
  "Re-hashes a user's stored password to bcrypt, selected by email.
  Useful for upgrading users who still have legacy plaintext passwords.
  Returns the user id, or nil if no user has that email."
  [email]
  (let [{:keys [biff/db] :as ctx} (get-context)
        user-id (biff/lookup-id db :user/email email)
        user    (when user-id (data/get-user ctx user-id))]
    (when user
      (biff/submit-tx ctx
        [{:db/doc-type :user
          :xt/id user-id
          :db/op :update
          :user/password (data/hash-password (:user/password user))}]))))

(defn bcrypt-all-plaintext-passwords
  "Sweeps the whole db and re-hashes every user whose stored password isn't
  already bcrypt. Returns the list of emails that were upgraded."
  []
  (let [{:keys [biff/db] :as ctx} (get-context)
        users (q db
                 '{:find (pull user [*])
                   :where [[user :user/email]]})
        plaintext-users (filter #(not (str/starts-with? (:user/password %) "bcrypt"))
                                users)]
    (doseq [user plaintext-users]
      (biff/submit-tx ctx
        [{:db/doc-type :user
          :xt/id (:xt/id user)
          :db/op :update
          :user/password (data/hash-password (:user/password user))}]))
    (mapv :user/email plaintext-users)))

(defn get-user-roles
  "Returns the set of roles for the user with the given email."
  [email]
  (let [{:keys [biff/db] :as ctx} (get-context)
        user-id (biff/lookup-id db :user/email email)
        user    (when user-id (data/get-user ctx user-id))]
    (:user/roles user)))

(defn add-user-role
  "Adds `role` to the user's :user/roles set (idempotent). Returns the
  user-id, or nil if no user has that email."
  [email role]
  (let [{:keys [biff/db] :as ctx} (get-context)
        user-id (biff/lookup-id db :user/email email)
        user    (when user-id (data/get-user ctx user-id))]
    (when user
      (biff/submit-tx ctx
        [{:db/doc-type :user
          :xt/id user-id
          :db/op :update
          :user/roles (conj (set (:user/roles user)) role)}])
      user-id)))

(defn remove-user-role
  "Removes `role` from the user's :user/roles set (idempotent). Returns the
  user-id, or nil if no user has that email."
  [email role]
  (let [{:keys [biff/db] :as ctx} (get-context)
        user-id (biff/lookup-id db :user/email email)
        user    (when user-id (data/get-user ctx user-id))]
    (when user
      (biff/submit-tx ctx
        [{:db/doc-type :user
          :xt/id user-id
          :db/op :update
          :user/roles (disj (set (:user/roles user)) role)}])
      user-id)))

(defn make-admin
  "Gives the user with the given email the :admin role. Returns the user-id,
  or nil if no user has that email."
  [email]
  (add-user-role email :admin))

(defn user-active?
  "Returns whether the user with the given email is active. Only users with an
  explicit :user/active true count as active; a missing or nil value counts as
  deactivated. Returns nil if no user has that email."
  [email]
  (let [{:keys [biff/db] :as ctx} (get-context)
        user-id (biff/lookup-id db :user/email email)
        user    (when user-id (data/get-user ctx user-id))]
    (when user
      (data/user-active? user))))

(defn set-user-active
  "Sets whether the user with the given email is active. Returns the user-id,
  or nil if no user has that email."
  [email active?]
  (let [{:keys [biff/db] :as ctx} (get-context)
        user-id (biff/lookup-id db :user/email email)]
    (when user-id
      (biff/submit-tx ctx
        [{:db/doc-type :user
          :xt/id user-id
          :db/op :update
          :user/active (boolean active?)}])
      user-id)))

(defn activate-user
  "Reactivates the user with the given email. Returns the user-id, or nil if
  no user has that email."
  [email]
  (set-user-active email true))

(defn deactivate-user
  "Deactivates the user with the given email. Deactivated users can't sign in.
  Returns the user-id, or nil if no user has that email."
  [email]
  (set-user-active email false))

;; ----------------------------------------------------------------------------
;; Production
;; ----------------------------------------------------------------------------
;;
;; The prod app runs its own nREPL server on port 7888 (see NREPL_PORT in
;; config.env), reachable only on the server itself. To use the prod-* helpers
;; below, first open an SSH tunnel from your machine, mapping a local port to
;; the server's 7888:
;;
;;   ssh -f -N -L 7889:localhost:7888 app@mygreed.co.za
;;
;; then, if you chose a local port other than 7889, set PROD_REPL_PORT to it.
;; The helpers send raw code strings over nREPL because prod runs a separate
;; process that doesn't share this project's classpath.

(def prod-port
  (or (some-> (System/getenv "PROD_REPL_PORT") Integer/parseInt)
      7889))

(defn prod-eval-code
  "Evaluates `code` on the production nREPL server and returns the result
  values. Prints any errors the server reports."
  [code]
  (let [conn   (nrepl/connect :host "localhost" :port prod-port)
        client (nrepl/client conn 30000)]
    (try
      (let [msgs (nrepl/message client {:op "eval" :code code})
            errors (filter #(or (:err %) (:ex %)) msgs)]
        (doseq [{:keys [err ex]} errors]
          (println "nREPL error:" (or err ex)))
        (nrepl/response-values msgs))
      (finally
        (.close conn)))))

(defn- prod-run-query
  "Runs an XTDB `pull` query for all docs of the given entity/anchor pair on
  prod. `anchor` is an attribute guaranteed to exist on every doc of that
  type (e.g. :user/email for users)."
  [entity anchor]
  (let [code (str "(let [{:keys [biff/db]} (repl/get-context)]"
                  "  (com.biffweb/q db"
                  "    '{:find (pull " entity " [*])"
                  "      :where [[" entity " " anchor "]]}))")]
    (prod-eval-code code)))

(defn prod-query-users
  "Returns all users on prod."
  []
  (prod-run-query "user" ":user/email"))

(defn prod-query-finances
  "Returns all finances docs on prod."
  []
  (prod-run-query "finances" ":finances/user-id"))

(defn prod-query-budget-items
  "Returns all budget items on prod."
  []
  (prod-run-query "budget-item" ":budget-item/user-id"))

(defn prod-query-goals
  "Returns all goals on prod."
  []
  (prod-run-query "goal" ":goal/user-id"))

(defn prod-query-events
  "Returns all events on prod."
  []
  (prod-run-query "event" ":event/user-id"))

(defn prod-query-tax-profiles
  "Returns all tax profiles on prod."
  []
  (prod-run-query "tax-profile" ":tax-profile/user-id"))

(defn prod-query-raw
  "Runs an arbitrary EDN XTDB query (as a string) on prod, pprinting the result."
  [code]
  (doseq [r (prod-eval-code code)]
    (pp/pprint r)))

(defn prod-query-all
  "Prints every doc of every doc-type present in the prod db."
  []
  (let [code (str "(let [{:keys [biff/db]} (repl/get-context)]"
                  "  (sort-by :db/doc-type"
                  "    (com.biffweb/q db"
                  "      '{:find [(pull e [*])]"
                  "        :where [[e :db/doc-type]]})))")
        docs (prod-eval-code code)]
    (doseq [d (first docs)]
      (pp/pprint d))))

(defn prod-bcrypt-user-password
  "Re-hashes a prod user's stored password to bcrypt, selected by email.
  Returns the user id (or nil if no such email)."
  [email]
  (let [code (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
                  "        user-id (com.biffweb/lookup-id db :user/email " (pr-str email) ")"
                  "        user (when user-id (com.greed.data.core/get-user ctx user-id))]"
                  "  (when user"
                  "    (com.biffweb/submit-tx ctx"
                  "      [{:db/doc-type :user"
                  "        :xt/id user-id"
                  "        :db/op :update"
                  "        :user/password (com.greed.data.core/hash-password (:user/password user))}]))"
                  "  user-id)")]
    (first (prod-eval-code code))))

(defn prod-bcrypt-all-plaintext-passwords
  "Re-hashes every prod user whose stored password isn't already bcrypt.
  Returns the list of upgraded emails."
  []
  (let [code (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
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
                  "  (mapv :user/email plaintext))")]
    (first (prod-eval-code code))))

(defn prod-get-user-roles
  "Returns the set of roles for the prod user with the given email. Requires
  the tunnel."
  [email]
  (first (prod-eval-code
           (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
                "      user (com.greed.data.core/get-user ctx"
                "            (com.biffweb/lookup-id db :user/email " (pr-str email) "))]"
                "  (:user/roles user))"))))

(defn prod-add-user-role
  "Adds `role` to the prod user's :user/roles set (idempotent). Requires the
  tunnel. Returns the user-id, or nil if no user has that email."
  [email role]
  (first (prod-eval-code
           (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
                "        user-id (com.biffweb/lookup-id db :user/email " (pr-str email) ")"
                "        user (when user-id (com.greed.data.core/get-user ctx user-id))]"
                "  (when user"
                "    (com.biffweb/submit-tx ctx"
                "      [{:db/doc-type :user"
                "        :xt/id user-id"
                "        :db/op :update"
                "        :user/roles (conj (set (:user/roles user)) " (pr-str role) ")}]))"
                "  user-id)"))))

(defn prod-remove-user-role
  "Removes `role` from the prod user's :user/roles set (idempotent). Requires
  the tunnel. Returns the user-id, or nil if no user has that email."
  [email role]
  (first (prod-eval-code
           (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
                "        user-id (com.biffweb/lookup-id db :user/email " (pr-str email) ")"
                "        user (when user-id (com.greed.data.core/get-user ctx user-id))]"
                "  (when user"
                "    (com.biffweb/submit-tx ctx"
                "      [{:db/doc-type :user"
                "        :xt/id user-id"
                "        :db/op :update"
                "        :user/roles (disj (set (:user/roles user)) " (pr-str role) ")}]))"
                "  user-id)"))))

(defn prod-make-admin
  "Gives the prod user with the given email the :admin role. Requires the
  tunnel. Returns the user-id, or nil if no user has that email."
  [email]
  (prod-add-user-role email :admin))

(defn prod-user-active?
  "Returns whether the prod user with the given email is active. Only users
  with an explicit :user/active true count as active; a missing or nil value
  counts as deactivated. Returns nil if no user has that email. Requires the
  tunnel."
  [email]
  (first (prod-eval-code
           (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
                "      user (com.greed.data.core/get-user ctx"
                "            (com.biffweb/lookup-id db :user/email " (pr-str email) "))]"
                "  (when user (com.greed.data.core/user-active? user)))"))))

(defn prod-set-user-active
  "Sets whether the prod user with the given email is active. Requires the
  tunnel. Returns the user-id, or nil if no user has that email."
  [email active?]
  (first (prod-eval-code
           (str "(let [{:keys [biff/db] :as ctx} (repl/get-context)"
                "        user-id (com.biffweb/lookup-id db :user/email " (pr-str email) ")]"
                "  (when user-id"
                "    (com.biffweb/submit-tx ctx"
                "      [{:db/doc-type :user"
                "        :xt/id user-id"
                "        :db/op :update"
                "        :user/active " (if active? "true" "false") "}]))"
                "  user-id)"))))

(defn prod-activate-user
  "Reactivates the prod user with the given email. Requires the tunnel.
  Returns the user-id, or nil if no user has that email."
  [email]
  (prod-set-user-active email true))

(defn prod-deactivate-user
  "Deactivates the prod user with the given email. Deactivated users can't
  sign in. Requires the tunnel. Returns the user-id, or nil if no user has
  that email."
  [email]
  (prod-set-user-active email false))

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
  ;; Querying the database
  ;; --------------------------------------------------------------------------
  ;;
  ;; The `clj -M:dev dev` command starts an nREPL server on port 7888. Connect
  ;; your editor to it (e.g. Cursive: "Connect to Running REPL" ->
  ;; nrepl://localhost:7888; Calva will pick up .nrepl-port automatically).
  ;;
  ;; All queries below read from the DEV database (local storage/xtdb). See the
  ;; "Production" section below for querying prod.

  ;; All users (the "user table")
  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db
       '{:find (pull user [*])
         :where [[user :user/email]]}))

  ;; One user by email
  (biff/lookup-id (:biff/db (get-context)) :user/email "you@example.com")

  ;; All budget items
  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db
       '{:find (pull b [*])
         :where [[b :budget-item/user-id]]}))

  ;; All goals
  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db
       '{:find (pull g [*])
         :where [[g :goal/user-id]]}))

  ;; All events
  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db
       '{:find (pull e [*])
         :where [[e :event/user-id]]}))

  ;; Count users
  (count (q (:biff/db (get-context))
            '{:find [user]
              :where [[user :user/email]]}))

  ;; Every "table" (XTDB doc type) present in the db
  (sort (map :db/doc-type
             (q (:biff/db (get-context))
                '{:find [(pull e [*])]
                  :where [[e :db/doc-type]]})))

  ;; --------------------------------------------------------------------------
  ;; Production
  ;; --------------------------------------------------------------------------
  ;;
  ;; The prod app also runs an nREPL server on port 7888 (see NREPL_PORT in
  ;; config.env), but it's only reachable on the server itself. To connect,
  ;; open an SSH tunnel from your machine, then connect your editor to the
  ;; LOCAL port you chose instead of 7888:
  ;;
  ;;   ssh -f -N -L 7889:localhost:7888 app@mygreed.co.za
  ;;
  ;;   -> connect to nrepl://localhost:7889
  ;;
  ;; Once connected, run the same queries as above — they'll hit the prod
  ;; Postgres-backed XTDB. To make sure you're on prod and not dev, check
  ;; which users exist (dev and prod have different seed data), e.g.:
  ;;
  ;;   (let [{:keys [biff/db]} (get-context)]
  ;;     (count (q db '{:find [user] :where [[user :user/email]]})))
  ;;
  ;; For one-shot queries you don't need to connect an editor at all: the
  ;; prod-* helpers below send code strings to the server through the tunnel.
  ;; They only work while the tunnel is up, and use prod-port (default 7889,
  ;; override with PROD_REPL_PORT) as the local port.

  ;; (prod-query-users)
  ;; (prod-query-finances)
  ;; (prod-query-budget-items)
  ;; (prod-query-goals)
  ;; (prod-query-events)
  ;; (prod-query-tax-profiles)
  ;; (prod-query-all)
  ;; (prod-query-raw "(count (com.biffweb/q (:biff/db (repl/get-context)) '{:find [u] :where [[u :user/email]]}))")

  ;; --------------------------------------------------------------------------
  ;; User roles / admin
  ;; --------------------------------------------------------------------------
  ;;
  ;; Users can carry a set of roles via :user/roles (e.g. #{:admin}). There's
  ;; no UI for this yet, so assign them from the REPL. The helpers below act on
  ;; the local dev db; prefix with prod- for the equivalent on prod (tunnel
  ;; required).

  ;; Make a user an admin:
  (make-admin "you@example.com")
  ;; (prod-make-admin "you@example.com")

  ;; Add / remove an arbitrary role:
  (add-user-role "you@example.com" :moderator)
  (remove-user-role "you@example.com" :moderator)
  ;; (prod-add-user-role "you@example.com" :moderator)
  ;; (prod-remove-user-role "you@example.com" :moderator)

  ;; Check what roles a user has:
  (get-user-roles "you@example.com")
  ;; (prod-get-user-roles "you@example.com")

  ;; --------------------------------------------------------------------------
  ;; Account status / activation
  ;; --------------------------------------------------------------------------
  ;;
  ;; Users are active by default. Deactivated users can't sign in (the signin
  ;; form tells them to contact support). The helpers below act on the local
  ;; dev db; prefix with prod- for the equivalent on prod (tunnel required).

  ;; Deactivate / reactivate a user:
  (deactivate-user "you@example.com")
  (activate-user "you@example.com")
  ;; (prod-deactivate-user "you@example.com")
  ;; (prod-activate-user "you@example.com")

  ;; Check whether a user is active (nil means no such email):
  (user-active? "you@example.com")
  ;; (prod-user-active? "you@example.com")

  ;; --------------------------------------------------------------------------
  ;; Password hashing / migration
  ;; --------------------------------------------------------------------------
  ;;
  ;; Accounts created before bcrypt hashing store plaintext passwords. They
  ;; still verify (validate-password? handles both), but re-hash them so they're
  ;; stored as bcrypt. The helpers below submit the write transaction.
  ;; Use the plain bcrypt-* helpers against the local dev db; use the
  ;; prod-bcrypt-* helpers (with the tunnel up) to do the same on prod.

  ;; Re-hash ONE user's password to bcrypt, selected by email (local dev db):
  (bcrypt-user-password "you@example.com")

  ;; Upgrade EVERY user with a non-bcrypt password in the local dev db.
  ;; Returns the list of emails that were upgraded:
  (bcrypt-all-plaintext-passwords)

  ;; Same as the two above, but against production (requires the tunnel):
  ;; (prod-bcrypt-user-password "you@example.com")
  ;; (prod-bcrypt-all-plaintext-passwords)

  ;; Or do it manually: fetch the user, then submit an update with a fresh hash.
  (let [{:keys [biff/db] :as ctx} (get-context)
        user-id (biff/lookup-id db :user/email "you@example.com")
        user    (data/get-user ctx user-id)]
    (biff/submit-tx ctx
      [{:db/doc-type :user
        :xt/id user-id
        :db/op :update
        :user/password (data/hash-password (:user/password user))}]))
  )

(comment
  ;; Call this function if you make a change to main/initial-system,
  ;; main/components, :tasks, :queues, config.env, or deps.edn.
  (main/refresh)

  ;; Call this in dev if you'd like to add some seed data to your database. If
  ;; you edit the seed data (in resources/fixtures.edn), you can reset the
  ;; database by running `rm -r storage/xtdb` (DON'T run that in prod),
  ;; restarting your app, and calling add-fixtures again.
  (add-fixtures)

  ;; Query the database
  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db
       '{:find (pull user [*])
         :where [[user :user/email]]}))

  ;; Update an existing user's email address
  (let [{:keys [biff/db] :as ctx} (get-context)
        user-id (biff/lookup-id db :user/email "hello@example.com")]
    (biff/submit-tx ctx
      [{:db/doc-type :user
        :xt/id user-id
        :db/op :update
        :user/email "new.address@example.com"}]))

  (sort (keys (get-context)))

  ;; Check the terminal for output.
  (biff/submit-job (get-context) :echo {:foo "bar"})
  (deref (biff/submit-job-for-result (get-context) :echo {:foo "bar"})))
