package com.company.featureflag.common.error;

import org.springframework.http.HttpStatus;

public class DuplicateFeatureKeyException extends ApiException {
    public DuplicateFeatureKeyException(String featureKey) {
        super("DUPLICATE_FEATURE_KEY", HttpStatus.CONFLICT,
                "A feature with key '%s' already exists".formatted(featureKey));
    }
}
