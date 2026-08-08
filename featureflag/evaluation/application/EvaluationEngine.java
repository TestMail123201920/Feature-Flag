package com.company.featureflag.evaluation.application;

import com.company.featureflag.configuration.application.FeatureConfigurationService;
import com.company.featureflag.configuration.domain.FeatureConfiguration;
import com.company.featureflag.configuration.domain.RuleConfig;
import com.company.featureflag.evaluation.domain.EvaluationContext;
import com.company.featureflag.evaluation.domain.EvaluationReason;
import com.company.featureflag.evaluation.domain.EvaluationResult;
import com.company.featureflag.rollout.domain.RolloutEvaluator;
import com.company.featureflag.rollout.domain.RolloutEvaluatorFactory;
import com.company.featureflag.rollout.domain.StrategyType;
import com.company.featureflag.rule.domain.TargetingRuleMatcher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Implements the pipeline from spec §13:
 * <pre>
 * lookup -> not found? -> kill switch? -> dependency check -> targeting
 * rules -> rollout strategy -> final decision
 * </pre>
 * Stateless: nothing here is persisted (no evaluation table, spec §4). Reads
 * exclusively through {@link FeatureConfigurationService} (cache-aside over
 * Redis/PostgreSQL, Phase 9) rather than querying repositories directly, so
 * a warm cache serves the whole pipeline — including recursive dependency
 * checks — from a single Redis round trip per feature instead of four or
 * five Postgres queries.
 *
 * Never logs raw {@link EvaluationContext} values (spec §37) — only
 * featureKey/version/decision/reason.
 */
@Service
public class EvaluationEngine {

    private static final Logger log = LoggerFactory.getLogger(EvaluationEngine.class);

    private final FeatureConfigurationService featureConfigurationService;
    private final TargetingRuleMatcher targetingRuleMatcher;
    private final RolloutEvaluatorFactory rolloutEvaluatorFactory;
    private final MeterRegistry meterRegistry;
    private final Timer evaluationTimer;

    public EvaluationEngine(FeatureConfigurationService featureConfigurationService,
                             TargetingRuleMatcher targetingRuleMatcher,
                             RolloutEvaluatorFactory rolloutEvaluatorFactory,
                             MeterRegistry meterRegistry) {
        this.featureConfigurationService = featureConfigurationService;
        this.targetingRuleMatcher = targetingRuleMatcher;
        this.rolloutEvaluatorFactory = rolloutEvaluatorFactory;
        this.meterRegistry = meterRegistry;
        this.evaluationTimer = Timer.builder("feature_flag.evaluation.duration")
                .description("Time spent in EvaluationEngine.evaluate(), including recursive dependency checks")
                .register(meterRegistry);
    }

    public EvaluationResult evaluate(String featureKey, EvaluationContext context) {
        return evaluationTimer.record(() -> evaluate(featureKey, context, new HashSet<>()));
    }

    private EvaluationResult evaluate(String featureKey, EvaluationContext context, Set<String> visiting) {
        try {
            Optional<FeatureConfiguration> maybeConfig = featureConfigurationService.getConfiguration(featureKey);
            if (maybeConfig.isEmpty()) {
                return result(featureKey, false, null, EvaluationReason.FEATURE_NOT_FOUND);
            }
            FeatureConfiguration config = maybeConfig.get();

            if (!"ACTIVE".equals(config.status())) {
                return result(featureKey, false, config.version(), EvaluationReason.FEATURE_DISABLED);
            }
            if (config.killSwitch()) {
                return result(featureKey, false, config.version(), EvaluationReason.KILL_SWITCH);
            }

            if (!dependenciesSatisfied(config, context, visiting)) {
                return result(featureKey, false, config.version(), EvaluationReason.DEPENDENCY_FAILED);
            }

            Optional<RuleConfig> matchedRule = targetingRuleMatcher.findFirstMatch(config.rules(), context);
            if (matchedRule.isPresent()) {
                return result(featureKey, true, config.version(), EvaluationReason.RULE_MATCH);
            }

            if (config.rollout() == null) {
                EvaluationReason reason = config.rules().isEmpty() ? EvaluationReason.DEFAULT : EvaluationReason.RULE_NO_MATCH;
                return result(featureKey, false, config.version(), reason);
            }

            StrategyType strategyType = StrategyType.valueOf(config.rollout().strategyType());
            RolloutEvaluator evaluator = rolloutEvaluatorFactory.resolve(strategyType);
            boolean matched = evaluator.evaluate(config.rollout(), featureKey, context);

            if (strategyType == StrategyType.BOOLEAN) {
                return result(featureKey, matched, config.version(), EvaluationReason.DEFAULT);
            }
            EvaluationReason reason = matched ? EvaluationReason.PERCENTAGE_ROLLOUT_MATCH : EvaluationReason.PERCENTAGE_ROLLOUT_MISS;
            return result(featureKey, matched, config.version(), reason);

        } catch (Exception ex) {
            log.error("Evaluation error for featureKey={}", featureKey, ex);
            return result(featureKey, false, null, EvaluationReason.ERROR_FALLBACK);
        }
    }

    /**
     * Recursively evaluates REQUIRES_ENABLED dependencies by feature key —
     * no separate repository lookups needed since {@code dependsOnFeatureKeys}
     * is already part of the cached configuration. Cycles are rejected at
     * write time by {@code DependencyGraphValidator}; this per-call
     * {@code visiting} set is defense in depth against evaluating a cycle
     * that somehow made it into the data.
     */
    private boolean dependenciesSatisfied(FeatureConfiguration config, EvaluationContext context, Set<String> visiting) {
        if (config.dependsOnFeatureKeys().isEmpty()) {
            return true;
        }
        if (!visiting.add(config.key())) {
            log.warn("Dependency cycle detected at evaluation time for featureKey={}", config.key());
            return false;
        }
        try {
            for (String dependsOnKey : config.dependsOnFeatureKeys()) {
                if (!evaluate(dependsOnKey, context, visiting).enabled()) {
                    return false;
                }
            }
            return true;
        } finally {
            visiting.remove(config.key());
        }
    }

    private EvaluationResult result(String featureKey, boolean enabled, Integer version, EvaluationReason reason) {
        log.debug("evaluation featureKey={} version={} decision={} reason={}", featureKey, version, enabled, reason);
        meterRegistry.counter("feature_flag.evaluations.total",
                "reason", reason.name(), "decision", String.valueOf(enabled)).increment();
        return EvaluationResult.of(featureKey, enabled, version, reason);
    }
}
