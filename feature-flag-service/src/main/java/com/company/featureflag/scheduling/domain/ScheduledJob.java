package com.company.featureflag.scheduling.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "scheduled_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduledJob {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "feature_id", nullable = false)
    private UUID featureId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScheduledAction action;

    @Column(name = "scheduled_time", nullable = false)
    private Instant scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduledJobStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_metadata", nullable = false)
    private Map<String, Object> executionMetadata = Map.of();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    private ScheduledJob(UUID featureId, ScheduledAction action, Instant scheduledTime) {
        this.id = UUID.randomUUID();
        this.featureId = featureId;
        this.action = action;
        this.scheduledTime = scheduledTime;
        this.status = ScheduledJobStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public static ScheduledJob of(UUID featureId, ScheduledAction action, Instant scheduledTime) {
        return new ScheduledJob(featureId, action, scheduledTime);
    }

    public void markExecuted() {
        this.status = ScheduledJobStatus.EXECUTED;
        this.executedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = ScheduledJobStatus.FAILED;
        this.executionMetadata = Map.of("error", reason);
        this.executedAt = Instant.now();
    }

    public void cancel() {
        this.status = ScheduledJobStatus.CANCELLED;
    }
}
