-- ============================================
-- V2: Create organizations table
-- ============================================
-- Organizations represent tenants in the multi-tenant SaaS model
-- Synced from Clerk organization webhooks

CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    clerk_org_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255),
    image_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_organizations_clerk_org_id ON organizations(clerk_org_id);
CREATE INDEX idx_organizations_slug ON organizations(slug);

COMMENT ON TABLE organizations IS 'Organization/tenant data synced from Clerk webhooks';
COMMENT ON COLUMN organizations.clerk_org_id IS 'Unique Clerk organization identifier';
COMMENT ON COLUMN organizations.slug IS 'URL-friendly organization identifier';
