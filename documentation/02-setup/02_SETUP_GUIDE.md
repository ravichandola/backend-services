# Setup Guide - Enterprise Multi-Tenant SaaS Backend

Complete setup guide for the payment system. For a quicker beginner-friendly version, see [01_Getting Started](../quick-start/01_GETTING_STARTED.md).

## 📋 Prerequisites

- **Docker Desktop** installed and running
- **Clerk Account** (free at https://clerk.com)
- **Git** (for cloning)

## 🚀 Quick Setup (5 Steps)

### Step 1: Clone Repository

```bash
git clone <repository-url>
cd payment
```

### Step 2: Configure Clerk

1. **Sign up** at https://clerk.com
2. **Create application** and enable Organizations (Settings → Organizations)
3. **Get your instance URL** from API Keys (e.g., `https://your-app.clerk.accounts.dev`)
4. **Set up webhook** (optional for now - see [02_Webhook Setup](../guides/02_WEBHOOK_SETUP.md))

### Step 3: Create `.env` File

Create `.env` in project root:

```env
# Clerk Configuration
CLERK_JWKS_URL=https://your-app.clerk.accounts.dev/.well-known/jwks.json
CLERK_ISSUER=https://your-app.clerk.accounts.dev
CLERK_WEBHOOK_SECRET=whsec_xxxxx

# Payment (optional - add if you have Razorpay keys)
# RAZORPAY_KEY=rzp_test_xxxxx
# RAZORPAY_SECRET=xxxxx
```

**Important:** Replace `your-app.clerk.accounts.dev` with your actual Clerk instance URL.

### Step 4: Start Services

```bash
docker-compose up --build
```

**Wait 5-10 minutes** (first time downloads images and builds everything).

### Step 5: Verify Setup

```bash
# Check services are running
docker-compose ps

# Test health endpoint
curl http://localhost:8080/api/health
```

**Expected response:**
```json
{
  "status": "UP",
  "service": "backend-service"
}
```

✅ **Setup complete!** See [02_Testing APIs](../quick-start/02_TESTING_APIS.md) to test the system.

---

## 📍 Service Access Points

| Service | URL | Credentials |
|---------|-----|------------|
| API Gateway | http://localhost:8080 | - |
| Backend Service | http://localhost:8081 | - |
| pgAdmin | http://localhost:5050 | admin@local.com / admin123 |
| PostgreSQL | localhost:5433 | appuser / apppass |

---

## 🔧 Detailed Configuration

### Clerk JWKS URL Format

**Correct format:**
```
https://your-app.clerk.accounts.dev/.well-known/jwks.json
```

**Wrong formats:**
- ❌ `https://api.clerk.dev/v1/jwks`
- ❌ `https://your-app.clerk.accounts.dev/v1/jwks`

### Webhook Configuration

For local development, use ngrok:

1. **Install ngrok:** https://ngrok.com/download
2. **Start tunnel:**
   ```bash
   ngrok http 8080
   ```
3. **Copy ngrok URL** (e.g., `https://abc123.ngrok.io`)
4. **Configure in Clerk:**
   - Go to Webhooks → Add Endpoint
   - URL: `https://abc123.ngrok.io/api/webhooks/clerk`
   - Events: `user.created`, `user.updated`, `organization.created`, `organizationMembership.created`, `organizationMembership.deleted`
   - Copy signing secret to `.env` as `CLERK_WEBHOOK_SECRET`

See [02_Webhook Setup Guide](../guides/02_WEBHOOK_SETUP.md) for detailed instructions.

---

## 🗄️ Database Access

### Using pgAdmin (Recommended)

1. Open http://localhost:5050
2. Login: `admin@local.com` / `admin123`
3. Add server:
   - **Host:** `postgres` (service name, NOT localhost!)
   - **Port:** `5432` (container port)
   - **Database:** `appdb`
   - **Username:** `appuser`
   - **Password:** `apppass`

See [pgAdmin Setup](./PGADMIN_SETUP.md) for detailed instructions.

### Using psql

```bash
docker exec -it postgres-db psql -U appuser -d appdb
```

---

## 🔄 Development Workflow

### Making Code Changes

**After code changes, rebuild containers:**

```bash
docker-compose build backend-service
docker-compose up -d backend-service
```

**Note:** `docker-compose restart` doesn't include new code - always rebuild!

### Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend-service
docker-compose logs -f api-gateway
```

### Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (deletes database)
docker-compose down -v
```

---

## ❌ Troubleshooting

### Port Already in Use

**Error:** `Port 8080 is already in use`

**Solution:**
```bash
# Find what's using the port
lsof -i :8080  # macOS/Linux
# OR
docker ps | grep 8080

# Stop conflicting container/process
docker stop <container-name>
```

### JWT Validation Fails

**Error:** `JWT validation failed` in gateway logs

**Solutions:**
1. Verify `CLERK_JWKS_URL` in `.env` is correct
2. Check JWKS URL format: `.well-known/jwks.json` (not `/v1/jwks`)
3. Test JWKS endpoint: `curl https://your-app.clerk.accounts.dev/.well-known/jwks.json`
4. Check JWT token hasn't expired (dev tokens expire in 60 seconds)

### Database Connection Fails

**Error:** `Connection refused` or timeout

**Solutions:**
1. Check PostgreSQL is running: `docker-compose ps postgres`
2. Wait for health check (10-20 seconds)
3. Verify connection URL in backend logs
4. Test connectivity: `docker exec -it backend-service ping postgres`

### Webhook Not Working

**Error:** Webhooks not creating records

**Solutions:**
1. Verify ngrok is running: Check ngrok terminal
2. Check webhook URL in Clerk matches ngrok URL exactly
3. Verify `CLERK_WEBHOOK_SECRET` in `.env` matches Clerk dashboard
4. Check backend logs: `docker-compose logs -f backend-service | grep webhook`

**More troubleshooting:** See [03_Troubleshooting Guide](../guides/03_TROUBLESHOOTING.md)

---

## 🚀 Production Deployment

### Environment Variables

Set these in your production environment:

```env
CLERK_JWKS_URL=https://your-prod-instance.clerk.accounts.dev/.well-known/jwks.json
CLERK_ISSUER=https://your-prod-instance.clerk.accounts.dev
CLERK_WEBHOOK_SECRET=whsec_production_secret
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/appdb
SPRING_DATASOURCE_USERNAME=prod_user
SPRING_DATASOURCE_PASSWORD=prod_password
RAZORPAY_KEY=rzp_live_xxxxx
RAZORPAY_SECRET=xxxxx
```

### Security Checklist

- [ ] Use HTTPS for all endpoints
- [ ] Set strong database passwords
- [ ] Use production Clerk instance
- [ ] Configure webhook with production URL
- [ ] Enable database backups
- [ ] Set up monitoring and alerting
- [ ] Configure rate limiting
- [ ] Set up log aggregation

---

## 📚 Next Steps

1. ✅ **Setup complete** - Services are running
2. 🧪 **Test the system** - See [02_Testing APIs](../quick-start/02_TESTING_APIS.md)
3. 📖 **Understand flows** - See [03_Understanding Flows](../quick-start/03_UNDERSTANDING_FLOWS.md)
4. 🏗️ **Learn architecture** - See [01_Architecture Overview](../architecture/01_ARCHITECTURE.md)

---

## 🔗 Related Documentation

- [01_Getting Started](../quick-start/01_GETTING_STARTED.md) - Beginner-friendly setup
- [02_Testing APIs](../quick-start/02_TESTING_APIS.md) - Test the system
- [Webhook Setup](../guides/WEBHOOK_SETUP.md) - Configure webhooks
- [Troubleshooting](../guides/TROUBLESHOOTING.md) - Common issues
- [Database Setup](./DATABASE_SETUP.md) - Database configuration
- [pgAdmin Setup](./PGADMIN_SETUP.md) - Database management tool

---

**Need help?** Check [Troubleshooting Guide](../guides/TROUBLESHOOTING.md) or review service logs.
