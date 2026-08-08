package com.company.featureflag.evaluation.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record EvaluateRequest(
        @NotBlank String featureKey,
        Map<String, Object> context
) {
}
