package com.company.featureflag.feature.api.dto;

import com.company.featureflag.feature.domain.VersionStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record FeatureVersionSummaryResponse(
        UUID id,
        int versionNumber,
        VersionStatus status,
        Instant createdAt,
        String createdBy
) {
}
