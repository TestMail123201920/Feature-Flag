package com.company.featureflag.configuration.api.dto;

import com.company.featureflag.configuration.domain.FeatureConfiguration;

import java.util.List;

public record SdkConfigurationResponse(
        long configurationVersion,
        List<FeatureConfiguration> features
) {
}
