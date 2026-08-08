-- Feature aggregate root. No environment column: environment separation is
-- handled by deploying separate databases per environment (see ARCHITECTURE.md).
CREATE TABLE feature (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key              VARCHAR(150)  NOT NULL,
    name             VARCHAR(255)  NOT NULL,
    description      TEXT,
    status           VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    kill_switch      BOOLEAN       NOT NULL DEFAULT FALSE,
    current_version  INTEGER       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by       VARCHAR(100)  NOT NULL,
    updated_by       VARCHAR(100)  NOT NULL,
    version          BIGINT        NOT NULL DEFAULT 0,  -- optimistic lock (@Version)

    CONSTRAINT uq_feature_key UNIQUE (key),
    CONSTRAINT chk_feature_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_feature_status ON feature (status);
CREATE INDEX idx_feature_updated_at ON feature (updated_at);

COMMENT ON TABLE feature IS 'Aggregate root: one row per feature flag, environment implied by the database instance.';
COMMENT ON COLUMN feature.key IS 'Stable business key, e.g. NEW_PAYMENT_FLOW. Immutable after creation.';
COMMENT ON COLUMN feature.current_version IS 'Denormalized pointer to the currently active feature_version.version_number for fast reads.';
COMMENT ON COLUMN feature.version IS 'JPA @Version column for optimistic locking on the mutable aggregate (status, kill_switch, current_version).';
