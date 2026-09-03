# Deploy MDL Platform on a Cloud VPS

Run the full stack (MariaDB, Spring Boot API, React UI, HTTPS) on any Linux VPS — DigitalOcean, Hetzner, AWS EC2, Linode, etc.

**Result:** Your team opens `https://your-domain.example` from anywhere.

---

## What you need

| Item | Notes |
|------|--------|
| VPS | 2 GB RAM minimum (4 GB recommended), Ubuntu 22.04 or 24.04 |
| Domain | A record → VPS public IP (required for free Let's Encrypt HTTPS) |
| SSH access | Root or sudo user |
| Git | To clone the repository |

The stack uses **Docker Compose** with **Caddy** for automatic HTTPS. Only ports **80** and **443** are exposed publicly; MariaDB and the API stay on the internal Docker network.

---

## 1. Prepare the VPS

SSH into the server and install Docker:

```bash
sudo apt update && sudo apt upgrade -y
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"
# Log out and back in so the docker group applies
```

Optional — restrict the firewall:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

---

## 2. DNS

Create an **A record** for your app hostname, e.g. `app.yourcompany.com` → your VPS IP.

Wait for DNS to propagate (often a few minutes). Caddy needs the domain to resolve to this server to issue a certificate.

---

## 3. Clone and configure

```bash
git clone https://github.com/YOUR_ORG/mdl-platform.git
cd mdl-platform
cp .env.production.example .env
```

Edit `.env` and set strong values:

```bash
nano .env
```

| Variable | How to set |
|----------|------------|
| `DOMAIN` | Your public hostname, e.g. `app.yourcompany.com` |
| `LETSENCRYPT_EMAIL` | Email for Let's Encrypt expiry notices |
| `DB_PASSWORD` / `DB_ROOT_PASSWORD` | Long random strings |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `OWNER_EMAIL` / `OWNER_PASSWORD` | Your real owner login (not demo defaults) |

Keep `OWNER_SEED_ENABLED=true` for the **first** deploy so the owner account is created. Keep `DEMO_SEED_ENABLED=false` (default in prod compose).

---

## 4. Start the stack

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

First build takes several minutes (Maven + npm). Watch logs:

```bash
docker compose -f docker-compose.prod.yml logs -f
```

When healthy:

- **App:** `https://your-domain.example`
- **Health:** `https://your-domain.example/api/health` (via nginx proxy)

Sign in with the owner email and password from `.env`.

---

## 5. Harden after first login

1. Change the owner password in the app (Settings → profile) if desired.
2. Set `OWNER_SEED_ENABLED=false` in `.env`.
3. Redeploy:

```bash
docker compose -f docker-compose.prod.yml up -d
```

---

## 6. Updates and redeploys

Pull latest code and rebuild:

```bash
cd mdl-platform
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

Flyway migrations run automatically on backend startup. Database data persists in the `mdl_mariadb_data` Docker volume.

---

## Architecture

```
Internet
   │
   ▼
Caddy (:443 TLS)
   │
   ▼
nginx (frontend container) — SPA + /api, /ws, /actuator proxy
   │
   ├──► Spring Boot (backend, internal)
   └──► MariaDB (internal)
```

The browser talks to one origin (`https://your-domain`). The frontend nginx forwards `/api/*` to the backend, so no separate API URL or CORS setup is needed.

---

## Provider-specific notes

### DigitalOcean

1. Create a Droplet (Ubuntu, 2–4 GB).
2. Add the droplet IP as an A record in your domain DNS (or use DO Networking).
3. Follow steps above.

### Hetzner

1. Create a CX22 or larger Cloud Server.
2. Attach a Floating IP if you want a stable address across rebuilds.
3. Point your domain A record to the server IP.

### AWS EC2

1. Launch Ubuntu t3.small (or larger) with a security group allowing **22, 80, 443** from appropriate sources.
2. Associate an Elastic IP.
3. Point DNS to the Elastic IP.

---

## Troubleshooting

### Certificate / HTTPS fails

- Confirm DNS: `dig +short your-domain.example` matches the VPS IP.
- Ensure ports 80 and 443 are open (Caddy uses HTTP-01 challenge on port 80).
- Check Caddy logs: `docker compose -f docker-compose.prod.yml logs caddy`

### Backend won't start in prod

Production rejects weak JWT secrets and demo seeding. Ensure:

- `JWT_SECRET` is 32+ characters and not a placeholder.
- `DEMO_SEED_ENABLED` is not `true`.
- `SPRING_PROFILES_ACTIVE=prod` (set automatically in `docker-compose.prod.yml`).

### "Insufficient stock" on sales

Sales deduct from each shop's **warehouse** location. Ensure Flyway migrations through V27+ have run (`docker compose -f docker-compose.prod.yml logs backend | grep Flyway`).

### Reset everything (destructive)

```bash
docker compose -f docker-compose.prod.yml down -v
docker compose -f docker-compose.prod.yml up -d --build
```

This deletes the database volume — use only for a fresh install.

---

## Local full-stack (no HTTPS)

For development or LAN testing without a domain:

```bash
docker compose -f docker-compose.stack.yml up -d --build
```

Open http://localhost:8081

---

## Related

- [Observability & Docker](../architecture/observability-deploy.md)
- [Production readiness](../architecture/production-readiness.md)
- `.env.production.example` — production environment template
