package com.company.featureflag.dependency.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * A directed edge: {@code featureId} depends on {@code dependsOnFeatureId}.
 * Cycles across the whole graph are rejected by
 * {@code DependencyGraphValidator} (application layer) before this row is
 * ever persisted — the DB only guards against a direct self-reference.
 */
@Entity
@Table(name = "feature_dependency")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeatureDependency {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "feature_id", nullable = false)
    private UUID featureId;

    @Column(name = "depends_on_feature_id", nullable = false)
    private UUID dependsOnFeatureId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false, length = 20)
    private DependencyType dependencyType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private FeatureDependency(UUID featureId, UUID dependsOnFeatureId, DependencyType dependencyType) {
        if (featureId.equals(dependsOnFeatureId)) {
            throw new IllegalArgumentException("A feature cannot depend on itself");
        }
        this.id = UUID.randomUUID();
        this.featureId = featureId;
        this.dependsOnFeatureId = dependsOnFeatureId;
        this.dependencyType = dependencyType;
        this.createdAt = Instant.now();
    }

    public static FeatureDependency of(UUID featureId, UUID dependsOnFeatureId, DependencyType dependencyType) {
        return new FeatureDependency(featureId, dependsOnFeatureId, dependencyType);
    }
}
