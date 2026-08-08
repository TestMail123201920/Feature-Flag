package com.company.featureflag.evaluation.api.dto;

import com.company.featureflag.evaluation.domain.EvaluationReason;

public record EvaluateResponse(
        String featureKey,
        boolean enabled,
        Integer version,
        EvaluationReason reason
) {
}
