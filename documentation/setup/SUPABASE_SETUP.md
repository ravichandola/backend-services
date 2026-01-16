# 🌐 Supabase Database Setup Guide

This guide explains how to:

1. Create tables in Supabase using Flyway migrations
2. Connect to Supabase from pgAdmin (both Docker and Desktop)

---

## 📋 Prerequisites

- Supabase database credentials (already configured in `application.yml`)
- Spring Boot application with Flyway enabled

---

## 🗄️ Step 1: Create Tables in Supabase

The application uses **Flyway** to automatically create tables when it starts. To create tables in Supabase:

### Option 1: Run Application (Automatic Migration)

Simply start the application with the `prod` profile (default):

```bash
# This will automatically run Flyway migrations on Supabase
./mvnw spring-boot:run
```

**What happens:**

- Flyway checks for existing migrations in Supabase
- Creates `payment_order` and `payment_transaction` tables if they don't exist
- Creates all indexes
- You'll see migration logs in the console

### Option 2: Manual Migration (If Needed)

If you want to verify or manually run migrations:

1. **Check Flyway status:**

   ```bash
   # Start app and check logs
   ./mvnw spring-boot:run
   # Look for: "Flyway migration successful" or migration logs
   ```

2. **Verify tables exist:**
   - Connect to Supabase using pgAdmin (see Step 2 below)
   - Check if `payment_order` and `payment_transaction` tables exist

### Expected Tables After Migration

After running migrations, you should see:

1. **`payment_order`** table with:

   - `id` (BIGSERIAL PRIMARY KEY)
   - `razorpay_order_id` (VARCHAR, UNIQUE)
   - `amount` (BIGINT)
   - `currency` (VARCHAR)
   - `status` (VARCHAR)
   - `created_at` (TIMESTAMP)
   - Indexes: `idx_payment_order_razorpay_order_id`, `idx_payment_order_status`

2. **`payment_transaction`** table with:

   - `id` (BIGSERIAL PRIMARY KEY)
   - `razorpay_payment_id` (VARCHAR)
   - `razorpay_order_id` (VARCHAR)
   - `razorpay_signature` (VARCHAR)
   - `status` (VARCHAR)
   - `created_at` (TIMESTAMP)
   - Indexes: `idx_payment_transaction_razorpay_payment_id`, `idx_payment_transaction_razorpay_order_id`, `idx_payment_transaction_status`

3. **`flyway_schema_history`** table (Flyway's internal tracking table)

---

## 🔌 Step 2: Connect to Supabase from pgAdmin

You can connect to Supabase using either:

- **Docker pgAdmin** (if network allows external connections)
- **Desktop pgAdmin** (recommended for external databases)

### Option A: Using Docker pgAdmin (If Network Allows)

**Note:** Docker pgAdmin may not be able to reach external hosts depending on your Docker network configuration. If this doesn't work, use Desktop pgAdmin (Option B).

1. **Start pgAdmin:**

   ```bash
   docker-compose up pgadmin -d
   ```

2. **Access pgAdmin:**

   - Open: `http://localhost:5050`
   - Login: `admin@local.com` / `admin123`

3. **Add Supabase Server:**

   - Right-click **"Servers"** → **"Register" → "Server..."**
   - **General Tab:**
     - **Name:** `Supabase Cloud DB`
   - **Connection Tab:**
     | Field | Value |
     | :----------------------- | :--------------------------------------------------------- |
     | **Host name/address** | `aws-1-ap-south-1.pooler.supabase.com` |
     | **Port** | `6543` |
     | **Maintenance database** | `postgres` |
     | **Username** | `postgres.kcykicebvvsshyirgldl` |
     | **Password** | `Adminpetsupbase12` |
   - Check **"Save password"**
   - Click **"Save"**

4. **Verify Connection:**
   - Expand the server in left sidebar
   - Navigate to: `Databases` → `postgres` → `Schemas` → `public` → `Tables`
   - You should see `payment_order` and `payment_transaction` tables

### Option B: Using Desktop pgAdmin (Recommended) ⭐

Desktop pgAdmin works better for external databases like Supabase.

1. **Install Desktop pgAdmin:**

   - Download from: https://www.pgadmin.org/download/
   - Install for your OS (macOS, Windows, Linux)

2. **Open pgAdmin Desktop**

3. **Add Supabase Server:**

   - Right-click **"Servers"** → **"Register" → "Server..."**
   - **General Tab:**
     - **Name:** `Supabase Cloud DB`
   - **Connection Tab:**
     | Field | Value |
     | :----------------------- | :--------------------------------------------------------- |
     | **Host name/address** | `aws-1-ap-south-1.pooler.supabase.com` |
     | **Port** | `6543` |
     | **Maintenance database** | `postgres` |
     | **Username** | `postgres.kcykicebvvsshyirgldl` |
     | **Password** | `Adminpetsupbase12` |
   - **Advanced Tab (Optional):**
     - **DB restriction:** Leave empty (or specify `postgres` if you want to see only one database)
   - Check **"Save password"**
   - Click **"Save"**

4. **Verify Connection:**
   - Expand the server in left sidebar
   - Navigate to: `Databases` → `postgres` → `Schemas` → `public` → `Tables`
   - You should see your tables

---

## 🔍 Verifying Tables in Supabase

### Using pgAdmin Query Tool

1. **Open Query Tool:**

   - Right-click on `postgres` database → **"Query Tool"**

2. **Run Query:**

   ```sql
   -- List all tables
   SELECT table_name
   FROM information_schema.tables
   WHERE table_schema = 'public'
   ORDER BY table_name;
   ```

3. **Expected Output:**
   ```
   table_name
   --------------------
   flyway_schema_history
   payment_order
   payment_transaction
   ```

### Check Table Structure

```sql
-- Check payment_order structure
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'payment_order'
ORDER BY ordinal_position;

-- Check payment_transaction structure
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'payment_transaction'
ORDER BY ordinal_position;
```

---

## 🧪 Testing the Setup

### 1. Verify Application Connects to Supabase

```bash
# Run app (defaults to prod/Supabase)
./mvnw spring-boot:run

# Check logs for:
# "HikariPool-1 - Starting..."
# "HikariPool-1 - Start completed."
# "Flyway migration successful" or migration version logs
```

### 2. Create a Test Order via API

```bash
curl -X POST http://localhost:8081/api/payments/orders \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 10000,
    "currency": "INR"
  }'
```

### 3. Verify in pgAdmin

1. Connect to Supabase in pgAdmin
2. Navigate to: `postgres` → `Schemas` → `public` → `Tables` → `payment_order`
3. Right-click → **"View/Edit Data" → "All Rows"**
4. You should see the test order

---

## ⚠️ Important Notes

### Port 6543 vs 5432

- **Port 6543:** pgBouncer connection pooler (recommended for applications)
- **Port 5432:** Direct connection (if available, but pooler is better)

**Always use port `6543` for Supabase connections.**

### Connection Pooling

Supabase uses **pgBouncer** on port 6543, which:

- ✅ Provides connection pooling
- ✅ Better for applications
- ✅ Handles multiple connections efficiently

### Security

⚠️ **The password shared via WhatsApp should be rotated in production!**

To rotate password:

1. Go to Supabase Dashboard
2. Settings → Database → Reset password
3. Update `application.yml` or `.env` file with new password

---

## 🔄 Switching Between Databases

### View Supabase Data

- Use Desktop pgAdmin (Option B above)
- Or Docker pgAdmin if network allows

### View Local Docker Data

- Use Docker pgAdmin: `http://localhost:5050`
- Connect to `postgres` service (see [pgAdmin Setup Guide](./PGADMIN_SETUP.md))

### Run App Against Supabase

```bash
# Default (prod profile)
./mvnw spring-boot:run
```

### Run App Against Local Docker

```bash
# Dev profile
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

---

## 🐛 Troubleshooting

### Issue: Can't Connect from Docker pgAdmin

**Problem:** Docker pgAdmin can't reach external Supabase host.

**Solution:**

- Use Desktop pgAdmin instead (Option B)
- Or check Docker network configuration

### Issue: Tables Not Created

**Problem:** Flyway migrations didn't run.

**Solution:**

1. Check application logs for Flyway errors
2. Verify Supabase credentials in `application.yml`
3. Manually run migrations:
   ```bash
   # Start app and check logs
   ./mvnw spring-boot:run
   # Look for Flyway migration logs
   ```

### Issue: Connection Timeout

**Problem:** Can't connect to Supabase.

**Solution:**

1. Verify host: `aws-1-ap-south-1.pooler.supabase.com`
2. Verify port: `6543` (not 5432)
3. Check internet connection
4. Verify credentials are correct

### Issue: "Database does not exist"

**Problem:** Wrong database name.

**Solution:**

- Use `postgres` as the database name (not `appdb`)
- Supabase default database is `postgres`

---

## 📚 Additional Resources

- [Main README](../../readme.md) - Complete project documentation
- [Database Setup Guide](./DATABASE_SETUP.md) - Database configuration overview
- [pgAdmin Setup](./PGADMIN_SETUP.md) - Local Docker pgAdmin setup
- [Flow Documentation](../guides/FLOW_DOCUMENTATION.md) - API and flow details
