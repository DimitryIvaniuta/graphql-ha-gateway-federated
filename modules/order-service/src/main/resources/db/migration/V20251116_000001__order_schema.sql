CREATE SCHEMA IF NOT EXISTS order_service;

CREATE TABLE IF NOT EXISTS order_service.order_header (
    id uuid PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    external_id VARCHAR(64),
    customer_id uuid,
    status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_order_header_customer_id
    ON order_service.order_header (customer_id);

CREATE INDEX IF NOT EXISTS idx_order_header_status
    ON order_service.order_header (status);
