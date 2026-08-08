package com.company.featureflag.sdk.core;

/** Mirrors the central service's reason set exactly (spec §13) so callers see identical semantics either way. */
public enum EvaluationReason {
    FEATURE_NOT_FOUND,
    FEATURE_DISABLED,
    KILL_SWITCH,
    DEPENDENCY_FAILED,
    RULE_MATCH,
    RULE_NO_MATCH,
    PERCENTAGE_ROLLOUT_MATCH,
    PERCENTAGE_ROLLOUT_MISS,
    DEFAULT,
    ERROR_FALLBACK,
    /** SDK-only: no cached configuration was available and fallback-enabled is false/true was applied. */
    STALE_OR_MISSING_CONFIGURATION
}
