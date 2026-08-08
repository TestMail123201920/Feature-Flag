package com.company.featureflag.rule.api.dto;

import com.company.featureflag.rule.domain.Combinator;
import com.company.featureflag.rule.domain.Operator;

import java.util.List;
import java.util.UUID;

public record RuleResponse(
        UUID id,
        int priority,
        Combinator combinator,
        boolean enabled,
        List<ConditionResponse> conditions
) {
    public record ConditionResponse(UUID id, String field, Operator operator, Object value) {
    }
}
