-- A targeting rule is a named group of conditions (see rule_condition) that
-- are combined with AND/OR and evaluated in priority order.
CREATE TABLE targeting_rule (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_version_id   UUID          NOT NULL,
    priority             INTEGER       NOT NULL,
    combinator           VARCHAR(10)   NOT NULL DEFAULT 'AND',
    enabled              BOOLEAN       NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_targeting_rule_version
        FOREIGN KEY (feature_version_id) REFERENCES feature_version (id) ON DELETE CASCADE,
    CONSTRAINT uq_targeting_rule_priority UNIQUE (feature_version_id, priority),
    CONSTRAINT chk_targeting_rule_combinator CHECK (combinator IN ('AND', 'OR'))
);

CREATE INDEX idx_targeting_rule_version_priority ON targeting_rule (feature_version_id, priority);

COMMENT ON TABLE targeting_rule IS 'Ordered rule set per feature_version. Lowest priority number is evaluated first; first match wins (see EVALUATION_SEMANTICS in README).';
