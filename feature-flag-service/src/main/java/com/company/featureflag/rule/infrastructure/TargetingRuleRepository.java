package com.company.featureflag.rule.infrastructure;

import com.company.featureflag.rule.domain.TargetingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TargetingRuleRepository extends JpaRepository<TargetingRule, UUID> {
    List<TargetingRule> findByFeatureVersionIdOrderByPriorityAsc(UUID featureVersionId);
}
