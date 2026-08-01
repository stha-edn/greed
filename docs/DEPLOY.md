# Deploying to DigitalOcean (Biff + XTDB + Postgres)

This is the single setup guide for running the Biff app in production. One Ubuntu droplet on
DigitalOcean hosts everything:

- **App:** Biff (Clojure) behind nginx + Let's Encrypt (HTTPS).
- **Storage:** XTDB uses the **jdbc** topology — the tx-log and document store live in a
  **Postgres** database on the same droplet. The index stays in RocksDB under
  `storage/xtdb/index` and is rebuilt from the tx-log on startup.

---

## 1. Create a DigitalOcean droplet

1. Sign up at <https://cloud.digitalocean.com/> (this [Biff referral link](https://m.do.co/c/141610534c91)
   gives new accounts $200 of credit for 60 days).
2. Go to **Droplets → Create droplet** (<https://cloud.digitalocean.com/droplets/new>).
3. **Region:** the region closest to your users (e.g. Amsterdam or Frankfurt if your users are in
   South Africa).
4. **Image:** Ubuntu **22.04 LTS** (or the latest LTS).
5. **Plan:** Basic → **Regular** (not Premium). **At least 1 GB RAM** ($6/mo); **2 GB** is more
   comfortable for the JVM + XTDB.
6. **Authentication:** add your SSH public key.
7. **Backups:** enable **Weekly backups** (under "Additional options"). This protects the droplet
   disk, which includes the Postgres data.
8. Create the droplet and note its **public IP**.

## 2. Domain and DNS

**With a domain** (recommended — e.g. `mygreed.co.za`):

1. In **Networking → Domains**, add your domain and follow the instructions to point its nameservers
   at DigitalOcean (or keep your registrar's nameservers and just add an A record there).
2. Create an **A record** for your domain (or a subdomain) pointing at the droplet's IP.

**Without a domain:** use a free wildcard DNS hostname — `<your-ip>.nip.io` resolves to your droplet
(e.g. `134.209.0.5.nip.io`). Use that everywhere below.

## 3. Firewall

Create a **cloud firewall** (Networking → Firewalls → Create Firewall) and apply it to the droplet,
allowing inbound:

| Port | Purpose |
|------|---------|
| 22   | SSH |
| 80   | HTTP (Let's Encrypt + redirect) |
| 443  | HTTPS |

Postgres stays bound to `localhost` — it is **not** exposed to the internet.

## 4. Prepare `config.env` locally

Create it if needed with `clj -M:dev generate-config`, then edit:

```bash
# Real domain, or <IP>.nip.io if you have none:
DOMAIN=mygreed.co.za

# Host the deploy task connects to. With a real domain, the same value works. With a nip.io domain,
# use the plain IP so Biff doesn't add a second .nip.io suffix:
DEPLOY_HOST=mygreed.co.za

# Storage: XTDB uses Postgres in production (self-hosted on the droplet).
PROD_XTDB_TOPOLOGY=jdbc
XTDB_JDBC_URL=jdbc:postgresql://localhost:5432/greed?user=greed&password=CHANGE_ME&sslmode=disable

# Email sign-in links (https://www.mailersend.com/):
MAILERSEND_API_KEY=
MAILERSEND_FROM=
MAILERSEND_REPLY_TO=

# Sign-in page bot protection (https://www.google.com/recaptcha/about/, v2 Invisible):
RECAPTCHA_SITE_KEY=
RECAPTCHA_SECRET_KEY=

# Keep dev on the filesystem — no Postgres needed locally:
XTDB_TOPOLOGY=standalone
```

Keep dev standalone so you don't need Postgres on your machine.

## 5. Install Postgres on the droplet

XTDB will store its tx-log and documents in Postgres. The Postgres JDBC driver and XTDB's jdbc
module are already bundled by Biff — no `deps.edn` change.

```bash
sudo apt update && sudo apt install -y postgresql
sudo systemctl enable --now postgresql

# Create the app role and database. The password MUST match XTDB_JDBC_URL in config.env.
sudo -u postgres psql -c "CREATE ROLE greed LOGIN PASSWORD 'CHANGE_ME';"
sudo -u postgres psql -c "CREATE DATABASE greed OWNER greed;"
```

XTDB creates its tables automatically the first time the app starts — no manual schema.

## 6. Run server setup

From your local machine:

```bash
scp server-setup.sh root@<your-domain-or-ip>:
ssh root@<your-domain-or-ip>
```

On the server:

```bash
bash server-setup.sh
```

- Answer the prompts; when **certbot** asks for a domain, enter the same value as `DOMAIN`.
- When it finishes, run `reboot`, wait a minute, and confirm you can SSH back in.
- The script creates the `app` user and a systemd service that runs the app on port 8080 behind
  nginx. (If `server-setup.sh` fails with `$'\r'` errors, it has Windows line endings — run
  `sed -i 's/\r$//' server-setup.sh` then `sudo bash server-setup.sh`.)

## 7. Add the git remote (Windows / no rsync)

This repo deploys with git (see `:biff.tasks/git-deploy-cmd` in `resources/config.edn`), so you need
the prod remote:

```bash
git remote add prod ssh://app@<your-domain-or-ip>/home/app/repo.git
```

## 8. Deploy and verify

```bash
clj -M:dev deploy
clj -M:dev logs
```

Wait for `System started`, then open `https://<your-domain>` in a browser.

> If deploy fails with `Error connecting to agent`, the SSH agent isn't running on Windows —
> `:biff.tasks/skip-ssh-agent true` is already set in `resources/config.edn`, so the task uses the
> `IdentityFile` from your `~/.ssh/config` instead. Make sure the host has an entry like:
>
> ```
> Host <your-domain-or-ip>
>   User app
>   IdentityFile path/to/your-key.pem
> ```

## 9. What lives where

| Data | Location |
|------|----------|
| XTDB transaction log + documents | Postgres (`greed` DB on `localhost:5432`) |
| XTDB index (transient; rebuilt from tx-log) | `/home/app/storage/xtdb/index` |
| App code, `config.env`, secrets | `/home/app` on the droplet |

Your app code is unchanged — queries (`q`) and writes (`biff/submit-tx`) work the same with the
Postgres-backed topology.

## 10. Backups

- **Droplet weekly backups** (step 1) snapshot the whole disk, including Postgres — the simplest
  option.
- For finer control, take a **volume snapshot** in the DO console before deploys, or run
  `pg_dump` if you only care about the current state. The XTDB index under `storage/xtdb/index` is
  disposable — it rebuilds from the tx-log on startup.

## 11. Migrating existing data

Moving from the old standalone setup (data under `storage/xtdb` on the old server)? The old
transaction log does not carry over to Postgres automatically. Either:

- Start fresh (fine if the app isn't live yet), or
- Copy current documents from the old node into the new one with a one-off script (old standalone
  node → new Postgres-backed node). Ask in the repo issue tracker if you need help writing it.

## 12. Useful commands

| Task | Command |
|------|---------|
| Deploy | `clj -M:dev deploy` |
| View logs | `clj -M:dev logs` |
| SSH as app user | `ssh app@<your-domain-or-ip>` |
| Restart app | `ssh app@<your-domain-or-ip> 'sudo systemctl restart app'` |
| Postgres status | `sudo systemctl status postgresql` |

## Troubleshooting

- **502 Bad Gateway:** the JVM can take 30–60 s to start. Check `clj -M:dev logs` or
  `ssh app@<host> 'sudo journalctl -u app -f'`.
- **App won't start after deploy:** Postgres must be up before the app (jdbc topology). Check
  `sudo systemctl status postgresql` and that `XTDB_JDBC_URL`'s password matches the role you
  created in step 5.
- **Certificate errors:** the hostname you used for certbot must match `DOMAIN`, and ports 80/443
  must be open in the firewall (step 3).
- **`fatal: 'prod' does not appear to be a git repository`:** add the remote (step 7).
- **Double `.nip.io`:** if `DOMAIN` is `<IP>.nip.io`, set `DEPLOY_HOST` to the plain IP.
- **SSH / scp from Windows:** use the `app` user for deploys, `root` only for `server-setup.sh`.
