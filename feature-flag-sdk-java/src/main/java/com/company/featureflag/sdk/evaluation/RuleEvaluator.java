package com.company.featureflag.sdk.evaluation;

import com.company.featureflag.sdk.config.RuleConfig;
import com.company.featureflag.sdk.core.EvaluationContext;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class RuleEvaluator {

    private final ConditionEvaluator conditionEvaluator = new ConditionEvaluator();

    boolean matches(RuleConfig rule, EvaluationContext context) {
        if (rule.conditions().isEmpty()) {
            return false;
        }
        return switch (rule.combinator()) {
            case "AND" -> rule.conditions().stream().allMatch(c -> conditionEvaluator.evaluate(c, context));
            case "OR" -> rule.conditions().stream().anyMatch(c -> conditionEvaluator.evaluate(c, context));
            default -> throw new IllegalStateException("Unsupported combinator: " + rule.combinator());
        };
    }
}
