-- Time-triggered feature state changes, e.g. "enable NEW_PAYMENT_FLOW at T".
-- Executed by a Spring @Scheduled poller today; the table shape is
-- deliberately generic so a distributed scheduler can take over later.
CREATE TABLE scheduled_job (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_id            UUID          NOT NULL,
    action                VARCHAR(30)   NOT NULL,
    scheduled_time        TIMESTAMPTZ   NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    execution_metadata    JSONB         NOT NULL DEFAULT '{}'::jsonb,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    executed_at           TIMESTAMPTZ,

    CONSTRAINT fk_scheduled_job_feature
        FOREIGN KEY (feature_id) REFERENCES feature (id) ON DELETE CASCADE,
    CONSTRAINT chk_scheduled_job_action CHECK (action IN ('ENABLE', 'DISABLE', 'ACTIVATE_KILL_SWITCH', 'DEACTIVATE_KILL_SWITCH', 'ACTIVATE_VERSION')),
    CONSTRAINT chk_scheduled_job_status CHECK (status IN ('PENDING', 'EXECUTED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_scheduled_job_due ON scheduled_job (status, scheduled_time);
CREATE INDEX idx_scheduled_job_feature_id ON scheduled_job (feature_id);

COMMENT ON TABLE scheduled_job IS 'Polled by a fixed-delay scheduler: SELECT ... WHERE status = PENDING AND scheduled_time <= now().';
