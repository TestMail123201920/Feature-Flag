package com.company.featureflag.sdk.evaluation;

import com.company.featureflag.sdk.config.RolloutConfig;
import com.company.featureflag.sdk.core.EvaluationContext;

final class RolloutEvaluator {

    private static final String REQUEST_ID_FIELD = "requestId";

    private final BucketStrategy bucketStrategy = new BucketStrategy();

    boolean evaluate(RolloutConfig strategy, String featureKey, EvaluationContext context) {
        return switch (strategy.strategyType()) {
            case "BOOLEAN" -> true;
            case "IDENTIFIER_PERCENTAGE", "CUSTOM_FIELD_PERCENTAGE" -> evaluateFieldPercentage(strategy, featureKey, context);
            case "REQUEST_PERCENTAGE" -> evaluateRequestPercentage(strategy, featureKey, context);
            case "TRAFFIC_CANARY" -> throw new UnsupportedOperationException(
                    "TRAFFIC_CANARY is reserved for a future integration and is not evaluated locally");
            default -> throw new IllegalStateException("Unsupported strategy type: " + strategy.strategyType());
        };
    }

    private boolean evaluateFieldPercentage(RolloutConfig strategy, String featureKey, EvaluationContext context) {
        if (strategy.identifierField() == null || strategy.percentage() == null) {
            return false;
        }
        return context.getAsString(strategy.identifierField())
                .map(identifier -> bucketStrategy.bucket(featureKey, identifier) < strategy.percentage())
                .orElse(false);
    }

    private boolean evaluateRequestPercentage(RolloutConfig strategy, String featureKey, EvaluationContext context) {
        if (strategy.percentage() == null) {
            return false;
        }
        return context.getAsString(REQUEST_ID_FIELD)
                .map(requestId -> bucketStrategy.bucket(featureKey, requestId) < strategy.percentage())
                .orElse(false);
    }
}
