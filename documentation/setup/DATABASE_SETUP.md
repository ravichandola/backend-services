# 🗄️ Database Setup Guide

## Quick Overview

This project supports **two database configurations**:

1. **🌐 Supabase Cloud Postgres** (Default/Production) - `prod` profile
2. **🐳 Local Docker Postgres** (Development/Testing) - `dev` profile

---

## 🚀 Quick Start

### Use Supabase (Default - No Setup Needed)

```bash
# Just run - connects to Supabase automatically
./mvnw spring-boot:run
```

### Use Local Docker Postgres

```bash
# Option 1: Set profile
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# Option 2: Use Docker Compose (automatically uses dev profile)
docker-compose up --build
```

---

## 📋 Database Connection Details

### Supabase Cloud Postgres (prod profile)

| Setting               | Value                                                                  |
| --------------------- | ---------------------------------------------------------------------- |
| **Host**              | `aws-1-ap-south-1.pooler.supabase.com`                                 |
| **Port**              | `6543` (pgBouncer pooler)                                              |
| **Database**          | `postgres`                                                             |
| **Username**          | `postgres.kcykicebvvsshyirgldl`                                        |
| **Password**          | `Adminpetsupbase12`                                                    |
| **Connection String** | `jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:6543/postgres` |

**Connect via External Tools (DBeaver, DataGrip, etc.):**

- Use the connection details above
- Port: `6543` (important - this is the pooler port)

### Local Docker Postgres (dev profile)

| Setting               | Value                                                       |
| --------------------- | ----------------------------------------------------------- |
| **Host**              | `localhost` (from host) or `postgres` (from Docker network) |
| **Port**              | `5433` (host) or `5432` (container)                         |
| **Database**          | `appdb`                                                     |
| **Username**          | `appuser`                                                   |
| **Password**          | `apppass`                                                   |
| **Connection String** | `jdbc:postgresql://localhost:5433/appdb`                    |

**Connect via pgAdmin (Docker):**

- Access: `http://localhost:5050`
- Login: `admin@local.com` / `admin123`
- Server Host: `postgres` (service name)
- Port: `5432` (container port)

**Connect via External Tools:**

- Host: `localhost`
- Port: `5433`

---

## 🔧 Configuration via Environment Variables

### Using .env file (Recommended)

The `.env` file in project root automatically configures database switching:

```env
# Database Profile Selection
# Use 'dev' for local Docker Postgres (default)
# Use 'prod' for Supabase Cloud Postgres
SPRING_PROFILES_ACTIVE=prod  # or 'dev' for local

# ============================================
# Supabase Database Configuration (PROD Profile)
# ============================================
SUPABASE_DATASOURCE_URL=jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:6543/postgres
SUPABASE_DATASOURCE_USERNAME=postgres.kcykicebvvsshyirgldl
SUPABASE_DATASOURCE_PASSWORD=Adminpetsupbase12

# ============================================
# Local Docker Postgres Configuration (DEV Profile)
# ============================================
POSTGRES_DB=appdb
LOCAL_DATASOURCE_URL=jdbc:postgresql://postgres:5432/appdb
LOCAL_DATASOURCE_USERNAME=appuser
LOCAL_DATASOURCE_PASSWORD=apppass
```

**Key Points:**
- Change `SPRING_PROFILES_ACTIVE` to switch between databases
- All variables are automatically loaded by docker-compose
- No need to manually edit docker-compose.yml

### Using Command Line

```bash
# Supabase (default)
./mvnw spring-boot:run

# Local Docker
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

---

## 🎯 When to Use Which Database?

### Use Supabase (prod profile) when:

- ✅ Production deployments
- ✅ Team collaboration
- ✅ No local setup needed
- ✅ Shared database for testing
- ✅ Quick setup

### Use Local Docker Postgres (dev profile) when:

- ✅ Local development
- ✅ Testing with mock data
- ✅ Offline development
- ✅ Data isolation
- ✅ Need to reset database frequently

---

## 🔄 Switching Between Databases

### Automatic Switching via .env

**Switch to Supabase (Production):**
```bash
# In .env file, set:
SPRING_PROFILES_ACTIVE=prod

# Restart containers
docker-compose down
docker-compose up -d
```

**Switch to Local Docker (Development):**
```bash
# In .env file, set:
SPRING_PROFILES_ACTIVE=dev

# Restart containers
docker-compose down
docker-compose up -d
```

### Manual Switching (Without Docker)

**From Supabase to Local Docker:**
1. Start Docker Postgres: `docker-compose up postgres -d`
2. Run app: `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run`

**From Local Docker to Supabase:**
1. Run app: `SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run`

---

## 🐳 Docker Compose Behavior

When you run `docker-compose up`, the services:

- Read `SPRING_PROFILES_ACTIVE` from `.env` file
- Automatically use correct database based on profile
- Pass all environment variables from `.env` to containers

**Profile Selection:**
- `SPRING_PROFILES_ACTIVE=prod` → Connects to Supabase
- `SPRING_PROFILES_ACTIVE=dev` → Connects to Local Docker Postgres

This allows you to:
- Switch databases by changing one variable in `.env`
- Keep configuration centralized
- No need to edit docker-compose.yml

---

## ⚠️ Security Notes

1. **Password Rotation:** The Supabase password shared via WhatsApp should be rotated in production
2. **Environment Variables:** Use `.env` file (already in `.gitignore`) for secrets
3. **Never Commit:** Never commit `.env` file or hardcode credentials

---

## 🧪 Testing Database Connection

### Test Supabase Connection

```bash
# Run app (defaults to Supabase)
./mvnw spring-boot:run

# Check logs for:
# "HikariPool-1 - Starting..."
# "HikariPool-1 - Start completed."
```

### Test Local Docker Connection

```bash
# Start Postgres
docker-compose up postgres -d

# Run app with dev profile
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

---

## 🗄️ Running Database Migrations

### Automatic Migration (On Startup)

Flyway is configured to run migrations automatically when the application starts. However, if migrations don't run automatically, you can trigger them manually.

### Manual Migration via API

**Run Migrations:**
```bash
curl -X POST http://localhost:8081/api/migrations/migrate
```

**Response:**
```json
{
  "success": true,
  "migrationsExecuted": 9,
  "currentVersion": "9",
  "message": "Migrations completed successfully"
}
```

**Check Migration Status:**
```bash
curl -X POST http://localhost:8081/api/migrations/info
```

**Response:**
```json
{
  "currentVersion": "9",
  "pendingMigrations": 0,
  "allMigrations": 9
}
```

### Manual Migration via pgAdmin

1. Connect to Supabase in pgAdmin (see pgAdmin Setup guide)
2. Open Query Tool
3. Run the SQL script: `run-migrations.sql` (located in project root)
4. All tables will be created

### Expected Tables After Migration

After running migrations, you should see these tables in the `public` schema:

1. `users` - User identity data from Clerk
2. `organizations` - Multi-tenant organizations
3. `roles` - Authorization roles (ADMIN, USER)
4. `memberships` - User-organization-role relationships
5. `user_events` - Audit trail for user webhooks
6. `organization_events` - Audit trail for org webhooks
7. `auth_sessions` - Authentication session tracking
8. `payment_order` - Razorpay payment orders
9. `payment_transaction` - Razorpay payment transactions
10. `flyway_schema_history` - Flyway migration tracking

---

## 📚 Additional Resources

- [Main README](../../readme.md) - Complete project documentation
- [Flow Documentation](../guides/FLOW_DOCUMENTATION.md) - API and flow details
- [pgAdmin Setup](./PGADMIN_SETUP.md) - pgAdmin configuration guide
- [Fixes and Improvements](../architecture/FIXES_AND_IMPROVEMENTS.md) - Detailed log of issues and fixes
