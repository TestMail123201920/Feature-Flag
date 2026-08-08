package com.company.featureflag.sdk.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RolloutConfig(
        String strategyType,
        String identifierField,
        Integer percentage,
        Map<String, Object> strategyConfig
) {
}
