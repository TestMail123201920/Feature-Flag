package com.company.featureflag.evaluation.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public record BatchEvaluateRequest(
        @NotEmpty List<String> features,
        Map<String, Object> context
) {
}
