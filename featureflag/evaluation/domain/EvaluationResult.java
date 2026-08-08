package com.company.featureflag.evaluation.domain;

/**
 * The output of one evaluation. Deliberately never persisted (spec §4/§13 —
 * evaluation is a stateless process, no evaluation table).
 */
public record EvaluationResult(String featureKey, boolean enabled, Integer version, EvaluationReason reason) {

    public static EvaluationResult of(String featureKey, boolean enabled, Integer version, EvaluationReason reason) {
        return new EvaluationResult(featureKey, enabled, version, reason);
    }
}
