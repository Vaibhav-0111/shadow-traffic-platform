-- Shadow Traffic Platform — Database Schema
-- Run automatically by docker-compose via postgres initdb mount

-- ── Comparison Results (written by comparator-service) ───────────────────────
CREATE TABLE IF NOT EXISTS comparison_results (
    id               BIGSERIAL PRIMARY KEY,
    request_id       VARCHAR(64)  NOT NULL,
    method           VARCHAR(10),
    path             TEXT,

    v1_status_code   INT,
    v2_status_code   INT,
    status_match     BOOLEAN      DEFAULT FALSE,

    v1_latency_ms    BIGINT,
    v2_latency_ms    BIGINT,
    latency_delta_ms BIGINT,

    body_match       BOOLEAN      DEFAULT FALSE,
    match            BOOLEAN      DEFAULT FALSE,

    diff_summary     TEXT,
    v1_body          TEXT,
    v2_body          TEXT,

    v1_success       BOOLEAN      DEFAULT TRUE,
    v2_success       BOOLEAN      DEFAULT TRUE,
    v1_error         TEXT,
    v2_error         TEXT,

    created_at       TIMESTAMPTZ  DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cr_created_at  ON comparison_results (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_cr_match       ON comparison_results (match);
CREATE INDEX IF NOT EXISTS idx_cr_path        ON comparison_results (path);
CREATE INDEX IF NOT EXISTS idx_cr_request_id  ON comparison_results (request_id);

-- ── Orders v1 ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders_v1 (
    id            BIGSERIAL PRIMARY KEY,
    customer_id   VARCHAR(64),
    status        VARCHAR(32)    DEFAULT 'PENDING',
    total_amount  NUMERIC(12,2),
    currency      VARCHAR(8)     DEFAULT 'USD',
    created_at    TIMESTAMPTZ    DEFAULT NOW(),
    updated_at    TIMESTAMPTZ    DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS order_v1_items (
    order_id      BIGINT REFERENCES orders_v1(id) ON DELETE CASCADE,
    product_id    VARCHAR(64),
    product_name  VARCHAR(256),
    quantity      INT,
    unit_price    NUMERIC(10,2)
);

-- ── Orders v2 ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders_v2 (
    id               BIGSERIAL PRIMARY KEY,
    customer_id      VARCHAR(64),
    status           VARCHAR(32)    DEFAULT 'PENDING',
    total_amount     NUMERIC(12,2),
    currency         VARCHAR(8)     DEFAULT 'USD',
    discount_applied BOOLEAN        DEFAULT FALSE,
    created_at       TIMESTAMPTZ    DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS order_v2_items (
    order_id      BIGINT REFERENCES orders_v2(id) ON DELETE CASCADE,
    product_id    VARCHAR(64),
    product_name  VARCHAR(256),
    quantity      INT,
    unit_price    NUMERIC(10,2)
);

-- ── Seed some fake comparison data so dashboard isn't empty on first run ──────
INSERT INTO comparison_results
    (request_id, method, path, v1_status_code, v2_status_code, status_match,
     v1_latency_ms, v2_latency_ms, latency_delta_ms,
     body_match, match, diff_summary, created_at)
SELECT
    'seed-' || gs,
    CASE WHEN random() < 0.7 THEN 'POST' ELSE 'GET' END,
    CASE WHEN random() < 0.7 THEN '/api/v1/orders' ELSE '/api/v1/orders/' || (random()*100)::int END,
    200,
    CASE WHEN random() < 0.85 THEN 200 ELSE 500 END,
    (random() < 0.85),
    (30 + random() * 80)::bigint,
    (30 + random() * 200)::bigint,
    (random() * 120)::bigint,
    (random() < 0.88),
    (random() < 0.82),
    CASE WHEN random() < 0.85 THEN '[]'
         ELSE '[{"path":"$.totalAmount","v1Value":"99.99","v2Value":"94.99","type":"VALUE_CHANGED"}]'
    END,
    NOW() - ((random() * 86400) || ' seconds')::interval
FROM generate_series(1, 200) AS gs;
