-- One row per feature_version describing how traffic is bucketed.
-- strategy_type is the discriminator; strategy_config carries type-specific
-- extras so new strategies (e.g. TRAFFIC_CANARY) don't require schema changes.
CREATE TABLE rollout_strategy (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_version_id   UUID          NOT NULL,
    strategy_type        VARCHAR(40)   NOT NULL,
    identifier_field     VARCHAR(100),
    percentage           INTEGER,
    strategy_config      JSONB         NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT fk_rollout_strategy_version
        FOREIGN KEY (feature_version_id) REFERENCES feature_version (id) ON DELETE CASCADE,
    CONSTRAINT chk_rollout_strategy_type CHECK (strategy_type IN (
        'BOOLEAN', 'IDENTIFIER_PERCENTAGE', 'REQUEST_PERCENTAGE',
        'CUSTOM_FIELD_PERCENTAGE', 'TRAFFIC_CANARY'
    )),
    CONSTRAINT chk_rollout_strategy_percentage CHECK (percentage IS NULL OR (percentage BETWEEN 0 AND 100))
);

CREATE UNIQUE INDEX uq_rollout_strategy_per_version ON rollout_strategy (feature_version_id);
CREATE INDEX idx_rollout_strategy_type ON rollout_strategy (strategy_type);

COMMENT ON TABLE rollout_strategy IS 'How a feature_version distributes TRUE/FALSE across an identifier space. One strategy per version currently.';
COMMENT ON COLUMN rollout_strategy.identifier_field IS 'EvaluationContext field used for hashing, e.g. phoneNumber, tenantId. Null for BOOLEAN strategy.';
COMMENT ON COLUMN rollout_strategy.strategy_config IS 'Type-specific extras, e.g. last-N-digits-modulo strategy config, without altering the schema.';
