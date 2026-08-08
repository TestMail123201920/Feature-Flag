package com.company.featureflag.configuration.domain;

import java.util.Map;

public record RolloutConfig(
        String strategyType,
        String identifierField,
        Integer percentage,
        Map<String, Object> strategyConfig
) {
}
