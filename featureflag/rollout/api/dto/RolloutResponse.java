package com.company.featureflag.rollout.api.dto;

import com.company.featureflag.rollout.domain.StrategyType;

import java.util.Map;
import java.util.UUID;

public record RolloutResponse(
        UUID id,
        StrategyType strategyType,
        String identifierField,
        Integer percentage,
        Map<String, Object> strategyConfig
) {
}
