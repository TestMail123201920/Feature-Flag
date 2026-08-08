package com.company.featureflag.feature.application;

import com.company.featureflag.common.error.FeatureNotFoundException;
import com.company.featureflag.common.error.FeatureVersionNotFoundException;
import com.company.featureflag.configuration.application.FeatureConfigurationInvalidator;
import com.company.featureflag.feature.api.dto.FeatureVersionDetailResponse;
import com.company.featureflag.feature.api.dto.FeatureVersionSummaryResponse;
import com.company.featureflag.feature.domain.Feature;
import com.company.featureflag.feature.domain.FeatureVersion;
import com.company.featureflag.feature.infrastructure.FeatureRepository;
import com.company.featureflag.feature.infrastructure.FeatureVersionRepository;
import com.company.featureflag.outbox.domain.OutboxEvent;
import com.company.featureflag.outbox.infrastructure.OutboxEventRepository;
import com.company.featureflag.rollout.domain.RolloutStrategy;
import com.company.featureflag.rollout.infrastructure.RolloutStrategyRepository;
import com.company.featureflag.rule.domain.RuleCondition;
import com.company.featureflag.rule.domain.TargetingRule;
import com.company.featureflag.rule.infrastructure.TargetingRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the "copy-on-write" mechanics of feature versioning: every meaningful
 * config change (rollback, a rule added/updated/removed, rollout changed)
 * creates a brand-new immutable {@link FeatureVersion} by cloning the
 * current one and applying the change, rather than mutating an existing
 * version in place (spec §6). {@link com.company.featureflag.rule.application.RuleService}
 * and {@link com.company.featureflag.rollout.application.RolloutService}
 * both build on the helpers here rather than duplicating the copy logic.
 */
@Service
public class FeatureVersionService {

    private final FeatureRepository featureRepository;
    private final FeatureVersionRepository featureVersionRepository;
    private final RolloutStrategyRepository rolloutStrategyRepository;
    private final TargetingRuleRepository targetingRuleRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final FeatureConfigurationInvalidator cacheInvalidator;

    public FeatureVersionService(FeatureRepository featureRepository,
                                  FeatureVersionRepository featureVersionRepository,
                                  RolloutStrategyRepository rolloutStrategyRepository,
                                  TargetingRuleRepository targetingRuleRepository,
                                  OutboxEventRepository outboxEventRepository,
                                  FeatureConfigurationInvalidator cacheInvalidator) {
        this.featureRepository = featureRepository;
        this.featureVersionRepository = featureVersionRepository;
        this.rolloutStrategyRepository = rolloutStrategyRepository;
        this.targetingRuleRepository = targetingRuleRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.cacheInvalidator = cacheInvalidator;
    }

    @Transactional
    public FeatureVersion createDraftVersion(UUID featureId, String actor, Map<String, Object> metadata) {
        int nextVersionNumber = featureVersionRepository.findMaxVersionNumberByFeatureId(featureId)
                .orElse(0) + 1;
        FeatureVersion version = FeatureVersion.draft(featureId, nextVersionNumber, actor, metadata);
        return featureVersionRepository.save(version);
    }

    @Transactional(readOnly = true)
    public List<FeatureVersionSummaryResponse> listVersions(String featureKey) {
        Feature feature = findFeatureOrThrow(featureKey);
        return featureVersionRepository.findByFeatureIdOrderByVersionNumberDesc(feature.getId()).stream()
                .map(v -> new FeatureVersionSummaryResponse(
                        v.getId(), v.getVersionNumber(), v.getStatus(), v.getCreatedAt(), v.getCreatedBy()))
                .toList();
    }

    @Transactional(readOnly = true)
    public FeatureVersionDetailResponse getVersion(String featureKey, int versionNumber) {
        Feature feature = findFeatureOrThrow(featureKey);
        FeatureVersion version = findVersionOrThrow(feature, versionNumber);
        return toDetailResponse(version);
    }

    /**
     * Rollback does NOT rewind the version pointer to the old row — it
     * creates a brand-new version whose rollout/rule configuration is a deep
     * copy of the target version's, then activates it. This keeps version
     * numbers monotonically increasing (per spec §6) while still achieving
     * "go back to what version 2 looked like".
     */
    @Transactional
    public FeatureVersionDetailResponse rollback(String featureKey, int targetVersionNumber, String actor) {
        Feature feature = findFeatureOrThrow(featureKey);
        FeatureVersion targetVersion = findVersionOrThrow(feature, targetVersionNumber);

        FeatureVersion newVersion = createDraftVersion(
                feature.getId(), actor, Map.of("rolledBackFrom", targetVersionNumber));
        copyRolloutStrategy(targetVersion, newVersion);
        copyTargetingRules(targetVersion, newVersion, null);
        activateNewVersion(feature, newVersion, actor);

        outboxEventRepository.save(OutboxEvent.of(
                "FEATURE_ROLLED_BACK", "FEATURE", feature.getId(),
                Map.of("featureKey", feature.getKey(), "rolledBackFrom", targetVersionNumber,
                        "newVersion", newVersion.getVersionNumber())));

        return toDetailResponse(newVersion);
    }

    /** The version a feature is currently pointing at (may be DRAFT or ACTIVE). */
    @Transactional(readOnly = true)
    public FeatureVersion findCurrentVersion(Feature feature) {
        return findVersionOrThrow(feature, feature.getCurrentVersion());
    }

    /** Copies {@code source}'s rollout strategy (if any) onto {@code target}. Skipped if source has none. */
    @Transactional
    public void copyRolloutStrategy(FeatureVersion source, FeatureVersion target) {
        rolloutStrategyRepository.findByFeatureVersion_Id(source.getId()).ifPresent(strategy ->
                rolloutStrategyRepository.save(RolloutStrategy.of(
                        target, strategy.getStrategyType(), strategy.getIdentifierField(),
                        strategy.getPercentage(), strategy.getStrategyConfig())));
    }

    /**
     * Copies all of {@code source}'s targeting rules (with conditions) onto
     * {@code target}, skipping the rule whose id equals {@code excludeRuleId}
     * (used when updating/deleting a specific rule — the caller re-adds the
     * updated version itself, or omits it entirely for a delete). Pass null
     * to copy everything unchanged.
     */
    @Transactional
    public List<TargetingRule> copyTargetingRules(FeatureVersion source, FeatureVersion target, UUID excludeRuleId) {
        List<TargetingRule> copies = new java.util.ArrayList<>();
        for (TargetingRule sourceRule : targetingRuleRepository.findByFeatureVersionIdOrderByPriorityAsc(source.getId())) {
            if (excludeRuleId != null && excludeRuleId.equals(sourceRule.getId())) {
                continue;
            }
            TargetingRule copiedRule = TargetingRule.of(target.getId(), sourceRule.getPriority(), sourceRule.getCombinator());
            if (!sourceRule.isEnabled()) {
                copiedRule.disable();
            }
            for (RuleCondition sourceCondition : sourceRule.getConditions()) {
                copiedRule.addCondition(RuleCondition.of(
                        copiedRule, sourceCondition.getField(), sourceCondition.getOperator(), sourceCondition.getValue()));
            }
            copies.add(targetingRuleRepository.save(copiedRule));
        }
        return copies;
    }

    /** Activates {@code newVersion}, supersedes whatever the feature currently points at, and moves the pointer. */
    @Transactional
    public void activateNewVersion(Feature feature, FeatureVersion newVersion, String actor) {
        featureVersionRepository.findByFeatureIdAndVersionNumber(feature.getId(), feature.getCurrentVersion())
                .ifPresent(FeatureVersion::supersede);
        newVersion.activate();
        featureVersionRepository.save(newVersion);
        feature.pointToVersion(newVersion.getVersionNumber(), actor);
        featureRepository.save(feature);
        cacheInvalidator.evictAfterCommit(feature.getKey());
    }

    FeatureVersionDetailResponse toDetailResponse(FeatureVersion version) {
        FeatureVersionDetailResponse.RolloutSummary rolloutSummary = rolloutStrategyRepository
                .findByFeatureVersion_Id(version.getId())
                .map(r -> new FeatureVersionDetailResponse.RolloutSummary(
                        r.getStrategyType(), r.getIdentifierField(), r.getPercentage()))
                .orElse(null);

        List<FeatureVersionDetailResponse.RuleSummary> ruleSummaries = targetingRuleRepository
                .findByFeatureVersionIdOrderByPriorityAsc(version.getId()).stream()
                .map(r -> new FeatureVersionDetailResponse.RuleSummary(
                        r.getPriority(), r.getCombinator().name(), r.isEnabled(), r.getConditions().size()))
                .toList();

        return new FeatureVersionDetailResponse(
                version.getId(), version.getVersionNumber(), version.getStatus(),
                version.getCreatedAt(), version.getCreatedBy(), version.getConfigurationMetadata(),
                rolloutSummary, ruleSummaries);
    }

    Feature findFeatureOrThrow(String key) {
        return featureRepository.findByKey(key)
                .orElseThrow(() -> new FeatureNotFoundException(key));
    }

    FeatureVersion findVersionOrThrow(Feature feature, int versionNumber) {
        return featureVersionRepository.findByFeatureIdAndVersionNumber(feature.getId(), versionNumber)
                .orElseThrow(() -> new FeatureVersionNotFoundException(feature.getKey(), versionNumber));
    }
}
