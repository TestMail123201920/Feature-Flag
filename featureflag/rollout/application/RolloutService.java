package com.company.featureflag.rollout.application;

import com.company.featureflag.common.error.FeatureNotFoundException;
import com.company.featureflag.common.error.InvalidRolloutPercentageException;
import com.company.featureflag.feature.application.FeatureVersionService;
import com.company.featureflag.feature.domain.Feature;
import com.company.featureflag.feature.domain.FeatureVersion;
import com.company.featureflag.feature.infrastructure.FeatureRepository;
import com.company.featureflag.outbox.domain.OutboxEvent;
import com.company.featureflag.outbox.infrastructure.OutboxEventRepository;
import com.company.featureflag.rollout.api.dto.RolloutResponse;
import com.company.featureflag.rollout.api.dto.SetRolloutRequest;
import com.company.featureflag.rollout.domain.RolloutStrategy;
import com.company.featureflag.rollout.domain.StrategyType;
import com.company.featureflag.rollout.infrastructure.RolloutStrategyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * Sets or replaces the single rollout strategy for a feature. Like
 * {@link com.company.featureflag.rule.application.RuleService}, this always
 * produces a new active version (copy-on-write) rather than editing the
 * current version's row — see spec §6 and §7's own worked example (10% ->
 * 20% -> 50% as three separate versions). POST and PUT are intentionally
 * the same operation: there is at most one rollout strategy per version, so
 * "create" and "replace" collapse into "set".
 */
@Service
public class RolloutService {

    private static final Set<StrategyType> PERCENTAGE_TYPES = Set.of(
            StrategyType.IDENTIFIER_PERCENTAGE, StrategyType.REQUEST_PERCENTAGE, StrategyType.CUSTOM_FIELD_PERCENTAGE);
    private static final Set<StrategyType> FIELD_BASED_TYPES = Set.of(
            StrategyType.IDENTIFIER_PERCENTAGE, StrategyType.CUSTOM_FIELD_PERCENTAGE);

    private final FeatureRepository featureRepository;
    private final FeatureVersionService featureVersionService;
    private final RolloutStrategyRepository rolloutStrategyRepository;
    private final OutboxEventRepository outboxEventRepository;

    public RolloutService(FeatureRepository featureRepository,
                           FeatureVersionService featureVersionService,
                           RolloutStrategyRepository rolloutStrategyRepository,
                           OutboxEventRepository outboxEventRepository) {
        this.featureRepository = featureRepository;
        this.featureVersionService = featureVersionService;
        this.rolloutStrategyRepository = rolloutStrategyRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public RolloutResponse setRollout(String featureKey, SetRolloutRequest request, String actor) {
        validate(request);

        Feature feature = findFeatureOrThrow(featureKey);
        FeatureVersion currentVersion = featureVersionService.findCurrentVersion(feature);

        FeatureVersion newVersion = featureVersionService.createDraftVersion(
                feature.getId(), actor, Map.of("change", "rollout_set", "strategyType", request.strategyType().name()));
        featureVersionService.copyTargetingRules(currentVersion, newVersion, null);

        RolloutStrategy strategy = RolloutStrategy.of(
                newVersion, request.strategyType(), request.identifierField(),
                request.percentage(), request.strategyConfig() == null ? Map.of() : request.strategyConfig());
        rolloutStrategyRepository.save(strategy);

        featureVersionService.activateNewVersion(feature, newVersion, actor);

        outboxEventRepository.save(OutboxEvent.of(
                "ROLLOUT_CHANGED", "FEATURE", feature.getId(),
                Map.of("featureKey", feature.getKey(), "strategyType", request.strategyType().name(),
                        "percentage", request.percentage() == null ? -1 : request.percentage(),
                        "newVersion", newVersion.getVersionNumber())));

        return toResponse(strategy);
    }

    private void validate(SetRolloutRequest request) {
        if (PERCENTAGE_TYPES.contains(request.strategyType())) {
            if (request.percentage() == null) {
                throw new IllegalArgumentException(
                        "percentage is required for strategy type " + request.strategyType());
            }
            if (request.percentage() < 0 || request.percentage() > 100) {
                throw new InvalidRolloutPercentageException(request.percentage());
            }
        }
        if (FIELD_BASED_TYPES.contains(request.strategyType())
                && (request.identifierField() == null || request.identifierField().isBlank())) {
            throw new IllegalArgumentException(
                    "identifierField is required for strategy type " + request.strategyType());
        }
    }

    private RolloutResponse toResponse(RolloutStrategy strategy) {
        return new RolloutResponse(strategy.getId(), strategy.getStrategyType(),
                strategy.getIdentifierField(), strategy.getPercentage(), strategy.getStrategyConfig());
    }

    private Feature findFeatureOrThrow(String key) {
        return featureRepository.findByKey(key)
                .orElseThrow(() -> new FeatureNotFoundException(key));
    }
}
