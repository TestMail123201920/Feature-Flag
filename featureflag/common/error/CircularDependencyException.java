package com.company.featureflag.common.error;

import org.springframework.http.HttpStatus;

public class CircularDependencyException extends ApiException {
    public CircularDependencyException(String featureKey, String dependsOnKey) {
        super("CIRCULAR_DEPENDENCY", HttpStatus.CONFLICT,
                "Adding dependency '%s' -> '%s' would create a circular dependency"
                        .formatted(featureKey, dependsOnKey));
    }
}
