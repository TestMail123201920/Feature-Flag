package com.company.featureflag.rollout.infrastructure;

import com.company.featureflag.rollout.domain.RolloutStrategy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RolloutStrategyRepository extends JpaRepository<RolloutStrategy, UUID> {
    Optional<RolloutStrategy> findByFeatureVersion_Id(UUID featureVersionId);
}
