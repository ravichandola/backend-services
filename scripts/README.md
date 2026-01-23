# Scripts Directory

Utility scripts for development, testing, and database operations.

## Available Scripts

### Admin Role Management

- **`fix-admin-role-complete.sh`** - Complete admin role fix script
  - Extracts organization info from JWT
  - Creates organization if needed
  - Fixes user role to ADMIN
  - Verifies and tests the fix
  
  Usage: `./scripts/fix-admin-role-complete.sh <JWT_TOKEN>`

- **`fix-role-now.sh`** - Quick role fix script
  - Checks current memberships
  - Fixes role to ADMIN if needed
  - Verifies the fix
  
  Usage: `./scripts/fix-role-now.sh <JWT_TOKEN>`

### API Testing

- **`test-all-apis.sh`** - Complete API test suite
  - Tests health check endpoint
  - Tests user endpoints
  - Tests organization endpoints
  - Tests membership endpoints
  - Provides test summary
  
  Usage: `./scripts/test-all-apis.sh <JWT_TOKEN>`

### Database Scripts

- **`db/fix-membership-supabase.sql`** - SQL script to fix memberships in Supabase
  - Find user, organization, and role IDs
  - Check existing memberships
  - Create or update membership with ADMIN role
  - Verify the fix
  
  Usage: Run in Supabase SQL Editor

## Getting JWT Token

All scripts that require authentication need a JWT token:

1. Start HTTP server: `python3 -m http.server 8000`
2. Open browser: `http://localhost:8000/clerk-login.html`
3. Login with Clerk and click "Get JWT Token"
4. Copy the token immediately (⚠️ tokens expire in ~60 seconds!)

## Notes

- Scripts should be run from the project root directory
- All scripts use `http://localhost:8080` as the base URL
- Scripts require `jq` for JSON parsing (optional but recommended)
- Database scripts are for Supabase (prod profile)
