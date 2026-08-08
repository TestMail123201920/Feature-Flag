package com.company.featureflag.sdk.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Arbitrary evaluation-time facts about the caller (userId, phoneNumber,
 * country, requestId, ...). Mirrors the shape of the central service's own
 * EvaluationContext by design (same evaluation semantics on both sides,
 * spec §28) but is intentionally a separate class — the SDK is a standalone
 * Maven artifact with no compile-time dependency on feature-flag-service.
 */
public final class EvaluationContext {

    private final Map<String, Object> attributes;

    private EvaluationContext(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static EvaluationContext empty() {
        return new EvaluationContext(Map.of());
    }

    public Optional<Object> get(String field) {
        return Optional.ofNullable(attributes.get(field));
    }

    public Optional<String> getAsString(String field) {
        return get(field).map(String::valueOf);
    }

    public boolean has(String field) {
        return attributes.containsKey(field);
    }

    public static final class Builder {
        private final Map<String, Object> attributes = new LinkedHashMap<>();

        public Builder put(String field, Object value) {
            attributes.put(field, value);
            return this;
        }

        public EvaluationContext build() {
            return new EvaluationContext(Map.copyOf(attributes));
        }
    }
}
