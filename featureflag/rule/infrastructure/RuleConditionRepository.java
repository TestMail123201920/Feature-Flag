package com.company.featureflag.rule.infrastructure;

import com.company.featureflag.rule.domain.RuleCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RuleConditionRepository extends JpaRepository<RuleCondition, UUID> {
}
