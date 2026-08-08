package com.company.featureflag.dependency.api.dto;

import com.company.featureflag.dependency.domain.DependencyType;

import java.time.Instant;
import java.util.UUID;

public record DependencyResponse(
        UUID id,
        String featureKey,
        String dependsOnFeatureKey,
        DependencyType dependencyType,
        Instant createdAt
) {
}
