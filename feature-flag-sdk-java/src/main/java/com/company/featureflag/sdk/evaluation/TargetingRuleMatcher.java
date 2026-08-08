package com.company.featureflag.sdk.evaluation;

import com.company.featureflag.sdk.config.RuleConfig;
import com.company.featureflag.sdk.core.EvaluationContext;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class TargetingRuleMatcher {

    private final RuleEvaluator ruleEvaluator = new RuleEvaluator();

    Optional<RuleConfig> findFirstMatch(List<RuleConfig> rules, EvaluationContext context) {
        return rules.stream()
                .filter(RuleConfig::enabled)
                .sorted(Comparator.comparingInt(RuleConfig::priority))
                .filter(rule -> ruleEvaluator.matches(rule, context))
                .findFirst();
    }
}
