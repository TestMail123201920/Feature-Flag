package com.company.featureflag.evaluation.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Arbitrary evaluation-time facts about the caller (userId, phoneNumber,
 * country, requestId, ...). Deliberately schemaless — the rule engine and
 * rollout evaluators look up fields dynamically by name, so adding a new
 * targetable attribute never requires a code change here.
 *
 * Never logged verbatim (see spec §37) — callers should log
 * {@code context.keySet()} at most, not values.
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

    public Map<String, Object> asMap() {
        return Map.copyOf(attributes);
    }

    public static final class Builder {
        private final Map<String, Object> attributes = new LinkedHashMap<>();

        public Builder put(String field, Object value) {
            attributes.put(field, value);
            return this;
        }

        public Builder putAll(Map<String, Object> fields) {
            attributes.putAll(fields);
            return this;
        }

        public EvaluationContext build() {
            return new EvaluationContext(Map.copyOf(attributes));
        }
    }
}
