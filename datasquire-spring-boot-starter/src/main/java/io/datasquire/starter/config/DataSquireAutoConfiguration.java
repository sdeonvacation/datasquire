package io.datasquire.starter.config;

import io.datasquire.core.session.SessionStore;
import io.datasquire.orchestrator.config.OrchestratorAutoConfiguration;
import io.datasquire.orchestrator.react.ReActOrchestrator;
import io.datasquire.orchestrator.registry.SubAgentRegistry;
import io.datasquire.rag.config.SchemaRagAutoConfiguration;
import io.datasquire.sql.config.TextToSqlAutoConfiguration;
import io.datasquire.starter.actuator.DataSquireHealthIndicator;
import io.datasquire.starter.controller.QueryController;
import io.datasquire.starter.metrics.DataSquireMetrics;
import io.datasquire.starter.session.CaffeineSessionStore;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;

/**
 * Main auto-configuration for the DataSquire Spring Boot Starter.
 * Imports sub-module configurations and provides default beans.
 */
@Configuration
@ConditionalOnProperty(name = "datasquire.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DataSquireProperties.class)
@Import({SchemaRagAutoConfiguration.class, TextToSqlAutoConfiguration.class, OrchestratorAutoConfiguration.class})
public class DataSquireAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SessionStore.class)
    public CaffeineSessionStore caffeineSessionStore(DataSquireProperties properties) {
        return new CaffeineSessionStore(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "datasquire.controller.enabled", havingValue = "true", matchIfMissing = true)
    public QueryController queryController(@Nullable ReActOrchestrator orchestrator,
                                           SessionStore sessionStore,
                                           SubAgentRegistry subAgentRegistry,
                                           DataSquireProperties properties,
                                           @Nullable DataSquireMetrics metrics) {
        return new QueryController(orchestrator, sessionStore, subAgentRegistry, properties, metrics);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSquireHealthIndicator dataSquireHealthIndicator(SubAgentRegistry subAgentRegistry,
                                                              SessionStore sessionStore) {
        return new DataSquireHealthIndicator(subAgentRegistry, sessionStore);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MeterRegistry.class)
    public DataSquireMetrics dataSquireMetrics(MeterRegistry meterRegistry) {
        return new DataSquireMetrics(meterRegistry);
    }
}
