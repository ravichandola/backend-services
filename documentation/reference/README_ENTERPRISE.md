# Enterprise Multi-Tenant SaaS Backend

A complete, production-ready enterprise-grade multi-tenant SaaS backend system built with Java Spring Boot, Spring Cloud Gateway, PostgreSQL, and Clerk authentication.

## 🎯 Key Features

- ✅ **Zero Authentication Logic in Backend** - All auth handled by Clerk
- ✅ **JWT Validation at Gateway** - Single point of JWT validation
- ✅ **Webhook-Driven Identity Sync** - Automatic user/org sync from Clerk
- ✅ **Multi-Tenant Architecture** - Organizations, Users, Roles, Memberships
- ✅ **Role-Based Authorization** - Service-layer authorization checks
- ✅ **Complete Audit Trail** - All webhook events stored for compliance
- ✅ **Dockerized Setup** - One command to run everything
- ✅ **Production Ready** - Enterprise best practices throughout

## 🏗️ Architecture

```
Browser → Clerk (Auth) → API Gateway (JWT Validation) → Backend Service (Business Logic) → PostgreSQL
                              ↓
                         Webhooks → Backend Service (Identity Sync)
```

**Key Principle**: Gateway validates JWT, backend trusts gateway. No JWT parsing in backend services.

## 📁 Project Structure

```
payment/
├── api-gateway/          # Spring Cloud Gateway with JWT validation
├── backend-service/      # Spring Boot backend with webhooks & authorization
├── docker-compose.yml    # Orchestrates all services
├── documentation/
│   └── architecture/
│       └── ARCHITECTURE.md       # Detailed architecture documentation
└── documentation/
    ├── setup/
    │   └── SETUP_GUIDE.md        # Step-by-step setup instructions
    ├── guides/
    ├── architecture/
    └── reference/
```

## 🚀 Quick Start

### Prerequisites

- Docker Desktop
- Clerk account (free tier works)

### Setup Steps

1. **Clone repository**

   ```bash
   git clone <repository-url>
   cd payment
   ```

2. **Configure Clerk**

   - Create Clerk application at https://clerk.com
   - Enable Organizations
   - Configure webhook endpoint
   - Get JWKS URL and webhook secret

3. **Create `.env` file**

   ```env
   CLERK_JWKS_URL=https://your-app.clerk.accounts.dev/v1/jwks
   CLERK_ISSUER=https://clerk.dev
   CLERK_WEBHOOK_SECRET=whsec_xxxxx
   ```

4. **Start services**

   ```bash
   docker-compose up --build
   ```

5. **Test**
   ```bash
   curl http://localhost:8080/api/health
   ```

See [Setup Guide](../setup/SETUP_GUIDE.md) for detailed instructions.

## 📚 Documentation

- **[Architecture Overview](../architecture/ARCHITECTURE.md)** - Complete system architecture, design decisions, and request lifecycle
- **[Setup Guide](../setup/SETUP_GUIDE.md)** - Step-by-step setup and troubleshooting

## 🔐 Security Model

### Authentication Flow

1. User authenticates with Clerk (frontend)
2. Clerk issues JWT token
3. Frontend sends JWT to API Gateway
4. Gateway validates JWT using Clerk JWKS
5. Gateway adds `X-User-Id` and `X-Org-Id` headers
6. Backend trusts gateway headers (no JWT parsing)

### Authorization Flow

1. Backend receives request with `X-User-Id` and `X-Org-Id` headers
2. Backend queries database for user membership
3. Backend checks role (ADMIN/USER) in membership
4. Access granted or denied based on role

### Webhook Security

- HMAC SHA256 signature verification
- Constant-time comparison (prevents timing attacks)
- Webhook secret stored in environment variable
- All events stored in audit tables

## 🗄️ Database Schema

### Core Tables

- **users** - User identity (synced from Clerk)
- **organizations** - Multi-tenant organizations
- **roles** - Predefined roles (ADMIN, USER)
- **memberships** - User ↔ Organization ↔ Role relationships

### Audit Tables

- **user_events** - User webhook events
- **organization_events** - Organization webhook events
- **auth_sessions** - Optional session tracking

See [Architecture Overview](../architecture/ARCHITECTURE.md) for complete schema details.

## 🔌 API Endpoints

### Public

- `GET /api/health` - Health check

### Protected (Require JWT)

- `GET /api/me` - Get current user
- `GET /api/org/{orgId}/admin-data` - Admin data (ADMIN role required)

### Webhooks

- `POST /api/webhooks/clerk` - Clerk webhook receiver

## 🧪 Testing

### Manual Testing

1. **Get JWT token** from Clerk (via frontend or dashboard)
2. **Call protected endpoint**:
   ```bash
   curl -H "Authorization: Bearer <jwt>" \
        http://localhost:8080/api/me
   ```

### Webhook Testing

1. Use ngrok for local webhook testing
2. Configure Clerk webhook to point to ngrok URL
3. Trigger events in Clerk dashboard
4. Check backend logs for webhook processing

## 🐳 Docker Services

| Service         | Port | Description                     |
| --------------- | ---- | ------------------------------- |
| api-gateway     | 8080 | API Gateway with JWT validation |
| backend-service | 8081 | Backend service                 |
| postgres        | 5433 | PostgreSQL database             |
| pgadmin         | 5050 | Database admin UI               |

## 🔧 Configuration

### Environment Variables

**API Gateway:**

- `CLERK_JWKS_URL` - Clerk JWKS endpoint
- `CLERK_ISSUER` - Clerk JWT issuer

**Backend Service:**

- `CLERK_WEBHOOK_SECRET` - Webhook signing secret
- `SPRING_DATASOURCE_URL` - Database URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password

## 📊 Monitoring

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f api-gateway
docker-compose logs -f backend-service
```

### Database Access

- **pgAdmin**: http://localhost:5050
  - Email: `admin@local.com`
  - Password: `admin123`
- **psql**: `docker exec -it postgres-db psql -U appuser -d appdb`

## 🚦 Request Lifecycle

### Authenticated Request

```
1. Browser → Clerk: Authenticate
2. Clerk → Browser: JWT token
3. Browser → Gateway: Request + JWT
4. Gateway: Validate JWT via JWKS
5. Gateway → Backend: Request + X-User-Id, X-Org-Id headers
6. Backend: Check authorization in database
7. Backend → Gateway: Response
8. Gateway → Browser: Response
```

### Webhook Request

```
1. Clerk → Gateway: Webhook (signed)
2. Gateway → Backend: Forward webhook
3. Backend: Verify signature
4. Backend: Process event (create/update user/org/membership)
5. Backend: Store event in audit table
6. Backend → Clerk: Success response
```

See [Architecture Overview](../architecture/ARCHITECTURE.md) for detailed flow diagrams.

## 🎓 Key Design Decisions

### Why JWT Validation Only at Gateway?

- **Separation of Concerns**: Gateway = auth, Backend = business logic
- **Performance**: Validate once, not in every service
- **Scalability**: Backend services scale without JWT overhead
- **Security**: Single validation point reduces attack surface

### Why Webhook-Driven Sync?

- **Reliability**: Data sync even if frontend doesn't call APIs
- **Audit Trail**: All identity changes logged
- **Idempotency**: Handlers check for existing records
- **Decoupling**: Backend independent of frontend API calls

### Why Trust Gateway Model?

- **Network Security**: Private Docker network
- **Simplified Backend**: No JWT parsing needed
- **Header Enrichment**: Gateway adds trusted context
- **Production Ready**: Can add internal API key validation

## 🔄 Webhook Events Handled

- `user.created` - Create user in database
- `user.updated` - Update user in database
- `organization.created` - Create organization
- `organizationMembership.created` - Create/update membership
- `organizationMembership.deleted` - Delete membership

## 🛠️ Development

### Making Changes

1. Edit code in `api-gateway/` or `backend-service/`
2. Rebuild: `docker-compose up --build`
3. Check logs: `docker-compose logs -f`

### Database Migrations

- Migrations in `backend-service/src/main/resources/db/migration/`
- Flyway runs migrations automatically on startup
- Follow naming: `V{version}__{description}.sql`

## 📈 Scaling

### Horizontal Scaling

- Gateway: Scale independently (use load balancer)
- Backend: Scale independently (stateless)
- Database: Use connection pooling (HikariCP configured)

### Future Enhancements

- Redis for caching
- Message queue for async webhook processing
- Read replicas for database
- Distributed tracing (OpenTelemetry)

## 🐛 Troubleshooting

See [Setup Guide](../setup/SETUP_GUIDE.md) for detailed troubleshooting.

Common issues:

- JWT validation fails → Check `CLERK_JWKS_URL`
- Webhook signature fails → Check `CLERK_WEBHOOK_SECRET`
- Database connection fails → Check PostgreSQL container
- User not found → Check webhook was processed

## 📝 License

[Your License Here]

## 🙏 Acknowledgments

Built with:

- Spring Boot 3.3.0
- Spring Cloud Gateway
- PostgreSQL 16
- Flyway
- Clerk

---

**Ready to build?** Start with [Setup Guide](../setup/SETUP_GUIDE.md)

**Want to understand the design?** Read [Architecture Overview](../architecture/ARCHITECTURE.md)
