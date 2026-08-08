package com.company.featureflag.feature.infrastructure;

import com.company.featureflag.feature.domain.FeatureVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureVersionRepository extends JpaRepository<FeatureVersion, UUID> {
    List<FeatureVersion> findByFeatureIdOrderByVersionNumberDesc(UUID featureId);
    Optional<FeatureVersion> findByFeatureIdAndVersionNumber(UUID featureId, int versionNumber);

    @Query("select max(fv.versionNumber) from FeatureVersion fv where fv.featureId = :featureId")
    Optional<Integer> findMaxVersionNumberByFeatureId(@Param("featureId") UUID featureId);
}
