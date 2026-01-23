# 🎯 Frontend API Guide - Clerk Webhook Data APIs

Yeh guide aapko batayega ki Clerk webhooks se aane wale data ko frontend mein kaise fetch karein.

## 📋 Table of Contents

1. [Authentication](#authentication)
2. [User APIs](#user-apis)
3. [Organization APIs](#organization-apis)
4. [Membership APIs](#membership-apis)
5. [Role Management APIs](#role-management-apis)
6. [Complete Examples](#complete-examples)

---

## 🔐 Authentication

**IMPORTANT:** Sabhi APIs ko JWT token chahiye. Gateway automatically JWT validate karta hai aur `X-User-Id` aur `X-Org-Id` headers add karta hai.

### Request Format
```javascript
// Frontend se request
fetch('http://localhost:8080/api/me', {
  headers: {
    'Authorization': 'Bearer <CLERK_JWT_TOKEN>'
  }
})
```

### Headers (Gateway automatically add karta hai)
- `X-User-Id`: Current user ka Clerk user ID
- `X-Org-Id`: Current organization ka Clerk org ID (optional, agar user organization context mein hai)

---

## 👤 User APIs

### 1. Get Current User
**GET** `/api/me`

Current logged-in user ki information fetch karta hai.

**Request:**
```bash
curl -H "Authorization: Bearer <TOKEN>" \
     http://localhost:8080/api/me
```

**Response:**
```json
{
  "id": 1,
  "clerkUserId": "user_2abc123",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "imageUrl": "https://img.clerk.com/..."
}
```

**Use Case:**
- User profile page
- Dashboard mein user info display karna
- Navigation bar mein user name/avatar

---

### 2. Get All Users (Admin Only)
**GET** `/api/users`

Sabhi users ki list fetch karta hai. **ADMIN role required.**

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `sort`: Sort field and direction (default: "createdAt,desc")
- `includeRoles`: `true` to include roles/memberships (default: `false`)

**Request:**
```bash
# Simple list
curl -H "Authorization: Bearer <TOKEN>" \
     "http://localhost:8080/api/users?page=0&size=20"

# With roles and memberships
curl -H "Authorization: Bearer <TOKEN>" \
     "http://localhost:8080/api/users?includeRoles=true&page=0&size=20"
```

**Response (with roles):**
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
          "organizationName": "My Organization",
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
  "totalPages": 1,
  "first": true,
  "last": true
}
```

**Use Case:**
- Admin dashboard mein user management
- User list page with roles
- Search/filter users

---

## 🏢 Organization APIs

### 1. Get User's Organizations
**GET** `/api/organizations`

Current user ki sabhi organizations fetch karta hai (jahan user ka membership hai).

**Request:**
```bash
curl -H "Authorization: Bearer <TOKEN>" \
     http://localhost:8080/api/organizations
```

**Response:**
```json
{
  "organizations": [
    {
      "id": 1,
      "clerkOrgId": "org_2abc123",
      "name": "My Organization",
      "slug": "my-org",
      "imageUrl": "https://...",
      "createdAt": "2026-01-23T10:00:00",
      "updatedAt": "2026-01-23T10:00:00",
      "memberCount": 5,
      "userRole": "ADMIN"
    }
  ],
  "total": 1
}
```

**Use Case:**
- Organization switcher/dropdown
- User dashboard mein organizations list
- Organization selection page

---

### 2. Get Organization by ID
**GET** `/api/organizations/{orgId}`

Specific organization ki details fetch karta hai. User ko is organization mein access hona chahiye.

**Request:**
```bash
curl -H "Authorization: Bearer <TOKEN>" \
     http://localhost:8080/api/organizations/1
```

**Response:**
```json
{
  "id": 1,
  "clerkOrgId": "org_2abc123",
  "name": "My Organization",
  "slug": "my-org",
  "imageUrl": "https://...",
  "createdAt": "2026-01-23T10:00:00",
  "updatedAt": "2026-01-23T10:00:00",
  "memberCount": 5,
  "userRole": "ADMIN"
}
```

**Use Case:**
- Organization settings page
- Organization details page
- Organization header/profile

---

### 3. Get Organization by Clerk Org ID
**GET** `/api/organizations/clerk/{clerkOrgId}`

Clerk org ID se organization fetch karta hai.

**Request:**
```bash
curl -H "Authorization: Bearer <TOKEN>" \
     http://localhost:8080/api/organizations/clerk/org_2abc123
```

**Response:** Same as above

**Use Case:**
- Clerk se organization ID milne par direct fetch karna
- Organization switcher mein Clerk IDs use karna

---

### 4. Get Organization Members
**GET** `/api/organizations/{orgId}/members`

Organization ke sabhi members fetch karta hai (paginated).

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

**Request:**
```bash
curl -H "Authorization: Bearer <TOKEN>" \
     "http://localhost:8080/api/organizations/1/members?page=0&size=20"
```

**Response:**
```json
{
  "members": [
    {
      "membershipId": 1,
      "clerkMembershipId": "mem_xxx",
      "roleName": "ADMIN",
      "roleId": 1,
      "userId": 1,
      "email": "admin@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "imageUrl": "https://..."
    },
    {
      "membershipId": 2,
      "clerkMembershipId": "mem_yyy",
      "roleName": "USER",
      "roleId": 2,
      "userId": 2,
      "email": "user@example.com",
      "firstName": "Jane",
      "lastName": "Smith",
      "imageUrl": "https://..."
    }
  ],
  "totalMembers": 2,
  "page": 0,
  "size": 20,
  "totalPages": 1
}
```

**Use Case:**
- Organization members page
- Team management
- Member list with roles
- Invite/remove members UI

---

## 🔗 Membership APIs

### 1. Get User's Memberships
**GET** `/api/organizations/memberships`

Current user ki sabhi memberships fetch karta hai (sabhi organizations ke saath).

**Request:**
```bash
curl -H "Authorization: Bearer <TOKEN>" \
     http://localhost:8080/api/organizations/memberships
```

**Response:**
```json
{
  "memberships": [
    {
      "id": 1,
      "clerkMembershipId": "mem_xxx",
      "createdAt": "2026-01-23T10:00:00",
      "updatedAt": "2026-01-23T10:00:00",
      "user": {
        "id": 1,
        "email": "user@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "imageUrl": "https://..."
      },
      "organization": {
        "id": 1,
        "clerkOrgId": "org_xxx",
        "name": "My Organization",
        "slug": "my-org",
        "imageUrl": "https://..."
      },
      "role": {
        "id": 1,
        "name": "ADMIN",
        "description": "Administrator role"
      }
    }
  ],
  "total": 1
}
```

**Use Case:**
- User profile mein organizations list
- Check user ki roles across organizations
- Membership management

---

## 👑 Role Management APIs

### 1. Update User Role (Admin Only)
**PUT** `/api/users/role`

User ki role update karta hai. **ADMIN role required.**

**Request Body:**
```json
{
  "userId": 1,                    // Optional: Database user ID
  "clerkUserId": "user_xxx",      // Optional: Clerk user ID (use either)
  "organizationId": 1,            // Optional: Database org ID
  "clerkOrgId": "org_xxx",        // Optional: Clerk org ID (use either)
  "roleName": "ADMIN"             // "ADMIN" or "USER"
}
```

**Request:**
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
    "organizationName": "My Organization"
  }
}
```

**Use Case:**
- Admin panel mein role change karna
- Promote/demote users
- Role management UI

---

## 🎯 Complete Examples

### Example 1: User Dashboard Load Karna

```javascript
// 1. Get current user
const userResponse = await fetch('http://localhost:8080/api/me', {
  headers: {
    'Authorization': `Bearer ${clerkToken}`
  }
});
const user = await userResponse.json();

// 2. Get user's organizations
const orgsResponse = await fetch('http://localhost:8080/api/organizations', {
  headers: {
    'Authorization': `Bearer ${clerkToken}`
  }
});
const { organizations } = await orgsResponse.json();

// 3. Display user info and organizations
console.log('User:', user);
console.log('Organizations:', organizations);
```

---

### Example 2: Organization Page Load Karna

```javascript
// 1. Get organization details
const orgId = 1; // From URL or state
const orgResponse = await fetch(`http://localhost:8080/api/organizations/${orgId}`, {
  headers: {
    'Authorization': `Bearer ${clerkToken}`
  }
});
const organization = await orgResponse.json();

// 2. Get organization members
const membersResponse = await fetch(
  `http://localhost:8080/api/organizations/${orgId}/members?page=0&size=20`,
  {
    headers: {
      'Authorization': `Bearer ${clerkToken}`
    }
  }
);
const membersData = await membersResponse.json();

// 3. Display organization and members
console.log('Organization:', organization);
console.log('Members:', membersData.members);
```

---

### Example 3: Admin User Management

```javascript
// 1. Get all users with roles
const usersResponse = await fetch(
  'http://localhost:8080/api/users?includeRoles=true&page=0&size=20',
  {
    headers: {
      'Authorization': `Bearer ${clerkToken}`
    }
  }
);
const usersData = await usersResponse.json();

// 2. Display users with their roles
usersData.content.forEach(user => {
  console.log(`${user.email} - Admin: ${user.isAdmin}`);
  user.memberships.forEach(membership => {
    console.log(`  - ${membership.organizationName}: ${membership.roleName}`);
  });
});

// 3. Update user role
async function updateUserRole(clerkUserId, orgId, newRole) {
  const response = await fetch('http://localhost:8080/api/users/role', {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${clerkToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      clerkUserId,
      organizationId: orgId,
      roleName: newRole
    })
  });
  
  const result = await response.json();
  console.log('Role updated:', result);
}
```

---

### Example 4: Organization Switcher

```javascript
// Get user's organizations for switcher
async function loadOrganizations() {
  const response = await fetch('http://localhost:8080/api/organizations', {
    headers: {
      'Authorization': `Bearer ${clerkToken}`
    }
  });
  
  const { organizations } = await response.json();
  
  // Create organization switcher dropdown
  const orgSwitcher = organizations.map(org => ({
    id: org.id,
    clerkOrgId: org.clerkOrgId,
    name: org.name,
    role: org.userRole,
    memberCount: org.memberCount
  }));
  
  return orgSwitcher;
}
```

---

## ⚠️ Error Handling

### Common Error Responses

**401 Unauthorized:**
```json
{
  "error": "Invalid signature"
}
```
- JWT token invalid ya expired
- Gateway se authentication fail

**403 Forbidden:**
```json
{
  "error": "Forbidden: ADMIN role required"
}
```
- User ko access nahi hai
- Role insufficient

**404 Not Found:**
```json
{
  "error": "User not found"
}
```
- Resource exist nahi karta
- Invalid ID

**500 Internal Server Error:**
```json
{
  "error": "Internal server error",
  "message": "Detailed error message"
}
```
- Server-side error
- Database issue

---

## 📝 Important Notes

1. **JWT Token Required:** Sabhi APIs ko valid JWT token chahiye
2. **Gateway Headers:** Gateway automatically `X-User-Id` aur `X-Org-Id` add karta hai
3. **Authorization:** Har API endpoint proper authorization check karta hai
4. **Pagination:** List APIs paginated hain (default: page 0, size 20)
5. **Clerk IDs:** Clerk IDs (`clerkUserId`, `clerkOrgId`) use karein frontend mein
6. **Database IDs:** Backend internally database IDs use karta hai, but APIs dono accept karte hain

---

## 🔄 Data Flow

```
Clerk Webhook → Backend (WebhookService) → Database
                                      ↓
Frontend API Request → Gateway (JWT validation) → Backend API → Database
                                      ↓
                              Frontend receives data
```

**Webhook Events:**
- `user.created` → User table mein entry
- `organization.created` → Organization table mein entry
- `organizationMembership.created` → Membership table mein entry
- `role.created/updated/deleted` → Role updates

**Frontend APIs:**
- User data fetch karein → `/api/me`, `/api/users`
- Organization data fetch karein → `/api/organizations`
- Membership data fetch karein → `/api/organizations/memberships`
- Role update karein → `/api/users/role`

---

## 🚀 Quick Start

1. **Get Current User:**
   ```bash
   GET /api/me
   ```

2. **Get User's Organizations:**
   ```bash
   GET /api/organizations
   ```

3. **Get Organization Members:**
   ```bash
   GET /api/organizations/{orgId}/members
   ```

4. **Update User Role (Admin):**
   ```bash
   PUT /api/users/role
   ```

---

**Happy Coding! 🎉**

Agar koi question ho ya koi aur API chahiye, toh batayein!
