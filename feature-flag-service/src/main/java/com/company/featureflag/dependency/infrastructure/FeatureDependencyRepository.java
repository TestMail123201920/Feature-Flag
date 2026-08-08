package com.company.featureflag.dependency.infrastructure;

import com.company.featureflag.dependency.domain.FeatureDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeatureDependencyRepository extends JpaRepository<FeatureDependency, UUID> {
    List<FeatureDependency> findByFeatureId(UUID featureId);
    List<FeatureDependency> findAllByFeatureIdIn(List<UUID> featureIds);
}
