-- ============================================
-- V7: Create auth_sessions table (Optional but recommended)
-- ============================================
-- Tracks active authentication sessions
-- Can be used for session management, analytics, and security monitoring
-- JWT claims are stored here for quick lookup without parsing JWT

CREATE TABLE auth_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE SET NULL,
    jwt_id VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_sessions_user_id ON auth_sessions(user_id);
CREATE INDEX idx_auth_sessions_organization_id ON auth_sessions(organization_id);
CREATE INDEX idx_auth_sessions_jwt_id ON auth_sessions(jwt_id);
CREATE INDEX idx_auth_sessions_expires_at ON auth_sessions(expires_at);

COMMENT ON TABLE auth_sessions IS 'Optional table for tracking authentication sessions';
COMMENT ON COLUMN auth_sessions.jwt_id IS 'JWT jti (JWT ID) claim for session tracking';
COMMENT ON COLUMN auth_sessions.organization_id IS 'Current organization context for the session';
