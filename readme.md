# 💳 Payment Gateway Backend (Dockerized)

A robust Spring Boot backend application for handling payment order creation and verification using **Razorpay**, **PostgreSQL**, and **Spring Data JPA**.

This project is **fully Dockerized**, meaning no Java, PostgreSQL, or Maven installation is required on your local machine.

---

## 🚀 Tech Stack

| Component              | Technology                         |
| :--------------------- | :--------------------------------- |
| **Java**               | 25 (Eclipse Temurin inside Docker) |
| **Spring Boot**        | 4.x                                |
| **Hibernate ORM**      | 7.x                                |
| **Database**           | PostgreSQL 16                      |
| **Build Tool**         | Maven Wrapper                      |
| **Containerization**   | Docker & Docker Compose            |
| **Payment Gateway**    | Razorpay (Test Mode)               |
| **Database Migration** | Flyway                             |
| **Environment Config** | Dotenv (.env file support)         |

---

## 📚 Documentation

This project includes comprehensive documentation to help you understand and work with the codebase:

- **[Flow Documentation](./documentation/FLOW_DOCUMENTATION.md)** - Detailed explanation of the payment flow, API endpoints, request/response formats, error handling, and complete system architecture
- **[pgAdmin Setup Guide](./documentation/PGADMIN_SETUP.md)** - Step-by-step guide for configuring and using pgAdmin 4 (Docker container) to manage your PostgreSQL database

> **Quick Links:**
>
> - For understanding the payment flow and API details → [FLOW_DOCUMENTATION.md](./documentation/FLOW_DOCUMENTATION.md)
> - For setting up and connecting to pgAdmin → [PGADMIN_SETUP.md](./documentation/PGADMIN_SETUP.md)

---

## ✨ Features Implemented

This branch includes a complete payment gateway integration with the following features:

### 🎯 Core Features

1. **Payment Order Creation**

   - Create Razorpay orders via REST API
   - Automatic order ID generation and tracking
   - Amount validation and currency support (INR)
   - Order status tracking (CREATED → PAID)

2. **Payment Verification**

   - Secure payment signature verification using Razorpay's signature algorithm
   - Payment transaction recording
   - Order status update upon successful payment

3. **Database Persistence**

   - Payment orders stored in `payment_order` table
   - Payment transactions stored in `payment_transaction` table
   - Automatic timestamp tracking for audit purposes
   - Database indexes for optimized queries

4. **Environment Configuration**

   - `.env` file support via Dotenv library
   - Environment variable injection for sensitive credentials
   - Configuration validation on application startup

5. **Error Handling**

   - Custom `PaymentException` for payment-related errors
   - Comprehensive error messages for debugging
   - Razorpay API error handling and translation

6. **Logging**
   - Structured logging using SLF4J
   - Payment flow tracking (order creation, verification)
   - Error logging with context

### 🏗️ Architecture Components

#### **Controllers**

- `PaymentController` - REST API endpoints for payment operations
  - `POST /api/payments/create-order` - Create a new payment order
  - `POST /api/payments/verify` - Verify payment signature

#### **Services**

- `PaymentService` - Interface defining payment operations
- `PaymentServiceImpl` - Implementation with Razorpay integration
  - Order creation with Razorpay API
  - Payment signature verification
  - Database persistence

#### **Entities**

- `PaymentOrder` - JPA entity for payment orders
  - Fields: id, razorpayOrderId, amount, currency, status, createdAt
- `PaymentTransaction` - JPA entity for payment transactions
  - Fields: id, razorpayPaymentId, razorpayOrderId, razorpaySignature, status, createdAt

#### **DTOs (Data Transfer Objects)**

- `CreateOrderRequest` - Request DTO with amount validation
- `CreateOrderResponse` - Response DTO with order details
- `VerifyPaymentRequest` - Request DTO for payment verification

#### **Repositories**

- `PaymentOrderRepository` - JPA repository with custom query method
  - `findByRazorpayOrderId()` - Find order by Razorpay order ID
- `PaymentTransactionRepository` - JPA repository for transactions

#### **Configuration**

- `RazorpayConfig` - Razorpay client bean configuration
  - Credential validation on startup
  - Key format validation
  - Masked credential logging
- `DotenvApplicationContextInitializer` - Loads `.env` file into Spring context

#### **Exception Handling**

- `PaymentException` - Custom exception for payment-related errors
  - HTTP 400 (Bad Request) status code
  - Descriptive error messages

### 🗄️ Database Schema

#### **Payment Order Table** (`payment_order`)

```sql
- id (BIGSERIAL PRIMARY KEY)
- razorpay_order_id (VARCHAR, UNIQUE, NOT NULL)
- amount (BIGINT, NOT NULL)
- currency (VARCHAR, NOT NULL)
- status (VARCHAR, NOT NULL)
- created_at (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
```

**Indexes:**

- `idx_payment_order_razorpay_order_id` - Fast lookup by Razorpay order ID
- `idx_payment_order_status` - Fast filtering by status

#### **Payment Transaction Table** (`payment_transaction`)

```sql
- id (BIGSERIAL PRIMARY KEY)
- razorpay_payment_id (VARCHAR)
- razorpay_order_id (VARCHAR)
- razorpay_signature (VARCHAR)
- status (VARCHAR)
- created_at (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
```

**Indexes:**

- `idx_payment_transaction_razorpay_payment_id` - Fast lookup by payment ID
- `idx_payment_transaction_razorpay_order_id` - Fast lookup by order ID
- `idx_payment_transaction_status` - Fast filtering by status

### 📦 Dependencies Added

1. **Razorpay Java SDK** (`com.razorpay:razorpay-java:1.4.8`)

   - Official Razorpay SDK for Java
   - Order creation and payment utilities

2. **Dotenv Java** (`io.github.cdimascio:dotenv-java:3.0.0`)

   - Load environment variables from `.env` file
   - Seamless integration with Spring Boot

3. **PostgreSQL Driver** (`org.postgresql:postgresql`)

   - Database connectivity for PostgreSQL

4. **Flyway** (`org.flywaydb:flyway-core` & `flyway-database-postgresql`)

   - Database migration tool
   - Version-controlled schema changes

5. **Spring Boot Validation** (`spring-boot-starter-validation`)

   - Request validation annotations
   - `@NotNull`, `@Min`, `@NotBlank` validations

6. **Lombok** (`org.projectlombok:lombok`)
   - Reduces boilerplate code
   - `@Builder`, `@Getter`, `@Setter`, `@RequiredArgsConstructor`

### 🔧 Configuration Details

#### **Application Configuration** (`application.yml`)

- PostgreSQL datasource configuration with environment variable support
- JPA/Hibernate settings (ddl-auto: validate, show-sql: true)
- Flyway migration configuration
- Server port: 8080
- Razorpay key and secret from environment variables

#### **Docker Compose** (`docker-compose.yml`)

- PostgreSQL 16 Alpine container
- Spring Boot application container
- Health checks for database readiness
- Volume persistence for database data
- Environment variable passthrough for Razorpay credentials

#### **Database Migrations** (Flyway)

- `V1__Create_payment_order_table.sql` - Creates payment_order table with indexes
- `V2__Create_payment_transaction_table.sql` - Creates payment_transaction table with indexes

### 🔐 Security Features

1. **Credential Validation**

   - Razorpay credentials validated on application startup
   - Clear error messages if credentials are missing or invalid
   - Key format validation (rzp*test* or rzp*live* prefix)

2. **Payment Signature Verification**

   - Cryptographic signature verification using Razorpay secret
   - Prevents payment tampering and fraud
   - Secure payload construction (orderId|paymentId)

3. **Environment Variable Security**
   - Sensitive credentials stored in environment variables
   - `.env` file support for local development
   - No hardcoded secrets in codebase

### 📝 API Endpoints

#### **1. Create Payment Order**

```
POST /api/payments/create-order
Content-Type: application/json

Request:
{
  "amount": 500
}

Response:
{
  "orderId": "order_xxxxx",
  "amount": 500,
  "currency": "INR",
  "status": "created",
  "receipt": "rcpt_xxxxx"
}
```

#### **2. Verify Payment**

```
POST /api/payments/verify
Content-Type: application/json

Request:
{
  "razorpayOrderId": "order_xxxxx",
  "razorpayPaymentId": "pay_xxxxx",
  "razorpaySignature": "signature_xxxxx"
}

Response:
"Payment verified successfully"
```

### 🐛 Issues Fixed

1. **Port 8080 Conflict**
   - Documented troubleshooting steps for port conflicts
   - Provided solutions for both macOS/Linux and Windows
   - Alternative port configuration option

---

## 🔄 Branch Changes: Database Migration to PostgreSQL

This branch includes significant changes migrating from MySQL to PostgreSQL and adding pgAdmin for database management.

### 📋 Changes Summary

#### 1. **Database Migration: MySQL → PostgreSQL**

- **Previous:** MySQL 8.4 database
- **Current:** PostgreSQL 16 Alpine
- **Port:** Changed from 3307 to 5433 (host) / 5432 (container)
- **Driver:** Updated from MySQL driver to PostgreSQL driver
- **Connection URL:** Updated to `jdbc:postgresql://localhost:5433/appdb`

#### 2. **pgAdmin Integration**

- Added pgAdmin 4 container for database management
- Accessible at `http://localhost:5050`
- Pre-configured with default credentials
- See [PGADMIN_SETUP.md](./documentation/PGADMIN_SETUP.md) for detailed setup instructions

#### 3. **Connection Pool Improvements**

- Added HikariCP connection pool configuration
- Connection timeout: 30 seconds (handles database startup delays)
- Maximum pool size: 10 connections
- Minimum idle: 5 connections
- Connection test query: `SELECT 1` for health checks
- Improved connection lifecycle management

#### 4. **Server Port Change**

- Changed from port `8080` to `8081` to avoid conflicts
- Updated in `application.yml`
- API endpoints now accessible at `http://localhost:8081`

#### 5. **Docker Compose Updates**

- Added `pgadmin` service with health check dependencies
- PostgreSQL service with health checks
- Proper service dependencies (app waits for database to be healthy)
- Volume persistence for both PostgreSQL and pgAdmin data

### 🔧 Configuration Changes

**Before (MySQL):**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/appdb
    driver-class-name: com.mysql.cj.jdbc.Driver
```

**After (PostgreSQL):**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/appdb
    driver-class-name: org.postgresql.Driver
    hikari:
      connection-timeout: 30000
      maximum-pool-size: 10
      minimum-idle: 5
      connection-test-query: SELECT 1
```

### 📝 Migration Notes

- **Database Schema:** Flyway migrations updated for PostgreSQL syntax
- **Data Types:** Changed from MySQL-specific types to PostgreSQL equivalents
- **Indexes:** Updated index creation syntax for PostgreSQL
- **Connection Handling:** Improved with HikariCP pool configuration

### 🚀 Running the Updated Setup

1. **Start all services:**

   ```bash
   docker-compose up -d
   ```

2. **Access the application:**

   - API: `http://localhost:8081`
   - pgAdmin: `http://localhost:5050`

3. **Connect to database:**
   - From host machine: `localhost:5433`
   - From Docker containers: `postgres:5432` (service name)

For detailed pgAdmin setup instructions, see [PGADMIN_SETUP.md](./documentation/PGADMIN_SETUP.md).

---

## 🖥️ System Requirements

You only need the following installed:

- ✅ **Docker Desktop**
- ✅ **Docker Compose** (included with Docker Desktop)
- ✅ **Git**

> **Note:** ❌ Java, PostgreSQL, and Maven are **not** required on your host machine.

---

## 📁 Project Structure

```text
.
├── Dockerfile              # Instructions to build the Spring Boot image
├── docker-compose.yml      # Orchestrates the App, PostgreSQL, and pgAdmin containers
├── pom.xml                 # Project dependencies
├── mvnw                    # Maven wrapper for Linux/macOS
├── mvnw.cmd                # Maven wrapper for Windows
├── src
│   └── main
│       ├── java            # Application source code
│       └── resources
│           └── application.yml # App configuration
└── README.md
```

---

## ⚙️ Configuration Overview

This project uses environment variables for configuration to ensure it is portable and secure.

- `application.yml` → Declares the configuration structure and reads environment variables.
- `docker-compose.yml` → Injects the actual values into the container.

---

## 🧾 application.yml

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5433/appdb}
    username: ${SPRING_DATASOURCE_USERNAME:appuser}
    password: ${SPRING_DATASOURCE_PASSWORD:apppass}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    validate-on-migrate: true

server:
  port: 8080

razorpay:
  key: ${RAZORPAY_KEY:your-razorpay-key-id}
  secret: ${RAZORPAY_SECRET:your-razorpay-secret-key}
```

> **Note:** The configuration uses environment variables with default values. For production, set `RAZORPAY_KEY` and `RAZORPAY_SECRET` environment variables or use a `.env` file.

---

## ▶️ Complete Setup Guide

This section provides a step-by-step guide to set up and run the Payment Gateway Backend project from scratch.

### 📋 Prerequisites Checklist

Before starting, ensure you have:

- [ ] **Docker Desktop** installed and running
  - Download from: https://www.docker.com/products/docker-desktop
  - Verify installation: `docker --version` and `docker-compose --version`
- [ ] **Git** installed (for cloning the repository)
- [ ] **Razorpay Account** with API keys
  - Sign up at: https://razorpay.com/
  - Get test keys from: https://dashboard.razorpay.com/app/keys

> **Note:** You do NOT need Java, Maven, or PostgreSQL installed on your machine. Everything runs in Docker containers.

---

### 🚀 Step-by-Step Setup

#### Step 1: Clone the Repository

```bash
git clone https://github.com/<your-username>/payment-gateway-backend.git
cd payment-gateway-backend
```

Or if you already have the repository:

```bash
cd payment-gateway-backend
```

#### Step 2: Configure Environment Variables

Create a `.env` file in the project root directory:

```bash
touch .env
```

Add your Razorpay credentials to the `.env` file:

```env
# Razorpay API Credentials (Required)
RAZORPAY_KEY=rzp_test_xxxxxxxxxxxxx
RAZORPAY_SECRET=your_razorpay_secret_key_here

# Database Configuration (Optional - has defaults)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/appdb
SPRING_DATASOURCE_USERNAME=appuser
SPRING_DATASOURCE_PASSWORD=apppass
```

**How to get Razorpay Keys:**

1. Log in to [Razorpay Dashboard](https://dashboard.razorpay.com/)
2. Navigate to **Settings** → **API Keys**
3. Click **Generate Test Key** (for development)
4. Copy the **Key ID** and **Key Secret**
5. Paste them in your `.env` file

> **Security Note:** The `.env` file is already in `.gitignore` and will not be committed to version control.

#### Step 3: Start Docker Desktop

1. Open Docker Desktop application
2. Wait until Docker Desktop is fully started (whale icon in system tray)
3. Verify Docker is running:
   ```bash
   docker ps
   ```
   Should show an empty list (no error messages)

#### Step 4: Start All Services

From the project root directory, run:

```bash
docker-compose up --build
```

**What this command does:**

1. **Builds the Spring Boot application:**

   - Uses the `Dockerfile` to build the application
   - Downloads Java 25 JDK image
   - Runs Maven to compile and package the application
   - Creates a JAR file

2. **Starts PostgreSQL database:**

   - Pulls `postgres:16-alpine` image (if not already present)
   - Creates `postgres-db` container
   - Initializes database `appdb` with user `appuser`
   - Waits for health check to pass

3. **Starts pgAdmin:**

   - Pulls `dpage/pgadmin4:latest` image
   - Creates `pgadmin` container
   - Waits for PostgreSQL to be healthy before starting

4. **Starts Spring Boot application:**
   - Builds application image
   - Creates `springboot-app` container
   - Waits for PostgreSQL to be healthy
   - Runs database migrations (Flyway)
   - Starts the application on port 8080

**Expected Output:**

```
[+] Building 15.2s
[+] Running 4/4
 ✔ Network payment_default      Created
 ✔ Container postgres-db        Started
 ✔ Container pgadmin            Started
 ✔ Container springboot-app     Started
```

#### Step 5: Monitor Startup Logs

Watch the application logs to verify everything starts correctly:

```bash
docker-compose logs -f app
```

**Look for these success messages:**

1. **Database connection:**

   ```
   HikariPool-1 - Starting...
   HikariPool-1 - Start completed.
   ```

2. **Flyway migrations:**

   ```
   Flyway Community Edition 10.x.x
   Successfully applied 2 migrations
   ```

3. **Application ready:**
   ```
   Started PaymentApplication in X.XXX seconds
   ```

**If you see errors:**

- **Connection refused:** PostgreSQL container might not be ready yet. Wait 10-20 seconds and check again.
- **Razorpay authentication failed:** Check your `.env` file credentials.
- **Port already in use:** See [Troubleshooting](#-troubleshooting) section.

#### Step 6: Verify All Services Are Running

Open a new terminal and check container status:

```bash
docker-compose ps
```

**Expected output:**

```
NAME             STATUS                    PORTS
postgres-db      Up X minutes (healthy)    0.0.0.0:5433->5432/tcp
pgadmin          Up X minutes              0.0.0.0:5050->80/tcp
springboot-app   Up X minutes              0.0.0.0:8080->8080/tcp
```

All containers should show `Up` status. PostgreSQL should show `(healthy)`.

#### Step 7: Test the API

**Option 1: Using curl**

```bash
curl -X POST http://localhost:8080/api/payments/create-order \
  -H "Content-Type: application/json" \
  -d '{"amount": 500}'
```

**Option 2: Using Postman**

1. Open Postman
2. Create a new POST request
3. URL: `http://localhost:8080/api/payments/create-order`
4. Headers: `Content-Type: application/json`
5. Body (raw JSON):
   ```json
   {
     "amount": 500
   }
   ```
6. Click **Send**

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

#### Step 8: Access pgAdmin (Optional)

1. Open browser: `http://localhost:5050`
2. Login with:
   - Email: `admin@local.com`
   - Password: `admin123`
3. Add PostgreSQL server (see [PGADMIN_SETUP.md](./documentation/PGADMIN_SETUP.md) for details)

---

### 🎯 Quick Start (TL;DR)

For experienced users, here's the quick version:

```bash
# 1. Clone repository
git clone <repository-url>
cd payment-gateway-backend

# 2. Create .env file with Razorpay keys
echo "RAZORPAY_KEY=rzp_test_xxxxx" > .env
echo "RAZORPAY_SECRET=your_secret" >> .env

# 3. Start services
docker-compose up --build

# 4. Wait for "Started PaymentApplication" in logs
# 5. Test API at http://localhost:8080/api/payments/create-order
```

---

### 💻 Running Locally (Without Docker)

If you prefer to run the application directly on your machine without Docker:

#### Prerequisites for Local Development

- **Java 25** (JDK) installed
- **Maven** installed (or use Maven Wrapper: `./mvnw`)
- **PostgreSQL 16** installed and running locally
- **Razorpay API keys** configured

#### Steps

1. **Start PostgreSQL locally:**

   ```bash
   # On macOS (using Homebrew)
   brew services start postgresql@16

   # On Linux
   sudo systemctl start postgresql

   # Create database and user
   psql -U postgres
   CREATE DATABASE appdb;
   CREATE USER appuser WITH PASSWORD 'apppass';
   GRANT ALL PRIVILEGES ON DATABASE appdb TO appuser;
   ```

2. **Create `.env` file:**

   ```env
   RAZORPAY_KEY=rzp_test_xxxxx
   RAZORPAY_SECRET=your_secret_key
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/appdb
   SPRING_DATASOURCE_USERNAME=appuser
   SPRING_DATASOURCE_PASSWORD=apppass
   ```

3. **Run the application:**

   ```bash
   # Using Maven Wrapper
   ./mvnw spring-boot:run

   # Or using Maven (if installed)
   mvn spring-boot:run
   ```

4. **Access the API:**
   - API: `http://localhost:8080` (or port configured in `application.yml`)

> **Note:** When running locally, the application connects to PostgreSQL on `localhost:5432` (default PostgreSQL port), not `5433`.

---

### 🔄 Running in Background (Detached Mode)

To run containers in the background:

```bash
docker-compose up -d --build
```

View logs:

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f app
docker-compose logs -f postgres
docker-compose logs -f pgadmin
```

---

### 🛑 Stopping the Application

**Stop all containers:**

```bash
docker-compose down
```

**Stop and remove all data (including database):**

```bash
docker-compose down -v
```

> **Warning:** `-v` flag removes all volumes, including database data. Use with caution!

---

### 🔁 Restarting After Changes

**After code changes:**

1. Stop containers: `docker-compose down`
2. Rebuild and start: `docker-compose up --build`

**After configuration changes (application.yml, docker-compose.yml):**

1. Stop containers: `docker-compose down`
2. Start again: `docker-compose up --build`

**After .env file changes:**

1. Restart only the app container:
   ```bash
   docker-compose restart app
   ```

---

### 📊 Service URLs & Ports

Once everything is running, access services at:

| Service        | URL                   | Credentials                |
| :------------- | :-------------------- | :------------------------- |
| **API**        | http://localhost:8080 | N/A                        |
| **pgAdmin**    | http://localhost:5050 | admin@local.com / admin123 |
| **PostgreSQL** | localhost:5433        | appuser / apppass          |

---

### ✅ Verification Checklist

After setup, verify everything works:

- [ ] Docker Desktop is running
- [ ] All containers are up: `docker-compose ps`
- [ ] PostgreSQL is healthy: `docker-compose ps` shows `(healthy)`
- [ ] Application logs show "Started PaymentApplication"
- [ ] API responds: `curl http://localhost:8080/api/payments/create-order` (with POST body)
- [ ] pgAdmin accessible: `http://localhost:5050`
- [ ] Can create payment orders via API

---

## 🧪 API Testing (Postman)

### Create Payment Order

**Endpoint:** `POST /api/payments/create-order`

**URL:** `http://localhost:8080/api/payments/create-order`

**Headers:**

```
Content-Type: application/json
```

**Request Body:**

```json
{
  "amount": 500
}
```

**Sample Response:**

```json
{
  "orderId": "order_xxxxx",
  "amount": 500,
  "currency": "INR",
  "status": "created",
  "receipt": "rcpt_xxxxx"
}
```

### Verify Payment

**Endpoint:** `POST /api/payments/verify`

**URL:** `http://localhost:8080/api/payments/verify`

**Headers:**

```
Content-Type: application/json
```

**Request Body:**

```json
{
  "razorpayOrderId": "order_xxxxx",
  "razorpayPaymentId": "pay_xxxxx",
  "razorpaySignature": "signature_xxxxx"
}
```

**Sample Response:**

```
Payment verified successfully
```

> **Note:** The `razorpaySignature` is provided by Razorpay after successful payment. It should be verified server-side to ensure payment authenticity.

---

## 🗄️ Database Details

PostgreSQL runs inside a Docker container. Flyway is configured to manage database migrations automatically.

### Connect Using GUI Tools

#### Option 1: pgAdmin (Docker Container) - Recommended ⭐

**Quick Start:**

1. Access pgAdmin at: `http://localhost:5050`
2. Login with: `admin@local.com` / `admin123`
3. Add server with:
   - Host: `postgres` (service name, NOT localhost)
   - Port: `5432` (container port)
   - Database: `appdb`
   - Username: `appuser`
   - Password: `apppass`

📖 **For detailed step-by-step instructions, see [PGADMIN_SETUP.md](./documentation/PGADMIN_SETUP.md)**

**Access pgAdmin:** `http://localhost:5050`

#### Option 2: External Tools (DBeaver / DataGrip / etc.)

| Field | Value     |
| :---- | :-------- |
| Host  | localhost |
| Port  | 5433      |
| User  | appuser   |
| Pass  | apppass   |
| DB    | appdb     |

> **Important:** When connecting from Docker containers (like pgAdmin), use:
>
> - Host: `postgres` (service name, not localhost)
> - Port: `5432` (container port, not 5433)

---

## 🔐 Environment Variables & Configuration

### Required Environment Variables

The application requires the following environment variables:

| Variable                     | Description                                       | Example                                  |
| :--------------------------- | :------------------------------------------------ | :--------------------------------------- |
| `RAZORPAY_KEY`               | Razorpay Key ID (from Razorpay Dashboard)         | `rzp_test_xxxxx`                         |
| `RAZORPAY_SECRET`            | Razorpay Secret Key (from Razorpay Dashboard)     | `your-secret-key`                        |
| `SPRING_DATASOURCE_URL`      | PostgreSQL connection URL (optional, has default) | `jdbc:postgresql://localhost:5433/appdb` |
| `SPRING_DATASOURCE_USERNAME` | Database username (optional, has default)         | `appuser`                                |
| `SPRING_DATASOURCE_PASSWORD` | Database password (optional, has default)         | `apppass`                                |

### Setting Environment Variables

#### Option 1: Using `.env` file (Recommended for Local Development)

Create a `.env` file in the project root:

```env
RAZORPAY_KEY=rzp_test_xxxxx
RAZORPAY_SECRET=your-secret-key
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/appdb
SPRING_DATASOURCE_USERNAME=appuser
SPRING_DATASOURCE_PASSWORD=apppass
```

The application automatically loads `.env` file using `DotenvApplicationContextInitializer`.

> **Note:** Add `.env` to `.gitignore` to prevent committing sensitive credentials.

#### Option 2: Export in Terminal (macOS/Linux)

```bash
export RAZORPAY_KEY="rzp_test_xxxxx"
export RAZORPAY_SECRET="your-secret-key"
```

#### Option 3: Docker Compose Environment Variables

Set variables in `docker-compose.yml` or pass them when running:

```bash
RAZORPAY_KEY=rzp_test_xxxxx RAZORPAY_SECRET=your-secret-key docker compose up
```

### Razorpay Configuration

The app uses Razorpay Test Keys for development. Get your keys from:

1. Log in to [Razorpay Dashboard](https://dashboard.razorpay.com/)
2. Navigate to Settings → API Keys
3. Generate Test Keys (or use existing ones)

**Key Validation:**

- Application validates credentials on startup
- Key format should start with `rzp_test_` (test mode) or `rzp_live_` (production)
- Clear error messages if credentials are missing or invalid

> **Security Tip:** Never commit your live `RAZORPAY_SECRET` to a public repository. Use environment variables or `.env` file (which should be in `.gitignore`).

---

## 🧹 Stop & Clean Up

### Stop containers:

```bash
docker compose down
```

### Stop and remove all database data (Volumes):

```bash
docker compose down -v
```

---

## 🔧 Troubleshooting

### Port 8080 Already in Use

If you encounter the error:

```
Web server failed to start. Port 8080 was already in use.
```

**Solution 1: Find and stop the process using port 8080**

On macOS/Linux:

```bash
# Find the process ID using port 8080
lsof -ti:8080

# Kill the process (replace <PID> with the actual process ID)
kill <PID>
```

On Windows:

```bash
# Find the process ID using port 8080
netstat -ano | findstr :8080

# Kill the process (replace <PID> with the actual process ID)
taskkill /PID <PID> /F
```

**Solution 2: Change the application port**

If you prefer to use a different port, modify `src/main/resources/application.yml`:

```yaml
server:
  port: 8082 # Change to any available port
```

Then update your API URLs accordingly (e.g., `http://localhost:8082`).

### Database Connection Issues

**Error:** `Connection to localhost:5433 refused` or `Connection to postgres:5432 refused`

**Solution:**

1. **Check Docker Desktop is running:**

   ```bash
   docker ps
   ```

   If you get an error, start Docker Desktop.

2. **Verify PostgreSQL container is running:**

   ```bash
   docker-compose ps
   ```

   Should show `postgres-db` as `Up (healthy)`

3. **If container is not running, start it:**

   ```bash
   docker-compose up -d postgres
   ```

4. **Wait for health check:**

   ```bash
   # Watch logs until you see "database system is ready"
   docker-compose logs -f postgres
   ```

   Or check status:

   ```bash
   docker-compose ps
   ```

   Wait until you see `(healthy)` status.

5. **If container keeps restarting, check logs:**

   ```bash
   docker-compose logs postgres
   ```

6. **Restart the application container:**
   ```bash
   docker-compose restart app
   ```

### Razorpay Authentication Errors

**Error:** `Razorpay authentication failed` or `Invalid Razorpay credentials`

**Solution:**

1. **Verify `.env` file exists and has correct format:**

   ```bash
   cat .env
   ```

   Should show:

   ```
   RAZORPAY_KEY=rzp_test_xxxxx
   RAZORPAY_SECRET=your_secret_key
   ```

2. **Check key format:**

   - Test keys should start with `rzp_test_`
   - Live keys should start with `rzp_live_`
   - No spaces or quotes around values

3. **Verify keys are correct:**

   - Log in to [Razorpay Dashboard](https://dashboard.razorpay.com/)
   - Go to Settings → API Keys
   - Regenerate keys if needed

4. **Restart application after changing `.env`:**
   ```bash
   docker-compose restart app
   ```

### Application Won't Start

**Error:** Application container keeps restarting

**Solution:**

1. **Check application logs:**

   ```bash
   docker-compose logs app
   ```

2. **Common issues:**

   - Database not ready: Wait for PostgreSQL to be healthy
   - Missing Razorpay keys: Check `.env` file
   - Port conflict: Change port in `application.yml` or `docker-compose.yml`
   - Build errors: Check Docker build logs

3. **Rebuild from scratch:**
   ```bash
   docker-compose down
   docker-compose build --no-cache
   docker-compose up
   ```

### Flyway Migration Errors

**Error:** `Migration checksum mismatch` or `Migration failed`

**Solution:**

1. **Check migration files are correct:**

   ```bash
   ls -la src/main/resources/db/migration/
   ```

2. **If you need to reset migrations:**

   ```bash
   # WARNING: This deletes all data!
   docker-compose down -v
   docker-compose up --build
   ```

3. **Check Flyway logs:**
   ```bash
   docker-compose logs app | grep -i flyway
   ```

For pgAdmin connection issues, see [PGADMIN_SETUP.md](./documentation/PGADMIN_SETUP.md).

---
