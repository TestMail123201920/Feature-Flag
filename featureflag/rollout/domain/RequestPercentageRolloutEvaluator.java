package com.company.featureflag.rollout.domain;

import com.company.featureflag.configuration.domain.RolloutConfig;
import com.company.featureflag.evaluation.domain.EvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Buckets on {@code requestId} (or another request-scoped identifier passed
 * under that key) rather than a stable user identifier. This is traffic
 * sampling, not a guarantee of exactly N% of every 100 requests — see
 * spec §9. Never buckets on pod name/IP (spec §44-45): those are ephemeral
 * and would make the "same request" bucket differently across retries
 * routed to different pods.
 */
@Component
public class RequestPercentageRolloutEvaluator implements RolloutEvaluator {

    private static final String REQUEST_ID_FIELD = "requestId";

    private final BucketStrategy bucketStrategy;

    public RequestPercentageRolloutEvaluator(BucketStrategy bucketStrategy) {
        this.bucketStrategy = bucketStrategy;
    }

    @Override
    public Set<StrategyType> supportedTypes() {
        return Set.of(StrategyType.REQUEST_PERCENTAGE);
    }

    @Override
    public boolean evaluate(RolloutConfig strategy, String featureKey, EvaluationContext context) {
        if (strategy.percentage() == null) {
            return false;
        }
        return context.getAsString(REQUEST_ID_FIELD)
                .map(requestId -> bucketStrategy.bucket(featureKey, requestId) < strategy.percentage())
                .orElse(false);
    }
}
