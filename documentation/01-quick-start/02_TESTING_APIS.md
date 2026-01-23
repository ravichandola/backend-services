# 🧪 Testing APIs - Quick Start

A beginner-friendly guide to test the payment system APIs.

## ✅ Before You Start

Make sure:
- ✅ Docker Desktop is running
- ✅ Services are started: `docker-compose up`
- ✅ You have a JWT token (see below)

## 🎫 Step 1: Get JWT Token

You need a token to access protected APIs.

### Quick Method (Using clerk-login.html)

1. **Start a local server:**
   ```bash
   cd /Users/ravichandola/Documents/payment
   python3 -m http.server 8000
   ```

2. **Open in browser:**
   ```
   http://localhost:8000/clerk-login.html
   ```

3. **Login with Clerk:**
   - Click "Login with Clerk"
   - Sign in with your Clerk account
   - Copy the JWT token shown

4. **Test the token:**
   ```bash
   curl http://localhost:8080/api/health
   ```

> 📖 **Need help?** See [Detailed JWT Guide](../guides/API_TESTING_DETAILED.md#getting-jwt-token)

## 💳 Step 2: Test Payment APIs

### Create Payment Order

**Simple test:**
```bash
curl -X POST http://localhost:8082/api/payments/create-order \
  -H "Content-Type: application/json" \
  -d '{"amount": 500}'
```

**What you'll get:**
```json
{
  "orderId": "order_abc123",
  "amount": 500,
  "currency": "INR",
  "status": "created"
}
```

**Save the `orderId`** - you'll need it later!

### Verify Payment

After a user pays (on Razorpay), verify the payment:

```bash
curl -X POST http://localhost:8082/api/payments/verify \
  -H "Content-Type: application/json" \
  -d '{
    "razorpayOrderId": "order_abc123",
    "razorpayPaymentId": "pay_xyz789",
    "razorpaySignature": "signature_here"
  }'
```

> 📖 **Need help?** See [Detailed Payment Testing](../guides/API_TESTING_DETAILED.md#testing-payment-apis)

## 🔐 Step 3: Test Backend APIs

### Health Check (No Token Needed)

```bash
curl http://localhost:8080/api/health
```

**Expected:**
```json
{
  "status": "UP",
  "service": "backend-service"
}
```

### Get Current User (Token Required)

```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/api/me
```

**Expected:**
```json
{
  "userId": "user_xxx",
  "email": "your@email.com"
}
```

> 📖 **Need help?** See [Detailed Backend Testing](../guides/API_TESTING_DETAILED.md#testing-backend-apis)

## 🔔 Step 4: Test Webhooks (Optional)

Webhooks let Clerk notify your system about user events.

### Quick Setup

1. **Install ngrok:**
   ```bash
   brew install ngrok/ngrok/ngrok
   ```

2. **Start ngrok:**
   ```bash
   ngrok http 8080
   ```

3. **Copy the URL** (e.g., `https://abc123.ngrok.io`)

4. **Configure in Clerk:**
   - Go to Clerk Dashboard → Webhooks
   - Add endpoint: `https://abc123.ngrok.io/api/webhooks/clerk`
   - Select events: `user.created`, `user.updated`
   - Copy the signing secret

5. **Add to `.env`:**
   ```env
   CLERK_WEBHOOK_SECRET=whsec_xxxxx
   ```

6. **Restart services:**
   ```bash
   docker-compose restart backend-service
   ```

> 📖 **Need help?** See [Detailed Webhook Setup](../guides/WEBHOOK_SETUP.md)

## 🗄️ Step 5: View Database

### Using pgAdmin

1. **Open:** http://localhost:5050
2. **Login:**
   - Email: `admin@local.com`
   - Password: `admin123`
3. **Connect to database:**
   - Server name: `Payment Gateway DB`
   - Host: `postgres` (not localhost!)
   - Port: `5432`
   - Database: `appdb`
   - Username: `appuser`
   - Password: `apppass`

4. **View tables:**
   - Expand `appdb` → `Schemas` → `public` → `Tables`
   - Right-click table → `View/Edit Data` → `All Rows`

> 📖 **Need help?** See [pgAdmin Setup](../setup/PGADMIN_SETUP.md)

## ❌ Common Issues

### "401 Unauthorized"
- **Fix:** Get a new JWT token (they expire quickly)
- **Check:** Token is in `Authorization: Bearer <token>` header

### "Connection refused"
- **Fix:** Make sure services are running: `docker-compose ps`
- **Check:** Wait for services to fully start (2-5 minutes)

### "404 Not Found"
- **Fix:** Check the URL is correct
- **Check:** Service is running on that port

### "500 Internal Server Error"
- **Fix:** Check service logs: `docker-compose logs backend-service`
- **Check:** Database is running: `docker-compose ps postgres`

> 📖 **More troubleshooting:** See [Troubleshooting Guide](../guides/TROUBLESHOOTING.md)

## 🎓 What's Next?

1. ✅ You can test APIs
2. 📖 Read [Understanding Flows](./UNDERSTANDING_FLOWS.md)
3. 🏗️ Explore [Architecture](../architecture/ARCHITECTURE.md)
4. 📚 See [Detailed Guides](../guides/) for advanced topics

---

**Quick Links:**
- [Getting Started](./GETTING_STARTED.md) - Setup guide
- [Understanding Flows](./UNDERSTANDING_FLOWS.md) - How system works
- [Detailed Testing](../guides/API_TESTING_DETAILED.md) - Advanced testing
