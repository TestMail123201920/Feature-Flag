package com.company.featureflag.dependency.application;

import com.company.featureflag.common.error.CircularDependencyException;
import com.company.featureflag.common.error.FeatureNotFoundException;
import com.company.featureflag.configuration.application.FeatureConfigurationInvalidator;
import com.company.featureflag.dependency.api.dto.CreateDependencyRequest;
import com.company.featureflag.dependency.api.dto.DependencyResponse;
import com.company.featureflag.dependency.domain.DependencyType;
import com.company.featureflag.dependency.domain.FeatureDependency;
import com.company.featureflag.dependency.infrastructure.FeatureDependencyRepository;
import com.company.featureflag.feature.domain.Feature;
import com.company.featureflag.feature.infrastructure.FeatureRepository;
import com.company.featureflag.outbox.domain.OutboxEvent;
import com.company.featureflag.outbox.infrastructure.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DependencyService {

    private final FeatureRepository featureRepository;
    private final FeatureDependencyRepository featureDependencyRepository;
    private final DependencyGraphValidator dependencyGraphValidator;
    private final OutboxEventRepository outboxEventRepository;
    private final FeatureConfigurationInvalidator cacheInvalidator;

    public DependencyService(FeatureRepository featureRepository,
                              FeatureDependencyRepository featureDependencyRepository,
                              DependencyGraphValidator dependencyGraphValidator,
                              OutboxEventRepository outboxEventRepository,
                              FeatureConfigurationInvalidator cacheInvalidator) {
        this.featureRepository = featureRepository;
        this.featureDependencyRepository = featureDependencyRepository;
        this.dependencyGraphValidator = dependencyGraphValidator;
        this.outboxEventRepository = outboxEventRepository;
        this.cacheInvalidator = cacheInvalidator;
    }

    @Transactional
    public DependencyResponse addDependency(String featureKey, CreateDependencyRequest request, String actor) {
        Feature feature = findFeatureOrThrow(featureKey);
        Feature dependsOn = findFeatureOrThrow(request.dependsOnFeatureKey());
        DependencyType type = request.dependencyType() == null ? DependencyType.REQUIRES_ENABLED : request.dependencyType();

        List<FeatureDependency> existingEdges = featureDependencyRepository.findAll();

        boolean alreadyExists = existingEdges.stream()
                .anyMatch(d -> d.getFeatureId().equals(feature.getId()) && d.getDependsOnFeatureId().equals(dependsOn.getId()));
        if (alreadyExists) {
            throw new IllegalArgumentException(
                    "Feature '%s' already depends on '%s'".formatted(featureKey, request.dependsOnFeatureKey()));
        }

        if (dependencyGraphValidator.wouldCreateCycle(feature.getId(), dependsOn.getId(), existingEdges)) {
            throw new CircularDependencyException(featureKey, request.dependsOnFeatureKey());
        }

        FeatureDependency dependency = FeatureDependency.of(feature.getId(), dependsOn.getId(), type);
        featureDependencyRepository.save(dependency);

        outboxEventRepository.save(OutboxEvent.of(
                "DEPENDENCY_ADDED", "FEATURE", feature.getId(),
                Map.of("featureKey", featureKey, "dependsOnFeatureKey", request.dependsOnFeatureKey())));
        cacheInvalidator.evictAfterCommit(featureKey);

        return new DependencyResponse(dependency.getId(), featureKey, request.dependsOnFeatureKey(), type, dependency.getCreatedAt());
    }

    private Feature findFeatureOrThrow(String key) {
        return featureRepository.findByKey(key)
                .orElseThrow(() -> new FeatureNotFoundException(key));
    }
}
