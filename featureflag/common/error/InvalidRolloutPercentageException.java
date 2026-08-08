package com.company.featureflag.common.error;

import org.springframework.http.HttpStatus;

public class InvalidRolloutPercentageException extends ApiException {
    public InvalidRolloutPercentageException(int percentage) {
        super("INVALID_ROLLOUT_PERCENTAGE", HttpStatus.BAD_REQUEST,
                "Rollout percentage must be between 0 and 100, got %d".formatted(percentage));
    }
}
