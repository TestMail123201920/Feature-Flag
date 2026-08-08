package com.company.featureflag.rule.api.dto;

import com.company.featureflag.rule.domain.Operator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConditionRequest(
        @NotBlank String field,
        @NotNull Operator operator,
        Object value
) {
}
