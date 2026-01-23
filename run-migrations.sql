-- ============================================
-- Combined Migration Script for Supabase
-- Run this in pgAdmin Query Tool
-- ============================================

-- V1: Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    clerk_user_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    image_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_clerk_user_id ON users(clerk_user_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- V2: Create organizations table
CREATE TABLE IF NOT EXISTS organizations (
    id BIGSERIAL PRIMARY KEY,
    clerk_org_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255),
    image_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_organizations_clerk_org_id ON organizations(clerk_org_id);
CREATE INDEX IF NOT EXISTS idx_organizations_slug ON organizations(slug);

-- V3: Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_roles_name ON roles(name);

-- Insert default roles (only if they don't exist)
INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Organization administrator with full access'),
    ('USER', 'Standard user with limited access')
ON CONFLICT (name) DO NOTHING;

-- V4: Create memberships table
CREATE TABLE IF NOT EXISTS memberships (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    clerk_membership_id VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, organization_id)
);

CREATE INDEX IF NOT EXISTS idx_memberships_user_id ON memberships(user_id);
CREATE INDEX IF NOT EXISTS idx_memberships_organization_id ON memberships(organization_id);
CREATE INDEX IF NOT EXISTS idx_memberships_role_id ON memberships(role_id);
CREATE INDEX IF NOT EXISTS idx_memberships_clerk_membership_id ON memberships(clerk_membership_id);
CREATE INDEX IF NOT EXISTS idx_memberships_user_org ON memberships(user_id, organization_id);

-- V5: Create user_events table
CREATE TABLE IF NOT EXISTS user_events (
    id BIGSERIAL PRIMARY KEY,
    clerk_user_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    clerk_event_id VARCHAR(255),
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_events_clerk_user_id ON user_events(clerk_user_id);
CREATE INDEX IF NOT EXISTS idx_user_events_event_type ON user_events(event_type);
CREATE INDEX IF NOT EXISTS idx_user_events_processed_at ON user_events(processed_at);
CREATE INDEX IF NOT EXISTS idx_user_events_clerk_event_id ON user_events(clerk_event_id);

-- V6: Create organization_events table
CREATE TABLE IF NOT EXISTS organization_events (
    id BIGSERIAL PRIMARY KEY,
    clerk_org_id VARCHAR(255),
    clerk_user_id VARCHAR(255),
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    clerk_event_id VARCHAR(255),
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_org_events_clerk_org_id ON organization_events(clerk_org_id);
CREATE INDEX IF NOT EXISTS idx_org_events_clerk_user_id ON organization_events(clerk_user_id);
CREATE INDEX IF NOT EXISTS idx_org_events_event_type ON organization_events(event_type);
CREATE INDEX IF NOT EXISTS idx_org_events_processed_at ON organization_events(processed_at);
CREATE INDEX IF NOT EXISTS idx_org_events_clerk_event_id ON organization_events(clerk_event_id);

-- V7: Create auth_sessions table
CREATE TABLE IF NOT EXISTS auth_sessions (
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

CREATE INDEX IF NOT EXISTS idx_auth_sessions_user_id ON auth_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_auth_sessions_organization_id ON auth_sessions(organization_id);
CREATE INDEX IF NOT EXISTS idx_auth_sessions_jwt_id ON auth_sessions(jwt_id);
CREATE INDEX IF NOT EXISTS idx_auth_sessions_expires_at ON auth_sessions(expires_at);

-- V8: Create payment_order table
CREATE TABLE IF NOT EXISTS payment_order (
    id BIGSERIAL PRIMARY KEY,
    razorpay_order_id VARCHAR(255) NOT NULL UNIQUE,
    amount BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_order_razorpay_order_id ON payment_order(razorpay_order_id);
CREATE INDEX IF NOT EXISTS idx_payment_order_status ON payment_order(status);

-- V9: Create payment_transaction table
CREATE TABLE IF NOT EXISTS payment_transaction (
    id BIGSERIAL PRIMARY KEY,
    razorpay_payment_id VARCHAR(255),
    razorpay_order_id VARCHAR(255),
    razorpay_signature VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_transaction_razorpay_payment_id ON payment_transaction(razorpay_payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_transaction_razorpay_order_id ON payment_transaction(razorpay_order_id);
CREATE INDEX IF NOT EXISTS idx_payment_transaction_status ON payment_transaction(status);

-- Success message
SELECT 'All migrations completed successfully!' AS status;
