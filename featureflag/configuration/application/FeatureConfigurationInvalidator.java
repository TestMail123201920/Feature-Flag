package com.company.featureflag.configuration.application;

import com.company.featureflag.configuration.infrastructure.FeatureConfigurationCache;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Evicts a feature's cached configuration only after the write transaction
 * that changed it actually commits — evicting mid-transaction would let a
 * concurrent reader repopulate the cache from pre-commit (and possibly
 * about-to-be-rolled-back) data, reintroducing the staleness this cache
 * layer exists to avoid. Falls back to an immediate evict if called outside
 * a transaction (e.g. from a test or a batch job).
 */
@Component
public class FeatureConfigurationInvalidator {

    private final FeatureConfigurationCache cache;

    public FeatureConfigurationInvalidator(FeatureConfigurationCache cache) {
        this.cache = cache;
    }

    public void evictAfterCommit(String featureKey) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cache.evict(featureKey);
                }
            });
        } else {
            cache.evict(featureKey);
        }
    }
}
