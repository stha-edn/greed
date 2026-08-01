# Data and application security (overview)

This is an overview of how secure your data and app are in the current setup (Biff + XTDB + Postgres on a DigitalOcean droplet — see [DEPLOY.md](DEPLOY.md)). No code or config changes are required; this is for your awareness.

---

## 1. Data in transit (browser ↔ server)

- **HTTPS:** You use nginx + Let’s Encrypt and `DOMAIN=...nip.io`, so traffic between users’ browsers and your server is encrypted (TLS). Passwords, session cookies, and all request/response data are protected in transit.
- **Prod middleware:** `:biff.middleware/secure` is `true` in prod, so the app expects HTTPS and can enforce secure cookies.

**Summary:** Traffic to and from the app is encrypted. Good.

---

## 2. Data at rest (on the server)

- **XTDB storage:** The XTDB transaction log and documents live in **Postgres** on the droplet; the index is a RocksDB cache under `/home/app/storage/xtdb/index`. Neither the app nor Postgres encrypts this data; it’s stored in plain form.
- **Disk encryption:** DigitalOcean offers encryption at rest for droplet disks and volumes — enable it when creating the droplet/volume if you want the on-disk data encrypted. Otherwise, if someone gets access to the disk (e.g. via a stolen snapshot), they can read the files.
- **Postgres access:** Postgres listens on `localhost` only; only the app process on the droplet connects to it. It is not exposed to the internet (see the firewall rules in DEPLOY.md).
- **Passwords:** Your app stores user passwords in XTDB. Biff’s auth typically hashes them (e.g. bcrypt); the hash is what’s at rest. If the disk is compromised, an attacker could try to crack those hashes, but not see the raw passwords.

**Summary:** Data at rest is not encrypted by the app; disk is only encrypted if you enabled droplet/volume encryption. Passwords are stored as hashes.

---

## 3. Who can access what

- **Application access:** Only signed-in users (via your Biff auth and session) can use the app. Routes are protected by middleware (e.g. `wrap-signed-in`). Session is tied to a signed cookie (using `COOKIE_SECRET`). So “data security” for users is: only they (and anyone with their session) can see their own data as exposed by your UI and API.
- **Database access:** Postgres is bound to `localhost` on the droplet; only the Clojure app process connects to it. There is no public database port for the internet to hit.
- **Server access:** Only people with (1) your SSH private key (for `root`/`app`) or (2) your DigitalOcean account credentials can get into the VM or the DO account. If someone has the key or the account, they can read everything on the server (code, config, Postgres data, env vars).

**Summary:** Access is controlled by login and sessions; the DB is not publicly exposed; server access is limited to SSH (and DigitalOcean) and is as secure as your keys and account.

---

## 4. Secrets (passwords, API keys, signing keys)

- **Where they live:** `config.env` (and env vars on the server) hold `COOKIE_SECRET`, `JWT_SECRET`, `XTDB_JDBC_URL` (contains the Postgres password), and any API keys (e.g. MailerSend, reCAPTCHA). That file is deployed to the server (in `:biff.tasks/deploy-untracked-files`), so secrets sit on the droplet.
- **How they’re used:** Cookie and JWT secrets sign session and token data; they are not sent to the browser. If an attacker gets the server (or a copy of `config.env`), they can forge sessions and impersonate users until you rotate those secrets and redeploy.
- **Best practice:** Keep `config.env` out of git (it is in `.gitignore`). Restrict who has SSH and DigitalOcean access. For higher sensitivity, use a secrets manager and inject env vars at runtime instead of deploying `config.env` as a file.

**Summary:** Secrets are on the server and in `config.env`; protect the server and the file. No automatic rotation unless you add it.

---

## 5. Things that improve security (optional)

- **Disk encryption:** Turn on encryption for the droplet’s root disk (and any extra volumes) so the disk is encrypted at rest.
- **Backups:** Enable DigitalOcean **weekly backups** or take periodic **volume snapshots** (protects Postgres). Store `config.env` in an access-controlled location as well.
- **Updates:** Keep the OS and the JVM/dependencies updated so you get security patches.
- **Narrow SSH:** In the cloud firewall, restrict SSH (port 22) to “My IP” or a small set of IPs instead of `0.0.0.0/0` if you don’t need SSH from anywhere.
- **Secrets manager:** For stricter control, store secrets in a secrets manager and load them into the app’s environment instead of deploying `config.env`.

---

## 6. Short summary

| Aspect              | How secure / what to know |
|---------------------|----------------------------|
| **In transit**      | HTTPS (TLS) via nginx + Let’s Encrypt. Traffic is encrypted. |
| **At rest**         | Postgres data and config on disk are not encrypted by the app. Use droplet/volume encryption if you want encrypted disk. Passwords are stored as hashes. |
| **App access**      | Only signed-in users; sessions and routes are protected. |
| **DB access**       | No public DB port; Postgres is bound to `localhost` and only the app process connects. |
| **Server access**   | Whoever has your SSH key or DigitalOcean account can access everything on the server. |
| **Secrets**         | Stored on the server in `config.env`; protect the server and restrict who can deploy. |

For a typical small app with moderate sensitivity, this setup is **reasonably secure** if you protect SSH keys and DigitalOcean access, keep HTTPS, and (optionally) add disk encryption and backups. For highly sensitive data (e.g. health, payments), you’d add encryption at rest, stricter secrets management, and possibly compliance-focused controls.
