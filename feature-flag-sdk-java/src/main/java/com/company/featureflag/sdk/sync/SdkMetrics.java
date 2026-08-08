package com.company.featureflag.sdk.sync;

/**
 * Deliberately a tiny interface rather than a hard Micrometer dependency —
 * the core SDK must stay embeddable in non-Spring apps. The optional
 * spring/ auto-configuration module supplies a Micrometer-backed
 * implementation when a {@code MeterRegistry} bean is present; otherwise
 * {@link #noOp()} is used and these calls are free.
 *
 * Covers spec §35's SDK-side metrics: configuration refresh success/failure,
 * configuration version, and staleness.
 */
public interface SdkMetrics {

    void refreshSucceeded(long configurationVersion);

    void refreshFailed();

    void staleConfigurationDetected();

    static SdkMetrics noOp() {
        return new SdkMetrics() {
            @Override
            public void refreshSucceeded(long configurationVersion) {
            }

            @Override
            public void refreshFailed() {
            }

            @Override
            public void staleConfigurationDetected() {
            }
        };
    }
}
