# 🚀 Quick Start Guide

Get up and running with the Enterprise Multi-Tenant SaaS Backend in minutes!

## What You Need

1. **Docker Desktop** (installed and running)
2. **Clerk Account** (free account at https://clerk.com)
3. **Git** (to clone repository)
4. **ngrok** (for webhook testing - see [API Testing Guide](./documentation/guides/API_TESTING_CONFIG.md))

## Step-by-Step Instructions

### Step 1: Clone Repository

```bash
git clone <repository-url>
cd payment
```

### Step 2: Create Clerk Application

1. Go to https://clerk.com and sign up/login
2. Create new application
3. Enable **Organizations** (Settings → Organizations → Enable)
4. Go to **API Keys** → Copy your instance URL
5. Go to **Webhooks** → Add endpoint → Copy signing secret

### Step 3: Create `.env` File

Create `.env` in project root:

```env
CLERK_JWKS_URL=https://your-app.clerk.accounts.dev/v1/jwks
CLERK_ISSUER=https://clerk.dev
CLERK_WEBHOOK_SECRET=whsec_xxxxx
```

**Replace:**

- `your-app.clerk.accounts.dev` with your Clerk instance URL
- `whsec_xxxxx` with your webhook signing secret

### Step 4: Start Services

```bash
docker-compose up --build
```

**Wait 5-10 minutes** (first time builds everything)

### Step 5: Verify Services Running

```bash
docker-compose ps
```

All services should show `Up` status.

### Step 6: Test

```bash
# Health check (no auth needed)
curl http://localhost:8080/api/health
```

## Access Points

- **API Gateway**: http://localhost:8080
- **Backend Service**: http://localhost:8081
- **PostgreSQL**: localhost:5433
- **pgAdmin**: http://localhost:5050 (admin@local.com / admin123)

## Test Protected Endpoint

```bash
# Get JWT token from Clerk (via your frontend or Clerk dashboard)
# Then test:
curl -H "Authorization: Bearer <your-jwt-token>" \
     http://localhost:8080/api/me
```

> **💡 Tip:** For detailed instructions on getting JWT tokens using `clerk-login.html`, see [API Testing Guide](./documentation/guides/API_TESTING_CONFIG.md)

## Stop Services

```bash
docker-compose down
```

## Reset Everything (Delete Database)

```bash
docker-compose down -v
docker-compose up --build
```

## Troubleshooting

**Port 8080 in use?**

```bash
# Find and kill process
lsof -ti:8080 | xargs kill
```

**Check logs:**

```bash
docker-compose logs -f
```

**Database connection fails?**

- Wait 20 seconds for PostgreSQL to start
- Check: `docker-compose ps postgres` shows `(healthy)`

**JWT validation fails?**

- Verify `CLERK_JWKS_URL` in `.env` is correct
- Check JWT token is from same Clerk instance

**Webhook signature fails?**

- Verify `CLERK_WEBHOOK_SECRET` matches Clerk dashboard

---

## 📚 Next Steps

Now that your system is running, explore these guides:

- **[API Testing & Configuration](./documentation/guides/API_TESTING_CONFIG.md)** - Complete guide for:

  - Getting JWT tokens using `clerk-login.html`
  - Setting up ngrok for webhook testing
  - Testing all APIs
  - Viewing database tables in pgAdmin

- **[Setup Guide](./documentation/setup/SETUP_GUIDE.md)** - Detailed setup instructions with troubleshooting

- **[Architecture Overview](../architecture/ARCHITECTURE.md)** - Understand the system design

- **[Flow Documentation](./documentation/guides/FLOW_DOCUMENTATION.md)** - Detailed API documentation

## 📖 Full Documentation

See [Main README](./readme.md) for complete documentation index, or browse [Documentation Folder](./documentation/README.md).

---

That's it! System is running. 🎉
