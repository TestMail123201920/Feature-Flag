package com.company.featureflag.common.error;

import org.springframework.http.HttpStatus;

public class FeatureNotFoundException extends ApiException {
    public FeatureNotFoundException(String featureKey) {
        super("FEATURE_NOT_FOUND", HttpStatus.NOT_FOUND,
                "Feature '%s' was not found".formatted(featureKey));
    }
}
