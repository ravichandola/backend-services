# 🐘 pgAdmin Setup Guide

This guide explains how to configure and use pgAdmin 4 (running in Docker) to manage your PostgreSQL database.

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Accessing pgAdmin](#accessing-pgadmin)
3. [Connecting to PostgreSQL](#connecting-to-postgresql)
4. [Important Notes](#important-notes)
5. [Troubleshooting](#troubleshooting)

---

## 🎯 Overview

pgAdmin 4 is a web-based administration tool for PostgreSQL. In this project, it runs as a Docker container alongside PostgreSQL and the Spring Boot application.

**Key Information:**

- **Access URL:** `http://localhost:5050`
- **Default Email:** `admin@local.com`
- **Default Password:** `admin123`
- **PostgreSQL Service Name:** `postgres` (when connecting from Docker)
- **PostgreSQL Port (from host):** `5433`
- **PostgreSQL Port (from Docker):** `5432`

---

## 🚀 Accessing pgAdmin

### Step 1: Start the Services

Make sure all Docker containers are running:

```bash
docker-compose up -d
```

Verify that pgAdmin is running:

```bash
docker-compose ps
```

You should see `pgadmin` container with status `Up`.

### Step 2: Open pgAdmin in Browser

1. Open your web browser
2. Navigate to: **http://localhost:5050**
3. You should see the pgAdmin login page

### Step 3: Login to pgAdmin

- **Email:** `admin@local.com`
- **Password:** `admin123`

> **Note:** These credentials are configured in `docker-compose.yml`. You can change them by updating the environment variables.

---

## 🔌 Connecting to PostgreSQL

### Step 1: Add a New Server

1. After logging in, right-click on **"Servers"** in the left sidebar
2. Select **"Register" → "Server..."**

### Step 2: General Tab

Fill in the **General** tab:

- **Name:** `Payment Gateway DB` (or any name you prefer)

### Step 3: Connection Tab

**⚠️ IMPORTANT: Since pgAdmin is running in Docker, use the service name, NOT localhost!**

Fill in the **Connection** tab with these values:

| Field                    | Value      | Notes                                           |
| :----------------------- | :--------- | :---------------------------------------------- |
| **Host name/address**    | `postgres` | ✅ **Use service name from docker-compose.yml** |
| **Port**                 | `5432`     | ✅ **Container port (NOT 5433)**                |
| **Maintenance database** | `appdb`    | Database name                                   |
| **Username**             | `appuser`  | Database user                                   |
| **Password**             | `apppass`  | Database password                               |

### Step 4: Save Password (Optional)

- Check **"Save password"** if you don't want to enter it every time
- Click **"Save"**

### Step 5: Verify Connection

1. The server should appear in the left sidebar under "Servers"
2. Click on it to expand
3. You should see:
   - `Databases` → `appdb`
   - `Schemas` → `public`
   - Tables: `payment_order`, `payment_transaction`

---

## 🌐 Connecting to Supabase Cloud Postgres

You can also connect to Supabase from pgAdmin to view the cloud database.

### Option A: Using Docker pgAdmin

**Note:** Docker pgAdmin may not be able to reach external hosts. If connection fails, use Desktop pgAdmin (Option B).

1. **Add Supabase Server:**

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

2. **Verify Connection:**
   - Expand the server in left sidebar
   - Navigate to: `Databases` → `postgres` → `Schemas` → `public` → `Tables`
   - You should see `payment_order` and `payment_transaction` tables (after running migrations)

### Option B: Using Desktop pgAdmin (Recommended for Supabase) ⭐

Desktop pgAdmin works better for external databases.

1. **Install Desktop pgAdmin:**

   - Download from: https://www.pgadmin.org/download/

2. **Add Supabase Server:**
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

### Creating Tables in Supabase

Before you can see tables in Supabase, you need to run Flyway migrations:

1. **Start the application** (defaults to Supabase):

   ```bash
   ./mvnw spring-boot:run
   ```

2. **Flyway will automatically:**

   - Create `payment_order` table
   - Create `payment_transaction` table
   - Create all indexes

3. **Verify in pgAdmin:**
   - Connect to Supabase
   - Navigate to: `postgres` → `Schemas` → `public` → `Tables`
   - You should see the tables

📖 **For detailed Supabase setup, see [SUPABASE_SETUP.md](./SUPABASE_SETUP.md)**

---

## ⚠️ Important Notes

### 🐳 Docker Network Connection

**When connecting from pgAdmin (Docker container):**

- ✅ **Host:** `postgres` (service name from docker-compose.yml)
- ✅ **Port:** `5432` (container's internal port)
- ❌ **DO NOT use:** `localhost` or `127.0.0.1`

**When connecting from external tools (DBeaver, DataGrip, etc.):**

- ✅ **Host:** `localhost` or `127.0.0.1`
- ✅ **Port:** `5433` (mapped port from docker-compose.yml)
- ❌ **DO NOT use:** `postgres` (service name only works inside Docker network)

### 🔐 Credentials Summary

**pgAdmin Login:**

- Email: `admin@local.com`
- Password: `admin123`

**PostgreSQL Database:**

- Host (from Docker): `postgres`
- Host (from host): `localhost`
- Port (from Docker): `5432`
- Port (from host): `5433`
- Database: `appdb`
- Username: `appuser`
- Password: `apppass`

---

## 🔍 Troubleshooting

### Issue 1: Cannot connect to PostgreSQL from pgAdmin

**Error:** `could not connect to server: Connection refused`

**Solution:**

1. Verify PostgreSQL container is running:

   ```bash
   docker-compose ps
   ```

   Should show `postgres-db` as `Up (healthy)`

2. Check if you're using the correct host:

   - From pgAdmin (Docker): Use `postgres`
   - From external tools: Use `localhost`

3. Verify port:
   - From pgAdmin (Docker): Use `5432`
   - From external tools: Use `5433`

### Issue 2: pgAdmin login page not loading

**Error:** Cannot access `http://localhost:5050`

**Solution:**

1. Check if pgAdmin container is running:

   ```bash
   docker-compose ps
   ```

2. Check container logs:

   ```bash
   docker logs pgadmin
   ```

3. Restart pgAdmin:
   ```bash
   docker-compose restart pgadmin
   ```

### Issue 3: Authentication failed

**Error:** `password authentication failed for user "appuser"`

**Solution:**

1. Verify database credentials in `docker-compose.yml`
2. Check PostgreSQL logs:

   ```bash
   docker logs postgres-db
   ```

3. Verify the database exists:
   ```bash
   docker exec postgres-db psql -U appuser -d appdb -c "\l"
   ```

### Issue 4: Database not found

**Error:** `database "appdb" does not exist`

**Solution:**

1. Check if database was created:

   ```bash
   docker exec postgres-db psql -U appuser -d postgres -c "\l"
   ```

2. If database doesn't exist, it should be created automatically by the PostgreSQL container based on `POSTGRES_DB` environment variable

3. Restart PostgreSQL container:
   ```bash
   docker-compose restart postgres
   ```

---

## 📊 Viewing Database Tables

Once connected, you can explore the database:

1. Expand **Servers** → **Payment Gateway DB** → **Databases** → **appdb** → **Schemas** → **public** → **Tables**

2. You should see:

   - `payment_order` - Stores payment orders
   - `payment_transaction` - Stores payment transactions

3. Right-click on any table → **View/Edit Data** → **All Rows** to see the data

---

## 🛠️ Running SQL Queries

1. Right-click on **appdb** database
2. Select **Query Tool**
3. Type your SQL query, for example:
   ```sql
   SELECT * FROM payment_order;
   ```
4. Click **Execute** (F5) or press `Ctrl+Enter`

---

## 🔄 Updating pgAdmin Credentials

To change pgAdmin login credentials, edit `docker-compose.yml`:

```yaml
pgadmin:
  environment:
    PGADMIN_DEFAULT_EMAIL: your-email@example.com
    PGADMIN_DEFAULT_PASSWORD: your-secure-password
```

Then restart the container:

```bash
docker-compose up -d pgadmin
```

---

## 📝 Quick Reference

| Item                          | Value                 |
| :---------------------------- | :-------------------- |
| pgAdmin URL                   | http://localhost:5050 |
| pgAdmin Email                 | admin@local.com       |
| pgAdmin Password              | admin123              |
| PostgreSQL Host (from Docker) | postgres              |
| PostgreSQL Host (from host)   | localhost             |
| PostgreSQL Port (from Docker) | 5432                  |
| PostgreSQL Port (from host)   | 5433                  |
| Database Name                 | appdb                 |
| Database User                 | appuser               |
| Database Password             | apppass               |

---

## ✅ Verification Checklist

- [ ] Docker containers are running (`docker-compose ps`)
- [ ] Can access pgAdmin at `http://localhost:5050`
- [ ] Can login with `admin@local.com` / `admin123`
- [ ] Successfully added PostgreSQL server using `postgres:5432`
- [ ] Can see `appdb` database
- [ ] Can see `payment_order` and `payment_transaction` tables
- [ ] Can run SQL queries successfully

---

**Need Help?**

- Check the [main README.md](../readme.md) for more information about the project setup
- For Supabase setup details, see [SUPABASE_SETUP.md](./SUPABASE_SETUP.md)
