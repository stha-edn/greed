(ns repl
  (:require [com.greed :as main]
            [com.biffweb :as biff :refer [q]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

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
  ;; You can also use the `prod` helpers below for the same queries in one go.
  ;; Note: these only work when the tunnel is up.

  ;; (prod/query-users)
  ;; (prod/query-all)
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
