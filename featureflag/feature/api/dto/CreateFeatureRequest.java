package com.company.featureflag.feature.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFeatureRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,149}$",
                message = "key must be UPPER_SNAKE_CASE, e.g. NEW_PAYMENT_FLOW")
        String key,

        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 2000)
        String description
) {
}
