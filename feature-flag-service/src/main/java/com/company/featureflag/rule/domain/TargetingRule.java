package com.company.featureflag.rule.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A named, prioritized group of {@link RuleCondition}s. Rules for a given
 * feature_version are evaluated in ascending priority order; the first rule
 * whose conditions match (per its combinator) decides the outcome. If no
 * rule matches, the pipeline falls through to the rollout strategy.
 */
@Entity
@Table(name = "targeting_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TargetingRule {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "feature_version_id", nullable = false)
    private UUID featureVersionId;

    @Column(nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Combinator combinator;

    @Column(nullable = false)
    private boolean enabled;

    @OneToMany(mappedBy = "targetingRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RuleCondition> conditions = new ArrayList<>();

    private TargetingRule(UUID featureVersionId, int priority, Combinator combinator) {
        this.id = UUID.randomUUID();
        this.featureVersionId = featureVersionId;
        this.priority = priority;
        this.combinator = combinator;
        this.enabled = true;
    }

    public static TargetingRule of(UUID featureVersionId, int priority, Combinator combinator) {
        return new TargetingRule(featureVersionId, priority, combinator);
    }

    public void addCondition(RuleCondition condition) {
        this.conditions.add(condition);
    }

    public void disable() {
        this.enabled = false;
    }
}
