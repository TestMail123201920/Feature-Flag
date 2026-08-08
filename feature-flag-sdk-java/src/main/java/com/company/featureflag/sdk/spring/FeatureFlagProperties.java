package com.company.featureflag.sdk.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Binds the {@code feature-flag.*} keys from spec §25:
 * <pre>
 * feature-flag:
 *   service-url: http://feature-flag-service
 *   refresh-interval: 30s
 *   connect-timeout: 2s
 *   read-timeout: 2s
 *   fallback-enabled: false
 * </pre>
 */
@ConfigurationProperties(prefix = "feature-flag")
public class FeatureFlagProperties {

    private String serviceUrl = "http://localhost:8080";
    private Duration refreshInterval = Duration.ofSeconds(30);
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(2);
    private boolean fallbackEnabled = false;

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }
}
