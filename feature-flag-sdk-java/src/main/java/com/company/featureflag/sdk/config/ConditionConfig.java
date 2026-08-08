package com.company.featureflag.sdk.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConditionConfig(
        String field,
        String operator,
        Object value
) {
}
