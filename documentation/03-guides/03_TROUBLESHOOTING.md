# 🔍 Troubleshooting Guide

Common issues and solutions for the payment system.

## 🚨 Quick Fixes

### Services Not Starting

**Problem:** Docker containers won't start

**Solution:**
```bash
# Check what's running
docker-compose ps

# Check logs
docker-compose logs

# Restart everything
docker-compose down
docker-compose up -d
```

---

## 🔐 Authentication Issues

### Issue 1: Cannot Connect to pgAdmin

**Error:** Can't access http://localhost:5050

**Solution:**
1. Check pgAdmin container is running:
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
4. Wait 30 seconds for pgAdmin to fully start
5. Try accessing again: http://localhost:5050

**Credentials:**
- Email: `admin@local.com`
- Password: `admin123`

---

### Issue 2: Cannot Connect to PostgreSQL from pgAdmin

**Error:** Connection refused or timeout

**Solution:**

**Important:** Since pgAdmin runs in Docker, use the service name, NOT localhost!

**Correct Connection Settings:**
- **Host:** `postgres` (service name from docker-compose.yml)
- **Port:** `5432` (container port, NOT 5433)
- **Database:** `appdb`
- **Username:** `appuser`
- **Password:** `apppass`

**Wrong Settings (will fail):**
- ❌ Host: `localhost` or `127.0.0.1`
- ❌ Port: `5433` (this is host port, not container port)

**Steps:**
1. In pgAdmin, right-click **Servers** → **Register** → **Server**
2. **General Tab:**
   - Name: `Payment Gateway DB`
3. **Connection Tab:**
   - Host: `postgres` ← **Important: Use service name!**
   - Port: `5432` ← **Container port, not 5433!**
   - Database: `appdb`
   - Username: `appuser`
   - Password: `apppass`
4. Click **Save**

---

### Issue 3: clerk-login.html Not Working

**Error:** Can't login or get JWT token

**Solution:**

1. **Check HTTP server is running:**
   ```bash
   # Start server
   cd /Users/ravichandola/Documents/payment
   python3 -m http.server 8000
   ```

2. **Check file exists:**
   ```bash
   ls clerk-login.html
   ```

3. **Open in browser:**
   ```
   http://localhost:8000/clerk-login.html
   ```

4. **Check browser console:**
   - Press F12 → Console tab
   - Look for errors

5. **Verify Clerk configuration:**
   - Check `.env` has `CLERK_JWKS_URL`
   - Check URL is correct format

---

### Issue 4: JWT Token Expired

**Error:** `401 Unauthorized` when using token

**Solution:**
1. Tokens expire in ~60 seconds
2. Get a new token from clerk-login.html
3. Use token immediately after getting it
4. Don't save tokens - get fresh ones each time

**Quick Test:**
```bash
# Get token and use immediately
curl -H "Authorization: Bearer <fresh-token>" \
     http://localhost:8080/api/me
```

---

## 🌐 Webhook Issues

### Issue 5: ngrok Not Working

**Error:** ngrok tunnel not connecting

**Solution:**
1. Verify ngrok is installed: `ngrok version`
2. Check authtoken is configured: `ngrok config check`
3. Make sure port 8080 is not blocked by firewall
4. Try different port: `ngrok http 8081`
5. Check ngrok status: Open http://127.0.0.1:4040

---

### Issue 6: Webhooks Not Received

**Error:** Webhooks not creating records in database

**Solution:**

1. **Verify ngrok is running:**
   ```bash
   # Check ngrok terminal is open
   # Check ngrok web interface: http://127.0.0.1:4040
   ```

2. **Check webhook URL in Clerk:**
   - Must match ngrok URL exactly
   - Format: `https://xxxx.ngrok-free.app/api/webhooks/clerk`

3. **Verify `CLERK_WEBHOOK_SECRET`:**
   ```bash
   # Check .env file
   cat .env | grep CLERK_WEBHOOK_SECRET
   ```
   - Must match Clerk Dashboard exactly
   - No extra spaces or quotes

4. **Check backend logs:**
   ```bash
   docker-compose logs -f backend-service | grep webhook
   ```

5. **Test webhook manually:**
   - Create a user in Clerk Dashboard
   - Check ngrok web interface for incoming request
   - Check backend logs for processing

6. **Restart backend:**
   ```bash
   docker-compose restart backend-service
   ```

---

## 💳 Payment Issues

### Issue 7: Payment API Returns Error

**Error:** `Razorpay authentication failed` or `500 Internal Server Error`

**Solution:**

1. **Check `.env` file:**
   ```bash
   cat .env | grep RAZORPAY
   ```
   - Must have `RAZORPAY_KEY` and `RAZORPAY_SECRET`
   - Test keys start with `rzp_test_`

2. **Verify keys are correct:**
   - Get keys from Razorpay Dashboard
   - Copy exactly (no extra spaces)

3. **Restart payment service:**
   ```bash
   docker-compose restart payment-service
   ```

4. **Check logs:**
   ```bash
   docker-compose logs payment-service
   ```

5. **Test payment endpoint:**
   ```bash
   curl -X POST http://localhost:8082/api/payments/create-order \
     -H "Content-Type: application/json" \
     -d '{"amount": 500}'
   ```

---

## 🗄️ Database Issues

### Issue 8: Tables Are Empty

**Problem:** Database tables exist but have no data

**This is normal!** Tables are populated by:
- **`users`** - Populated via webhooks (user.created event)
- **`organizations`** - Populated via webhooks (organization.created event)
- **`payment_order`** - Populated when you create payment orders via API
- **`payment_transaction`** - Populated when you verify payments

**To populate:**
1. **Users/Organizations:** Set up webhooks (see [Webhook Setup](./WEBHOOK_SETUP.md))
2. **Payments:** Create payment orders via API (see [Testing APIs](../quick-start/TESTING_APIS.md))

**Check if tables exist:**
```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
```

**Check row counts:**
```sql
SELECT
    'users' as table_name, COUNT(*) as rows FROM users
UNION ALL
SELECT 'payment_order', COUNT(*) FROM payment_order
UNION ALL
SELECT 'payment_transaction', COUNT(*) FROM payment_transaction;
```

---

### Issue 9: Cannot Connect to Database

**Error:** Connection refused or authentication failed

**Solution:**

1. **Check PostgreSQL is running:**
   ```bash
   docker-compose ps postgres
   ```

2. **Check logs:**
   ```bash
   docker-compose logs postgres
   ```

3. **Test connection:**
   ```bash
   docker exec -it postgres-db psql -U appuser -d appdb
   ```

4. **Check credentials in `.env`:**
   - `LOCAL_DATASOURCE_URL`
   - `LOCAL_DATASOURCE_USERNAME`
   - `LOCAL_DATASOURCE_PASSWORD`

---

## 🔧 General Issues

### Issue 10: Port Already Allocated

**Error:** `Bind for 0.0.0.0:8080 failed: port is already allocated`

**Solution:**

1. **Find what's using the port:**
   ```bash
   # macOS/Linux
   lsof -i :8080
   
   # Or check Docker
   docker ps | grep 8080
   ```

2. **Stop conflicting service:**
   ```bash
   # If it's a Docker container
   docker stop <container-name>
   
   # If it's another process
   kill -9 <PID>
   ```

3. **Start services again:**
   ```bash
   docker-compose up
   ```

---

### Issue 11: Services Keep Restarting

**Error:** Containers restart in a loop

**Solution:**

1. **Check logs:**
   ```bash
   docker-compose logs <service-name>
   ```

2. **Common causes:**
   - Missing environment variables
   - Database connection failed
   - Port conflict
   - Configuration error

3. **Check `.env` file:**
   ```bash
   cat .env
   ```
   - All required variables present?
   - No syntax errors?

4. **Rebuild containers:**
   ```bash
   docker-compose down
   docker-compose build --no-cache
   docker-compose up
   ```

---

## 📝 Quick Reference

### Service Ports

| Service | Port | URL |
|---------|------|-----|
| API Gateway | 8080 | http://localhost:8080 |
| Backend Service | 8081 | http://localhost:8081 |
| Payment Service | 8082 | http://localhost:8082 |
| pgAdmin | 5050 | http://localhost:5050 |
| PostgreSQL | 5433 | localhost:5433 |

### Common Commands

```bash
# Check services
docker-compose ps

# View logs
docker-compose logs -f <service-name>

# Restart service
docker-compose restart <service-name>

# Rebuild and restart
docker-compose down
docker-compose build
docker-compose up -d
```

---

## 🆘 Still Having Issues?

1. **Check logs:** `docker-compose logs`
2. **Check service status:** `docker-compose ps`
3. **Review setup:** [Quick Start Guide](../quick-start/GETTING_STARTED.md)
4. **Check fixes:** [Fixes & Improvements](../architecture/fixes/)

---

**Related Guides:**
- [Quick Start Testing](../quick-start/TESTING_APIS.md)
- [Webhook Setup](./WEBHOOK_SETUP.md)
- [Detailed API Testing](./API_TESTING_DETAILED.md)
