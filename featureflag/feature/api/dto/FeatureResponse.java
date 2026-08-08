package com.company.featureflag.feature.api.dto;

import com.company.featureflag.feature.domain.FeatureStatus;

import java.time.Instant;
import java.util.UUID;

public record FeatureResponse(
        UUID id,
        String key,
        String name,
        String description,
        FeatureStatus status,
        boolean killSwitch,
        int currentVersion,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}
