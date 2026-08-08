package com.company.featureflag.configuration.api;

import com.company.featureflag.configuration.api.dto.SdkConfigurationResponse;
import com.company.featureflag.configuration.application.FeatureConfigurationService;
import com.company.featureflag.configuration.domain.FeatureConfiguration;
import com.company.featureflag.feature.domain.Feature;
import com.company.featureflag.feature.domain.FeatureStatus;
import com.company.featureflag.feature.infrastructure.FeatureRepository;
import com.company.featureflag.outbox.infrastructure.OutboxEventRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * What the SDK polls (spec §31). Returns only ACTIVE features — DRAFT/ARCHIVED
 * features are never evaluated client-side and shipping them would just be
 * noise. Each feature entry is read through the same cache-aside
 * {@link FeatureConfigurationService} the evaluation pipeline uses, so a
 * warm Redis serves both.
 *
 * {@code configurationVersion} is a coarse global counter (total outbox
 * events emitted) rather than a per-feature version: it exists so the SDK
 * can cheaply tell "something changed since I last polled" without diffing
 * the whole payload; it is not meant to be precise about *what* changed.
 */
@RestController
@RequestMapping("/api/v1/sdk/configuration")
public class SdkConfigurationController {

    private final FeatureRepository featureRepository;
    private final FeatureConfigurationService featureConfigurationService;
    private final OutboxEventRepository outboxEventRepository;

    public SdkConfigurationController(FeatureRepository featureRepository,
                                       FeatureConfigurationService featureConfigurationService,
                                       OutboxEventRepository outboxEventRepository) {
        this.featureRepository = featureRepository;
        this.featureConfigurationService = featureConfigurationService;
        this.outboxEventRepository = outboxEventRepository;
    }

    @GetMapping
    public SdkConfigurationResponse getConfiguration() {
        List<FeatureConfiguration> features = featureRepository.findByStatus(FeatureStatus.ACTIVE, Pageable.unpaged())
                .map(Feature::getKey)
                .stream()
                .map(featureConfigurationService::getConfiguration)
                .flatMap(java.util.Optional::stream)
                .toList();

        long configurationVersion = outboxEventRepository.count();

        return new SdkConfigurationResponse(configurationVersion, features);
    }
}
