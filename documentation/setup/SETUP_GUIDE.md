# Setup Guide - Enterprise Multi-Tenant SaaS Backend

## Quick Start

This guide will help you set up and run the complete enterprise-grade multi-tenant SaaS backend system.

## Prerequisites

- **Docker Desktop** installed and running
- **Docker Compose** (included with Docker Desktop)
- **Clerk Account** with application configured
- **Git** (for cloning)

## Step 1: Clone and Navigate

```bash
git clone <repository-url>
cd payment
```

## Step 2: Configure Clerk

### 2.1 Create Clerk Application

1. Sign up at https://clerk.com
2. Create a new application
3. **Enable Organizations** (required for multi-tenancy):
   - Go to Settings → Organizations
   - Enable "Organizations"

### 2.2 Get JWKS URL

1. In Clerk Dashboard, go to **API Keys**
2. Note your Clerk instance URL (e.g., `https://your-app.clerk.accounts.dev`)
3. JWKS URL format: `https://your-app.clerk.accounts.dev/v1/jwks`

### 2.3 Configure Webhook

1. In Clerk Dashboard, go to **Webhooks**
2. Click **Add Endpoint**
3. Set endpoint URL: `https://your-domain.com/api/webhooks/clerk`
   - For local development, use a tool like ngrok: `ngrok http 8080`
   - Then use: `https://your-ngrok-url.ngrok.io/api/webhooks/clerk`
4. Select these events:
   - ✅ `user.created`
   - ✅ `user.updated`
   - ✅ `organization.created`
   - ✅ `organizationMembership.created`
   - ✅ `organizationMembership.deleted`
5. Copy the **Signing Secret** (starts with `whsec_`)

## Step 3: Create Environment File

Create a `.env` file in the project root:

```bash
touch .env
```

Add the following (replace with your actual values):

```env
# Clerk Configuration
CLERK_JWKS_URL=https://your-app.clerk.accounts.dev/v1/jwks
CLERK_ISSUER=https://clerk.dev
CLERK_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxx

# Database Configuration (optional - defaults work for Docker)
# SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/appdb
# SPRING_DATASOURCE_USERNAME=appuser
# SPRING_DATASOURCE_PASSWORD=apppass
```

**Important**:

- Replace `your-app.clerk.accounts.dev` with your actual Clerk instance
- Replace `whsec_xxxxxxxxxxxxx` with your actual webhook secret from Clerk

## Step 4: Start Services

```bash
docker-compose up --build
```

This will:

1. Build PostgreSQL container
2. Build and start backend-service
3. Build and start api-gateway
4. Run Flyway migrations automatically

**First run may take 5-10 minutes** (downloading images, building applications).

## Step 5: Verify Services

### Check Container Status

```bash
docker-compose ps
```

Expected output:

```
NAME              STATUS                    PORTS
api-gateway       Up X minutes             0.0.0.0:8080->8080/tcp
backend-service   Up X minutes             0.0.0.0:8081->8081/tcp
postgres-db       Up X minutes (healthy)   0.0.0.0:5433->5432/tcp
pgadmin           Up X minutes             0.0.0.0:5050->80/tcp
```

### Check Logs

```bash
# Gateway logs
docker-compose logs -f api-gateway

# Backend logs
docker-compose logs -f backend-service

# All logs
docker-compose logs -f
```

Look for:

- ✅ `Started GatewayApplication` (gateway)
- ✅ `Started BackendApplication` (backend)
- ✅ `Flyway migration successful` (backend)

## Step 6: Test the System

### 6.1 Health Check (No Auth Required)

```bash
curl http://localhost:8080/api/health
```

Expected response:

```json
{
  "status": "UP",
  "service": "backend-service"
}
```

### 6.2 Get JWT Token from Clerk

1. In your frontend application, authenticate with Clerk
2. Get the JWT token from Clerk session
3. Or use Clerk's test token from dashboard (for testing)

### 6.3 Test Protected Endpoint

```bash
# Replace <jwt-token> with actual JWT from Clerk
curl -H "Authorization: Bearer <jwt-token>" \
     http://localhost:8080/api/me
```

Expected response:

```json
{
  "id": 1,
  "clerkUserId": "user_xxxxx",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### 6.4 Test Webhook (Using ngrok for Local Development)

1. **Install ngrok**: https://ngrok.com/download
2. **Start ngrok**:
   ```bash
   ngrok http 8080
   ```
3. **Copy ngrok URL** (e.g., `https://abc123.ngrok.io`)
4. **Update Clerk webhook endpoint**:
   - Go to Clerk Dashboard → Webhooks
   - Update endpoint URL to: `https://abc123.ngrok.io/api/webhooks/clerk`
5. **Trigger a webhook**:
   - Create a user in Clerk
   - Or create an organization
   - Check backend logs for webhook processing

## Step 7: Access Database (Optional)

### Using pgAdmin

1. Open http://localhost:5050
2. Login:
   - Email: `admin@local.com`
   - Password: `admin123`
3. Add PostgreSQL server:
   - Host: `postgres` (service name, not localhost)
   - Port: `5432`
   - Database: `appdb`
   - Username: `appuser`
   - Password: `apppass`

### Using psql

```bash
docker exec -it postgres-db psql -U appuser -d appdb
```

## Troubleshooting

### Issue: Port Already in Use

**Error**: `Port 8080 is already in use`

**Solution**:

1. Find process using port:

   ```bash
   # macOS/Linux
   lsof -ti:8080

   # Windows
   netstat -ano | findstr :8080
   ```

2. Kill the process or change port in `docker-compose.yml`

### Issue: JWT Validation Fails

**Error**: `JWT validation failed` in gateway logs

**Solutions**:

1. Verify `CLERK_JWKS_URL` is correct in `.env`
2. Check JWT token is from correct Clerk instance
3. Verify JWT hasn't expired
4. Check gateway can reach Clerk JWKS endpoint:
   ```bash
   curl https://your-app.clerk.accounts.dev/v1/jwks
   ```

### Issue: Webhook Signature Verification Fails

**Error**: `Invalid webhook signature` in backend logs

**Solutions**:

1. Verify `CLERK_WEBHOOK_SECRET` matches Clerk dashboard
2. Check webhook headers are present
3. For local development with ngrok, ensure HTTPS is used

### Issue: Database Connection Fails

**Error**: `Connection refused` or `Connection timeout`

**Solutions**:

1. Verify PostgreSQL container is running:
   ```bash
   docker-compose ps postgres
   ```
2. Wait for health check to pass (may take 10-20 seconds)
3. Check connection URL in backend logs
4. Verify network connectivity:
   ```bash
   docker exec -it backend-service ping postgres
   ```

### Issue: User Not Found in Database

**Error**: `User not found` when calling `/api/me`

**Solutions**:

1. Verify webhook was processed (check `user_events` table)
2. Manually trigger webhook from Clerk dashboard
3. Check backend logs for webhook processing errors
4. Verify user exists in Clerk dashboard

### Issue: Authorization Fails

**Error**: `Forbidden: ADMIN role required`

**Solutions**:

1. Verify membership exists in database:
   ```sql
   SELECT * FROM memberships WHERE user_id = <user_id> AND organization_id = <org_id>;
   ```
2. Check role assignment:
   ```sql
   SELECT r.name FROM roles r
   JOIN memberships m ON r.id = m.role_id
   WHERE m.user_id = <user_id> AND m.organization_id = <org_id>;
   ```
3. Ensure webhook processed `organizationMembership.created` event

## Development Workflow

### Making Code Changes

1. **Edit code** in `api-gateway/` or `backend-service/`
2. **Rebuild and restart**:
   ```bash
   docker-compose up --build
   ```

### Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f api-gateway
docker-compose logs -f backend-service
docker-compose logs -f postgres
```

### Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (deletes database data)
docker-compose down -v
```

### Resetting Database

```bash
# Stop services and remove volumes
docker-compose down -v

# Start again (Flyway will recreate schema)
docker-compose up --build
```

## Production Deployment

### Environment Variables

Set these in your production environment:

```env
CLERK_JWKS_URL=https://your-prod-instance.clerk.accounts.dev/v1/jwks
CLERK_ISSUER=https://clerk.dev
CLERK_WEBHOOK_SECRET=whsec_production_secret
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/appdb
SPRING_DATASOURCE_USERNAME=prod_user
SPRING_DATASOURCE_PASSWORD=prod_password
```

### Security Checklist

- [ ] Use HTTPS for all endpoints
- [ ] Set strong database passwords
- [ ] Use production Clerk instance
- [ ] Configure webhook endpoint with production URL
- [ ] Enable database backups
- [ ] Set up monitoring and alerting
- [ ] Configure rate limiting
- [ ] Set up log aggregation

## Next Steps

1. **Integrate Frontend**: Connect your frontend application to Clerk
2. **Add More APIs**: Extend `ApiController` with your business logic
3. **Add More Roles**: Extend roles table and authorization logic
4. **Add Caching**: Consider Redis for performance
5. **Add Monitoring**: Set up Prometheus and Grafana
6. **Add Logging**: Set up centralized logging (ELK stack)

## Support

For issues or questions:

1. Check [Architecture Overview](../architecture/ARCHITECTURE.md) for system design
2. Review logs: `docker-compose logs -f`
3. Check database state via pgAdmin
4. Verify Clerk configuration in dashboard
