package com.company.featureflag.sdk.sync;

import com.company.featureflag.sdk.cache.FeatureStore;
import com.company.featureflag.sdk.config.SdkConfigurationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Polls {@code GET /api/v1/sdk/configuration} on {@code refreshInterval}
 * and atomically replaces the {@link FeatureStore} snapshot (spec §25).
 *
 * Uses the JDK's built-in {@code java.net.http.HttpClient} rather than
 * WebClient specifically to keep the core module dependency-free of
 * Spring/Reactor — it must be embeddable in a plain Java app, not just a
 * Spring Boot one. The optional spring/ module wires this up automatically
 * when Spring Boot is present.
 *
 * Failure handling (spec §26): a hand-rolled bounded retry with backoff,
 * plus a simple consecutive-failure "circuit" that pauses refresh attempts
 * for a cooldown period rather than hammering a down service — deliberately
 * not a full resilience4j integration, to keep the SDK's dependency
 * footprint small; swapping in resilience4j's Retry/CircuitBreaker
 * decorators around {@link #doRefresh()} is a contained, drop-in change.
 *
 * Startup never blocks (spec §27): {@link #start()} kicks off the first
 * fetch asynchronously and returns immediately; the host application starts
 * with an empty cache if the very first fetch hasn't landed yet.
 */
public final class PollingConfigurationProvider implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(PollingConfigurationProvider.class);
    private static final String CONFIG_PATH = "/api/v1/sdk/configuration";
    private static final int MAX_ATTEMPTS_PER_REFRESH = 3;
    private static final int CONSECUTIVE_FAILURES_BEFORE_COOLDOWN = 5;

    private final FeatureFlagClientProperties properties;
    private final FeatureStore featureStore;
    private final SdkMetrics metrics;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler;

    private volatile Instant lastSuccessfulRefresh;
    private volatile Instant lastRefreshAttempt;
    private volatile int consecutiveFailures = 0;
    private volatile Instant cooldownUntil;

    public PollingConfigurationProvider(FeatureFlagClientProperties properties, FeatureStore featureStore, SdkMetrics metrics) {
        this.properties = properties;
        this.featureStore = featureStore;
        this.metrics = metrics;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "feature-flag-sdk-refresh");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        long intervalMillis = Math.max(properties.getRefreshInterval().toMillis(), 1000);
        scheduler.execute(this::refreshWithRetry); // fire-and-forget first fetch, does not block start()
        scheduler.scheduleWithFixedDelay(this::refreshWithRetry, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    private void refreshWithRetry() {
        lastRefreshAttempt = Instant.now();

        if (cooldownUntil != null && Instant.now().isBefore(cooldownUntil)) {
            log.debug("Skipping refresh attempt, in cooldown until {}", cooldownUntil);
            return;
        }

        long backoffMillis = 200;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_REFRESH; attempt++) {
            try {
                doRefresh();
                consecutiveFailures = 0;
                cooldownUntil = null;
                return;
            } catch (Exception ex) {
                log.warn("Configuration refresh attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS_PER_REFRESH, ex.getMessage());
                if (attempt < MAX_ATTEMPTS_PER_REFRESH) {
                    sleepQuietly(backoffMillis);
                    backoffMillis *= 2;
                }
            }
        }

        consecutiveFailures++;
        metrics.refreshFailed();
        if (consecutiveFailures >= CONSECUTIVE_FAILURES_BEFORE_COOLDOWN) {
            cooldownUntil = Instant.now().plus(properties.getRefreshInterval().multipliedBy(2));
            log.warn("{} consecutive refresh failures, pausing refresh attempts until {}", consecutiveFailures, cooldownUntil);
        }

        checkStaleness();
    }

    private void doRefresh() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getServiceUrl() + CONFIG_PATH))
                .timeout(properties.getReadTimeout())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected status code " + response.statusCode() + " from " + CONFIG_PATH);
        }

        SdkConfigurationResponse parsed = objectMapper.readValue(response.body(), SdkConfigurationResponse.class);

        if (featureStore.isUpToDate(parsed.configurationVersion())) {
            lastSuccessfulRefresh = Instant.now(); // confirmed fresh, nothing to replace (spec §41)
            return;
        }

        featureStore.replaceSnapshot(parsed);
        lastSuccessfulRefresh = Instant.now();
        metrics.refreshSucceeded(parsed.configurationVersion());
        log.debug("Configuration refreshed, version={} features={}", parsed.configurationVersion(), parsed.features().size());
    }

    private void checkStaleness() {
        if (!featureStore.hasConfiguration() || lastSuccessfulRefresh == null) {
            return;
        }
        Duration staleFor = Duration.between(lastSuccessfulRefresh, Instant.now());
        if (staleFor.compareTo(properties.getRefreshInterval().multipliedBy(3)) > 0) {
            metrics.staleConfigurationDetected();
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Instant getLastSuccessfulRefresh() {
        return lastSuccessfulRefresh;
    }

    public Instant getLastRefreshAttempt() {
        return lastRefreshAttempt;
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
