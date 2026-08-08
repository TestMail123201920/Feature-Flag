package com.company.featureflag.evaluation.domain;

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
    ERROR_FALLBACK
}
