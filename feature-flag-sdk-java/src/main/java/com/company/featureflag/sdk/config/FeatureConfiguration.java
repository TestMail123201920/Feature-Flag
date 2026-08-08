package com.company.featureflag.sdk.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Mirrors {@code GET /api/v1/sdk/configuration}'s response shape exactly
 * (spec §31). Field names must match the service's JSON output one-for-one
 * — this is the wire contract between the two Maven projects; there is no
 * shared code, only a shared shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FeatureConfiguration(
        String key,
        int version,
        String status,
        boolean killSwitch,
        RolloutConfig rollout,
        List<RuleConfig> rules,
        List<String> dependsOnFeatureKeys
) {
    public boolean isPotentiallyActive() {
        return "ACTIVE".equals(status) && !killSwitch;
    }
}
