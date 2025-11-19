-- Enable pgcrypto for bcrypt hashing (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Seed default ADMIN
INSERT INTO users (
    id,
    tenant_id,
    username,
    password_hash,
    roles,
    enabled,
    locked,
    created_at,
    updated_at
)
VALUES (
           '00000000-0000-0000-0000-000000000001',
           'default',
           'admin',
           crypt('admin123', gen_salt('bf')),
           'ROLE_ADMIN,ROLE_USER',
           TRUE,
           FALSE,
           NOW(),
           NOW()
       )
    ON CONFLICT (tenant_id, username) DO NOTHING;

-- Seed default USER
INSERT INTO users (
    id,
    tenant_id,
    username,
    password_hash,
    roles,
    enabled,
    locked,
    created_at,
    updated_at
)
VALUES (
           '00000000-0000-0000-0000-000000000002',
           'default',
           'user',
           crypt('user123', gen_salt('bf')),
           'ROLE_USER',
           TRUE,
           FALSE,
           NOW(),
           NOW()
       )
    ON CONFLICT (tenant_id, username) DO NOTHING;
