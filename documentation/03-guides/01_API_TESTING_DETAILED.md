# 🧪 API Testing & Configuration Guide

Complete step-by-step guide for testing APIs, getting JWT tokens, setting up webhooks, and viewing database tables.

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Starting the System](#starting-the-system)
3. [Getting JWT Token (Using clerk-login.html)](#getting-jwt-token-using-clerk-loginhtml)
4. [Setting Up ngrok for Webhooks](#setting-up-ngrok-for-webhooks)
5. [Testing Payment APIs](#testing-payment-apis)
6. [Testing Backend APIs](#testing-backend-apis)
7. [Testing Webhooks](#testing-webhooks)
8. [Viewing Database Tables](#viewing-database-tables)
9. [Understanding Empty Tables](#understanding-empty-tables)
10. [Troubleshooting](#troubleshooting)

---

## ✅ Prerequisites

Before starting, ensure you have:

- ✅ **Docker Desktop** installed and running
- ✅ **`.env` file** configured with:
  - `RAZORPAY_KEY` (for payment testing)
  - `RAZORPAY_SECRET` (for payment testing)
  - `CLERK_JWKS_URL` (for authentication)
  - `CLERK_WEBHOOK_SECRET` (for webhooks)
- ✅ **ngrok** installed (for webhook testing - see installation below)

### Installing ngrok

**Why ngrok?** Clerk webhooks need a public URL to send events to your local backend. ngrok creates a secure tunnel from a public URL to your localhost.

#### Step 1: Download ngrok

**macOS (using Homebrew):**

```bash
brew install ngrok/ngrok/ngrok
```

**macOS/Linux (Manual):**

1. Visit: https://ngrok.com/download
2. Download for your OS
3. Extract and add to PATH

**Windows:**

1. Visit: https://ngrok.com/download
2. Download `ngrok.exe`
3. Add to PATH or use from download location

#### Step 2: Sign Up for Free Account

1. Go to: https://dashboard.ngrok.com/signup
2. Create a free account
3. Get your authtoken from: https://dashboard.ngrok.com/get-started/your-authtoken

#### Step 3: Configure ngrok

```bash
ngrok config add-authtoken <your-authtoken>
```

#### Step 4: Verify Installation

```bash
ngrok version
```

Should show version number if installed correctly.

---

## 🚀 Starting the System

### Step 1: Start All Services

```bash
cd /Users/ravichandola/Documents/payment
docker-compose up --build
```

**Wait for all services to start** (this may take 2-5 minutes on first run).

### Step 2: Verify Services Are Running

Open a new terminal and check:

```bash
docker-compose ps
```

**Expected Output:**

```
NAME              STATUS                    PORTS
postgres-db       Up X minutes (healthy)    0.0.0.0:5433->5432/tcp
pgadmin           Up X minutes              0.0.0.0:5050->80/tcp
backend-service   Up X minutes              0.0.0.0:8081->8081/tcp
api-gateway       Up X minutes              0.0.0.0:8080->8080/tcp
payment-service   Up X minutes              0.0.0.0:8082->8082/tcp
```

All services should show `Up` status. PostgreSQL should show `(healthy)`.

### Step 3: Service URLs

Once running, access services at:

| Service             | URL                   | Purpose                          |
| :------------------ | :-------------------- | :------------------------------- |
| **API Gateway**     | http://localhost:8080 | Main entry point (validates JWT) |
| **Backend Service** | http://localhost:8081 | Direct access (for testing)      |
| **Payment Service** | http://localhost:8082 | Direct access (for testing)      |
| **pgAdmin**         | http://localhost:5050 | Database management UI           |
| **PostgreSQL**      | localhost:5433        | Direct database connection       |

---

## 🎫 Getting JWT Token (Using clerk-login.html)

To test protected APIs, you need a JWT token from Clerk. The project includes `clerk-login.html` to easily get tokens.

### Step 1: Start Local HTTP Server

**⚠️ IMPORTANT:** Clerk SDK doesn't work with `file://` protocol. You need to run a local HTTP server.

**Option A: Using Python (Recommended)**

```bash
# Navigate to project root
cd /Users/ravichandola/Documents/payment

# Start HTTP server on port 8000
python3 -m http.server 8000
```

**Option B: Using Node.js**

```bash
# Install http-server globally (one time)
npm install -g http-server

# Start server
cd /Users/ravichandola/Documents/payment
http-server -p 8000
```

**Option C: Using PHP**

```bash
cd /Users/ravichandola/Documents/payment
php -S localhost:8000
```

### Step 2: Open clerk-login.html

1. Open your web browser
2. Navigate to: **http://localhost:8000/clerk-login.html**

### Step 3: Login with Clerk

1. Click **"Login with Clerk"** button
2. You'll be redirected to Clerk's sign-in page
3. Sign in with your email/password (or create account if new)
4. After successful login, you'll be redirected back

### Step 4: Get JWT Token

1. After logging in, click **"Get JWT Token"** button
2. The token will be displayed in a text area
3. **⚠️ IMPORTANT:** Token expires in ~60 seconds - use it immediately!
4. Click **"📋 Copy Token"** to copy to clipboard

### Step 5: Test Token

A test command will be displayed. You can:

1. Click **"📋 Copy Command"** to copy the curl command
2. Open a new terminal
3. Paste and run the command:

```bash
curl -H "Authorization: Bearer <your-token-here>" \
     http://localhost:8080/api/me
```

**Expected Response:**

```json
{
  "userId": "user_xxx",
  "email": "user@example.com",
  ...
}
```

### Troubleshooting clerk-login.html

**Error: "Clerk SDK not loaded"**

- Make sure you're using HTTP server (not `file://`)
- Check internet connection (Clerk SDK loads from CDN)
- Hard refresh: `Cmd+Shift+R` (Mac) or `Ctrl+Shift+R` (Windows/Linux)

**Error: "Token expired"**

- Tokens expire in ~60 seconds
- Click "Get JWT Token" again to get a new one

**Error: "No active session"**

- Make sure you're logged in
- Click "Login with Clerk" if not logged in

---

## 🌐 Setting Up ngrok for Webhooks

Webhooks require a public URL. Use ngrok to expose your local backend to the internet.

### Step 1: Start ngrok Tunnel

Open a new terminal and run:

```bash
ngrok http 8080
```

**Expected Output:**

```
ngrok

Session Status                online
Account                       Your Name (Plan: Free)
Version                       3.x.x
Region                        United States (us)
Latency                       -
Web Interface                 http://127.0.0.1:4040
Forwarding                    https://xxxx-xx-xx-xx-xx.ngrok-free.app -> http://localhost:8080

Connections                   ttl     opn     rt1     rt5     p50     p90
                              0       0       0.00    0.00    0.00    0.00
```

**Important:** Copy the `Forwarding` URL (e.g., `https://xxxx-xx-xx-xx-xx.ngrok-free.app`)

### Step 2: Configure Clerk Webhook

1. Go to: https://dashboard.clerk.com
2. Select your application
3. Navigate to: **Webhooks** → **Add Endpoint**
4. **Endpoint URL:** `https://your-ngrok-url.ngrok-free.app/api/webhooks/clerk`
   - Example: `https://xxxx-xx-xx-xx-xx.ngrok-free.app/api/webhooks/clerk`
5. **Select Events:**
   - ✅ `user.created`
   - ✅ `user.updated`
   - ✅ `organization.created`
   - ✅ `organizationMembership.created`
   - ✅ `organizationMembership.deleted`
6. Click **"Add Endpoint"**
7. **Copy the Signing Secret** (starts with `whsec_`)
8. Add to your `.env` file:
   ```env
   CLERK_WEBHOOK_SECRET=whsec_xxxxx
   ```
9. Restart backend service:
   ```bash
   docker-compose restart backend-service
   ```

### Step 3: Test Webhook

1. **Create a new user in Clerk:**

   - Go to Clerk Dashboard → Users → Create User
   - Or sign up via your frontend

2. **Check ngrok Web Interface:**

   - Open: http://127.0.0.1:4040
   - You'll see incoming webhook requests

3. **Check Backend Logs:**

   ```bash
   docker-compose logs -f backend-service | grep webhook
   ```

4. **Verify in Database:**
   - See [Viewing Database Tables](#viewing-database-tables) section below
   - Check `users` table for new user
   - Check `user_events` table for webhook event

### Important Notes

- **ngrok URL Changes:** Free ngrok URLs change every time you restart ngrok
- **Update Clerk:** You need to update webhook URL in Clerk Dashboard each time

### 🎯 Avoiding URL Updates Every Time

**Problem:** Free ngrok URLs change on every restart, requiring Clerk webhook URL updates.

**Solutions:**

#### Option 1: Paid ngrok (Recommended)
- Fixed domain: `your-app.ngrok.io`
- Set once in Clerk, never change again
- Cost: ~$8/month
- **Best for:** Regular development

#### Option 2: Cloudflare Tunnel (Free)
- Free fixed subdomain
- No URL changes
- Setup: `cloudflared tunnel --url http://localhost:8080`
- **Best for:** Free fixed URL

#### Option 3: Production Deployment
- Fixed domain (e.g., `api.yourdomain.com`)
- Permanent solution
- **Best for:** Production use

See [Webhook Setup Guide](./02_WEBHOOK_SETUP.md#solutions-avoid-updating-url-every-time) for detailed instructions.
- **Paid ngrok:** For fixed URLs, upgrade to paid plan
- **Keep ngrok Running:** Keep the ngrok terminal open while testing webhooks

---

## 💳 Testing Payment APIs

### Prerequisites

- ✅ Razorpay test keys configured in `.env` file
- ✅ Payment service running (port 8082)

### Test 1: Create Payment Order

**Using curl:**

```bash
curl -X POST http://localhost:8082/api/payments/create-order \
  -H "Content-Type: application/json" \
  -d '{"amount": 500}'
```

**Using Postman:**

1. **Method:** `POST`
2. **URL:** `http://localhost:8082/api/payments/create-order`
3. **Headers:**
   ```
   Content-Type: application/json
   ```
4. **Body (raw JSON):**
   ```json
   {
     "amount": 500
   }
   ```

**Expected Response:**

```json
{
  "orderId": "order_xxxxx",
  "amount": 500,
  "currency": "INR",
  "status": "created",
  "receipt": "rcpt_xxxxx"
}
```

**Save the `orderId`** - you'll need it for payment verification.

### Test 2: Verify Payment

**Note:** This requires a real payment from Razorpay. For testing, you can use Razorpay test cards.

**Using curl:**

```bash
curl -X POST http://localhost:8082/api/payments/verify \
  -H "Content-Type: application/json" \
  -d '{
    "razorpayOrderId": "order_xxxxx",
    "razorpayPaymentId": "pay_xxxxx",
    "razorpaySignature": "signature_xxxxx"
  }'
```

**Expected Response:**

```
Payment verified successfully
```

### Test 3: View Payment Data

After creating orders, you can view them in the database. See [Viewing Database Tables](#viewing-database-tables) section below.

---

## 🔐 Testing Backend APIs

### Test 1: Health Check (No Authentication)

**Using curl:**

```bash
curl http://localhost:8080/api/health
```

**Expected Response:**

```json
{
  "status": "UP",
  "service": "backend-service"
}
```

### Test 2: Get Current User (Requires JWT)

**Prerequisites:**

- ✅ JWT token from [Getting JWT Token](#getting-jwt-token-using-clerk-loginhtml) section above

**Using curl:**

```bash
curl -H "Authorization: Bearer <your-jwt-token>" \
     http://localhost:8080/api/me
```

**Expected Response:**

```json
{
  "userId": "user_xxx",
  "email": "user@example.com",
  ...
}
```

**Note:** If you get 401 Unauthorized:

1. Token might be expired (get a new one from clerk-login.html)
2. Verify `CLERK_JWKS_URL` in `.env` is correct
3. Check gateway logs: `docker-compose logs api-gateway`

### Test 3: Direct Backend Service Access

You can also test directly on backend service (bypassing gateway):

```bash
# Health check
curl http://localhost:8081/api/health

# Note: /api/me requires X-User-Id header (added by gateway)
```

---

## 🔔 Testing Webhooks

### Prerequisites

- ✅ ngrok running (see [Setting Up ngrok](#setting-up-ngrok-for-webhooks))
- ✅ Webhook configured in Clerk Dashboard
- ✅ `CLERK_WEBHOOK_SECRET` in `.env` file

### Test 1: User Created Webhook

1. **Create a new user in Clerk:**

   - Go to Clerk Dashboard → Users → Create User
   - Or sign up via your frontend/clerk-login.html

2. **Check ngrok Web Interface:**

   - Open: http://127.0.0.1:4040
   - Click on the webhook request
   - View request/response details

3. **Check Backend Logs:**

   ```bash
   docker-compose logs -f backend-service | grep -i "user.created"
   ```

4. **Verify in Database:**
   - See [Viewing Database Tables](#viewing-database-tables) section
   - Check `users` table for new user
   - Check `user_events` table for event

### Test 2: Organization Created Webhook

1. **Create organization in Clerk:**

   - Go to Clerk Dashboard → Organizations → Create Organization
   - Or create via API/frontend

2. **Verify:**
   - Check `organizations` table in database
   - Check `organization_events` table

### Test 3: Membership Created Webhook

1. **Add member to organization in Clerk**
2. **Verify:**
   - Check `memberships` table in database
   - Check `organization_events` table

### Troubleshooting Webhooks

**Webhook not received:**

- Verify ngrok is running
- Check webhook URL in Clerk Dashboard matches ngrok URL
- Check backend service is running: `docker-compose ps backend-service`

**Webhook signature verification fails:**

- Verify `CLERK_WEBHOOK_SECRET` in `.env` matches Clerk Dashboard
- Restart backend: `docker-compose restart backend-service`

**Webhook received but no data in database:**

- Check backend logs: `docker-compose logs backend-service`
- Verify database connection
- Check webhook payload in ngrok interface

---

## 🗄️ Viewing Database Tables

### Step 1: Access pgAdmin

1. Open your web browser
2. Navigate to: **http://localhost:5050**
3. Login with:
   - **Email:** `admin@local.com`
   - **Password:** `admin123`

### Step 2: Add PostgreSQL Server

1. **Right-click** on **"Servers"** in the left sidebar
2. Select **"Register" → "Server..."**

### Step 3: Configure Connection

#### General Tab:

- **Name:** `Payment Gateway DB` (or any name you prefer)

#### Connection Tab:

**⚠️ IMPORTANT: Since pgAdmin is running in Docker, use the service name, NOT localhost!**

| Field                    | Value      | Notes                                       |
| :----------------------- | :--------- | :------------------------------------------ |
| **Host name/address**    | `postgres` | ✅ Use service name from docker-compose.yml |
| **Port**                 | `5432`     | ✅ Container port (NOT 5433)                |
| **Maintenance database** | `appdb`    | Database name                               |
| **Username**             | `appuser`  | Database user                               |
| **Password**             | `apppass`  | Database password                           |

- ✅ Check **"Save password"** (optional, but recommended)
- Click **"Save"**

### Step 4: Navigate to Tables

1. Expand: **Servers** → **Payment Gateway DB** → **Databases** → **appdb** → **Schemas** → **public** → **Tables**

### Step 5: View Table Data

**Option A: View All Rows**

1. Right-click on any table (e.g., `users`)
2. Select **"View/Edit Data"** → **"All Rows"**
3. Data will be displayed in a grid

**Option B: Run SQL Query**

1. Right-click on **`appdb`** database
2. Select **"Query Tool"**
3. Type your SQL query:
   ```sql
   SELECT * FROM users;
   ```
4. Click **Execute** (F5) or press `Ctrl+Enter` (Windows/Linux) or `Cmd+Enter` (Mac)

### Step 6: Available Tables

You should see these tables:

| Table Name              | Purpose                        | Expected Data                              |
| :---------------------- | :----------------------------- | :----------------------------------------- |
| `users`                 | User identity from Clerk       | ✅ Should have data (if webhooks received) |
| `organizations`         | Multi-tenant organizations     | ⚠️ Empty until org created in Clerk        |
| `roles`                 | Predefined roles (ADMIN, USER) | ✅ Should have 2 rows                      |
| `memberships`           | User-Organization-Role links   | ⚠️ Empty until memberships created         |
| `user_events`           | Audit trail for user events    | ⚠️ Empty until webhooks received           |
| `organization_events`   | Audit trail for org events     | ⚠️ Empty until webhooks received           |
| `auth_sessions`         | Session tracking               | ⚠️ **Always empty** (not implemented)      |
| `payment_order`         | Payment orders                 | ⚠️ Empty until payment API called          |
| `payment_transaction`   | Payment transactions           | ⚠️ Empty until payment verified            |
| `flyway_schema_history` | Migration history              | ✅ Should have data                        |

### Step 7: Useful SQL Queries

**Check Table Row Counts:**

```sql
SELECT
    'users' as table_name, COUNT(*) as row_count FROM users
UNION ALL
SELECT 'organizations', COUNT(*) FROM organizations
UNION ALL
SELECT 'roles', COUNT(*) FROM roles
UNION ALL
SELECT 'memberships', COUNT(*) FROM memberships
UNION ALL
SELECT 'payment_order', COUNT(*) FROM payment_order
UNION ALL
SELECT 'payment_transaction', COUNT(*) FROM payment_transaction
UNION ALL
SELECT 'auth_sessions', COUNT(*) FROM auth_sessions
ORDER BY table_name;
```

**View All Users:**

```sql
SELECT * FROM users ORDER BY created_at DESC;
```

**View Payment Orders:**

```sql
SELECT
    id,
    razorpay_order_id,
    amount,
    currency,
    status,
    created_at
FROM payment_order
ORDER BY created_at DESC;
```

**View Webhook Events:**

```sql
-- User events
SELECT
    event_type,
    clerk_user_id,
    processed_at
FROM user_events
ORDER BY processed_at DESC;

-- Organization events
SELECT
    event_type,
    clerk_org_id,
    processed_at
FROM organization_events
ORDER BY processed_at DESC;
```

---

## ❓ Understanding Empty Tables

### Why Are Tables Empty?

#### 1. `auth_sessions` - Always Empty

**Reason:** This table is **optional and not implemented**.

- ✅ Table exists (created by migration)
- ❌ No repository (`AuthSessionRepository`) exists
- ❌ No service code saves data to this table
- 📝 **Status:** Reserved for future use

**To Implement (Future):**

- Create `AuthSessionRepository`
- Add session tracking in `GatewayHeaderAuthenticationFilter` or `JwtAuthenticationFilter`
- Save session data on each authenticated request

#### 2. `payment_order` / `payment_transaction` - Empty Until API Calls

**Reason:** Data is created when payment APIs are called.

**To Populate:**

1. Call `POST /api/payments/create-order` → Creates record in `payment_order`
2. Complete payment and verify → Creates record in `payment_transaction`

**Test:**

```bash
# This will create a record in payment_order
curl -X POST http://localhost:8082/api/payments/create-order \
  -H "Content-Type: application/json" \
  -d '{"amount": 500}'
```

#### 3. `organizations` / `memberships` - Empty Until Webhooks

**Reason:** Data is created when Clerk sends webhook events.

**To Populate:**

1. Set up ngrok (see [Setting Up ngrok](#setting-up-ngrok-for-webhooks))
2. Configure webhook in Clerk Dashboard
3. Create an organization in Clerk Dashboard
4. Clerk sends `organization.created` webhook
5. Backend processes webhook → Creates record in `organizations`
6. Add members to organization → Creates records in `memberships`

#### 4. `users` - Populated via Webhooks

**Reason:** Created when Clerk sends `user.created` webhook.

**To Populate:**

1. Set up ngrok and webhook (see above)
2. User signs up in Clerk (via clerk-login.html or frontend)
3. Clerk sends `user.created` webhook
4. Backend processes webhook → Creates record in `users`

**Check Current Users:**

```sql
SELECT * FROM users;
```

#### 5. `user_events` / `organization_events` - Audit Tables

**Reason:** Populated when webhooks are processed.

**To Populate:**

- Automatically populated when webhooks are received
- Stores full webhook payload for audit trail

**View Events:**

```sql
-- User events
SELECT
    event_type,
    clerk_user_id,
    processed_at
FROM user_events
ORDER BY processed_at DESC;

-- Organization events
SELECT
    event_type,
    clerk_org_id,
    processed_at
FROM organization_events
ORDER BY processed_at DESC;
```

---

## 🔍 Troubleshooting

### Issue 1: Cannot Connect to pgAdmin

**Error:** Cannot access `http://localhost:5050`

**Solution:**

1. Check if pgAdmin container is running:
   ```bash
   docker-compose ps pgadmin
   ```
2. Check logs:
   ```bash
   docker-compose logs pgadmin
   ```
3. Restart pgAdmin:
   ```bash
   docker-compose restart pgadmin
   ```

### Issue 2: Cannot Connect to PostgreSQL from pgAdmin

**Error:** `could not connect to server: Connection refused`

**Solution:**

1. Verify you're using **`postgres`** as host (NOT `localhost`)
2. Verify port is **`5432`** (NOT `5433`)
3. Check PostgreSQL is healthy:
   ```bash
   docker-compose ps postgres
   ```
   Should show `(healthy)`
4. Wait 10-20 seconds for PostgreSQL to fully start

### Issue 3: clerk-login.html Not Working

**Error:** "Clerk SDK not loaded" or "file:// protocol not supported"

**Solution:**

1. Make sure you're using HTTP server (not opening file directly)
2. Start server: `python3 -m http.server 8000`
3. Access: `http://localhost:8000/clerk-login.html`
4. Check browser console (F12) for errors

### Issue 4: JWT Token Expired

**Error:** `401 Unauthorized` when using token

**Solution:**

1. Tokens expire in ~60 seconds
2. Get a new token from clerk-login.html
3. Use token immediately after getting it

### Issue 5: ngrok Not Working

**Error:** ngrok tunnel not connecting

**Solution:**

1. Verify ngrok is installed: `ngrok version`
2. Check authtoken is configured: `ngrok config check`
3. Make sure port 8080 is not blocked by firewall
4. Try different port: `ngrok http 8081`

### Issue 6: Webhooks Not Received

**Error:** Webhooks not creating records in database

**Solution:**

1. Verify ngrok is running and URL is accessible
2. Check webhook URL in Clerk Dashboard matches ngrok URL
3. Verify `CLERK_WEBHOOK_SECRET` in `.env` matches Clerk Dashboard
4. Check backend logs: `docker-compose logs backend-service | grep webhook`
5. Check ngrok web interface: http://127.0.0.1:4040

### Issue 7: Payment API Returns Error

**Error:** `Razorpay authentication failed`

**Solution:**

1. Check `.env` file has `RAZORPAY_KEY` and `RAZORPAY_SECRET`
2. Verify keys are correct (test keys start with `rzp_test_`)
3. Restart payment service:
   ```bash
   docker-compose restart payment-service
   ```
4. Check logs:
   ```bash
   docker-compose logs payment-service
   ```

### Issue 8: Tables Are Empty

**See:** [Understanding Empty Tables](#understanding-empty-tables) section above

**Quick Check:**

```sql
-- Check if tables exist
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

-- Check row counts
SELECT
    'users' as table_name, COUNT(*) as rows FROM users
UNION ALL
SELECT 'payment_order', COUNT(*) FROM payment_order
UNION ALL
SELECT 'payment_transaction', COUNT(*) FROM payment_transaction;
```

---

## 📝 Quick Reference

### Service Ports

| Service                            | Port | URL                   |
| :--------------------------------- | :--- | :-------------------- |
| API Gateway                        | 8080 | http://localhost:8080 |
| Backend Service                    | 8081 | http://localhost:8081 |
| Payment Service                    | 8082 | http://localhost:8082 |
| pgAdmin                            | 5050 | http://localhost:5050 |
| PostgreSQL                         | 5433 | localhost:5433        |
| HTTP Server (for clerk-login.html) | 8000 | http://localhost:8000 |
| ngrok Web Interface                | 4040 | http://127.0.0.1:4040 |

### pgAdmin Credentials

- **URL:** http://localhost:5050
- **Email:** admin@local.com
- **Password:** admin123

### PostgreSQL Connection (from pgAdmin)

- **Host:** `postgres` (service name, NOT localhost)
- **Port:** `5432` (container port, NOT 5433)
- **Database:** `appdb`
- **Username:** `appuser`
- **Password:** `apppass`

### Common Commands

```bash
# Start HTTP server for clerk-login.html
python3 -m http.server 8000

# Start ngrok tunnel
ngrok http 8080

# Get JWT token
# Open: http://localhost:8000/clerk-login.html

# Test health check
curl http://localhost:8080/api/health

# Test with JWT token
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/me

# Create payment order
curl -X POST http://localhost:8082/api/payments/create-order \
  -H "Content-Type: application/json" \
  -d '{"amount": 500}'
```

---

## ✅ Testing Checklist

After setup, verify:

- [ ] All Docker containers are running (`docker-compose ps`)
- [ ] ngrok is installed and configured
- [ ] HTTP server running for clerk-login.html (`python3 -m http.server 8000`)
- [ ] Can access clerk-login.html at http://localhost:8000/clerk-login.html
- [ ] Can login and get JWT token from clerk-login.html
- [ ] ngrok tunnel is running (`ngrok http 8080`)
- [ ] Webhook configured in Clerk Dashboard with ngrok URL
- [ ] Health check works: `curl http://localhost:8080/api/health`
- [ ] Can test protected API with JWT: `curl -H "Authorization: Bearer <token>" http://localhost:8080/api/me`
- [ ] Can create payment order: `POST /api/payments/create-order`
- [ ] Can access pgAdmin at http://localhost:5050
- [ ] Can connect to PostgreSQL in pgAdmin
- [ ] Can see all 10 tables in database
- [ ] Can view data in tables using SQL queries

---

**Need More Help?**

- See [Architecture Overview](../architecture/ARCHITECTURE.md) for system architecture details
- See [pgAdmin Setup Guide](../setup/PGADMIN_SETUP.md) for detailed pgAdmin setup
- See [Quick Start Guide](../setup/QUICK_START.md) for quick setup instructions
