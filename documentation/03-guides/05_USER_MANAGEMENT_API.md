# 👥 User Management API Guide

## 📋 APIs Created

### 1. List Users with Roles
**GET** `/api/users?includeRoles=true`

Lists all users with their roles and memberships.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `sort`: Sort field and direction (default: "createdAt,desc")
- `includeRoles`: `true` to include roles/memberships (default: `false`)

**Example:**
```bash
curl -H "Authorization: Bearer <TOKEN>" \
     "http://localhost:8080/api/users?includeRoles=true&page=0&size=20"
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "imageUrl": "https://...",
      "createdAt": "2026-01-23T10:00:00",
      "memberships": [
        {
          "membershipId": 1,
          "organizationId": 1,
          "organizationName": "Payment Organization",
          "clerkOrgId": "org_xxx",
          "roleName": "ADMIN",
          "roleId": 1
        }
      ],
      "totalOrganizations": 1,
      "isAdmin": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

### 2. Update User Role
**PUT** `/api/users/role`

Update a user's role in an organization. Requires ADMIN access.

**Request Body:**
```json
{
  "userId": 1,  // Optional: Database user ID
  "clerkUserId": "user_xxx",  // Optional: Clerk user ID (use either userId or clerkUserId)
  "organizationId": 1,  // Optional: Database organization ID
  "clerkOrgId": "org_xxx",  // Optional: Clerk organization ID (use either organizationId or clerkOrgId)
  "roleName": "ADMIN"  // or "USER"
}
```

**Example:**
```bash
curl -X PUT \
     -H "Authorization: Bearer <TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{
       "clerkUserId": "user_xxx",
       "organizationId": 1,
       "roleName": "ADMIN"
     }' \
     http://localhost:8080/api/users/role
```

**Response:**
```json
{
  "success": true,
  "message": "User role updated successfully",
  "clerkUserId": "user_xxx",
  "organizationId": 1,
  "roleName": "ADMIN",
  "membership": {
    "id": 1,
    "roleName": "ADMIN",
    "organizationName": "Payment Organization"
  }
}
```

### 3. List Users (Simple - Backward Compatible)
**GET** `/api/users` (without `includeRoles=true`)

Returns users without roles (for backward compatibility).

## 🔄 Webhook Responsibility

### Who Stores Data in Database?

**Answer: `WebhookService` is responsible for storing all webhook data in the database.**

### Webhook Flow

```
Clerk → Webhook → API Gateway → Backend Service → WebhookService → Database
```

### WebhookService Responsibilities

1. **User Events:**
   - `user.created` → Creates user in `users` table
   - `user.updated` → Updates user in `users` table
   - Stores event in `user_events` table for audit

2. **Organization Events:**
   - `organization.created` → Creates organization in `organizations` table
   - Stores event in `organization_events` table for audit

3. **Membership Events:**
   - `organizationMembership.created` → Creates/updates membership in `memberships` table
   - `organizationMembership.deleted` → Deletes membership from `memberships` table
   - Stores event in `organization_events` table for audit

### Code Location

**WebhookService:** `backend-service/src/main/java/com/demo/backend/service/user/WebhookService.java`

**Key Methods:**
- `processUserCreated()` - Handles `user.created` webhook
- `processUserUpdated()` - Handles `user.updated` webhook
- `processOrganizationCreated()` - Handles `organization.created` webhook
- `processOrganizationMembershipCreated()` - Handles `organizationMembership.created` webhook
- `processOrganizationMembershipDeleted()` - Handles `organizationMembership.deleted` webhook

### Important Points

1. **WebhookService is the single source of truth** for webhook data storage
2. **All webhook events are stored in audit tables** (`user_events`, `organization_events`)
3. **Idempotency:** Webhook handlers check if data already exists before creating
4. **Transaction safety:** All webhook processing is `@Transactional`

### Manual Updates vs Webhooks

- **Webhooks:** Automatic sync from Clerk (preferred)
- **Manual APIs:** For fixing/updating data when webhooks fail or are missed
  - `/api/test/users` - Create user manually
  - `/api/test/organizations` - Create organization manually
  - `/api/debug/fix-role` - Fix role manually
  - `/api/users/role` - Update user role (admin operation)

## 📊 Database Tables

### Core Tables (Populated by Webhooks)

1. **`users`** - User data from `user.created` webhook
2. **`organizations`** - Organization data from `organization.created` webhook
3. **`memberships`** - User-Organization-Role links from `organizationMembership.created` webhook
4. **`roles`** - Predefined roles (ADMIN, USER) - created by migrations

### Audit Tables (Populated by Webhooks)

1. **`user_events`** - Full webhook payload for user events
2. **`organization_events`** - Full webhook payload for organization events

## 🎯 Best Practices

1. **Prefer Webhooks:** Let Clerk webhooks sync data automatically
2. **Use Manual APIs Only When Needed:** For fixing missing data or testing
3. **Check Audit Tables:** If data is missing, check `user_events` or `organization_events` to see if webhook was received
4. **Idempotency:** Webhook handlers are idempotent - safe to retry

## 🔍 Troubleshooting

### Users Not Appearing in Database

1. Check if webhook was received:
   ```sql
   SELECT * FROM user_events 
   WHERE clerk_user_id = 'user_xxx' 
   ORDER BY processed_at DESC;
   ```

2. Check webhook logs:
   ```bash
   docker logs backend-service | grep -i "user.created"
   ```

3. If webhook wasn't received, create manually:
   ```bash
   POST /api/test/users
   ```

### Organizations Not Appearing

1. Check organization events:
   ```sql
   SELECT * FROM organization_events 
   WHERE clerk_org_id = 'org_xxx' 
   ORDER BY processed_at DESC;
   ```

2. Create manually if needed:
   ```bash
   POST /api/test/organizations
   ```

### Roles Not Set Correctly

1. Check memberships:
   ```sql
   SELECT u.email, o.name, r.name 
   FROM memberships m
   JOIN users u ON m.user_id = u.id
   JOIN organizations o ON m.organization_id = o.id
   JOIN roles r ON m.role_id = r.id;
   ```

2. Fix role manually:
   ```bash
   PUT /api/users/role
   ```
