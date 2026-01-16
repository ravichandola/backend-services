-- ============================================
-- V1: Create users table
-- ============================================
-- This table stores user identity data synced from Clerk webhooks
-- No passwords stored - authentication handled entirely by Clerk

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    clerk_user_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    image_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_clerk_user_id ON users(clerk_user_id);
CREATE INDEX idx_users_email ON users(email);

COMMENT ON TABLE users IS 'User identity data synced from Clerk webhooks';
COMMENT ON COLUMN users.clerk_user_id IS 'Unique Clerk user identifier';
COMMENT ON COLUMN users.email IS 'User email address (unique per Clerk account)';
