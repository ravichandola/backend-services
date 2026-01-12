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

Create a `.env` file in project root:

```env
# Razorpay
RAZORPAY_KEY=rzp_test_xxxxx
RAZORPAY_SECRET=your-secret-key

# Database Profile (optional - defaults to 'prod')
# SPRING_PROFILES_ACTIVE=prod  # Supabase (default)
# SPRING_PROFILES_ACTIVE=dev   # Local Docker

# Override Supabase credentials (optional - has defaults)
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:6543/postgres
SPRING_DATASOURCE_USERNAME=postgres.kcykicebvvsshyirgldl
SPRING_DATASOURCE_PASSWORD=Adminpetsupbase12

# Override Local Docker credentials (optional - has defaults)
# SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/appdb
# SPRING_DATASOURCE_USERNAME=appuser
# SPRING_DATASOURCE_PASSWORD=apppass
```

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

### From Supabase to Local Docker

1. **Start Docker Postgres:**

   ```bash
   docker-compose up postgres -d
   ```

2. **Run app with dev profile:**
   ```bash
   SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
   ```

### From Local Docker to Supabase

1. **Stop Docker Postgres (optional):**

   ```bash
   docker-compose down
   ```

2. **Run app (defaults to prod/Supabase):**
   ```bash
   ./mvnw spring-boot:run
   ```

---

## 🐳 Docker Compose Behavior

When you run `docker-compose up`, the app service automatically:

- Uses `dev` profile
- Connects to local `postgres` service
- Ignores Supabase configuration

This allows you to:

- Keep Supabase as default for standalone runs
- Use local Docker when running full stack

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

## 📚 Additional Resources

- [Main README](./readme.md) - Complete project documentation
- [Flow Documentation](./documentation/FLOW_DOCUMENTATION.md) - API and flow details
- [pgAdmin Setup](./documentation/PGADMIN_SETUP.md) - pgAdmin configuration guide
