-- Feature-to-feature dependencies, e.g. NEW_CHECKOUT depends on PAYMENT_V2.
-- Cycle detection (A -> B -> A) is enforced in the application layer
-- (DependencyGraphValidator), not in the database.
CREATE TABLE feature_dependency (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_id               UUID          NOT NULL,
    depends_on_feature_id    UUID          NOT NULL,
    dependency_type          VARCHAR(20)   NOT NULL DEFAULT 'REQUIRES_ENABLED',
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_feature_dependency_feature
        FOREIGN KEY (feature_id) REFERENCES feature (id) ON DELETE CASCADE,
    CONSTRAINT fk_feature_dependency_depends_on
        FOREIGN KEY (depends_on_feature_id) REFERENCES feature (id) ON DELETE CASCADE,
    CONSTRAINT uq_feature_dependency_pair UNIQUE (feature_id, depends_on_feature_id),
    CONSTRAINT chk_feature_dependency_not_self CHECK (feature_id <> depends_on_feature_id),
    CONSTRAINT chk_feature_dependency_type CHECK (dependency_type IN ('REQUIRES_ENABLED'))
);

CREATE INDEX idx_feature_dependency_feature_id ON feature_dependency (feature_id);
CREATE INDEX idx_feature_dependency_depends_on ON feature_dependency (depends_on_feature_id);

COMMENT ON TABLE feature_dependency IS 'Directed edges in the feature dependency graph. Direct self-reference blocked at DB level; full cycles blocked in application code before insert.';
