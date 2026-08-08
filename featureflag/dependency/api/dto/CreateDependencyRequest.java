package com.company.featureflag.dependency.api.dto;

import com.company.featureflag.dependency.domain.DependencyType;
import jakarta.validation.constraints.NotBlank;

public record CreateDependencyRequest(
        @NotBlank String dependsOnFeatureKey,
        DependencyType dependencyType
) {
}
