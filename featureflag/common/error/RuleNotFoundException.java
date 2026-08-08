package com.company.featureflag.common.error;

import org.springframework.http.HttpStatus;

public class RuleNotFoundException extends ApiException {
    public RuleNotFoundException(String ruleId) {
        super("RULE_NOT_FOUND", HttpStatus.NOT_FOUND,
                "Rule '%s' was not found on the feature's current version".formatted(ruleId));
    }
}
