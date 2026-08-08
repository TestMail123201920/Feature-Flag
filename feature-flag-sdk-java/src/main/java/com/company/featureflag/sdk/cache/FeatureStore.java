package com.company.featureflag.sdk.cache;

import com.company.featureflag.sdk.config.FeatureConfiguration;
import com.company.featureflag.sdk.config.SdkConfigurationResponse;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Holds exactly one immutable snapshot at a time, swapped atomically via
 * {@link AtomicReference} (spec §38: no partially updated configuration
 * ever visible to a concurrent evaluation). Evaluation never touches
 * Postgres/Redis/Kafka/HTTP (spec §39) — it only ever reads this in-memory
 * snapshot.
 */
public final class FeatureStore {

    /** Empty until the first successful sync; evaluation falls back to fallback-enabled until then (spec §27). */
    private final AtomicReference<Snapshot> current = new AtomicReference<>(Snapshot.empty());

    public void replaceSnapshot(SdkConfigurationResponse response) {
        Map<String, FeatureConfiguration> byKey = response.features().stream()
                .collect(Collectors.toMap(FeatureConfiguration::key, f -> f));
        current.set(new Snapshot(response.configurationVersion(), byKey, Instant.now()));
    }

    public Optional<FeatureConfiguration> get(String featureKey) {
        return Optional.ofNullable(current.get().featuresByKey().get(featureKey));
    }

    public boolean hasConfiguration() {
        return current.get().lastUpdated() != null;
    }

    public long configurationVersion() {
        return current.get().configurationVersion();
    }

    public Instant lastUpdated() {
        return current.get().lastUpdated();
    }

    /** True if the snapshot's own version already matches, so a refresh can skip replacing it (spec §41). */
    public boolean isUpToDate(long remoteConfigurationVersion) {
        return hasConfiguration() && current.get().configurationVersion() == remoteConfigurationVersion;
    }

    private record Snapshot(long configurationVersion, Map<String, FeatureConfiguration> featuresByKey, Instant lastUpdated) {
        static Snapshot empty() {
            return new Snapshot(-1, Map.of(), null);
        }
    }
}
