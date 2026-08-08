package com.company.featureflag.configuration.domain;

import java.util.List;

/**
 * The single "evaluation-ready" representation of a feature — exactly what
 * both this service's own evaluation pipeline and the future SDK need to
 * decide TRUE/FALSE, nothing more (no internal DB ids, no JPA entities).
 * This is what gets cached in Redis under {@code feature:<key>} (spec §19)
 * and, unchanged, is what {@code GET /api/v1/sdk/configuration} will hand
 * to the SDK in Phase 11 — one shape, two consumers.
 *
 * Deliberately string-typed for status/strategyType/operator/combinator
 * rather than referencing internal enums directly, so this package has no
 * dependency on feature/rule/rollout domain code and stays a stable,
 * serialization-friendly wire contract.
 */
public record FeatureConfiguration(
        String key,
        int version,
        String status,
        boolean killSwitch,
        RolloutConfig rollout,
        List<RuleConfig> rules,
        List<String> dependsOnFeatureKeys
) {
    /** Quick precheck some callers may want without running the full pipeline. */
    public boolean isPotentiallyActive() {
        return "ACTIVE".equals(status) && !killSwitch;
    }
}
