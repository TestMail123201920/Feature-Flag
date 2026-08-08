package com.company.featureflag.configuration.application;

import com.company.featureflag.configuration.domain.ConditionConfig;
import com.company.featureflag.configuration.domain.FeatureConfiguration;
import com.company.featureflag.configuration.domain.RolloutConfig;
import com.company.featureflag.configuration.domain.RuleConfig;
import com.company.featureflag.dependency.infrastructure.FeatureDependencyRepository;
import com.company.featureflag.feature.domain.Feature;
import com.company.featureflag.feature.domain.FeatureVersion;
import com.company.featureflag.feature.infrastructure.FeatureRepository;
import com.company.featureflag.feature.infrastructure.FeatureVersionRepository;
import com.company.featureflag.rollout.infrastructure.RolloutStrategyRepository;
import com.company.featureflag.rule.infrastructure.TargetingRuleRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * The one place that translates persisted JPA entities (Feature,
 * FeatureVersion, RolloutStrategy, TargetingRule, RuleCondition,
 * FeatureDependency) into the flat {@link FeatureConfiguration} wire
 * format. Runs on a cache miss (see {@link FeatureConfigurationService})
 * and, in Phase 11, backs the SDK-facing configuration endpoint directly.
 */
@Component
public class FeatureConfigurationAssembler {

    private final FeatureRepository featureRepository;
    private final FeatureVersionRepository featureVersionRepository;
    private final RolloutStrategyRepository rolloutStrategyRepository;
    private final TargetingRuleRepository targetingRuleRepository;
    private final FeatureDependencyRepository featureDependencyRepository;

    public FeatureConfigurationAssembler(FeatureRepository featureRepository,
                                          FeatureVersionRepository featureVersionRepository,
                                          RolloutStrategyRepository rolloutStrategyRepository,
                                          TargetingRuleRepository targetingRuleRepository,
                                          FeatureDependencyRepository featureDependencyRepository) {
        this.featureRepository = featureRepository;
        this.featureVersionRepository = featureVersionRepository;
        this.rolloutStrategyRepository = rolloutStrategyRepository;
        this.targetingRuleRepository = targetingRuleRepository;
        this.featureDependencyRepository = featureDependencyRepository;
    }

    public FeatureConfiguration assemble(Feature feature) {
        FeatureVersion version = featureVersionRepository
                .findByFeatureIdAndVersionNumber(feature.getId(), feature.getCurrentVersion())
                .orElse(null);

        RolloutConfig rollout = version == null ? null : rolloutStrategyRepository
                .findByFeatureVersion_Id(version.getId())
                .map(r -> new RolloutConfig(r.getStrategyType().name(), r.getIdentifierField(), r.getPercentage(), r.getStrategyConfig()))
                .orElse(null);

        List<RuleConfig> rules = version == null ? List.of() : targetingRuleRepository
                .findByFeatureVersionIdOrderByPriorityAsc(version.getId()).stream()
                .map(rule -> new RuleConfig(
                        rule.getPriority(),
                        rule.getCombinator().name(),
                        rule.isEnabled(),
                        rule.getConditions().stream()
                                .map(c -> new ConditionConfig(c.getField(), c.getOperator().name(), c.getValue()))
                                .toList()))
                .toList();

        List<String> dependsOnFeatureKeys = featureDependencyRepository.findByFeatureId(feature.getId()).stream()
                .map(dependency -> featureRepository.findById(dependency.getDependsOnFeatureId()).map(Feature::getKey).orElse(null))
                .filter(Objects::nonNull)
                .toList();

        return new FeatureConfiguration(
                feature.getKey(), feature.getCurrentVersion(), feature.getStatus().name(),
                feature.isKillSwitch(), rollout, rules, dependsOnFeatureKeys);
    }
}
