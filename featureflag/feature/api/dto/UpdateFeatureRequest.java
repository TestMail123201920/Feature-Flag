package com.company.featureflag.feature.api.dto;

import com.company.featureflag.feature.domain.FeatureStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Full-replace update for the mutable feature attributes. {@code status} and
 * {@code killSwitch} are optional (null = leave unchanged); when present they
 * are applied via {@code Feature.transitionTo}/kill-switch methods so
 * invariants (valid state transitions) are still enforced.
 */
public record UpdateFeatureRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 2000)
        String description,

        FeatureStatus status,

        Boolean killSwitch
) {
}
