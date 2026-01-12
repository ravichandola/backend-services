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

## 🖥️ System Requirements

You only need the following installed:

- ✅ **Docker Desktop**
- ✅ **Docker Compose** (included with Docker Desktop)
- ✅ **Git**

> **Note:** ❌ Java, MySQL, and Maven are **not** required on your host machine.

---

## 📁 Project Structure

```text
.
├── Dockerfile              # Instructions to build the Spring Boot image
├── docker-compose.yml      # Orchestrates the App and MySQL containers
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

## ▶️ How to Run the Project

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/<your-username>/payment-gateway-backend.git
cd payment-gateway-backend
```

### 2️⃣ Start the Application

```bash
docker compose up --build
```

⏳ The first run may take a few minutes as it downloads the base images and dependencies.

### 3️⃣ Verify Startup

Watch the logs until you see:

```
Started PaymentApplication in X seconds
```

### 4️⃣ Application URL

The API will be available at: **http://localhost:8080**

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

### Connect Using GUI (pgAdmin / DBeaver / DataGrip)

| Field | Value     |
| :---- | :-------- |
| Host  | localhost |
| Port  | 5433      |
| User  | appuser   |
| Pass  | apppass   |
| DB    | appdb     |

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
  port: 8081 # Change to any available port
```

Then update your API URLs accordingly (e.g., `http://localhost:8081`).

---
