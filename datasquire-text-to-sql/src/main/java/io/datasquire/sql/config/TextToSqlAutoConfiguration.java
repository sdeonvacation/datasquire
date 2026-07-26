package io.datasquire.sql.config;

import io.datasquire.core.schema.SchemaProvider;
import io.datasquire.sql.agent.TextToSqlAgent;
import io.datasquire.sql.executor.SqlExecutorTool;
import io.datasquire.sql.executor.SqlSafetyEnforcer;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Auto-configuration for the text-to-sql module.
 * Activates when datasquire.text-to-sql.enabled is true (default).
 * Requires JdbcTemplate, ChatModel, and SchemaProvider to be present.
 */
@Configuration
@ConditionalOnProperty(name = "datasquire.text-to-sql.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TextToSqlProperties.class)
public class TextToSqlAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SqlSafetyEnforcer sqlSafetyEnforcer() {
        return new SqlSafetyEnforcer();
    }

    /**
     * Creates the SQL executor tool. If a bean named "datasquireReaderJdbc" exists,
     * qualify your injection to use it; otherwise the primary JdbcTemplate is used.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(JdbcTemplate.class)
    public SqlExecutorTool sqlExecutorTool(JdbcTemplate jdbcTemplate,
                                           TextToSqlProperties properties,
                                           SqlSafetyEnforcer safetyEnforcer) {
        return new SqlExecutorTool(jdbcTemplate, properties, safetyEnforcer);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({ChatModel.class, SchemaProvider.class, SqlExecutorTool.class})
    public TextToSqlAgent textToSqlAgent(ChatModel chatModel,
                                         SchemaProvider schemaProvider,
                                         SqlExecutorTool sqlExecutorTool,
                                         TextToSqlProperties properties) {
        return new TextToSqlAgent(chatModel, schemaProvider, sqlExecutorTool, properties);
    }
}
