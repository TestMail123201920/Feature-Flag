package com.company.featureflag.feature.application;

import com.company.featureflag.feature.api.dto.FeatureResponse;
import com.company.featureflag.feature.api.dto.FeatureVersionSummaryResponse;
import com.company.featureflag.feature.domain.Feature;
import com.company.featureflag.feature.domain.FeatureVersion;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FeatureMapper {

    FeatureResponse toResponse(Feature feature);

    FeatureVersionSummaryResponse toSummaryResponse(FeatureVersion version);
}
