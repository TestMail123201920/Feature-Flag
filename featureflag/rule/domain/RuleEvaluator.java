package com.company.featureflag.rule.domain;

import com.company.featureflag.configuration.domain.RuleConfig;
import com.company.featureflag.evaluation.domain.EvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Evaluates a single {@link RuleConfig}: all (AND) or any (OR) of its
 * conditions must match, per {@code rule.combinator}. Negation is expressed
 * at the condition level (NOT_EQUALS, NOT_IN, NOT_EXISTS) rather than a
 * rule-level NOT, matching the operator set actually persisted.
 */
@Component
public class RuleEvaluator {

    private final ConditionEvaluator conditionEvaluator;

    public RuleEvaluator(ConditionEvaluator conditionEvaluator) {
        this.conditionEvaluator = conditionEvaluator;
    }

    public boolean matches(RuleConfig rule, EvaluationContext context) {
        if (rule.conditions().isEmpty()) {
            return false;
        }
        return switch (Combinator.valueOf(rule.combinator())) {
            case AND -> rule.conditions().stream().allMatch(c -> conditionEvaluator.evaluate(c, context));
            case OR -> rule.conditions().stream().anyMatch(c -> conditionEvaluator.evaluate(c, context));
        };
    }
}
