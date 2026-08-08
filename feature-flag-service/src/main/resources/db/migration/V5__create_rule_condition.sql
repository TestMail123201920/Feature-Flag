-- Individual comparisons within a targeting_rule, e.g. country == IN.
CREATE TABLE rule_condition (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    targeting_rule_id UUID          NOT NULL,
    field             VARCHAR(100)  NOT NULL,
    operator          VARCHAR(30)   NOT NULL,
    value             JSONB         NOT NULL,

    CONSTRAINT fk_rule_condition_rule
        FOREIGN KEY (targeting_rule_id) REFERENCES targeting_rule (id) ON DELETE CASCADE,
    CONSTRAINT chk_rule_condition_operator CHECK (operator IN (
        'EQUALS', 'NOT_EQUALS', 'IN', 'NOT_IN', 'CONTAINS', 'STARTS_WITH',
        'ENDS_WITH', 'GREATER_THAN', 'GREATER_THAN_OR_EQUAL',
        'LESS_THAN', 'LESS_THAN_OR_EQUAL', 'EXISTS', 'NOT_EXISTS'
    ))
);

CREATE INDEX idx_rule_condition_rule_id ON rule_condition (targeting_rule_id);

COMMENT ON TABLE rule_condition IS 'Field/operator/value triples ANDed or ORed together per targeting_rule.combinator.';
COMMENT ON COLUMN rule_condition.value IS 'JSON to support scalars, lists (IN/NOT_IN), and typed comparisons uniformly.';
