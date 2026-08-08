-- Transactional outbox: written in the same DB transaction as the domain
-- change it describes, then relayed to Kafka asynchronously by a poller.
-- Also the mechanism by which a future Audit Service consumes change history
-- without an audit_log table existing in this service.
CREATE TABLE outbox_event (
    event_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type        VARCHAR(60)   NOT NULL,
    aggregate_type    VARCHAR(60)   NOT NULL,
    aggregate_id      UUID          NOT NULL,
    payload           JSONB         NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    published_at      TIMESTAMPTZ,
    retry_count       INTEGER       NOT NULL DEFAULT 0,

    CONSTRAINT chk_outbox_event_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_outbox_event_pending ON outbox_event (status, created_at) WHERE status = 'PENDING';
CREATE INDEX idx_outbox_event_aggregate ON outbox_event (aggregate_type, aggregate_id);

COMMENT ON TABLE outbox_event IS 'Reliable event publication. Relay poller selects PENDING rows, publishes to Kafka, then marks PUBLISHED; failures increment retry_count and stay PENDING/FAILED for retry.';
