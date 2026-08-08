package com.company.featureflag.rollout.domain;

/**
 * Deterministic bucketing behind a replaceable abstraction (spec §43). The
 * same (featureKey, identifier) pair must always hash to the same bucket
 * across app instances, restarts, and pod rescheduling — never derive this
 * from randomness or ephemeral infra identity (pod name/IP).
 */
public interface BucketStrategy {
    /** Returns a bucket in [0, 99]. */
    int bucket(String featureKey, String identifier);
}
