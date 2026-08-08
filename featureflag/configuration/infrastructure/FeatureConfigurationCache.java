package com.company.featureflag.configuration.infrastructure;

import com.company.featureflag.configuration.domain.FeatureConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * PostgreSQL remains the source of truth; this is purely a fast-read cache
 * (spec §19) — never written to directly except as a side effect of a
 * cache-aside read-through or an explicit evict-on-write (see
 * {@code FeatureConfigurationService} / {@code FeatureConfigurationInvalidator}).
 */
@Component
public class FeatureConfigurationCache {

    private static final String KEY_PREFIX = "feature:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration ttl;

    public FeatureConfigurationCache(RedisTemplate<String, Object> redisTemplate,
                                      @Value("${feature-flag.cache.ttl:5m}") String ttl) {
        this.redisTemplate = redisTemplate;
        this.ttl = DurationStyle.detectAndParse(ttl);
    }

    public Optional<FeatureConfiguration> get(String featureKey) {
        Object value = redisTemplate.opsForValue().get(key(featureKey));
        if (value instanceof FeatureConfiguration config) {
            return Optional.of(config);
        }
        return Optional.empty();
    }

    public void put(FeatureConfiguration config) {
        redisTemplate.opsForValue().set(key(config.key()), config, ttl);
    }

    public void evict(String featureKey) {
        redisTemplate.delete(key(featureKey));
    }

    private String key(String featureKey) {
        return KEY_PREFIX + featureKey;
    }
}
