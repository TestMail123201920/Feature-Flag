package com.company.featureflag.sdk.evaluation;

import com.company.featureflag.sdk.cache.FeatureStore;
import com.company.featureflag.sdk.config.FeatureConfiguration;
import com.company.featureflag.sdk.config.RuleConfig;
import com.company.featureflag.sdk.core.EvaluationContext;
import com.company.featureflag.sdk.core.EvaluationReason;
import com.company.featureflag.sdk.core.EvaluationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Same pipeline shape as the central service's EvaluationEngine (spec §13),
 * running entirely against the in-memory {@link FeatureStore} snapshot —
 * no PostgreSQL, Redis, Kafka, or HTTP call on the hot path (spec §39).
 *
 * If no configuration has been synced yet (fresh startup or extended
 * outage with no cache), falls back to {@code fallbackEnabled} rather than
 * throwing (spec §26-27) — the host application must never fail because
 * feature-flag infrastructure is unavailable.
 */
public final class EvaluationEngine {

    private static final Logger log = LoggerFactory.getLogger(EvaluationEngine.class);

    private final FeatureStore featureStore;
    private final TargetingRuleMatcher targetingRuleMatcher = new TargetingRuleMatcher();
    private final RolloutEvaluator rolloutEvaluator = new RolloutEvaluator();
    private final boolean fallbackEnabled;

    public EvaluationEngine(FeatureStore featureStore, boolean fallbackEnabled) {
        this.featureStore = featureStore;
        this.fallbackEnabled = fallbackEnabled;
    }

    public EvaluationResult evaluate(String featureKey, EvaluationContext context) {
        return evaluate(featureKey, context, new HashSet<>());
    }

    private EvaluationResult evaluate(String featureKey, EvaluationContext context, Set<String> visiting) {
        try {
            if (!featureStore.hasConfiguration()) {
                return result(featureKey, fallbackEnabled, null, EvaluationReason.STALE_OR_MISSING_CONFIGURATION);
            }

            Optional<FeatureConfiguration> maybeConfig = featureStore.get(featureKey);
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

            boolean matched = rolloutEvaluator.evaluate(config.rollout(), featureKey, context);
            if ("BOOLEAN".equals(config.rollout().strategyType())) {
                return result(featureKey, matched, config.version(), EvaluationReason.DEFAULT);
            }
            EvaluationReason reason = matched ? EvaluationReason.PERCENTAGE_ROLLOUT_MATCH : EvaluationReason.PERCENTAGE_ROLLOUT_MISS;
            return result(featureKey, matched, config.version(), reason);

        } catch (Exception ex) {
            log.error("Local evaluation error for featureKey={}", featureKey, ex);
            return result(featureKey, fallbackEnabled, null, EvaluationReason.ERROR_FALLBACK);
        }
    }

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
        return EvaluationResult.of(featureKey, enabled, version, reason);
    }
}
