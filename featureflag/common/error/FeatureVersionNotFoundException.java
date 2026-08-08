package com.company.featureflag.common.error;

import org.springframework.http.HttpStatus;

public class FeatureVersionNotFoundException extends ApiException {
    public FeatureVersionNotFoundException(String featureKey, int versionNumber) {
        super("VERSION_NOT_FOUND", HttpStatus.NOT_FOUND,
                "Version %d of feature '%s' was not found".formatted(versionNumber, featureKey));
    }
}
