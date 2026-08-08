package com.company.featureflag.sdk.sync;

import java.time.Duration;

/**
 * Plain config holder — deliberately not a Spring {@code @ConfigurationProperties}
 * class itself (that binding lives in the optional spring/ module) so the
 * core SDK has no Spring dependency.
 */
public final class FeatureFlagClientProperties {

    private String serviceUrl = "http://localhost:8080";
    private Duration refreshInterval = Duration.ofSeconds(30);
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(2);
    private boolean fallbackEnabled = false;

    public String getServiceUrl() {
        return serviceUrl;
    }

    public FeatureFlagClientProperties setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
        return this;
    }

    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    public FeatureFlagClientProperties setRefreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
        return this;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public FeatureFlagClientProperties setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public FeatureFlagClientProperties setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public FeatureFlagClientProperties setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
        return this;
    }
}
