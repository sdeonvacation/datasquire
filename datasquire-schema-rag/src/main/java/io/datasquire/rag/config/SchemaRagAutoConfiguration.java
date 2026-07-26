package io.datasquire.rag.config;

import io.datasquire.rag.embedding.EmbeddingService;
import io.datasquire.rag.repository.ChunkRepository;
import io.datasquire.rag.retriever.SchemaRetriever;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Auto-configuration for the schema RAG module.
 * Activates when datasquire.schema-rag.enabled is true (default).
 * Requires both a DataSource (for pgvector) and an EmbeddingModel to be present.
 */
@Configuration
@ConditionalOnProperty(name = "datasquire.schema-rag.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean({DataSource.class, EmbeddingModel.class})
@EnableConfigurationProperties(SchemaRagProperties.class)
public class SchemaRagAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "schemaRagJdbcTemplate")
    public JdbcTemplate schemaRagJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(ChunkRepository.class)
    public ChunkRepository chunkRepository(JdbcTemplate schemaRagJdbcTemplate) {
        return new ChunkRepository(schemaRagJdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingService.class)
    public EmbeddingService embeddingService(EmbeddingModel embeddingModel) {
        return new EmbeddingService(embeddingModel);
    }

    @Bean
    @ConditionalOnMissingBean(SchemaRetriever.class)
    public SchemaRetriever schemaRetriever(
            ChunkRepository chunkRepository,
            EmbeddingService embeddingService,
            SchemaRagProperties properties,
            @Value("${datasquire.schema-rag.fallback-schema:classpath:schema/full-schema.md}")
            Resource fullSchemaFallback) {
        return new SchemaRetriever(chunkRepository, embeddingService, properties, fullSchemaFallback);
    }
}
