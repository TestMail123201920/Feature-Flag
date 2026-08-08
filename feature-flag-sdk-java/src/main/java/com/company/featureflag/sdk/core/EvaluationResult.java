package com.company.featureflag.sdk.core;

public record EvaluationResult(String featureKey, boolean enabled, Integer version, EvaluationReason reason) {

    public static EvaluationResult of(String featureKey, boolean enabled, Integer version, EvaluationReason reason) {
        return new EvaluationResult(featureKey, enabled, version, reason);
    }
}
