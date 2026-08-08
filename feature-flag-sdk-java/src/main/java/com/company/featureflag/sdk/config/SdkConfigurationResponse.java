package com.company.featureflag.sdk.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SdkConfigurationResponse(
        long configurationVersion,
        List<FeatureConfiguration> features
) {
}
