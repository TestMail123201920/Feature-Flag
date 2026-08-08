package com.company.featureflag.rollout.domain;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * bucket = SHA-256(featureKey + ":" + identifier) % 100.
 *
 * Namespacing by featureKey means the same user can land in different
 * buckets for different features — independent, uncorrelated rollouts —
 * while still being stable for a given feature across restarts/instances.
 */
@Component
public class Sha256BucketStrategy implements BucketStrategy {

    @Override
    public int bucket(String featureKey, String identifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((featureKey + ":" + identifier).getBytes(StandardCharsets.UTF_8));
            // Use the first 4 bytes as an unsigned int for a well-distributed, deterministic bucket.
            int unsignedInt = ((hash[0] & 0xFF) << 24) | ((hash[1] & 0xFF) << 16)
                    | ((hash[2] & 0xFF) << 8) | (hash[3] & 0xFF);
            return Math.floorMod(unsignedInt, 100);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a mandatory JDK algorithm; this branch is unreachable in practice.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
