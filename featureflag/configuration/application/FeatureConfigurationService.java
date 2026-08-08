package com.company.featureflag.configuration.application;

import com.company.featureflag.configuration.domain.FeatureConfiguration;
import com.company.featureflag.configuration.infrastructure.FeatureConfigurationCache;
import com.company.featureflag.feature.domain.Feature;
import com.company.featureflag.feature.infrastructure.FeatureRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Cache-aside read path: Redis hit returns immediately; a miss falls
 * through to PostgreSQL (via {@link FeatureConfigurationAssembler}) and
 * warms the cache for next time. This is the read path used by both the
 * evaluation pipeline and the SDK configuration endpoint, so a warm cache
 * serves both without hitting Postgres per request.
 */
@Service
public class FeatureConfigurationService {

    private final FeatureRepository featureRepository;
    private final FeatureConfigurationAssembler assembler;
    private final FeatureConfigurationCache cache;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;

    public FeatureConfigurationService(FeatureRepository featureRepository,
                                        FeatureConfigurationAssembler assembler,
                                        FeatureConfigurationCache cache,
                                        MeterRegistry meterRegistry) {
        this.featureRepository = featureRepository;
        this.assembler = assembler;
        this.cache = cache;
        this.cacheHitCounter = Counter.builder("feature_flag.cache.hit")
                .description("Feature configuration served from Redis without a Postgres read")
                .register(meterRegistry);
        this.cacheMissCounter = Counter.builder("feature_flag.cache.miss")
                .description("Feature configuration assembled from Postgres and (re)cached")
                .register(meterRegistry);
    }

    @Transactional(readOnly = true)
    public Optional<FeatureConfiguration> getConfiguration(String featureKey) {
        Optional<FeatureConfiguration> cached = cache.get(featureKey);
        if (cached.isPresent()) {
            cacheHitCounter.increment();
            return cached;
        }
        cacheMissCounter.increment();

        Optional<Feature> feature = featureRepository.findByKey(featureKey);
        if (feature.isEmpty()) {
            return Optional.empty();
        }

        FeatureConfiguration assembled = assembler.assemble(feature.get());
        cache.put(assembled);
        return Optional.of(assembled);
    }
}
