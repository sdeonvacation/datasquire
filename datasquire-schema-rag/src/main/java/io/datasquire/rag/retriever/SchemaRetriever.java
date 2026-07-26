package io.datasquire.rag.retriever;

import io.datasquire.core.exception.SchemaRetrievalException;
import io.datasquire.core.schema.ChunkKind;
import io.datasquire.core.schema.SchemaChunk;
import io.datasquire.core.schema.SchemaProvider;
import io.datasquire.rag.config.SchemaRagProperties;
import io.datasquire.rag.embedding.EmbeddingService;
import io.datasquire.rag.repository.ChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RAG-based schema provider that retrieves relevant schema chunks
 * using embedding similarity search against pgvector.
 */
@Component
public class SchemaRetriever implements SchemaProvider {

    private static final Logger log = LoggerFactory.getLogger(SchemaRetriever.class);
    private static final String CHUNK_SEPARATOR = "\n\n---\n\n";

    private final ChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final SchemaRagProperties properties;
    private final String fullSchemaContent;

    public SchemaRetriever(ChunkRepository chunkRepository,
                           EmbeddingService embeddingService,
                           SchemaRagProperties properties,
                           @Value("${datasquire.schema-rag.fallback-schema:classpath:schema/full-schema.md}")
                           Resource fullSchemaFallback) {
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
        this.properties = properties;
        this.fullSchemaContent = loadResource(fullSchemaFallback);
    }

    @Override
    public String getSchemaContext(String query) {
        try {
            List<SchemaChunk> alwaysIncluded = chunkRepository.findAlwaysIncluded(
                    properties.getNamespace(), properties.getAlwaysIncludeKinds());

            float[] queryEmbedding = embeddingService.embed(query);

            List<String> excludeKinds = properties.getAlwaysIncludeKinds();
            List<SchemaChunk> similar = chunkRepository.findByEmbeddingSimilarity(
                    properties.getNamespace(), queryEmbedding, properties.getTopK(), excludeKinds);

            // Deduplicate while preserving order: always-include first, then similarity results
            Set<String> seenIds = new LinkedHashSet<>();
            String result = Stream.concat(alwaysIncluded.stream(), similar.stream())
                    .filter(chunk -> seenIds.add(chunk.chunkId()))
                    .map(SchemaChunk::content)
                    .collect(Collectors.joining(CHUNK_SEPARATOR));

            return result.isEmpty() ? getFullSchema() : result;

        } catch (Exception e) {
            log.error("Schema RAG retrieval failed for query: {}", query, e);
            if (properties.isFailOnError()) {
                throw new SchemaRetrievalException("RAG retrieval failed: " + e.getMessage(), e);
            }
            return getFullSchema();
        }
    }

    @Override
    public String getFullSchema() {
        return fullSchemaContent;
    }

    @Override
    public Set<ChunkKind> getChunkKinds() {
        return Set.of(ChunkKind.values());
    }

    private static String loadResource(Resource resource) {
        try {
            if (resource != null && resource.exists()) {
                return resource.getContentAsString(StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("Failed to load fallback schema resource: {}", e.getMessage());
        }
        return "";
    }
}
