# XTDB setup and usage

XTDB is already wired up in your Biff app. This doc summarizes how it’s configured and how to use it.
For the production deployment (DigitalOcean + Postgres), see **[DEPLOY.md](DEPLOY.md)**.

---

## 1. Current configuration

### Config (`resources/config.edn` + `config.env`)

| Setting | Value | Meaning |
|--------|--------|--------|
| `:biff.xtdb/dir` | `"storage/xtdb"` | Where XTDB keeps its RocksDB **index** (both topologies). |
| `XTDB_TOPOLOGY` | `standalone` (config.env) | Dev: data on local disk, no Postgres needed. |
| `PROD_XTDB_TOPOLOGY` | `jdbc` (config.env) | Prod: XTDB stores tx-log + documents in Postgres. |
| `XTDB_JDBC_URL` | `jdbc:postgresql://localhost:5432/greed?...` | Postgres connection for prod (see DEPLOY.md). |

### Where data lives

- **Dev:** `storage/xtdb` under your project — standalone, no Postgres required.
- **Prod:** the XTDB **transaction log and documents** live in Postgres (the `greed` database on the
  droplet); the **index** is a disposable RocksDB cache under `/home/app/storage/xtdb/index` that
  rebuilds from the tx-log on startup.

---

## 2. How the app uses XTDB

- **Startup:** `com.greed` uses Biff’s `biff/use-xtdb` and `biff/use-xtdb-tx-listener` so the system starts an XTDB node and injects `biff/db` and `biff.xtdb/node` where needed.
- **Schema:** `com.greed.schema/schema` defines your entity shapes (user, finances, budget-item). The schema is used for validation; XTDB itself is schemaless.
- **Queries:** Handlers and data layer get `:biff/db` from the request/system and use `q` (Biff’s query helper). Example: `com.greed.data.core/get-user`, `get-budget-items`, etc.
- **Writes:** Same code uses `biff/submit-tx` with maps that include `:db/doc-type` (e.g. `:user`, `:finances`, `:budget-item`) and `:xt/id`.

So “setting up XTDB” for this project means: **config is already set; just run the app (dev or prod).**

---

## 3. Dev: run and inspect

1. Start the app:
   ```bash
   clj -M:dev dev
   ```
2. Use the app; data is written to `storage/xtdb` under the project.
3. (Optional) In a REPL connected to dev, you can read the DB, e.g.:
   ```clojure
   (require '[com.biffweb :as biff])
   (def db (:biff/db @com.greed/system))
   (biff/q db '{:find [(pull user [*])], :where [[user :user/email]]})
   ```

---

## 4. Production: Postgres storage

Prod runs XTDB with the **jdbc** topology: tx-log + documents in Postgres, index in RocksDB. Your
app code (queries, `submit-tx`) is unchanged.

Full setup — installing Postgres on the droplet, creating the `greed` role/database, the
`XTDB_JDBC_URL` value, and backups — is in **[DEPLOY.md](DEPLOY.md)**.

Leave `XTDB_TOPOLOGY=standalone` for dev so you don’t need Postgres locally.

---

## 5. Quick reference: query and write

- **Query:** In a handler or service that receives `ctx` (with `:biff/db`):
  ```clojure
  (q (:biff/db ctx) '{:find [(pull user [*])], :where [[user :user/email email]]} email)
  ```
- **Write:** Use `biff/submit-tx` with one or more maps:
  ```clojure
  (biff/submit-tx ctx [{:db/doc-type :user
                        :xt/id (random-uuid)
                        :user/email "a@b.com"
                        ...}])
  ```
- **Schema:** Add or change entity shapes in `com.greed.schema/schema` and use `:db/doc-type` in transactions to match.

For more detail, see [Biff’s XTDB docs](https://biffweb.com/docs/reference/database/) and your existing `com.greed.data.core` and `com.greed.schema` namespaces.
