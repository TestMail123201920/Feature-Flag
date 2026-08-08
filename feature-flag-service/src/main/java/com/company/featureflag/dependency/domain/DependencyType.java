package com.company.featureflag.dependency.domain;

public enum DependencyType {
    /** If the depended-on feature evaluates false, this feature evaluates false too. */
    REQUIRES_ENABLED
}
