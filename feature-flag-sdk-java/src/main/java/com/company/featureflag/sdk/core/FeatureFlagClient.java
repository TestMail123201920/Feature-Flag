package com.company.featureflag.sdk.core;

/** The only API most consumers need. See spec §23. */
public interface FeatureFlagClient {

    boolean isEnabled(String featureKey);

    boolean isEnabled(String featureKey, EvaluationContext context);

    EvaluationResult evaluate(String featureKey, EvaluationContext context);
}
