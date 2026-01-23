# 🚀 Getting Started

Complete beginner-friendly guide to get the payment system up and running.

## ✅ What You Need

Before starting, make sure you have:

1. **Docker Desktop** - [Download here](https://www.docker.com/products/docker-desktop)
2. **Clerk Account** - [Sign up free](https://clerk.com) (for authentication)
3. **Git** - Usually pre-installed
4. **Text Editor** - Any editor to edit `.env` file

**That's it!** No coding experience needed.

---

## 📦 Step 1: Get the Code

### Option A: Clone Repository

```bash
git clone <repository-url>
cd payment
```

### Option B: Download ZIP

1. Download repository as ZIP
2. Extract to a folder
3. Open terminal in that folder

---

## 🔐 Step 2: Set Up Clerk (Authentication)

Clerk handles user authentication. You need to create an account and get some keys.

### 2.1 Create Clerk Account

1. Go to: https://clerk.com
2. Click **"Sign Up"** (free account)
3. Create your account

### 2.2 Create Application

1. After login, click **"Create Application"**
2. Choose a name (e.g., "Payment System")
3. Click **"Create"**

### 2.3 Enable Organizations

Organizations are needed for multi-tenant features:

1. Go to: **Settings** → **Organizations**
2. Toggle **"Enable Organizations"** to ON
3. Click **"Save"**

### 2.4 Get Your Keys

1. Go to: **API Keys**
2. Find your **Instance URL** (looks like: `https://your-app.clerk.accounts.dev`)
3. Copy it - you'll need it in Step 3

### 2.5 Set Up Webhook (Optional for now)

You can skip this for now and set it up later:

1. Go to: **Webhooks** → **Add Endpoint**
2. We'll configure this later (see [Webhook Setup](../guides/WEBHOOK_SETUP.md))

---

## ⚙️ Step 3: Create Configuration File

Create a file named `.env` in the project root folder.

### 3.1 Create `.env` File

**On macOS/Linux:**
```bash
cd /Users/ravichandola/Documents/payment
touch .env
```

**On Windows:**
- Create a new file named `.env` (no extension)
- Make sure it's in the project root folder

### 3.2 Add Configuration

Open `.env` file and add:

```env
# Clerk Configuration
CLERK_JWKS_URL=https://your-app.clerk.accounts.dev/.well-known/jwks.json
CLERK_ISSUER=https://your-app.clerk.accounts.dev
CLERK_WEBHOOK_SECRET=whsec_xxxxx

# Database (optional - defaults work for Docker)
# LOCAL_DATASOURCE_URL=jdbc:postgresql://postgres:5432/appdb
# LOCAL_DATASOURCE_USERNAME=appuser
# LOCAL_DATASOURCE_PASSWORD=apppass

# Payment (optional - add if you have Razorpay keys)
# RAZORPAY_KEY=rzp_test_xxxxx
# RAZORPAY_SECRET=xxxxx
```

**Replace:**
- `your-app.clerk.accounts.dev` with your Clerk instance URL from Step 2.4
- `whsec_xxxxx` with your webhook secret (if you set up webhooks)

**Important:** 
- Keep the `.well-known/jwks.json` part in the JWKS URL
- Don't add quotes around values
- Don't add spaces around `=`

---

## 🐳 Step 4: Start Services

This will start all services (database, backend, etc.) using Docker.

### 4.1 Start Everything

```bash
cd /Users/ravichandola/Documents/payment
docker-compose up --build
```

**What happens:**
- Downloads Docker images (first time only)
- Builds your application
- Starts all services
- This takes 5-10 minutes the first time

**Keep the terminal open!** You'll see logs from all services.

### 4.2 Wait for Services

Wait until you see messages like:
```
backend-service    | Started BackendApplication in X.XXX seconds
```

This means everything is ready!

### 4.3 Check Services (Optional)

Open a **new terminal** and run:

```bash
docker-compose ps
```

You should see all services with status `Up`:
```
NAME                STATUS
postgres-db         Up
pgadmin             Up
backend-service     Up
api-gateway         Up
```

---

## ✅ Step 5: Test It Works

### 5.1 Health Check

Open a **new terminal** and run:

```bash
curl http://localhost:8080/api/health
```

**Expected response:**
```json
{
  "status": "UP",
  "service": "backend-service"
}
```

**If you see this, everything is working!** 🎉

### 5.2 Access Points

You can now access:

- **API Gateway:** http://localhost:8080
- **Backend Service:** http://localhost:8081
- **pgAdmin (Database UI):** http://localhost:5050
  - Email: `admin@local.com`
  - Password: `admin123`

---

## 🎓 What's Next?

Now that everything is running:

1. ✅ **Test APIs** - See [Testing APIs](./TESTING_APIS.md)
2. 📖 **Understand Flows** - See [Understanding Flows](./UNDERSTANDING_FLOWS.md)
3. 🏗️ **Learn Architecture** - See [Architecture](../architecture/ARCHITECTURE.md)

---

## ❌ Troubleshooting

### Services Won't Start

**Check:**
1. Docker Desktop is running
2. Ports 8080, 8081, 5050 are not in use
3. `.env` file exists and has correct format

**Fix:**
```bash
# Stop everything
docker-compose down

# Start again
docker-compose up --build
```

### "Port Already Allocated"

**Fix:**
```bash
# Find what's using the port
lsof -i :8080  # macOS/Linux

# Stop it, then restart
docker-compose up
```

### "Connection Refused"

**Fix:**
1. Wait 2-3 minutes for services to fully start
2. Check services are running: `docker-compose ps`
3. Check logs: `docker-compose logs backend-service`

### Still Having Issues?

See [Troubleshooting Guide](../guides/TROUBLESHOOTING.md) for more help.

---

## 📚 Related Guides

- [02_Testing APIs](./02_TESTING_APIS.md) - Test the system
- [03_Understanding Flows](./03_UNDERSTANDING_FLOWS.md) - How it works
- [02_Complete Setup Guide](../setup/02_SETUP_GUIDE.md) - Detailed setup
- [03_Troubleshooting](../guides/03_TROUBLESHOOTING.md) - Common issues

---

**Congratulations!** 🎉 You've set up the payment system. Now explore and test it!
