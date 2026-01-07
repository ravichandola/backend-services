## 🖥️ System Requirements

Before setting up the project, ensure your system meets the following requirements:

### Software Requirements

| Component | Required Version         |
|--------|--------------------------|
| Java | JDK 25 or higher         |
| Maven | 3.8+ (or Maven Wrapper)  |
| MySQL | 8.0+                     |
| Git | Latest                   |
| Postman | Latest (for API testing) |

### External Services

- **Razorpay Account**
    - Test Mode enabled
    - API Key ID and Secret Key available

### Operating System

- Windows 10 / 11
- macOS
- Linux (Ubuntu preferred)

---

## ⚙️ Project Setup & Installation

Follow the steps below to set up and run the project locally.

---

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/<your-username>/payment-gateway-backend.git
cd payment-gateway-backend
```
### 2️⃣ Configure Database (MySQL)

Login to MySQL and create the database:

```mysql
CREATE DATABASE payment_db;
```
### ⚠️ Note:
#### Do NOT create tables manually.
Hibernate will auto-generate tables at runtime.

### 3️⃣ Configure Application Properties
```bash
src/main/resources/application.yml
```
```yaml
spring:
application:
name: payment-gateway

datasource:
url: jdbc:mysql://localhost:3306/payment_db
username: root
password: root

jpa:
hibernate:
ddl-auto: update
show-sql: true

razorpay:
key: rzp_test_xxxxx
secret: xxxxx
```
### 🔐 Important Notes
**Use Razorpay TEST keys only**

**Do NOT commit real or live keys to GitHub**

### 4️⃣ Build the Project

```bash
mvn clean install
```
#### This will:

**Download dependencies**

**Compile the project**

**Run basic checks**

### 5️⃣ Run the Application
You can run the application using either method:
```bash
mvn spring-boot:run
```
**OR : IDE (IntelliJ / Eclipse)**

**1. Open PaymentApplication.java**

**2. Click Run**

**Application will be running at:**

```bash
http://localhost:8080
```

## API Testing (Postman)
### Create Order API

**Endpoint**

```bash
POST /api/payments/create-order
```

**URL**

```bash
http://localhost:8080/api/payments/create-order
```

**HEADERS**

```bash
Content-Type: application/json
```

**Request Body**
```json lines
{
  "amount": 500
}
```

**Successful Response**
```json
{
  "orderId": "order_xxxxx",
  "amount": 500,
  "currency": "INR",
  "status": "created",
  "receipt": "rcpt_xxxxx"
}
```

### Notes for Developers

**1. JDBC URL is used only for DB connection**

**2. Hibernate (JPA) handles ORM internally**

**3. No raw JDBC code is written**

**4. DTOs protect API contracts**

**5. Service layer contains all business logic**