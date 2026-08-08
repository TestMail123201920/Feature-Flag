package com.company.featureflag.rollout.domain;

import com.company.featureflag.configuration.domain.RolloutConfig;
import com.company.featureflag.evaluation.domain.EvaluationContext;

import java.util.Set;

/**
 * Strategy interface for rollout evaluation, resolved per-{@link StrategyType}
 * by {@link RolloutEvaluatorFactory}. Adding a new rollout strategy (e.g.
 * geography, weighted variants) means adding one implementation here — no
 * other code changes. Operates on the wire-format {@link RolloutConfig} so
 * the same logic is reusable by the future SDK evaluation engine.
 */
public interface RolloutEvaluator {

    Set<StrategyType> supportedTypes();

    boolean evaluate(RolloutConfig strategy, String featureKey, EvaluationContext context);
}
