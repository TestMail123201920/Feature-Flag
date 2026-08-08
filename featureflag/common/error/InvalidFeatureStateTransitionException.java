package com.company.featureflag.common.error;

import org.springframework.http.HttpStatus;

public class InvalidFeatureStateTransitionException extends ApiException {
    public InvalidFeatureStateTransitionException(String message) {
        super("INVALID_STATE_TRANSITION", HttpStatus.CONFLICT, message);
    }
}
