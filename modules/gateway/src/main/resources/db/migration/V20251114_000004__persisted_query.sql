CREATE TABLE IF NOT EXISTS persisted_query (
    id            uuid PRIMARY KEY,
    query_id      VARCHAR(128) NOT NULL UNIQUE,
    document      TEXT NOT NULL,
    operation_name VARCHAR(128),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_used_at  TIMESTAMPTZ,
    use_count     BIGINT NOT NULL DEFAULT 0,
    version       BIGINT NOT NULL DEFAULT 0
);
