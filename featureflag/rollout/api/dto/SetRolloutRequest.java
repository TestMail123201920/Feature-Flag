package com.company.featureflag.rollout.api.dto;

import com.company.featureflag.rollout.domain.StrategyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SetRolloutRequest(
        @NotNull StrategyType strategyType,
        String identifierField,
        @Min(0) @Max(100) Integer percentage,
        Map<String, Object> strategyConfig
) {
}
