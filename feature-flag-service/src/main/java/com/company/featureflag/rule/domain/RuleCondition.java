package com.company.featureflag.rule.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "rule_condition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RuleCondition {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "targeting_rule_id", nullable = false)
    private TargetingRule targetingRule;

    @Column(nullable = false, length = 100)
    private String field;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Operator operator;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Object value;

    private RuleCondition(TargetingRule targetingRule, String field, Operator operator, Object value) {
        this.id = UUID.randomUUID();
        this.targetingRule = targetingRule;
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public static RuleCondition of(TargetingRule targetingRule, String field, Operator operator, Object value) {
        return new RuleCondition(targetingRule, field, operator, value);
    }
}
