package com.company.featureflag.rollout.domain;

import com.company.featureflag.feature.domain.FeatureVersion;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "rollout_strategy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RolloutStrategy {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_version_id", nullable = false)
    private FeatureVersion featureVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", nullable = false, length = 40)
    private StrategyType strategyType;

    @Column(name = "identifier_field", length = 100)
    private String identifierField;

    @Column
    private Integer percentage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strategy_config", nullable = false)
    private Map<String, Object> strategyConfig = Map.of();

    private RolloutStrategy(FeatureVersion featureVersion, StrategyType strategyType,
                             String identifierField, Integer percentage,
                             Map<String, Object> strategyConfig) {
        if (percentage != null && (percentage < 0 || percentage > 100)) {
            throw new IllegalArgumentException("percentage must be between 0 and 100");
        }
        this.id = UUID.randomUUID();
        this.featureVersion = featureVersion;
        this.strategyType = strategyType;
        this.identifierField = identifierField;
        this.percentage = percentage;
        this.strategyConfig = strategyConfig == null ? Map.of() : strategyConfig;
    }

    public static RolloutStrategy of(FeatureVersion featureVersion, StrategyType strategyType,
                                      String identifierField, Integer percentage,
                                      Map<String, Object> strategyConfig) {
        return new RolloutStrategy(featureVersion, strategyType, identifierField, percentage, strategyConfig);
    }
}
