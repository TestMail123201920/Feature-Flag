package com.company.featureflag.sdk.core;

import com.company.featureflag.sdk.cache.FeatureStore;
import com.company.featureflag.sdk.evaluation.EvaluationEngine;
import com.company.featureflag.sdk.sync.FeatureFlagClientProperties;
import com.company.featureflag.sdk.sync.PollingConfigurationProvider;
import com.company.featureflag.sdk.sync.SdkMetrics;

/**
 * The composition root for the SDK: owns the {@link FeatureStore} (local
 * cache), the {@link EvaluationEngine} (local evaluation, spec §28), and the
 * {@link PollingConfigurationProvider} (config sync, spec §25). Construction
 * never blocks on the network (spec §27) — call {@link #start()} once after
 * building to kick off the background sync.
 */
public final class DefaultFeatureFlagClient implements FeatureFlagClient, AutoCloseable {

    private final FeatureStore featureStore;
    private final EvaluationEngine evaluationEngine;
    private final PollingConfigurationProvider configurationProvider;

    public DefaultFeatureFlagClient(FeatureFlagClientProperties properties) {
        this(properties, SdkMetrics.noOp());
    }

    public DefaultFeatureFlagClient(FeatureFlagClientProperties properties, SdkMetrics metrics) {
        this.featureStore = new FeatureStore();
        this.evaluationEngine = new EvaluationEngine(featureStore, properties.isFallbackEnabled());
        this.configurationProvider = new PollingConfigurationProvider(properties, featureStore, metrics);
    }

    /** Starts the background polling loop; safe to call exactly once. */
    public DefaultFeatureFlagClient start() {
        configurationProvider.start();
        return this;
    }

    @Override
    public boolean isEnabled(String featureKey) {
        return isEnabled(featureKey, EvaluationContext.empty());
    }

    @Override
    public boolean isEnabled(String featureKey, EvaluationContext context) {
        return evaluate(featureKey, context).enabled();
    }

    @Override
    public EvaluationResult evaluate(String featureKey, EvaluationContext context) {
        return evaluationEngine.evaluate(featureKey, context);
    }

    /** Diagnostics for health checks / /actuator-style endpoints in the host app. */
    public boolean hasConfiguration() {
        return featureStore.hasConfiguration();
    }

    public java.time.Instant lastSuccessfulRefresh() {
        return configurationProvider.getLastSuccessfulRefresh();
    }

    @Override
    public void close() {
        configurationProvider.close();
    }
}
