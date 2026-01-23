# 🔔 Webhook Setup Guide

Complete guide for setting up Clerk webhooks with ngrok.

## 🎯 What Are Webhooks?

Webhooks let Clerk notify your backend when events happen:
- User created
- User updated
- Organization created
- Membership created/deleted

## 📋 Prerequisites

- ✅ Docker services running
- ✅ ngrok installed (see below)
- ✅ Clerk account with application

## 🔧 Installing ngrok

### Why ngrok?

Clerk webhooks need a public URL. ngrok creates a secure tunnel from a public URL to your localhost.

### Step 1: Download ngrok

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

### Step 2: Sign Up for Free Account

1. Go to: https://dashboard.ngrok.com/signup
2. Create a free account
3. Get your authtoken from: https://dashboard.ngrok.com/get-started/your-authtoken

### Step 3: Configure ngrok

```bash
ngrok config add-authtoken <your-authtoken>
```

### Step 4: Verify Installation

```bash
ngrok version
```

Should show version number if installed correctly.

---

## 🚀 Setting Up Webhooks

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

**Keep this terminal open!** ngrok must keep running.

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
   - Connect to pgAdmin (see [pgAdmin Setup](../setup/PGADMIN_SETUP.md))
   - Check `users` table for new user
   - Check `user_events` table for webhook event

---

## ⚠️ Important Notes

### ngrok URL Changes

- **Free ngrok URLs change** every time you restart ngrok
- **Update Clerk** webhook URL in Clerk Dashboard each time
- **Paid ngrok:** For fixed URLs, upgrade to paid plan

### Keep ngrok Running

- **Keep the ngrok terminal open** while testing webhooks
- If you close it, webhooks will stop working
- Restart ngrok and update Clerk URL if needed

### Webhook Security

- Always verify webhook signatures (handled automatically)
- Never expose `CLERK_WEBHOOK_SECRET` in code
- Use environment variables for secrets

---

## 🔍 Troubleshooting

### Webhooks Not Received

**Check:**
1. ngrok is running: Check terminal where you started ngrok
2. ngrok URL is accessible: Open URL in browser (should show 404, not connection error)
3. Webhook URL in Clerk matches ngrok URL exactly
4. `CLERK_WEBHOOK_SECRET` in `.env` matches Clerk Dashboard
5. Backend service is running: `docker-compose ps backend-service`

**Debug:**
```bash
# Check backend logs
docker-compose logs -f backend-service | grep webhook

# Check ngrok web interface
# Open: http://127.0.0.1:4040
# You'll see all requests going through ngrok
```

### ngrok Not Working

**Check:**
1. ngrok is installed: `ngrok version`
2. Authtoken is configured: `ngrok config check`
3. Port 8080 is not blocked by firewall
4. Try different port: `ngrok http 8081` (if backend is on 8081)

### Webhook Signature Verification Fails

**Check:**
1. `CLERK_WEBHOOK_SECRET` in `.env` is correct
2. Secret matches Clerk Dashboard exactly
3. No extra spaces or quotes in `.env` file
4. Restart backend after changing `.env`

---

## 📚 Related Guides

- [Quick Start Testing](../quick-start/TESTING_APIS.md) - Quick testing guide
- [Detailed API Testing](./API_TESTING_DETAILED.md) - Complete testing guide
- [pgAdmin Setup](../setup/PGADMIN_SETUP.md) - Database management

---

**Quick Reference:**
- ngrok Web Interface: http://127.0.0.1:4040
- Webhook Endpoint: `/api/webhooks/clerk`
- Required Events: `user.created`, `user.updated`, `organization.created`, `organizationMembership.created`, `organizationMembership.deleted`
