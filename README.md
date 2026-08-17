# Greed

Personal finance and tax management app for South African users, built with [Biff](https://biffweb.com).

## Stack

| Layer | Technology |
|---|---|
| Framework | Biff v1.9.0 (Clojure full-stack) |
| Database | XTDB (standalone in dev, JDBC/PostgreSQL in prod) |
| Frontend | Server-rendered HTML (Rum), htmx for interactivity |
| CSS | Tailwind CSS v3.4.17 with `@tailwindcss/forms` |
| Auth | Biff auth + reCAPTCHA v2 invisible |
| Email | MailerSend |
| Server | Jetty (Clojure), Nginx reverse proxy, Let's Encrypt SSL |
| Runtime | Clojure 1.12.0, Java 21 |

## Getting started

### Prerequisites

- Java 17+
- Clojure CLI tools (install via https://clojure.org/guides/install_clojure)
- Node.js (optional, for npm-based tooling)

### Local development

```bash
# Copy and fill in environment variables
cp resources/config.template.env config.env

# Start the dev server (hot reload + Tailwind watch)
clj -M:dev dev

# Or start without Tailwind watch
clj -M:dev start
```

The app runs on http://localhost:8080. An nREPL server is available on port 7888.

### Useful alias

Add to your `.bashrc` or PowerShell profile:
```bash
alias biff='clj -M:dev'
```

## Commands

All commands run via the Biff task runner:

| Command | Description |
|---|---|
| `clj -M:dev dev` | Start dev server with hot reload and Tailwind watch |
| `clj -M:dev start` | Start the app (no file watching) |
| `clj -M:dev stop` | Stop the app |
| `clj -M:dev restart` | Restart the app |
| `clj -M:dev css` | Build Tailwind CSS |
| `clj -M:dev css --minify` | Build minified Tailwind CSS (production) |
| `clj -M:dev run-tests` | Run tests |
| `clj -M:dev deploy` | Full deploy to production server |
| `clj -M:dev soft-deploy` | Hot-reload code on the server without restarting |
| `clj -M:dev prod-dev` | Watch local files and auto-soft-deploy on change |
| `clj -M:dev uberjar` | Build a production uberjar |
| `clj -M:dev generate-secrets` | Generate new `COOKIE_SECRET` and `JWT_SECRET` values |
| `clj -M:dev bump-version` | Increment app version to trigger user-facing "app updated" banner |
| `clj -M:dev install-tailwind` | Download the Tailwind standalone binary |
| `clj -M:dev help` | List all available tasks |

## Deployment

### Production server

The app is deployed to a self-hosted VPS (`mygreed.co.za`) managed by systemd.

**Automated (CI/CD):** Push to `master` triggers a [GitHub Actions workflow](.github/workflows/deploy.yml) that:

1. Builds minified Tailwind CSS
2. Uploads the CSS to the server
3. Pushes code to the prod git remote
4. The server's `post-receive` hook checks out the code and restarts the app

**Manual:**

```bash
# Full deploy (builds assets + pushes to server)
clj -M:dev deploy

# Quick hot-reload (evals new code without full restart)
clj -M:dev soft-deploy

# Push code directly via git
git push prod master
```

### Server setup

The VPS is provisioned by `server-setup.sh`, which installs:

- Java runtime, Clojure CLI, Babashka, Trenchman
- Nginx (reverse proxy with SSL via Let's Encrypt)
- PostgreSQL (for XTDB prod storage)
- An `app` systemd service with auto-restart on failure

### Configuring production secrets

Secrets live in `config.env` on the server (not in git). To update:

```bash
ssh app@mygreed.co.za
nano /home/app/config.env
sudo systemctl restart app
```

## Environment variables

See [`resources/config.template.env`](resources/config.template.env) for the full list.

Key secrets (set in `config.env` on the server):

| Variable | Purpose |
|---|---|
| `DOMAIN` | Production domain name |
| `COOKIE_SECRET` | Encrypts session cookies |
| `JWT_SECRET` | Encrypts email sign-in links |
| `MAILERSEND_API_KEY` | MailerSend API token |
| `RECAPTCHA_SECRET_KEY` | reCAPTCHA v2 secret |
| `XTDB_JDBC_URL` | PostgreSQL connection string for XTDB |

## Project structure

```
src/com/
  greed.clj              # App entry point, system bootstrap
  core.clj               # Core utilities
  app.clj                # Biff system config
  email.clj              # Email sending (MailerSend)
  password.clj           # Password reset logic
  schema.clj             # Database schema
  data/                  # Data layer
  ui/
    pages/               # Public pages (home, signin, signup, etc.)
    app/                 # Authenticated app views (dashboard, finances, etc.)
    components/          # Reusable UI components (nav, cards, tables, etc.)
    tools/               # Tax calculators
resources/
  config.edn             # Main config (deps, aliases, task config)
  config.template.env    # Template for config.env
  tailwind.css           # Tailwind CSS source
  tailwind.config.js     # Tailwind configuration
  public/                # Static assets (fonts, images, JS)
dev/
  tasks.clj              # Custom task overrides
  repl.clj               # REPL helpers
```
