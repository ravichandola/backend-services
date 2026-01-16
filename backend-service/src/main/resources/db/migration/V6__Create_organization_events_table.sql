-- ============================================
-- V6: Create organization_events table
-- ============================================
-- Audit trail for organization-related webhook events
-- Stores full webhook payload for debugging and audit purposes

CREATE TABLE organization_events (
    id BIGSERIAL PRIMARY KEY,
    clerk_org_id VARCHAR(255),
    clerk_user_id VARCHAR(255),
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    clerk_event_id VARCHAR(255),
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_org_events_clerk_org_id ON organization_events(clerk_org_id);
CREATE INDEX idx_org_events_clerk_user_id ON organization_events(clerk_user_id);
CREATE INDEX idx_org_events_event_type ON organization_events(event_type);
CREATE INDEX idx_org_events_processed_at ON organization_events(processed_at);
CREATE INDEX idx_org_events_clerk_event_id ON organization_events(clerk_event_id);

COMMENT ON TABLE organization_events IS 'Audit trail for organization-related Clerk webhook events';
COMMENT ON COLUMN organization_events.event_type IS 'Webhook event type (organization.created, organizationMembership.created, etc.)';
COMMENT ON COLUMN organization_events.event_data IS 'Full webhook payload as JSONB for audit purposes';
COMMENT ON COLUMN organization_events.clerk_event_id IS 'Unique Clerk event identifier for deduplication';
