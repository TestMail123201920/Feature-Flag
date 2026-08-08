package com.company.featureflag.feature.infrastructure;

import com.company.featureflag.feature.domain.Feature;
import com.company.featureflag.feature.domain.FeatureStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FeatureRepository extends JpaRepository<Feature, UUID> {
    Optional<Feature> findByKey(String key);
    boolean existsByKey(String key);
    Page<Feature> findByStatus(FeatureStatus status, Pageable pageable);
}
