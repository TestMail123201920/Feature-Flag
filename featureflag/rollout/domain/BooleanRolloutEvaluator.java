package com.company.featureflag.rollout.domain;

import com.company.featureflag.configuration.domain.RolloutConfig;
import com.company.featureflag.evaluation.domain.EvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Plain on/off — no bucketing needed, reached once kill switch/rules/dependencies pass. */
@Component
public class BooleanRolloutEvaluator implements RolloutEvaluator {

    @Override
    public Set<StrategyType> supportedTypes() {
        return Set.of(StrategyType.BOOLEAN);
    }

    @Override
    public boolean evaluate(RolloutConfig strategy, String featureKey, EvaluationContext context) {
        return true;
    }
}
