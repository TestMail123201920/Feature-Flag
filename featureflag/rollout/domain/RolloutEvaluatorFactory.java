package com.company.featureflag.rollout.domain;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the {@link RolloutEvaluator} for a given {@link StrategyType}.
 * New strategies plug in purely by existing as a Spring bean implementing
 * {@link RolloutEvaluator} — this factory never needs a code change for a
 * new type, only a new evaluator bean declaring which type(s) it supports.
 */
@Component
public class RolloutEvaluatorFactory {

    private final Map<StrategyType, RolloutEvaluator> evaluatorsByType = new EnumMap<>(StrategyType.class);

    public RolloutEvaluatorFactory(List<RolloutEvaluator> evaluators) {
        for (RolloutEvaluator evaluator : evaluators) {
            for (StrategyType type : evaluator.supportedTypes()) {
                evaluatorsByType.put(type, evaluator);
            }
        }
    }

    public RolloutEvaluator resolve(StrategyType type) {
        RolloutEvaluator evaluator = evaluatorsByType.get(type);
        if (evaluator == null) {
            throw new UnsupportedOperationException(
                    "No rollout evaluator registered for strategy type " + type
                            + " (TRAFFIC_CANARY is reserved for a future integration and not evaluated locally)");
        }
        return evaluator;
    }
}
