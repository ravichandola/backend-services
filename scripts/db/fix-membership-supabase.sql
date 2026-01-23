-- ============================================
-- Fix Membership in Supabase Database
-- Run this in Supabase SQL Editor
-- ============================================
-- This script creates/updates membership for user
-- to have ADMIN role in the organization

-- Step 1: Find the user, organization, and role IDs
-- (Run this first to get the IDs)

SELECT 
    u.id as user_id,
    u.email,
    u.clerk_user_id,
    o.id as org_id,
    o.name as org_name,
    r.id as role_id,
    r.name as role_name
FROM users u
CROSS JOIN organizations o
CROSS JOIN roles r
WHERE u.clerk_user_id = 'user_38LrIbulxWo0PdTbuW47vIqRZds'
  AND r.name = 'ADMIN'
ORDER BY o.id;

-- Step 2: Check if membership already exists
SELECT 
    m.id as membership_id,
    u.email,
    o.name as org_name,
    r.name as role_name
FROM memberships m
JOIN users u ON m.user_id = u.id
JOIN organizations o ON m.organization_id = o.id
JOIN roles r ON m.role_id = r.id
WHERE u.clerk_user_id = 'user_38LrIbulxWo0PdTbuW47vIqRZds';

-- Step 3: Create or Update Membership
-- Replace <USER_ID>, <ORG_ID>, and <ROLE_ID> with values from Step 1

-- Option A: If membership doesn't exist, create it
INSERT INTO memberships (user_id, organization_id, role_id, clerk_membership_id, created_at, updated_at)
SELECT 
    u.id,
    o.id,
    r.id,
    'mem_fix_' || u.id || '_' || o.id || '_' || EXTRACT(EPOCH FROM NOW())::bigint,
    NOW(),
    NOW()
FROM users u
CROSS JOIN organizations o
CROSS JOIN roles r
WHERE u.clerk_user_id = 'user_38LrIbulxWo0PdTbuW47vIqRZds'
  AND r.name = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM memberships m 
    WHERE m.user_id = u.id 
      AND m.organization_id = o.id
  )
ON CONFLICT (user_id, organization_id) DO NOTHING;

-- Option B: If membership exists, update role to ADMIN
UPDATE memberships m
SET 
    role_id = r.id,
    updated_at = NOW()
FROM users u
JOIN organizations o ON m.organization_id = o.id
JOIN roles r ON r.name = 'ADMIN'
WHERE m.user_id = u.id
  AND u.clerk_user_id = 'user_38LrIbulxWo0PdTbuW47vIqRZds'
  AND m.role_id != r.id;

-- Step 4: Verify the fix
SELECT 
    m.id as membership_id,
    u.id as user_id,
    u.email,
    u.clerk_user_id,
    o.id as org_id,
    o.name as org_name,
    r.id as role_id,
    r.name as role_name,
    m.created_at,
    m.updated_at
FROM memberships m
JOIN users u ON m.user_id = u.id
JOIN organizations o ON m.organization_id = o.id
JOIN roles r ON m.role_id = r.id
WHERE u.clerk_user_id = 'user_38LrIbulxWo0PdTbuW47vIqRZds'
ORDER BY m.id;
