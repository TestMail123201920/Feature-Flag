package com.company.featureflag.feature.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RollbackRequest(
        @NotNull
        @Min(1)
        Integer targetVersionNumber
) {
}
