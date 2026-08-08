package com.company.featureflag.sdk.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RuleConfig(
        int priority,
        String combinator,
        boolean enabled,
        List<ConditionConfig> conditions
) {
}
