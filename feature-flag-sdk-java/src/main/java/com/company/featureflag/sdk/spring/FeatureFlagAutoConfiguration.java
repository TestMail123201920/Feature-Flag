package com.company.featureflag.sdk.spring;

import com.company.featureflag.sdk.core.DefaultFeatureFlagClient;
import com.company.featureflag.sdk.core.FeatureFlagClient;
import com.company.featureflag.sdk.sync.FeatureFlagClientProperties;
import com.company.featureflag.sdk.sync.SdkMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

/**
 * Auto-registers a {@link FeatureFlagClient} bean bound to {@code feature-flag.*}
 * properties (spec §25) as soon as this JAR is on the classpath — the "Spring
 * Boot integration / auto-configuration" requirement from the SDK tech stack.
 * Background polling starts on {@link ApplicationReadyEvent} rather than
 * during bean creation, so it never delays context startup (spec §27), and
 * is torn down cleanly via the bean's {@code destroyMethod}.
 */
@AutoConfiguration
@EnableConfigurationProperties(FeatureFlagProperties.class)
public class FeatureFlagAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    public SdkMetrics micrometerSdkMetrics(MeterRegistry meterRegistry) {
        return new MicrometerSdkMetrics(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(SdkMetrics.class)
    public SdkMetrics noOpSdkMetrics() {
        return SdkMetrics.noOp();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public DefaultFeatureFlagClient featureFlagClient(FeatureFlagProperties properties, SdkMetrics metrics) {
        FeatureFlagClientProperties clientProperties = new FeatureFlagClientProperties()
                .setServiceUrl(properties.getServiceUrl())
                .setRefreshInterval(properties.getRefreshInterval())
                .setConnectTimeout(properties.getConnectTimeout())
                .setReadTimeout(properties.getReadTimeout())
                .setFallbackEnabled(properties.isFallbackEnabled());
        return new DefaultFeatureFlagClient(clientProperties, metrics);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startPolling(ApplicationReadyEvent event) {
        event.getApplicationContext().getBean(DefaultFeatureFlagClient.class).start();
    }
}
