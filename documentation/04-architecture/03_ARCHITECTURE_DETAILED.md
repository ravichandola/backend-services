# Enterprise Multi-Tenant SaaS Backend Architecture (Detailed)

> This is the **detailed** architecture document. For a shorter, high-level view, see `01_ARCHITECTURE.md`.

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
    └── 04-architecture/
        └── 01_ARCHITECTURE.md   # Summary architecture document
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
- `CLERK_ISSUER`: Clerk JWT issuer

#### Backend Service

- `CLERK_WEBHOOK_SECRET`: Clerk webhook signing secret
- `SPRING_DATASOURCE_URL`: PostgreSQL connection URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password

## Running the System

See the setup docs for full instructions:

- `02-setup/01_QUICK_START.md`
- `02-setup/02_SETUP_GUIDE.md`

## API Endpoints (Summary)

### Through API Gateway (Port 8080)

- `GET /api/health` – Health check (no auth)
- `GET /api/me` – Current user (JWT required)
- `GET /api/org/{orgId}/admin-data` – Org admin data (JWT + ADMIN role)
- `POST /api/payments/create-order` – Create Razorpay order
- `POST /api/payments/verify` – Verify Razorpay payment
- `POST /api/webhooks/clerk` – Clerk webhooks (svix signature)

## Scaling Considerations

### Horizontal Scaling

- Scale gateway independently
- Scale backend services independently
- Use load balancer in front of gateway

### Database Scaling

- Read replicas for read-heavy workloads
- Connection pooling (HikariCP)
- Indexes for auth-related queries

### Caching

- JWT public keys cached at gateway
- Future: Redis for session/data caching

## Database Tables Overview

See `05-reference/README_ENTERPRISE.md` for full table documentation. High level:

- `users`, `organizations`, `roles`, `memberships` – multi-tenant auth model
- `user_events`, `organization_events` – webhook audit
- `payment_order`, `payment_transaction` – payments
- `auth_sessions` – optional, future use

## Future Enhancements

- Implement `auth_sessions` fully
- Add internal API key between gateway and backend
- Add rate limiting, metrics, tracing
- Add Razorpay payment webhooks

