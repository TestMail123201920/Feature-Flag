package com.company.featureflag.configuration.domain;

import java.util.List;

public record RuleConfig(
        int priority,
        String combinator,
        boolean enabled,
        List<ConditionConfig> conditions
) {
}
