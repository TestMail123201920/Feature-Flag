package com.company.featureflag.rollout.domain;

/**
 * Discriminator for rollout strategies. New strategies (e.g. weighted
 * variants, geography) are added here plus a matching {@code RolloutEvaluator}
 * implementation resolved by {@code RolloutEvaluatorFactory} — the rest of
 * the pipeline is untouched.
 */
public enum StrategyType {
    BOOLEAN,
    IDENTIFIER_PERCENTAGE,
    REQUEST_PERCENTAGE,
    CUSTOM_FIELD_PERCENTAGE,
    TRAFFIC_CANARY
}
