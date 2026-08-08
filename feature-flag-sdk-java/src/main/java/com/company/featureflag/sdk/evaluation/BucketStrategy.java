package com.company.featureflag.sdk.evaluation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * bucket = SHA-256(featureKey + ":" + identifier) % 100 — must match the
 * central service's algorithm exactly (see feature-flag-service's
 * Sha256BucketStrategy) or the same user could get different decisions
 * depending on whether they're served by the SDK or the direct evaluation
 * API. Deterministic across app instances, restarts, and pod rescheduling;
 * never derived from randomness or ephemeral infra identity (spec §43-45).
 */
final class BucketStrategy {

    int bucket(String featureKey, String identifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((featureKey + ":" + identifier).getBytes(StandardCharsets.UTF_8));
            int unsignedInt = ((hash[0] & 0xFF) << 24) | ((hash[1] & 0xFF) << 16)
                    | ((hash[2] & 0xFF) << 8) | (hash[3] & 0xFF);
            return Math.floorMod(unsignedInt, 100);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
