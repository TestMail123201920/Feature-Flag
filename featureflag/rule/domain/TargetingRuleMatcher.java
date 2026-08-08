package com.company.featureflag.rule.domain;

import com.company.featureflag.configuration.domain.RuleConfig;
import com.company.featureflag.evaluation.domain.EvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Rules are evaluated in ascending priority order (priority 1 first); the
 * first enabled rule whose conditions match wins. Disabled rules are
 * skipped entirely. If no rule matches, the caller (evaluation pipeline)
 * falls through to the rollout strategy. This "first match wins" semantic
 * is the documented behavior for multiple matching rules (spec §12).
 */
@Component
public class TargetingRuleMatcher {

    private final RuleEvaluator ruleEvaluator;

    public TargetingRuleMatcher(RuleEvaluator ruleEvaluator) {
        this.ruleEvaluator = ruleEvaluator;
    }

    public Optional<RuleConfig> findFirstMatch(List<RuleConfig> rules, EvaluationContext context) {
        return rules.stream()
                .filter(RuleConfig::enabled)
                .sorted(Comparator.comparingInt(RuleConfig::priority))
                .filter(rule -> ruleEvaluator.matches(rule, context))
                .findFirst();
    }
}
