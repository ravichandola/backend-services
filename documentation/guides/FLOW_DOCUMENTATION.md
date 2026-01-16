# 🔄 Payment Gateway - Complete Flow Documentation

This document explains the complete flow from client request to backend processing, including Spring Boot startup, MVC interactions, and database operations.

---

## 📋 Table of Contents

1. [Application Startup Flow](#1-application-startup-flow)
2. [Request Flow Overview](#2-request-flow-overview)
3. [Create Order Flow](#3-create-order-flow)
4. [Verify Payment Flow](#4-verify-payment-flow)
5. [MVC Layer Interactions](#5-mvc-layer-interactions)
6. [Database Operations](#6-database-operations)
7. [Error Handling Flow](#7-error-handling-flow)

---

## 1. Application Startup Flow

### 1.0 Docker Container Startup Sequence

Before the Spring Boot application starts, Docker Compose orchestrates the container startup:

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Docker Compose Initialization                            │
│    - docker-compose up --build command executed             │
│    - Reads docker-compose.yml configuration                 │
│    - Creates Docker network for inter-container communication│
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. PostgreSQL Container Startup                             │
│    - Pulls postgres:16-alpine image (if not cached)        │
│    - Creates postgres-db container                          │
│    - Initializes database: appdb                           │
│    - Creates user: appuser                                  │
│    - Starts PostgreSQL server on port 5432                 │
│    - Health check: pg_isready -U appuser -d appdb         │
│    - Waits until healthy (max 10 retries)                  │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. pgAdmin Container Startup                                │
│    - Pulls dpage/pgadmin4:latest image                     │
│    - Creates pgadmin container                              │
│    - Waits for postgres service to be healthy               │
│    - Starts pgAdmin web server on port 80 (mapped to 5050) │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. Spring Boot Application Container Build                 │
│    - Builds Docker image using Dockerfile                   │
│    - Stage 1: Build stage (eclipse-temurin:25-jdk)         │
│      • Copies pom.xml and Maven wrapper                    │
│      • Downloads dependencies (mvnw dependency:go-offline)│
│      • Copies source code                                   │
│      • Compiles and packages (mvnw clean package)           │
│      • Creates JAR file: target/*.jar                       │
│    - Stage 2: Runtime stage (eclipse-temurin:25-jre)        │
│      • Copies JAR file to /app/app.jar                       │
│      • Sets entrypoint: java -jar app.jar                   │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. Spring Boot Application Container Startup                │
│    - Waits for postgres service to be healthy               │
│    - Starts container with environment variables            │
│    - Executes: java -jar app.jar                            │
│    - JVM starts and loads PaymentApplication                │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. Spring Boot Application Initialization (see below)       │
└─────────────────────────────────────────────────────────────┘
```

### 1.1 Initial Boot Sequence

When the Spring Boot application starts inside the Docker container, the following sequence occurs:

```
┌─────────────────────────────────────────────────────────────┐
│ 1. JVM Starts                                                │
│    - PaymentApplication.main() is invoked                   │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. SpringApplication Initialization                          │
│    - SpringApplication instance created                      │
│    - DotenvApplicationContextInitializer added               │
│    - application.run(args) called                            │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. ApplicationContext Initialization                         │
│    - DotenvApplicationContextInitializer.initialize()       │
│      • Loads .env file from project root                    │
│      • Converts to MapPropertySource                        │
│      • Adds to Environment with highest priority             │
│    - Spring Boot auto-configuration starts                   │
│    - Component scanning begins                               │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. Bean Creation & Dependency Injection                      │
│    - @Configuration classes processed                        │
│    - RazorpayConfig class loaded                            │
│      • @Value annotations inject environment variables       │
│      • @PostConstruct validateCredentials() executes        │
│        - Validates RAZORPAY_KEY and RAZORPAY_SECRET          │
│        - Checks key format (rzp_test_ or rzp_live_)         │
│        - Logs masked credentials                             │
│      • @Bean razorpayClient() method creates RazorpayClient  │
│    - Repository interfaces scanned                          │
│    - Service classes instantiated                           │
│    - Controller classes instantiated                        │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. Database Connection & Migration                          │
│    - HikariCP connection pool initialized                   │
│      • Connection timeout: 30 seconds                      │
│      • Maximum pool size: 10                                │
│      • Minimum idle: 5                                     │
│      • Connection test query: SELECT 1                     │
│    - Connects to PostgreSQL                                 │
│      • From Docker: postgres:5432 (service name)           │
│      • From local: localhost:5433 (mapped port)            │
│    - Flyway migration tool executes                         │
│      • Checks migration history in flyway_schema_history   │
│      • Executes V1__Create_payment_order_table.sql         │
│      • Executes V2__Create_payment_transaction_table.sql   │
│      • Validates migrations on startup                      │
│    - JPA/Hibernate session factory created                  │
│      • Entity classes scanned                               │
│      • Table mappings validated                              │
│      • DDL mode: validate (no auto-creation)               │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. Web Server Startup                                        │
│    - Embedded Tomcat server starts                          │
│    - DispatcherServlet initialized                          │
│    - Request mappings registered                             │
│      • POST /api/payments/create-order                      │
│      • POST /api/payments/verify                            │
│    - Server listens on port 8080 (container)                │
│      • Mapped to host port 8080 via docker-compose         │
│      • Accessible at http://localhost:8080                  │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 7. Application Ready                                         │
│    - "Started PaymentApplication in X seconds" logged       │
│    - Application ready to accept requests                    │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Environment Variable Loading

**Priority Order (highest to lowest):**

1. **Environment variables from Docker Compose** (`docker-compose.yml`)

   - `SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/appdb`
   - `SPRING_DATASOURCE_USERNAME: appuser`
   - `SPRING_DATASOURCE_PASSWORD: apppass`
   - `RAZORPAY_KEY: ${RAZORPAY_KEY}` (from host environment or .env)
   - `RAZORPAY_SECRET: ${RAZORPAY_SECRET}` (from host environment or .env)

2. **`.env` file** (loaded by `DotenvApplicationContextInitializer`)

   - Located in project root
   - Loaded before Spring context initialization
   - Converted to `MapPropertySource` with highest priority

3. **`application.yml` default values**
   - Used as fallback if environment variables not set
   - Example: `jdbc:postgresql://localhost:5433/appdb`

**Important Notes:**

- When running in Docker, the app connects to PostgreSQL using service name `postgres:5432`
- When running locally (IDE), the app connects using `localhost:5433`
- Razorpay credentials must be provided via environment variables or `.env` file

### 1.3 Key Components Initialized

| Component                             | Purpose                         | Initialization Order        |
| :------------------------------------ | :------------------------------ | :-------------------------- |
| `DotenvApplicationContextInitializer` | Loads `.env` file               | 1st (before Spring context) |
| `RazorpayConfig`                      | Configures Razorpay client      | Early (Configuration phase) |
| `RazorpayClient`                      | Razorpay SDK client bean        | After credential validation |
| `HikariCP Connection Pool`            | Database connection pool        | After datasource config     |
| `Flyway Migration Tool`               | Database schema migration       | After datasource ready      |
| `PaymentOrderRepository`              | JPA repository for orders       | After JPA initialization    |
| `PaymentTransactionRepository`        | JPA repository for transactions | After JPA initialization    |
| `PaymentServiceImpl`                  | Business logic service          | After repositories          |
| `PaymentController`                   | REST API endpoints              | After services              |

---

## 2. Request Flow Overview

### 2.1 High-Level Request Flow

```
┌─────────────┐
│   Client    │
│  (Browser/  │
│  Postman)   │
└──────┬──────┘
       │ HTTP Request
       │ POST /api/payments/create-order
       │ { "amount": 500 }
       ↓
┌──────────────────────────────────────────────────────────────┐
│                    Spring MVC Layer                            │
│  ┌────────────────────────────────────────────────────────┐   │
│  │ 1. DispatcherServlet                                   │   │
│  │    - Receives HTTP request                            │   │
│  │    - Determines handler based on @RequestMapping      │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                     │
│  ┌────────────────────────────────────────────────────────┐   │
│  │ 2. HandlerMapping                                      │   │
│  │    - Maps URL to PaymentController.createOrder()      │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                     │
│  ┌────────────────────────────────────────────────────────┐   │
│  │ 3. HandlerAdapter                                      │   │
│  │    - Invokes controller method                        │   │
│  │    - Handles @RequestBody deserialization             │   │
│  │    - Validates @Valid annotations                     │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                     │
│  ┌────────────────────────────────────────────────────────┐   │
│  │ 4. PaymentController                                   │   │
│  │    - Receives CreateOrderRequest                       │   │
│  │    - Calls paymentService.createOrder()               │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                     │
│  ┌────────────────────────────────────────────────────────┐   │
│  │ 5. PaymentServiceImpl                                  │   │
│  │    - Business logic execution                         │   │
│  │    - Razorpay API call                                │   │
│  │    - Database operations                              │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                     │
│  ┌────────────────────────────────────────────────────────┐   │
│  │ 6. PaymentOrderRepository                              │   │
│  │    - JPA save operation                               │   │
│  │    - Hibernate executes SQL                            │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                     │
│  ┌────────────────────────────────────────────────────────┐   │
│  │ 7. PostgreSQL Database                                 │   │
│  │    - INSERT INTO payment_order                         │   │
│  └────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│                    Response Flow                               │
│  ┌────────────────────────────────────────────────────────┐   │
│  │ 8. CreateOrderResponse built                           │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                     │
│  ┌────────────────────────────────────────────────────────┐   │
│  │ 9. ResponseEntity.ok() wraps response                  │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                     │
│  ┌────────────────────────────────────────────────────────┐   │
│  │ 10. HttpMessageConverter serializes to JSON            │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                     │
│  ┌────────────────────────────────────────────────────────┐   │
│  │ 11. DispatcherServlet sends HTTP response              │   │
│  └────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────┐
│   Client    │
│ Receives    │
│ Response    │
└─────────────┘
```

---

## 3. Create Order Flow

### 3.1 Detailed Step-by-Step Flow

#### Step 1: Client Sends Request

```http
POST http://localhost:8080/api/payments/create-order
Content-Type: application/json

{
  "amount": 500
}
```

#### Step 2: DispatcherServlet Receives Request

- **Component**: `DispatcherServlet` (Spring MVC core)
- **Action**:
  - Parses HTTP request
  - Extracts URL path: `/api/payments/create-order`
  - Determines HTTP method: `POST`
  - Reads request body

#### Step 3: HandlerMapping Resolution

- **Component**: `RequestMappingHandlerMapping`
- **Action**:
  - Scans `@RequestMapping("/api/payments")` on `PaymentController`
  - Matches `/create-order` with `@PostMapping("/create-order")`
  - Identifies handler method: `PaymentController.createOrder()`

#### Step 4: Request Deserialization & Validation

- **Component**: `RequestResponseBodyMethodProcessor`
- **Action**:
  - Reads JSON request body: `{"amount": 500}`
  - Deserializes to `CreateOrderRequest` object
  - Creates instance: `CreateOrderRequest{amount=500}`
  - **Validation** (via `@Valid`):
    - Checks `@NotNull` on `amount` field ✓
    - Checks `@Min(1)` on `amount` field ✓
  - If validation fails → Returns HTTP 400 Bad Request

#### Step 5: Controller Method Invocation

**File**: `PaymentController.java`

```java
@PostMapping("/create-order")
public ResponseEntity<CreateOrderResponse> createOrder(
        @Valid @RequestBody CreateOrderRequest request) {

    // request.getAmount() = 500
    return ResponseEntity.ok(
            paymentService.createOrder(request.getAmount())
    );
}
```

- **Dependency Injection**: `PaymentService` is injected via constructor (`@RequiredArgsConstructor`)
- **Action**:
  - Extracts `amount` from request: `500`
  - Calls `paymentService.createOrder(500)`

#### Step 6: Service Layer Processing

**File**: `PaymentServiceImpl.java`

```java
@Override
public CreateOrderResponse createOrder(Long amount) {
    // Step 6.1: Input Validation
    if (amount == null || amount <= 0) {
        throw new PaymentException("Invalid amount...");
    }

    // Step 6.2: Prepare Razorpay Order Options
    JSONObject options = new JSONObject();
    options.put("amount", amount * 100);  // 500 * 100 = 50000 paise
    options.put("currency", "INR");
    options.put("receipt", "rcpt_" + System.currentTimeMillis());

    // Step 6.3: Call Razorpay API
    Order order = razorpayClient.orders.create(options);
    // Returns: {id: "order_xxxxx", amount: 50000, currency: "INR", ...}

    // Step 6.4: Save to Database
    PaymentOrder paymentOrder = PaymentOrder.builder()
            .razorpayOrderId(order.get("id"))
            .amount(amount)
            .currency("INR")
            .status("CREATED")
            .build();

    orderRepo.save(paymentOrder);  // JPA save operation

    // Step 6.5: Build Response
    return CreateOrderResponse.builder()
            .orderId(order.get("id"))
            .amount(500)
            .currency("INR")
            .status("created")
            .receipt(order.get("receipt"))
            .build();
}
```

**Detailed Sub-Steps**:

##### 6.1 Input Validation

- Checks if `amount` is null or <= 0
- If invalid → throws `PaymentException` (HTTP 400)

##### 6.2 Razorpay Order Preparation

- Converts amount from rupees to paise (500 → 50000)
- Creates receipt ID with timestamp
- Prepares JSON object for Razorpay API

##### 6.3 Razorpay API Call

- **Component**: `RazorpayClient` (injected via constructor)
- **Action**:
  - Makes HTTP POST to Razorpay API
  - URL: `https://api.razorpay.com/v1/orders`
  - Headers: Authorization with API key/secret
  - Request body: `{amount: 50000, currency: "INR", receipt: "rcpt_..."}`
  - Response: `{id: "order_abc123", amount: 50000, status: "created", ...}`

##### 6.4 Database Persistence

- **Component**: `PaymentOrderRepository` (JPA Repository)
- **Action**:
  - `orderRepo.save(paymentOrder)` triggers Hibernate
  - Hibernate generates SQL:
    ```sql
    INSERT INTO payment_order
    (razorpay_order_id, amount, currency, status, created_at)
    VALUES ('order_abc123', 500, 'INR', 'CREATED', CURRENT_TIMESTAMP)
    ```
  - PostgreSQL executes INSERT
  - Returns entity with generated `id`

##### 6.5 Response Building

- Maps Razorpay order data to `CreateOrderResponse` DTO
- Converts amount back from paise to rupees (50000 → 500)

#### Step 7: Response Serialization

- **Component**: `MappingJackson2HttpMessageConverter`
- **Action**:
  - Serializes `CreateOrderResponse` to JSON
  - Output:
    ```json
    {
      "orderId": "order_abc123",
      "amount": 500,
      "currency": "INR",
      "status": "created",
      "receipt": "rcpt_1234567890"
    }
    ```

#### Step 8: HTTP Response Sent

- **Component**: `DispatcherServlet`
- **Action**:
  - Sets HTTP status: `200 OK`
  - Sets Content-Type: `application/json`
  - Writes JSON to response body
  - Sends response to client

#### Step 9: Client Receives Response

Client receives:

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "orderId": "order_abc123",
  "amount": 500,
  "currency": "INR",
  "status": "created",
  "receipt": "rcpt_1234567890"
}
```

---

## 4. Verify Payment Flow

### 4.1 Detailed Step-by-Step Flow

#### Step 1: Client Sends Verification Request

After payment is completed on Razorpay checkout, client sends:

```http
POST http://localhost:8080/api/payments/verify
Content-Type: application/json

{
  "razorpayOrderId": "order_abc123",
  "razorpayPaymentId": "pay_xyz789",
  "razorpaySignature": "signature_hash_12345"
}
```

#### Step 2-4: Request Processing (Same as Create Order)

- DispatcherServlet receives request
- HandlerMapping resolves to `PaymentController.verifyPayment()`
- Request deserialized to `VerifyPaymentRequest`
- Validation checks `@NotBlank` on all fields

#### Step 5: Controller Method Invocation

**File**: `PaymentController.java`

```java
@PostMapping("/verify")
public ResponseEntity<?> verifyPayment(
        @Valid @RequestBody VerifyPaymentRequest request) {

    paymentService.verifyPayment(request);
    return ResponseEntity.ok("Payment verified successfully");
}
```

#### Step 6: Service Layer Verification

**File**: `PaymentServiceImpl.java`

```java
@Override
public void verifyPayment(VerifyPaymentRequest request) {
    // Step 6.1: Input Validation
    if (request == null || request.getRazorpayOrderId() == null || ...) {
        throw new PaymentException("Invalid payment verification request...");
    }

    // Step 6.2: Build Signature Payload
    String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
    // payload = "order_abc123|pay_xyz789"

    // Step 6.3: Generate Expected Signature
    String generatedSignature = Utils.getHash(payload, razorpaySecret);
    // Uses HMAC SHA256 with Razorpay secret key

    // Step 6.4: Verify Signature
    if (!generatedSignature.equals(request.getRazorpaySignature())) {
        throw new PaymentException("Invalid payment signature...");
    }

    // Step 6.5: Save Transaction
    PaymentTransaction transaction = PaymentTransaction.builder()
            .razorpayOrderId(request.getRazorpayOrderId())
            .razorpayPaymentId(request.getRazorpayPaymentId())
            .razorpaySignature(request.getRazorpaySignature())
            .status("SUCCESS")
            .build();

    txRepo.save(transaction);  // INSERT INTO payment_transaction

    // Step 6.6: Update Order Status
    PaymentOrder order = orderRepo
            .findByRazorpayOrderId(request.getRazorpayOrderId())
            .orElseThrow(() -> new PaymentException("Order not found..."));

    order.setStatus("PAID");
    orderRepo.save(order);  // UPDATE payment_order SET status = 'PAID'
}
```

**Detailed Sub-Steps**:

##### 6.1 Input Validation

- Validates all required fields are present
- If missing → throws `PaymentException`

##### 6.2 Signature Payload Construction

- Concatenates: `orderId|paymentId`
- Format: `"order_abc123|pay_xyz789"`

##### 6.3 Signature Generation

- **Component**: `com.razorpay.Utils.getHash()`
- **Algorithm**: HMAC SHA256
- **Input**: `payload` + `razorpaySecret`
- **Output**: Generated signature hash

##### 6.4 Signature Verification

- Compares generated signature with received signature
- If mismatch → throws `PaymentException` (possible tampering)

##### 6.5 Transaction Persistence

- **Component**: `PaymentTransactionRepository`
- **Action**:
  - `txRepo.save(transaction)` triggers Hibernate
  - SQL generated:
    ```sql
    INSERT INTO payment_transaction
    (razorpay_payment_id, razorpay_order_id, razorpay_signature, status, created_at)
    VALUES ('pay_xyz789', 'order_abc123', 'signature_hash_12345', 'SUCCESS', CURRENT_TIMESTAMP)
    ```

##### 6.6 Order Status Update

- **Component**: `PaymentOrderRepository`
- **Action**:
  - `findByRazorpayOrderId()` executes:
    ```sql
    SELECT * FROM payment_order
    WHERE razorpay_order_id = 'order_abc123'
    ```
  - If not found → throws `PaymentException`
  - Updates status: `CREATED` → `PAID`
  - `orderRepo.save(order)` executes:
    ```sql
    UPDATE payment_order
    SET status = 'PAID'
    WHERE id = 1
    ```

#### Step 7: Response Sent

- Returns: `"Payment verified successfully"`
- HTTP 200 OK

---

## 5. MVC Layer Interactions

### 5.1 MVC Architecture in This Application

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                           │
│                    (Browser / Postman / App)                  │
└────────────────────────────┬──────────────────────────────────┘
                             │ HTTP Request/Response
                             ↓
┌─────────────────────────────────────────────────────────────┐
│                    CONTROLLER LAYER                           │
│                  (PaymentController)                          │
│  ┌───────────────────────────────────────────────────────┐   │
│  │ Responsibilities:                                     │   │
│  │ • Receives HTTP requests                              │   │
│  │ • Validates request data (@Valid)                    │   │
│  │ • Delegates business logic to Service                │   │
│  │ • Builds HTTP responses                              │   │
│  │ • Handles HTTP status codes                          │   │
│  └───────────────────────────────────────────────────────┘   │
│                          ↓                                    │
│                    Calls Service                               │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                            │
│                  (PaymentServiceImpl)                         │
│  ┌───────────────────────────────────────────────────────┐   │
│  │ Responsibilities:                                     │   │
│  │ • Contains business logic                            │   │
│  │ • Orchestrates external API calls (Razorpay)        │   │
│  │ • Coordinates database operations                    │   │
│  │ • Handles exceptions and error translation           │   │
│  │ • Data transformation (DTO ↔ Entity)                 │   │
│  └───────────────────────────────────────────────────────┘   │
│                          ↓                                    │
│              Uses Repository & External APIs                  │
└─────────────────────────────────────────────────────────────┘
                             ↓
        ┌────────────────────┴────────────────────┐
        ↓                                         ↓
┌───────────────────────┐            ┌──────────────────────────┐
│   REPOSITORY LAYER     │            │   EXTERNAL API LAYER      │
│ (PaymentOrderRepo,     │            │   (RazorpayClient)       │
│  PaymentTransactionRepo)│            │                           │
│  ┌───────────────────┐ │            │  ┌────────────────────┐  │
│  │ Responsibilities: │ │            │  │ Responsibilities:  │  │
│  │ • Data access     │ │            │  │ • API integration  │  │
│  │ • CRUD operations │ │            │  │ • Order creation   │  │
│  │ • Query methods   │ │            │  │ • Signature utils  │  │
│  └───────────────────┘ │            │  └────────────────────┘  │
└───────────────────────┘            └──────────────────────────┘
        ↓                                         ↓
┌─────────────────────────────────────────────────────────────┐
│                      DATABASE LAYER                           │
│                    (PostgreSQL via JPA)                       │
│  ┌───────────────────────────────────────────────────────┐   │
│  │ • payment_order table                                 │   │
│  │ • payment_transaction table                           │   │
│  │ • Hibernate ORM manages entity ↔ table mapping       │   │
│  └───────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Component Responsibilities

#### Controller Layer (`PaymentController`)

**Role**: HTTP request/response handling

- **Receives**: HTTP requests with JSON body
- **Validates**: Request data using `@Valid` annotation
- **Delegates**: Business logic to Service layer
- **Returns**: HTTP responses with appropriate status codes

**Key Annotations**:

- `@RestController` - Marks as REST controller, auto-serializes responses
- `@RequestMapping("/api/payments")` - Base URL mapping
- `@PostMapping` - HTTP POST method mapping
- `@RequestBody` - Deserializes JSON to Java object
- `@Valid` - Triggers Bean Validation

#### Service Layer (`PaymentServiceImpl`)

**Role**: Business logic orchestration

- **Contains**: Core business logic
- **Coordinates**: Multiple operations (API calls, database)
- **Transforms**: Data between DTOs and Entities
- **Handles**: Business exceptions

**Key Annotations**:

- `@Service` - Marks as Spring service component
- `@RequiredArgsConstructor` - Generates constructor for dependency injection
- `@Slf4j` - Provides logging capabilities

**Dependencies Injected**:

- `RazorpayClient` - External API client
- `PaymentOrderRepository` - Data access for orders
- `PaymentTransactionRepository` - Data access for transactions
- `@Value("${razorpay.secret}")` - Configuration property

#### Repository Layer (`PaymentOrderRepository`, `PaymentTransactionRepository`)

**Role**: Data persistence

- **Extends**: `JpaRepository<Entity, ID>`
- **Provides**: CRUD operations automatically
- **Custom Methods**: `findByRazorpayOrderId()` - Custom query method

**How It Works**:

- Spring Data JPA automatically implements repository interfaces
- Method names like `findByRazorpayOrderId()` generate SQL queries
- Hibernate translates method calls to SQL

#### Model/Entity Layer (`PaymentOrder`, `PaymentTransaction`)

**Role**: Data representation

- **Maps**: Java objects to database tables
- **Annotated**: With JPA annotations (`@Entity`, `@Table`, `@Column`)
- **Managed**: By Hibernate ORM

**Key Annotations**:

- `@Entity` - Marks as JPA entity
- `@Table(name = "...")` - Maps to database table
- `@Id` - Primary key
- `@GeneratedValue` - Auto-increment ID
- `@CreationTimestamp` - Auto-set timestamp on creation

### 5.3 Data Flow Between Layers

#### Request Flow (Create Order Example)

```
1. Client → Controller
   JSON: {"amount": 500}
   ↓
   CreateOrderRequest{amount=500}

2. Controller → Service
   paymentService.createOrder(500)
   ↓
   Long amount = 500

3. Service → External API (Razorpay)
   razorpayClient.orders.create(options)
   ↓
   Order{id="order_abc123", amount=50000, ...}

4. Service → Repository
   orderRepo.save(paymentOrder)
   ↓
   PaymentOrder entity

5. Repository → Database (via Hibernate)
   Hibernate generates SQL
   ↓
   INSERT INTO payment_order ...

6. Service → Controller
   CreateOrderResponse{orderId="order_abc123", ...}
   ↓
   ResponseEntity.ok(response)

7. Controller → Client
   JSON: {"orderId": "order_abc123", ...}
```

---

## 6. Database Operations

### 6.1 JPA/Hibernate Interaction

#### Entity to Table Mapping

**PaymentOrder Entity**:

```java
@Entity
@Table(name = "payment_order")
public class PaymentOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String razorpayOrderId;
    // ...
}
```

**Maps to Table**:

```sql
CREATE TABLE payment_order (
    id BIGSERIAL PRIMARY KEY,
    razorpay_order_id VARCHAR(255) NOT NULL UNIQUE,
    -- ...
);
```

#### Repository Method Execution

**Repository Method**:

```java
Optional<PaymentOrder> findByRazorpayOrderId(String razorpayOrderId);
```

**Generated SQL** (by Spring Data JPA):

```sql
SELECT * FROM payment_order
WHERE razorpay_order_id = ?
```

**Execution Flow**:

1. Method called: `orderRepo.findByRazorpayOrderId("order_abc123")`
2. Spring Data JPA generates SQL query
3. Hibernate prepares PreparedStatement
4. PostgreSQL executes query
5. Hibernate maps ResultSet to `PaymentOrder` entity
6. Returns `Optional<PaymentOrder>`

#### Save Operation Flow

**Repository Method**:

```java
orderRepo.save(paymentOrder);
```

**Execution Flow**:

1. Hibernate checks if entity has ID
2. If ID is null → INSERT operation
3. Hibernate generates SQL:
   ```sql
   INSERT INTO payment_order
   (razorpay_order_id, amount, currency, status, created_at)
   VALUES (?, ?, ?, ?, ?)
   ```
4. Binds parameters from entity fields
5. PostgreSQL executes INSERT
6. Returns generated ID
7. Hibernate updates entity with generated ID

### 6.2 Transaction Management

Spring Boot uses **declarative transaction management** by default:

- `@Transactional` annotation (implicit on repository methods)
- Each repository method runs in a transaction
- If exception occurs → transaction rolls back
- If successful → transaction commits

**Example**:

```java
// In PaymentServiceImpl
orderRepo.save(paymentOrder);  // Transaction 1
txRepo.save(transaction);     // Transaction 2
orderRepo.save(order);         // Transaction 3 (UPDATE)
```

Each `save()` operation is in its own transaction by default.

---

## 7. Error Handling Flow

### 7.1 Exception Hierarchy

```
Exception
  └── RuntimeException
       └── PaymentException (@ResponseStatus(HttpStatus.BAD_REQUEST))
```

### 7.2 Error Flow Example

**Scenario**: Invalid amount sent in request

```
1. Client sends: {"amount": -100}

2. Controller receives request
   ↓
3. @Valid validation passes (amount is not null, but negative)
   ↓
4. Service method called: createOrder(-100)
   ↓
5. Service validation: if (amount <= 0) → throws PaymentException
   ↓
6. Exception propagates up:
   PaymentException → Service → Controller → DispatcherServlet
   ↓
7. @ResponseStatus(HttpStatus.BAD_REQUEST) annotation
   causes Spring to return HTTP 400
   ↓
8. Response sent to client:
   HTTP/1.1 400 Bad Request
   {
     "timestamp": "2024-01-01T12:00:00",
     "status": 400,
     "error": "Bad Request",
     "message": "Invalid amount. Amount must be greater than 0",
     "path": "/api/payments/create-order"
   }
```

### 7.3 Exception Types Handled

| Exception Type                    | Source            | HTTP Status     | Example                                |
| :-------------------------------- | :---------------- | :-------------- | :------------------------------------- |
| `PaymentException`                | Service layer     | 400 Bad Request | Invalid amount, missing credentials    |
| `RazorpayException`               | Razorpay API      | 400 Bad Request | Authentication failed, invalid request |
| `ValidationException`             | Bean Validation   | 400 Bad Request | Missing required fields                |
| `MethodArgumentNotValidException` | Spring Validation | 400 Bad Request | Invalid request body format            |

### 7.4 Error Handling in Service Layer

**File**: `PaymentServiceImpl.java`

```java
try {
    // Razorpay API call
    Order order = razorpayClient.orders.create(options);
} catch (RazorpayException e) {
    // Translate Razorpay exception to PaymentException
    if (e.getMessage().contains("Authentication failed")) {
        throw new PaymentException("Razorpay authentication failed...");
    } else {
        throw new PaymentException("Failed to create Razorpay order: " + e.getMessage());
    }
} catch (Exception e) {
    // Catch-all for unexpected errors
    log.error("Unexpected error while creating order", e);
    throw new PaymentException("Order creation failed: " + e.getMessage());
}
```

---

## 8. Complete Request-Response Cycle Summary

### 8.1 Create Order - Full Cycle

```
┌─────────────────────────────────────────────────────────────┐
│ TIME  │ COMPONENT              │ ACTION                      │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+0ms │ Client                 │ Sends POST /api/payments/   │
│       │                        │ create-order                │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+1ms │ DispatcherServlet      │ Receives HTTP request       │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+2ms │ HandlerMapping         │ Maps to PaymentController   │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+3ms │ HttpMessageConverter   │ Deserializes JSON to DTO    │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+4ms │ Bean Validation        │ Validates @Valid            │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+5ms │ PaymentController      │ Calls paymentService        │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+6ms │ PaymentServiceImpl     │ Validates amount           │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+7ms │ PaymentServiceImpl     │ Prepares Razorpay options  │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+8ms │ RazorpayClient         │ HTTP POST to Razorpay API  │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+200ms│ Razorpay API          │ Returns order response      │
│       │                        │ (network latency)           │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+201ms│ PaymentServiceImpl    │ Builds PaymentOrder entity │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+202ms│ PaymentOrderRepository│ Hibernate generates SQL     │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+203ms│ PostgreSQL            │ Executes INSERT             │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+210ms│ PaymentServiceImpl    │ Builds CreateOrderResponse  │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+211ms│ PaymentController     │ Wraps in ResponseEntity     │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+212ms│ HttpMessageConverter  │ Serializes to JSON         │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+213ms│ DispatcherServlet     │ Sends HTTP response         │
├───────┼───────────────────────┼─────────────────────────────┤
│ T+214ms│ Client                 │ Receives response          │
└───────┴───────────────────────┴─────────────────────────────┘
```

**Total Time**: ~214ms (varies based on network latency to Razorpay)

---

## 9. Key Takeaways

### 9.1 MVC Pattern Implementation

- **Model**: Entities (`PaymentOrder`, `PaymentTransaction`) + Repositories
- **View**: JSON responses (REST API, no HTML views)
- **Controller**: `PaymentController` handles HTTP requests/responses

### 9.2 Separation of Concerns

- **Controller**: HTTP handling only
- **Service**: Business logic and orchestration
- **Repository**: Data access only
- **Entity**: Data representation

### 9.3 Dependency Injection

- All dependencies injected via constructor
- Spring manages object lifecycle
- No manual object creation in business code

### 9.4 Transaction Management

- Automatic transaction management via Spring
- Each repository operation in its own transaction
- Rollback on exceptions

### 9.5 Error Handling

- Custom exceptions for business errors
- HTTP status codes mapped via `@ResponseStatus`
- Exception translation from external APIs

---

## 10. Testing the Flow

### 10.1 Prerequisites for Testing

Before testing, ensure:

1. **All Docker containers are running:**

   ```bash
   docker-compose ps
   ```

   All services should show `Up` status.

2. **Application is ready:**
   Check logs for: `Started PaymentApplication in X seconds`

   ```bash
   docker-compose logs app | grep "Started PaymentApplication"
   ```

3. **Razorpay credentials are configured:**
   - Check `.env` file has `RAZORPAY_KEY` and `RAZORPAY_SECRET`
   - Or verify environment variables are set

### 10.2 Using Postman

1. **Create Order**:

   ```
   POST http://localhost:8080/api/payments/create-order
   Content-Type: application/json

   Body (raw JSON):
   {
     "amount": 500
   }
   ```

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

2. **Verify Payment** (after payment on Razorpay):

   ```
   POST http://localhost:8080/api/payments/verify
   Content-Type: application/json

   Body (raw JSON):
   {
     "razorpayOrderId": "order_xxx",
     "razorpayPaymentId": "pay_xxx",
     "razorpaySignature": "signature_xxx"
   }
   ```

   **Expected Response:**

   ```
   "Payment verified successfully"
   ```

### 10.3 Using curl

**Create Order:**

```bash
curl -X POST http://localhost:8080/api/payments/create-order \
  -H "Content-Type: application/json" \
  -d '{"amount": 500}'
```

**Verify Payment:**

```bash
curl -X POST http://localhost:8080/api/payments/verify \
  -H "Content-Type: application/json" \
  -d '{
    "razorpayOrderId": "order_xxx",
    "razorpayPaymentId": "pay_xxx",
    "razorpaySignature": "signature_xxx"
  }'
```

### 10.2 Monitoring the Flow

Enable SQL logging in `application.yml`:

```yaml
spring:
  jpa:
    show-sql: true
```

This will show all SQL queries executed by Hibernate in the console.

---

**End of Flow Documentation**
