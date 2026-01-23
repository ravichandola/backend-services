# Enterprise Multi-Tenant SaaS Backend Architecture

> **Goal of this doc:** give a **fast, clear mental model** of the system.  
> For full details (all tables, flows, configs), see `03_ARCHITECTURE_DETAILED.md`.

## 1. Big Picture

This project is an **enterprise multi-tenant SaaS backend** with integrated payments:

- **API Gateway (`api-gateway/`)**
  - Validates Clerk JWTs
  - Adds `X-User-Id` and `X-Org-Id` headers
  - Routes traffic to backend and payment services

- **Backend Service (`backend-service/`)**
  - Handles webhooks from Clerk
  - Provides protected APIs (e.g. `/api/me`, org admin APIs)
  - Enforces **authorization** using DB (`users`, `organizations`, `roles`, `memberships`)

- **Payment Service (`payment-service/`)**
  - Integrates with Razorpay
  - Creates and verifies payment orders
  - Persists orders and transactions

- **PostgreSQL**
  - Stores users, organizations, roles, memberships
  - Stores webhook events (audit)
  - Stores payment orders and transactions

- **Clerk**
  - Handles all login/signup UI and flows
  - Issues JWTs for authenticated users
  - Sends webhooks on user/org changes

## 2. Core Principles

### 2.1 Authentication – Gateway Only

- Backend **never** parses or validates JWTs
- **Only the API Gateway**:
  - Talks to Clerk JWKS
  - Validates JWT signature, issuer, expiry
  - Extracts `sub` (user ID) and `org_id`
  - Adds trusted headers:
    - `X-User-Id` (Clerk user ID)
    - `X-Org-Id` (current org, if any)

Backend services **trust these headers completely**.

### 2.2 Authorization – Service Layer + Database

- Multi-tenant model:
  - `users` – one row per Clerk user
  - `organizations` – one row per tenant
  - `roles` – e.g. `ADMIN`, `USER`
  - `memberships` – user ↔ organization ↔ role
- Authorization rules live in `AuthorizationService`:
  - “Is this user a member of this org?”
  - “Is this user an ADMIN in this org?”

### 2.3 Webhook-Driven Identity Sync

- Backend does **not** depend on frontend calling “create user/org” APIs
- Clerk sends webhooks for:
  - `user.created`, `user.updated`
  - `organization.created`
  - `organizationMembership.created`, `organizationMembership.deleted`
- Webhook handlers:
  - Verify signatures using `CLERK_WEBHOOK_SECRET`
  - Upsert users, orgs, memberships
  - Store full payloads in `user_events` / `organization_events`

## 3. High-Level Architecture Diagram

```text
Client (Browser / Postman)
        │
        │ 1. Login with Clerk → Get JWT
        │
        ▼
   Clerk (Auth)
        │           ▲
        │ 2. JWT    │ 3. Webhooks (user/org events)
        ▼           │
 API Gateway (8080) │
  - Validates JWT   │
  - Adds X-User-Id  │
  - Adds X-Org-Id   │
        │
        ▼
  Backend Service (8081)          Payment Service (8082)
  - Webhooks                      - Create Razorpay order
  - Business logic                - Verify payment
  - Authorization                 - Persist orders/transactions
        │
        ▼
      PostgreSQL
  - users / organizations / memberships / roles
  - user_events / organization_events
  - payment_order / payment_transaction
```

## 4. Request & Webhook Flows (Summary)

### 4.1 Authenticated Request: `GET /api/me`

1. User logs in with Clerk → gets a JWT.
2. Client calls Gateway:

   ```http
   GET /api/me
   Authorization: Bearer <jwt>
   ```

3. Gateway:
   - Validates JWT using Clerk JWKS
   - Extracts user ID and org ID
   - Adds `X-User-Id`, `X-Org-Id`
   - Forwards to Backend

4. Backend:
   - Uses `X-User-Id` to look up the user
   - Uses `memberships` + `roles` for org/role checks (if needed)
   - Returns user info

### 4.2 Webhook: `user.created`

1. Clerk sends webhook to `/api/webhooks/clerk`.
2. Gateway forwards (no auth required for this path).
3. Backend:
   - Verifies signature using `CLERK_WEBHOOK_SECRET`
   - Parses `user.created` payload
   - Upserts row in `users`
   - Stores full payload in `user_events`

> **Full, step-by-step sequence diagrams live in** `03_ARCHITECTURE_DETAILED.md`.

## 5. Data Model – Mental Model

You don’t need every column; just know the **roles** of each table:

- **Identity & Tenancy**
  - `users` – who
  - `organizations` – which tenant
  - `roles` – what level (ADMIN, USER, etc.)
  - `memberships` – who is what in which tenant

- **Audit**
  - `user_events`, `organization_events` – every Clerk event, stored for debugging and audit

- **Payments**
  - `payment_order` – one row per Razorpay order
  - `payment_transaction` – one row per verified payment

Think of it as:

> **“Users in organizations with roles, plus audit logs and payments.”**

For full schemas and edge cases, see:

- `03_ARCHITECTURE_DETAILED.md`
- `05-reference/README_ENTERPRISE.md`

## 6. Services & Their Responsibilities

- **API Gateway (`api-gateway/`)**
  - Validates JWTs with Clerk JWKS
  - Adds `X-User-Id`, `X-Org-Id`
  - Routes to backend/payment services

- **Backend Service (`backend-service/`)**
  - Exposes `/api/me`, `/api/org/{orgId}/admin-data`, etc.
  - Handles `/api/webhooks/clerk` from Clerk
  - Uses `AuthorizationService` + DB for all authorization

- **Payment Service (`payment-service/`)**
  - `POST /api/payments/create-order`
  - `POST /api/payments/verify`
  - Persists to `payment_order` and `payment_transaction`

- **Database**
  - Shared Postgres instance (via Flyway migrations)
  - Multi-tenant auth model + payments + audit

## 7. Security Model (Summary)

- **Authentication**
  - Only Gateway validates JWTs.
  - Backend trusts headers from Gateway.

- **Authorization**
  - Service-layer checks using `memberships` and `roles`.
  - No “role” logic in controllers – all in services.

- **Webhooks**
  - HMAC signature verification (`CLERK_WEBHOOK_SECRET`).
  - Constant-time comparison for signatures.

- **Network**
  - Only Gateway is exposed publicly.
  - Backend and DB are on private Docker network.

## 8. How This Fits With the Rest of the Docs

Use this doc as the **map**, then jump into details as needed:

- **Run it locally:**  
  `02-setup/01_QUICK_START.md` → `02-setup/02_SETUP_GUIDE.md`

- **Test real flows:**  
  `03-guides/01_API_TESTING_DETAILED.md` → `03-guides/04_FLOW_DETAILED.md`

- **Understand what was built:**  
  `04-architecture/02_IMPLEMENTATION_SUMMARY.md`

- **See all mistakes & fixes:**  
  `04-architecture/fixes/01_MISTAKES_PART1.md` → `04-architecture/fixes/02_MISTAKES_AND_DESIGN.md`

- **Need every detail?**  
  `04-architecture/03_ARCHITECTURE_DETAILED.md`

# Enterprise Multi-Tenant SaaS Backend Architecture

## Overview

This is a complete enterprise-grade multi-tenant SaaS backend system built with:

- **Java Spring Boot** (latest stable)
- **PostgreSQL** with **Flyway** migrations
- **Spring Cloud Gateway** as API Gateway
- **Clerk** as Authentication Provider
- **JWT-based authentication** (issued by Clerk)
- **Docker & Docker Compose** for local setup

## Architecture Principles

### 1. Authentication Model

- **NO username/password** in backend
- **NO login or signup APIs** in backend
- Authentication handled **entirely by Clerk**
- Frontend receives Clerk JWT
- **API Gateway validates Clerk JWT** using JWKS
- Backend services **TRUST the gateway**
- JWT verification happens **ONLY at the gateway**

### 2. Authorization Model

- **Multi-tenant SaaS model**
- Support for **Organizations** (tenants)
- Support for **Users**
- Support for **Roles** (ADMIN, USER)
- **Membership table** links: user ↔ organization ↔ role
- Authorization enforced at **service layer**
- Example: only ADMIN of an organization can access admin APIs

### 3. Webhook-Driven Sync

- Backend does **NOT rely on signup/signin API calls**
- Uses **Clerk Webhooks** to sync identity data
- Secure webhook endpoint with **signature verification**
- Handles events:
  - `user.created`
  - `user.updated`
  - `organization.created`
  - `organizationMembership.created`
  - `organizationMembership.deleted`
- Persists webhook data into database
- Stores event history (audit-friendly)

## System Architecture

```
┌─────────────┐
│   Browser   │
│  (Frontend) │
└──────┬──────┘
       │
       │ 1. Authenticate with Clerk
       │    (Get JWT token)
       │
       ▼
┌─────────────────────────────────────┐
│         Clerk (Auth Provider)       │
│  - Issues JWT tokens                 │
│  - Sends webhooks to backend         │
└──────┬───────────────────┬───────────┘
       │                   │
       │ 2. JWT Token      │ 3. Webhooks
       │                   │
       ▼                   ▼
┌─────────────────────────────────────────────┐
│         API Gateway (Port 8080)              │
│  - Validates JWT via JWKS                    │
│  - Adds headers (X-User-Id, X-Org-Id)        │
│  - Routes to backend services                │
└──────┬──────────────────┬────────────────────┘
       │                  │
       │ 4. Authenticated │ 5. Public/Webhook
       │    Requests       │    Requests
       │                  │
       ▼                  ▼
┌──────────────────┐  ┌──────────────────┐
│ Backend Service  │  │ Payment Service  │
│ (Port 8081)      │  │ (Port 8082)      │
│                  │  │                  │
│ - Webhook        │  │ - Payment Order  │
│   handler        │  │   Creation      │
│ - Business       │  │ - Payment        │
│   logic          │  │   Verification   │
│ - Authorization  │  │ - Razorpay       │
│ - Data access    │  │   Integration    │
└────────┬─────────┘  └────────┬─────────┘
         │                     │
         │ 6. Database Queries │
         │                     │
         └──────────┬──────────┘
                    │
                    ▼
         ┌──────────────────┐
         │   PostgreSQL       │
         │   (Port 5432)     │
         │                    │
         │  - Users           │
         │  - Organizations  │
         │  - Roles           │
         │  - Memberships     │
         │  - Events (audit)  │
         │  - Payment Orders  │
         │  - Transactions    │
         └───────────────────┘
                    │
                    ▼
         ┌──────────────────┐
         │   pgAdmin        │
         │   (Port 5050)    │
         │   Database UI    │
         └──────────────────┘
```

## Request Lifecycle

### Authenticated API Request Flow

1. **Frontend authenticates with Clerk**

   - User logs in via Clerk UI
   - Clerk issues JWT token
   - Frontend stores JWT token

2. **Frontend makes API request**

   ```
   GET /api/me
   Authorization: Bearer <jwt-token>
   ```

3. **API Gateway receives request**

   - Extracts JWT from `Authorization` header
   - Fetches public keys from Clerk JWKS endpoint
   - Validates JWT signature and claims
   - Extracts `sub` (user ID) and `org_id` from JWT claims
   - Adds headers: `X-User-Id` and `X-Org-Id`
   - Forwards request to backend service

4. **Backend Service receives request**

   - Reads `X-User-Id` and `X-Org-Id` headers (trusts gateway)
   - Does NOT parse JWT (trusts gateway)
   - Performs authorization checks using database
   - Returns response

5. **Response flows back**
   - Backend → Gateway → Frontend

### Webhook Flow

1. **Clerk sends webhook**

   ```
   POST /api/webhooks/clerk
   svix-id: <event-id>
   svix-timestamp: <timestamp>
   svix-signature: v1,<signature>
   Body: { "type": "user.created", "data": {...} }
   ```

2. **API Gateway receives webhook**

   - Allows webhook endpoint without authentication
   - Forwards to backend service

3. **Backend Service processes webhook**
   - Verifies webhook signature using `CLERK_WEBHOOK_SECRET`
   - Parses event type and data
   - Calls appropriate `WebhookService` method
   - Updates database (users, organizations, memberships)
   - Stores event in audit table
   - Returns success response

## Database Schema

### Core Tables

#### `users`

- Stores user identity data synced from Clerk
- Key fields: `clerk_user_id`, `email`, `first_name`, `last_name`
- **No passwords stored**

#### `organizations`

- Represents tenants in multi-tenant model
- Key fields: `clerk_org_id`, `name`, `slug`

#### `roles`

- Predefined roles: `ADMIN`, `USER`
- Can be extended with more roles

#### `memberships`

- Links users to organizations with roles
- **Core of multi-tenant authorization**
- Unique constraint: (user_id, organization_id)
- Key fields: `user_id`, `organization_id`, `role_id`, `clerk_membership_id`

### Audit Tables

#### `user_events`

- Audit trail for user-related webhook events
- Stores full webhook payload as JSONB
- Indexed by `clerk_user_id`, `event_type`, `processed_at`

#### `organization_events`

- Audit trail for organization-related webhook events
- Stores full webhook payload as JSONB
- Indexed by `clerk_org_id`, `event_type`, `processed_at`

#### `auth_sessions` (Optional - Currently Not Implemented)

- **Status**: Table created but not actively used
- **Purpose**: Tracks active authentication sessions
- **Note**: No repository or service implementation exists yet
- **Future Use**: Can be used for analytics, security monitoring, and session management
- **Why Empty**: No code currently saves data to this table

#### `payment_order`

- Stores payment orders created via Razorpay
- Key fields: `razorpay_order_id`, `amount`, `currency`, `status`, `user_id`
- Status values: `CREATED`, `PAID`, `FAILED`

#### `payment_transaction`

- Stores payment transaction records
- Key fields: `razorpay_payment_id`, `razorpay_order_id`, `razorpay_signature`, `status`, `user_id`
- Links to `payment_order` via `razorpay_order_id`

## Project Structure

```
payment/
├── api-gateway/              # Spring Cloud Gateway
│   ├── src/
│   │   └── main/
│   │       ├── java/com/demo/gateway/
│   │       │   ├── GatewayApplication.java
│   │       │   └── config/
│   │       │       ├── JwtAuthenticationFilter.java  # JWT validation
│   │       │       └── DotenvApplicationContextInitializer.java
│   │       └── resources/
│   │           └── application.yml
│   ├── pom.xml
│   └── Dockerfile
│
├── backend-service/          # Spring Boot Backend
│   ├── src/
│   │   └── main/
│   │       ├── java/com/demo/backend/
│   │       │   ├── BackendApplication.java
│   │       │   ├── entity/          # JPA entities
│   │       │   ├── repository/      # JPA repositories
│   │       │   ├── service/         # Business logic
│   │       │   │   ├── WebhookService.java
│   │       │   │   └── AuthorizationService.java
│   │       │   ├── controller/      # REST controllers
│   │       │   │   ├── WebhookController.java
│   │       │   │   └── ApiController.java
│   │       │   └── config/
│   │       │       ├── SecurityConfig.java
│   │       │       └── DotenvApplicationContextInitializer.java
│   │       └── resources/
│   │           ├── application.yml
│   │           └── db/migration/     # Flyway migrations
│   │               ├── V1__Create_users_table.sql
│   │               ├── V2__Create_organizations_table.sql
│   │               ├── V3__Create_roles_table.sql
│   │               ├── V4__Create_memberships_table.sql
│   │               ├── V5__Create_user_events_table.sql
│   │               ├── V6__Create_organization_events_table.sql
│   │               └── V7__Create_auth_sessions_table.sql
│   ├── pom.xml
│   └── Dockerfile
│
├── payment-service/          # Payment Service (Razorpay Integration)
│   ├── src/
│   │   └── main/
│   │       ├── java/com/demo/payment/
│   │       │   ├── PaymentApplication.java
│   │       │   ├── controller/
│   │       │   │   └── PaymentController.java
│   │       │   ├── service/
│   │       │   │   └── PaymentService.java
│   │       │   ├── entity/
│   │       │   │   ├── PaymentOrder.java
│   │       │   │   └── PaymentTransaction.java
│   │       │   └── config/
│   │       │       └── RazorpayConfig.java
│   │       └── resources/
│   │           ├── application.yml
│   │           └── db/migration/
│   │               ├── V1__Create_payment_order_table.sql
│   │               └── V2__Create_payment_transaction_table.sql
│   ├── pom.xml
│   └── Dockerfile
│
├── docker-compose.yml        # Orchestrates all services
│   ├── postgres (Port 5433/5432)
│   ├── pgadmin (Port 5050)
│   ├── api-gateway (Port 8080)
│   ├── backend-service (Port 8081)
│   └── payment-service (Port 8082)
└── documentation/
    └── architecture/
        └── ARCHITECTURE.md   # This file
```

## Key Design Decisions

### Why JWT Validation Only at Gateway?

1. **Separation of Concerns**: Gateway handles authentication, backend handles authorization
2. **Performance**: JWT validation happens once at gateway, not in every service
3. **Scalability**: Backend services can scale independently without JWT validation overhead
4. **Security**: Single point of JWT validation reduces attack surface

### Why Webhook-Driven Sync?

1. **Reliability**: Webhooks ensure data consistency even if frontend doesn't call APIs
2. **Audit Trail**: All identity changes are logged in event tables
3. **Idempotency**: Webhook handlers check for existing records before creating
4. **Decoupling**: Backend doesn't depend on frontend API calls for identity data

### Why Trust Gateway Model?

1. **Network Security**: Gateway and backend are in private network (Docker network)
2. **Simplified Backend**: Backend doesn't need JWT parsing libraries
3. **Header Enrichment**: Gateway adds trusted headers (X-User-Id, X-Org-Id)
4. **Production Ready**: Can add internal API key validation for extra security

## Security Considerations

### JWT Validation

- Gateway validates JWT signature using Clerk JWKS
- Gateway validates JWT expiration
- Gateway validates JWT issuer
- Public keys are cached for performance

### Webhook Security

- Webhook signature verification using HMAC SHA256
- Constant-time comparison to prevent timing attacks
- Webhook secret stored in environment variable

### Database Security

- No passwords stored
- Foreign key constraints ensure data integrity
- Unique constraints prevent duplicate memberships
- Indexes optimize authorization queries

### Network Security

- Gateway and backend communicate over private Docker network
- Only gateway exposed to external network
- Backend not directly accessible from outside

## Configuration

### Environment Variables

#### API Gateway

- `CLERK_JWKS_URL`: Clerk JWKS endpoint URL
- `CLERK_ISSUER`: Clerk JWT issuer (usually `https://clerk.dev`)

#### Backend Service

- `CLERK_WEBHOOK_SECRET`: Clerk webhook signing secret
- `SPRING_DATASOURCE_URL`: PostgreSQL connection URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password

### Clerk Setup

1. **Create Clerk Application**

   - Sign up at https://clerk.com
   - Create a new application
   - Enable Organizations (for multi-tenancy)

2. **Get JWKS URL**

   - Format: `https://<your-instance>.clerk.accounts.dev/v1/jwks`
   - Set in `CLERK_JWKS_URL`

3. **Configure Webhook**
   - In Clerk Dashboard → Webhooks
   - Add endpoint: `https://your-domain.com/api/webhooks/clerk`
   - Select events:
     - `user.created`
     - `user.updated`
     - `organization.created`
     - `organizationMembership.created`
     - `organizationMembership.deleted`
   - Copy webhook signing secret → Set in `CLERK_WEBHOOK_SECRET`

## Running the System

### Prerequisites

- Docker Desktop
- Docker Compose
- Clerk account with application configured

### Steps

1. **Clone and configure**

   ```bash
   git clone <repository>
   cd payment
   ```

2. **Create `.env` file**

   ```env
   CLERK_JWKS_URL=https://your-instance.clerk.accounts.dev/v1/jwks
   CLERK_ISSUER=https://clerk.dev
   CLERK_WEBHOOK_SECRET=whsec_xxxxx
   ```

3. **Start services**

   ```bash
   docker-compose up --build
   ```

4. **Verify services**

   - API Gateway: http://localhost:8080
   - Backend Service: http://localhost:8081
   - Payment Service: http://localhost:8082
   - PostgreSQL: localhost:5433
   - pgAdmin: http://localhost:5050

5. **Test endpoints**

   ```bash
   # Health check (no auth)
   curl http://localhost:8080/api/health

   # Get current user (requires JWT)
   curl -H "Authorization: Bearer <jwt-token>" \
        http://localhost:8080/api/me
   ```

## API Endpoints

### API Gateway (Port 8080)

All requests go through the API Gateway which validates JWT tokens.

#### Public Endpoints (No Authentication Required)

- `GET /api/health` - Health check endpoint
  - Returns: `{"status": "UP", "service": "backend-service"}`

#### Protected Endpoints (Require JWT Token)

- `GET /api/me` - Get current user information

  - Requires: Valid JWT token in `Authorization: Bearer <token>` header
  - Returns: User information from database

- `GET /api/org/{orgId}/admin-data` - Get admin data
  - Requires: Valid JWT token + ADMIN role in the organization
  - Returns: Admin-level data for the organization

#### Payment Endpoints (Public - No JWT Required)

- `POST /api/payments/create-order` - Create Razorpay payment order

  - Request Body: `{"amount": 500}`
  - Returns: `{"orderId": "order_xxx", "amount": 500, "currency": "INR", "status": "created", "receipt": "rcpt_xxx"}`
  - Service: Payment Service (Port 8082)

- `POST /api/payments/verify` - Verify payment signature
  - Request Body: `{"razorpayOrderId": "order_xxx", "razorpayPaymentId": "pay_xxx", "razorpaySignature": "sig_xxx"}`
  - Returns: `"Payment verified successfully"`
  - Service: Payment Service (Port 8082)

#### Webhook Endpoints

- `POST /api/webhooks/clerk` - Clerk webhook receiver
  - Requires: Valid webhook signature (svix headers)
  - Handles: `user.created`, `user.updated`, `organization.created`, `organizationMembership.created`, `organizationMembership.deleted`
  - Service: Backend Service (Port 8081)

### Direct Service Access (For Testing)

#### Backend Service (Port 8081)

- `GET /api/health` - Health check
- `GET /api/me` - Get current user (requires X-User-Id header from gateway)
- `POST /api/webhooks/clerk` - Webhook endpoint

#### Payment Service (Port 8082)

- `POST /api/payments/create-order` - Create payment order
- `POST /api/payments/verify` - Verify payment

## Scaling Considerations

### Horizontal Scaling

- Gateway can be scaled independently
- Backend services can be scaled independently
- Use load balancer in front of gateway
- Database connection pooling handles multiple backend instances

### Database Scaling

- Read replicas for read-heavy workloads
- Connection pooling (HikariCP) configured
- Indexes optimized for authorization queries

### Caching

- JWT public keys cached in gateway
- Consider Redis for session caching (future enhancement)

## Database Tables Overview

### Current State

| Table Name              | Purpose                        | Status      | Notes                              |
| :---------------------- | :----------------------------- | :---------- | :--------------------------------- |
| `users`                 | User identity from Clerk       | ✅ Active   | Populated via webhooks             |
| `organizations`         | Multi-tenant organizations     | ✅ Active   | Populated via webhooks             |
| `roles`                 | Predefined roles (ADMIN, USER) | ✅ Active   | Static data                        |
| `memberships`           | User-Organization-Role links   | ✅ Active   | Populated via webhooks             |
| `user_events`           | Audit trail for user events    | ✅ Active   | Populated via webhooks             |
| `organization_events`   | Audit trail for org events     | ✅ Active   | Populated via webhooks             |
| `auth_sessions`         | Session tracking               | ⚠️ Optional | Table exists but no implementation |
| `payment_order`         | Payment orders                 | ✅ Active   | Populated via payment API          |
| `payment_transaction`   | Payment transactions           | ✅ Active   | Populated via payment API          |
| `flyway_schema_history` | Migration history              | ✅ Active   | Managed by Flyway                  |

### Why Tables Might Be Empty

1. **`auth_sessions`**: No repository or service implementation exists. This is an optional table for future use.

2. **`payment_order` / `payment_transaction`**: Empty until payment APIs are called.

3. **`organizations` / `memberships`**: Empty until Clerk webhooks are received (when organizations are created in Clerk).

4. **`users`**: Populated when Clerk sends `user.created` webhook events.

### Viewing Database Data

Use pgAdmin (http://localhost:5050) to view tables:

- Login: `admin@local.com` / `admin123`
- Connect to PostgreSQL using service name `postgres` (not localhost)
- Navigate to: Servers → Payment Gateway DB → Databases → appdb → Schemas → public → Tables

See [API Testing & Configuration Guide](./documentation/guides/API_TESTING_CONFIG.md) for detailed step-by-step instructions.

## Future Enhancements

1. **Auth Sessions Implementation**: Add repository and service to track active sessions in `auth_sessions` table
2. **Internal API Key**: Add API key validation between gateway and backend services
3. **Rate Limiting**: Add rate limiting at gateway level
4. **Request Logging**: Add request/response logging middleware
5. **Metrics**: Add Prometheus metrics for monitoring
6. **Distributed Tracing**: Add OpenTelemetry tracing across services
7. **Redis Cache**: Add Redis for session and data caching
8. **Message Queue**: Add message queue (RabbitMQ/Kafka) for async webhook processing
9. **Payment Webhooks**: Add Razorpay webhook support for payment status updates

## Troubleshooting

### JWT Validation Fails

- Check `CLERK_JWKS_URL` is correct
- Verify JWT token is from correct Clerk instance
- Check JWT hasn't expired
- Verify gateway can reach Clerk JWKS endpoint

### Webhook Signature Verification Fails

- Check `CLERK_WEBHOOK_SECRET` matches Clerk dashboard
- Verify webhook headers are present (svix-id, svix-timestamp, svix-signature)
- Check webhook payload format

### Database Connection Issues

- Verify PostgreSQL container is running
- Check connection URL, username, password
- Verify network connectivity between containers

### Authorization Fails

- Verify user exists in database (check webhook was processed)
- Verify membership exists for user-organization pair
- Check role assignment in membership table

---

## 📚 Related Documentation

This document provides a high-level architecture overview. For detailed guides, see:

### 🚀 Getting Started

- **[Quick Start Guide](../setup/QUICK_START.md)** - Quick setup and run instructions
- **[Setup Guide](./documentation/setup/SETUP_GUIDE.md)** - Detailed setup with troubleshooting

### 📘 How-To Guides

- **[API Testing & Configuration](./documentation/guides/API_TESTING_CONFIG.md)** - Complete guide for testing APIs, JWT tokens, webhooks, and database viewing
- **[Flow Documentation](./documentation/guides/FLOW_DOCUMENTATION.md)** - Detailed payment flow and API documentation

### 🏗️ Architecture & Design

- **[Implementation Summary](./documentation/architecture/IMPLEMENTATION_SUMMARY.md)** - What was built and implementation details
- **[Mistakes & Design Decisions](./documentation/architecture/fixes/MISTAKES_AND_DESIGN.md)** - Design decisions and lessons learned

### 📖 Setup & Configuration

- **[Database Setup](./documentation/setup/DATABASE_SETUP.md)** - Database configuration
- **[pgAdmin Setup](./documentation/setup/PGADMIN_SETUP.md)** - Database management UI setup
- **[Supabase Setup](./documentation/setup/SUPABASE_SETUP.md)** - Cloud database setup

### 📚 Reference

- **[Main README](./readme.md)** - Complete project documentation index
- **[Enterprise README](./documentation/reference/README_ENTERPRISE.md)** - Enterprise features overview
- **[Documentation Index](./documentation/README.md)** - Browse all documentation
