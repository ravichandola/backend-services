# 💳 Payment Gateway Backend (Dockerized)

A robust Spring Boot backend application for handling payment order creation using **Razorpay**, **MySQL 8.4**, and **Hibernate (JPA)**.

This project is **fully Dockerized**, meaning no Java, MySQL, or Maven installation is required on your local machine.

---

## 🚀 Tech Stack

| Component          | Technology                           |
| :----------------- | :----------------------------------- |
| **Java**           | 25 (Eclipse Temurin inside Docker)   |
| **Spring Boot**    | 4.x                                  |
| **Hibernate ORM**  | 7.x                                  |
| **Database**       | MySQL 8.4                            |
| **Build Tool**     | Maven Wrapper                        |
| **Containerization** | Docker & Docker Compose             |
| **Payment Gateway** | Razorpay (Test Mode)                 |

---

## 🖥️ System Requirements

You only need the following installed:
* ✅ **Docker Desktop**
* ✅ **Docker Compose** (included with Docker Desktop)
* ✅ **Git**

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
  application:
    name: payment

  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

razorpay:
  key: ${RAZORPAY_KEY}
  secret: ${RAZORPAY_SECRET}
```

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

---

## 🗄️ Database Details

MySQL runs inside a Docker container. Hibernate is configured to automatically create and update tables.

### Connect Using GUI (MySQL Workbench / DBeaver)

| Field | Value      |
| :---- | :--------- |
| Host  | localhost  |
| Port  | 3307       |
| User  | root       |
| Pass  | root1234   |
| DB    | payment_db |

---

## 🔐 Razorpay Configuration

The app uses Razorpay Test Keys.

Keys are injected via Docker environment variables.

> **Security Tip:** Never commit your live `RAZORPAY_SECRET` to a public repository.

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

