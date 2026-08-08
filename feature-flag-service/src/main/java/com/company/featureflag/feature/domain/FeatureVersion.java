package com.company.featureflag.feature.domain;

import com.company.featureflag.rollout.domain.RolloutStrategy;
import com.company.featureflag.rule.domain.TargetingRule;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable configuration snapshot. Never updated in place after it leaves
 * DRAFT — every meaningful change (rollout %, rules) is a brand new row with
 * an incremented {@code versionNumber}, which is what makes rollback and
 * diffing trivial.
 */
@Entity
@Table(name = "feature_version")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeatureVersion {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "feature_id", nullable = false)
    private UUID featureId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VersionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration_metadata", nullable = false)
    private Map<String, Object> configurationMetadata = Map.of();

    @OneToOne(mappedBy = "featureVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private RolloutStrategy rolloutStrategy;

    @OneToMany(mappedBy = "featureVersionId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TargetingRule> targetingRules = new ArrayList<>();

    private FeatureVersion(UUID featureId, int versionNumber, String createdBy,
                            Map<String, Object> configurationMetadata) {
        this.id = UUID.randomUUID();
        this.featureId = featureId;
        this.versionNumber = versionNumber;
        this.status = VersionStatus.DRAFT;
        this.createdAt = Instant.now();
        this.createdBy = createdBy;
        this.configurationMetadata = configurationMetadata == null ? Map.of() : configurationMetadata;
    }

    public static FeatureVersion draft(UUID featureId, int versionNumber, String createdBy,
                                        Map<String, Object> configurationMetadata) {
        return new FeatureVersion(featureId, versionNumber, createdBy, configurationMetadata);
    }

    public void activate() {
        this.status = VersionStatus.ACTIVE;
    }

    public void supersede() {
        if (this.status == VersionStatus.ACTIVE) {
            this.status = VersionStatus.SUPERSEDED;
        }
    }

    public void markRolledBackTo() {
        this.status = VersionStatus.ROLLED_BACK;
    }
}
