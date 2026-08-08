package com.company.featureflag.sdk.spring;

import com.company.featureflag.sdk.sync.SdkMetrics;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Reports the SDK-side metrics from spec §35 (configuration refresh
 * success/failure, configuration version, stale configuration) to whatever
 * registry the host Spring Boot app already has configured (Prometheus,
 * CloudWatch, etc. — auto-configured transitively by their own starters).
 */
final class MicrometerSdkMetrics implements SdkMetrics {

    private final MeterRegistry meterRegistry;

    MicrometerSdkMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        meterRegistry.gauge("feature_flag.sdk.configuration_version", this, m -> lastVersion);
    }

    private volatile double lastVersion = -1;

    @Override
    public void refreshSucceeded(long configurationVersion) {
        this.lastVersion = configurationVersion;
        meterRegistry.counter("feature_flag.sdk.refresh.success").increment();
    }

    @Override
    public void refreshFailed() {
        meterRegistry.counter("feature_flag.sdk.refresh.failure").increment();
    }

    @Override
    public void staleConfigurationDetected() {
        meterRegistry.counter("feature_flag.sdk.stale_configuration").increment();
    }
}
