-- ============================================
-- V3: Create roles table
-- ============================================
-- Predefined roles for authorization
-- ADMIN: Full access to organization resources
-- USER: Standard user access

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_roles_name ON roles(name);

-- Insert default roles
INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Organization administrator with full access'),
    ('USER', 'Standard user with limited access');

COMMENT ON TABLE roles IS 'Predefined roles for authorization';
COMMENT ON COLUMN roles.name IS 'Role name (ADMIN, USER)';
