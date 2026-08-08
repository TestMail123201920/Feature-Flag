package com.company.featureflag.rule.application;

import com.company.featureflag.common.error.FeatureNotFoundException;
import com.company.featureflag.common.error.RuleNotFoundException;
import com.company.featureflag.feature.application.FeatureVersionService;
import com.company.featureflag.feature.domain.Feature;
import com.company.featureflag.feature.domain.FeatureVersion;
import com.company.featureflag.feature.infrastructure.FeatureRepository;
import com.company.featureflag.outbox.domain.OutboxEvent;
import com.company.featureflag.outbox.infrastructure.OutboxEventRepository;
import com.company.featureflag.rule.api.dto.ConditionRequest;
import com.company.featureflag.rule.api.dto.CreateRuleRequest;
import com.company.featureflag.rule.api.dto.RuleResponse;
import com.company.featureflag.rule.api.dto.UpdateRuleRequest;
import com.company.featureflag.rule.domain.RuleCondition;
import com.company.featureflag.rule.domain.TargetingRule;
import com.company.featureflag.rule.infrastructure.TargetingRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every mutation here creates a brand-new active {@link FeatureVersion} that
 * clones the current one and applies the change (see
 * {@link FeatureVersionService}) — it never edits an existing version's rows
 * in place. Practical consequence: a rule's id is only stable until the next
 * mutation; callers should use the id returned by the mutation response (or
 * a fresh GET) rather than caching it indefinitely.
 */
@Service
public class RuleService {

    private final FeatureRepository featureRepository;
    private final FeatureVersionService featureVersionService;
    private final TargetingRuleRepository targetingRuleRepository;
    private final OutboxEventRepository outboxEventRepository;

    public RuleService(FeatureRepository featureRepository,
                        FeatureVersionService featureVersionService,
                        TargetingRuleRepository targetingRuleRepository,
                        OutboxEventRepository outboxEventRepository) {
        this.featureRepository = featureRepository;
        this.featureVersionService = featureVersionService;
        this.targetingRuleRepository = targetingRuleRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public RuleResponse addRule(String featureKey, CreateRuleRequest request, String actor) {
        Feature feature = findFeatureOrThrow(featureKey);
        FeatureVersion currentVersion = featureVersionService.findCurrentVersion(feature);

        assertNoDuplicatePriority(currentVersion, request.priority(), null);

        FeatureVersion newVersion = featureVersionService.createDraftVersion(
                feature.getId(), actor, Map.of("change", "rule_added", "priority", request.priority()));
        featureVersionService.copyRolloutStrategy(currentVersion, newVersion);
        featureVersionService.copyTargetingRules(currentVersion, newVersion, null);

        TargetingRule newRule = buildRule(newVersion.getId(), request.priority(), request.combinator(), request.conditions());
        targetingRuleRepository.save(newRule);

        featureVersionService.activateNewVersion(feature, newVersion, actor);

        outboxEventRepository.save(OutboxEvent.of(
                "RULE_ADDED", "FEATURE", feature.getId(),
                Map.of("featureKey", feature.getKey(), "priority", request.priority(),
                        "newVersion", newVersion.getVersionNumber())));

        return toResponse(newRule);
    }

    @Transactional
    public RuleResponse updateRule(String featureKey, UUID ruleId, UpdateRuleRequest request, String actor) {
        Feature feature = findFeatureOrThrow(featureKey);
        FeatureVersion currentVersion = featureVersionService.findCurrentVersion(feature);

        assertRuleExists(currentVersion, ruleId);
        assertNoDuplicatePriority(currentVersion, request.priority(), ruleId);

        FeatureVersion newVersion = featureVersionService.createDraftVersion(
                feature.getId(), actor, Map.of("change", "rule_updated", "ruleId", ruleId.toString()));
        featureVersionService.copyRolloutStrategy(currentVersion, newVersion);
        featureVersionService.copyTargetingRules(currentVersion, newVersion, ruleId);

        TargetingRule updatedRule = buildRule(newVersion.getId(), request.priority(), request.combinator(), request.conditions());
        if (!request.enabled()) {
            updatedRule.disable();
        }
        targetingRuleRepository.save(updatedRule);

        featureVersionService.activateNewVersion(feature, newVersion, actor);

        outboxEventRepository.save(OutboxEvent.of(
                "RULE_UPDATED", "FEATURE", feature.getId(),
                Map.of("featureKey", feature.getKey(), "ruleId", ruleId.toString(),
                        "newVersion", newVersion.getVersionNumber())));

        return toResponse(updatedRule);
    }

    @Transactional
    public void deleteRule(String featureKey, UUID ruleId, String actor) {
        Feature feature = findFeatureOrThrow(featureKey);
        FeatureVersion currentVersion = featureVersionService.findCurrentVersion(feature);

        assertRuleExists(currentVersion, ruleId);

        FeatureVersion newVersion = featureVersionService.createDraftVersion(
                feature.getId(), actor, Map.of("change", "rule_deleted", "ruleId", ruleId.toString()));
        featureVersionService.copyRolloutStrategy(currentVersion, newVersion);
        featureVersionService.copyTargetingRules(currentVersion, newVersion, ruleId);

        featureVersionService.activateNewVersion(feature, newVersion, actor);

        outboxEventRepository.save(OutboxEvent.of(
                "RULE_DELETED", "FEATURE", feature.getId(),
                Map.of("featureKey", feature.getKey(), "ruleId", ruleId.toString(),
                        "newVersion", newVersion.getVersionNumber())));
    }

    private TargetingRule buildRule(UUID versionId, int priority, com.company.featureflag.rule.domain.Combinator combinator,
                                     List<ConditionRequest> conditions) {
        TargetingRule rule = TargetingRule.of(versionId, priority, combinator);
        for (ConditionRequest c : conditions) {
            rule.addCondition(RuleCondition.of(rule, c.field(), c.operator(), c.value()));
        }
        return rule;
    }

    private void assertRuleExists(FeatureVersion version, UUID ruleId) {
        boolean exists = targetingRuleRepository.findByFeatureVersionIdOrderByPriorityAsc(version.getId()).stream()
                .anyMatch(r -> r.getId().equals(ruleId));
        if (!exists) {
            throw new RuleNotFoundException(ruleId.toString());
        }
    }

    private void assertNoDuplicatePriority(FeatureVersion version, int priority, UUID excludeRuleId) {
        boolean duplicate = targetingRuleRepository.findByFeatureVersionIdOrderByPriorityAsc(version.getId()).stream()
                .anyMatch(r -> r.getPriority() == priority && !r.getId().equals(excludeRuleId));
        if (duplicate) {
            throw new IllegalArgumentException("Priority %d is already used by another rule on this version".formatted(priority));
        }
    }

    private RuleResponse toResponse(TargetingRule rule) {
        List<RuleResponse.ConditionResponse> conditions = rule.getConditions().stream()
                .map(c -> new RuleResponse.ConditionResponse(c.getId(), c.getField(), c.getOperator(), c.getValue()))
                .toList();
        return new RuleResponse(rule.getId(), rule.getPriority(), rule.getCombinator(), rule.isEnabled(), conditions);
    }

    private Feature findFeatureOrThrow(String key) {
        return featureRepository.findByKey(key)
                .orElseThrow(() -> new FeatureNotFoundException(key));
    }
}
