-- Immutable configuration snapshots. Every meaningful config change creates
-- a new row here rather than overwriting the previous configuration.
CREATE TABLE feature_version (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_id               UUID          NOT NULL,
    version_number           INTEGER       NOT NULL,
    status                   VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by               VARCHAR(100)  NOT NULL,
    configuration_metadata   JSONB         NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT fk_feature_version_feature
        FOREIGN KEY (feature_id) REFERENCES feature (id) ON DELETE CASCADE,
    CONSTRAINT uq_feature_version_number UNIQUE (feature_id, version_number),
    CONSTRAINT chk_feature_version_status CHECK (status IN ('DRAFT', 'ACTIVE', 'SUPERSEDED', 'ROLLED_BACK'))
);

CREATE INDEX idx_feature_version_feature_id ON feature_version (feature_id);
CREATE INDEX idx_feature_version_status ON feature_version (feature_id, status);

COMMENT ON TABLE feature_version IS 'Immutable version history per feature enabling rollback, diffing, and audit.';
COMMENT ON COLUMN feature_version.version_number IS 'Monotonically increasing per feature_id, assigned by the application (max+1) inside the write transaction.';
COMMENT ON COLUMN feature_version.configuration_metadata IS 'Free-form JSON snapshot metadata (e.g. change summary) not otherwise modeled relationally.';
