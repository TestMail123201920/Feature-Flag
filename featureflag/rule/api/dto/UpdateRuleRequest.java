package com.company.featureflag.rule.api.dto;

import com.company.featureflag.rule.domain.Combinator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateRuleRequest(
        @NotNull @Min(1) Integer priority,
        @NotNull Combinator combinator,
        @NotNull Boolean enabled,
        @NotEmpty @Valid List<ConditionRequest> conditions
) {
}
