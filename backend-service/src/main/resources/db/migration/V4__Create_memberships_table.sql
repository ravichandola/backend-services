-- ============================================
-- V4: Create memberships table
-- ============================================
-- Links users to organizations with specific roles
-- This is the core of multi-tenant authorization
-- Synced from Clerk organizationMembership webhooks

CREATE TABLE memberships (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    clerk_membership_id VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure a user can only have one role per organization
    UNIQUE(user_id, organization_id)
);

CREATE INDEX idx_memberships_user_id ON memberships(user_id);
CREATE INDEX idx_memberships_organization_id ON memberships(organization_id);
CREATE INDEX idx_memberships_role_id ON memberships(role_id);
CREATE INDEX idx_memberships_clerk_membership_id ON memberships(clerk_membership_id);
CREATE INDEX idx_memberships_user_org ON memberships(user_id, organization_id);

COMMENT ON TABLE memberships IS 'User-organization-role relationships for multi-tenant authorization';
COMMENT ON COLUMN memberships.clerk_membership_id IS 'Unique Clerk membership identifier';
COMMENT ON COLUMN memberships.user_id IS 'Reference to users table';
COMMENT ON COLUMN memberships.organization_id IS 'Reference to organizations table';
COMMENT ON COLUMN memberships.role_id IS 'Reference to roles table (ADMIN or USER)';
