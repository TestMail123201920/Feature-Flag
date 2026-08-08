package com.company.featureflag.feature.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root for a feature flag. Holds only the mutable "current state"
 * concepts (status, kill switch, pointer to the active version). The actual
 * versioned configuration lives in {@code FeatureVersion} rows so this row
 * never needs to be rewritten for a rollout percentage change.
 */
@Entity
@Table(name = "feature")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feature {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String key;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeatureStatus status;

    @Column(name = "kill_switch", nullable = false)
    private boolean killSwitch;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Version
    @Column(nullable = false)
    private long version;

    private Feature(String key, String name, String description, String createdBy) {
        this.id = UUID.randomUUID();
        this.key = key;
        this.name = name;
        this.description = description;
        this.status = FeatureStatus.DRAFT;
        this.killSwitch = false;
        this.currentVersion = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    public static Feature create(String key, String name, String description, String createdBy) {
        return new Feature(key, name, description, createdBy);
    }

    public void rename(String name, String description, String updatedBy) {
        this.name = name;
        this.description = description;
        touch(updatedBy);
    }

    public void transitionTo(FeatureStatus target, String updatedBy) {
        if (!this.status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Invalid feature state transition: %s -> %s".formatted(this.status, target));
        }
        this.status = target;
        touch(updatedBy);
    }

    public void activateKillSwitch(String updatedBy) {
        this.killSwitch = true;
        touch(updatedBy);
    }

    public void deactivateKillSwitch(String updatedBy) {
        this.killSwitch = false;
        touch(updatedBy);
    }

    /** Called when a new FeatureVersion becomes the active one (create or rollback). */
    public void pointToVersion(int versionNumber, String updatedBy) {
        this.currentVersion = versionNumber;
        touch(updatedBy);
    }

    private void touch(String updatedBy) {
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }
}
