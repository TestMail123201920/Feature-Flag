package com.company.featureflag.feature.api.dto;

import com.company.featureflag.feature.domain.VersionStatus;
import com.company.featureflag.rollout.domain.StrategyType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Full version detail including a summary of its rollout strategy and
 * targeting rules. Rule/rollout management gets its own dedicated DTOs and
 * endpoints in Phase 6-7; these nested summaries exist so a version can
 * already be inspected end-to-end.
 */
public record FeatureVersionDetailResponse(
        UUID id,
        int versionNumber,
        VersionStatus status,
        Instant createdAt,
        String createdBy,
        Map<String, Object> configurationMetadata,
        RolloutSummary rollout,
        List<RuleSummary> rules
) {
    public record RolloutSummary(
            StrategyType strategyType,
            String identifierField,
            Integer percentage
    ) {
    }

    public record RuleSummary(
            int priority,
            String combinator,
            boolean enabled,
            int conditionCount
    ) {
    }
}
