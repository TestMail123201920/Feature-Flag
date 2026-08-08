package com.company.featureflag.rollout.domain;

import com.company.featureflag.configuration.domain.RolloutConfig;
import com.company.featureflag.evaluation.domain.EvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Mechanically identical for IDENTIFIER_PERCENTAGE and CUSTOM_FIELD_PERCENTAGE:
 * both bucket on {@code strategy.identifierField} read dynamically from the
 * context (spec §10 — "SDK should dynamically obtain the configured field").
 * A missing field is a miss, not an error — an unauthenticated/anonymous
 * caller simply doesn't get the rollout rather than the request failing.
 */
@Component
public class FieldPercentageRolloutEvaluator implements RolloutEvaluator {

    private final BucketStrategy bucketStrategy;

    public FieldPercentageRolloutEvaluator(BucketStrategy bucketStrategy) {
        this.bucketStrategy = bucketStrategy;
    }

    @Override
    public Set<StrategyType> supportedTypes() {
        return Set.of(StrategyType.IDENTIFIER_PERCENTAGE, StrategyType.CUSTOM_FIELD_PERCENTAGE);
    }

    @Override
    public boolean evaluate(RolloutConfig strategy, String featureKey, EvaluationContext context) {
        if (strategy.identifierField() == null || strategy.percentage() == null) {
            return false;
        }
        return context.getAsString(strategy.identifierField())
                .map(identifier -> bucketStrategy.bucket(featureKey, identifier) < strategy.percentage())
                .orElse(false);
    }
}
