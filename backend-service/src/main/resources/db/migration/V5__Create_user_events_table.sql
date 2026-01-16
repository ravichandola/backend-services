-- ============================================
-- V5: Create user_events table
-- ============================================
-- Audit trail for user-related webhook events
-- Stores full webhook payload for debugging and audit purposes

CREATE TABLE user_events (
    id BIGSERIAL PRIMARY KEY,
    clerk_user_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    clerk_event_id VARCHAR(255),
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_events_clerk_user_id ON user_events(clerk_user_id);
CREATE INDEX idx_user_events_event_type ON user_events(event_type);
CREATE INDEX idx_user_events_processed_at ON user_events(processed_at);
CREATE INDEX idx_user_events_clerk_event_id ON user_events(clerk_event_id);

COMMENT ON TABLE user_events IS 'Audit trail for user-related Clerk webhook events';
COMMENT ON COLUMN user_events.event_type IS 'Webhook event type (user.created, user.updated, etc.)';
COMMENT ON COLUMN user_events.event_data IS 'Full webhook payload as JSONB for audit purposes';
COMMENT ON COLUMN user_events.clerk_event_id IS 'Unique Clerk event identifier for deduplication';
