package com.company.featureflag.configuration.domain;

public record ConditionConfig(
        String field,
        String operator,
        Object value
) {
}
