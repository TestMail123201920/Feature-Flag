package com.company.featureflag.feature.domain;

/**
 * Lifecycle states for a {@link Feature}. Deliberately a plain enum rather
 * than a full state-machine library — the transition rules below are the
 * only guard we need today, and new states can be added here without
 * touching evaluation code, which only cares whether status == ACTIVE.
 */
public enum FeatureStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED;

    public boolean canTransitionTo(FeatureStatus target) {
        return switch (this) {
            case DRAFT -> target == ACTIVE || target == ARCHIVED;
            case ACTIVE -> target == ARCHIVED;
            case ARCHIVED -> false;
        };
    }
}
