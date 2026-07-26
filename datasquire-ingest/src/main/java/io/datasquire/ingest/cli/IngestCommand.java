package io.datasquire.ingest.cli;

import io.datasquire.core.schema.ChunkKind;
import io.datasquire.core.schema.SchemaChunk;
import io.datasquire.ingest.chunker.SchemaChunker;
import io.datasquire.ingest.enricher.SampleRowEnricher;
import io.datasquire.ingest.introspection.SchemaIntrospector;
import io.datasquire.rag.embedding.EmbeddingService;
import io.datasquire.rag.repository.ChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CLI runner that orchestrates the schema ingestion pipeline:
 * parse markdown -> chunk -> optionally enrich -> embed -> store.
 */
@Component
@ConditionalOnProperty(name = "datasquire.ingest.enabled", havingValue = "true")
public class IngestCommand implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestCommand.class);

    private final SchemaChunker chunker;
    private final EmbeddingService embeddingService;
    private final ChunkRepository chunkRepository;
    private final Optional<SampleRowEnricher> sampleRowEnricher;
    private final Optional<SchemaIntrospector> schemaIntrospector;

    @Value("${datasquire.ingest.namespace:default}")
    private String defaultNamespace;

    public IngestCommand(SchemaChunker chunker,
                         EmbeddingService embeddingService,
                         ChunkRepository chunkRepository,
                         Optional<SampleRowEnricher> sampleRowEnricher,
                         Optional<SchemaIntrospector> schemaIntrospector) {
        this.chunker = chunker;
        this.embeddingService = embeddingService;
        this.chunkRepository = chunkRepository;
        this.sampleRowEnricher = sampleRowEnricher;
        this.schemaIntrospector = schemaIntrospector;
    }

    @Override
    public void run(String... args) throws Exception {
        long startTime = System.currentTimeMillis();
        String namespace = parseArg(args, "--namespace", defaultNamespace);
        boolean reset = hasFlag(args, "--reset");

        String markdownContent = resolveContent(args);
        if (markdownContent == null) {
            log.error("Usage: --schema-file=<path> OR --introspect");
            log.error("  Optional flags: --namespace=<name> --reset");
            return;
        }

        log.info("Starting schema ingestion into namespace '{}'", namespace);

        if (reset) {
            log.info("Resetting namespace '{}'...", namespace);
            chunkRepository.deleteByNamespace(namespace);
        }

        // Chunk
        List<SchemaChunk> chunks = chunker.chunk(markdownContent);
        log.info("Chunked into {} pieces", chunks.size());

        // Enrich TABLE chunks with sample rows
        if (sampleRowEnricher.isPresent()) {
            log.info("Enriching TABLE chunks with sample rows...");
            chunks = sampleRowEnricher.get().enrich(chunks);
        }

        // Embed
        List<String> embedTexts = chunks.stream().map(SchemaChunk::embedText).toList();
        log.info("Embedding {} chunks...", embedTexts.size());
        List<float[]> embeddings = embeddingService.embedBatch(embedTexts);

        // Store
        log.info("Storing chunks in vector store...");
        for (int i = 0; i < chunks.size(); i++) {
            chunkRepository.saveChunk(chunks.get(i), embeddings.get(i), namespace);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        printSummary(chunks, elapsed, namespace);
    }

    private String resolveContent(String[] args) throws IOException {
        String schemaFile = parseArg(args, "--schema-file", null);
        boolean introspect = hasFlag(args, "--introspect");

        if (schemaFile != null) {
            Path path = Path.of(schemaFile);
            if (!Files.exists(path)) {
                log.error("Schema file not found: {}", schemaFile);
                return null;
            }
            log.info("Reading schema from file: {}", path.toAbsolutePath());
            return Files.readString(path);
        }

        if (introspect) {
            if (schemaIntrospector.isEmpty()) {
                log.error("--introspect requires a database connection (JdbcTemplate not available)");
                return null;
            }
            return schemaIntrospector.get().introspect();
        }

        return null;
    }

    private void printSummary(List<SchemaChunk> chunks, long elapsedMs, String namespace) {
        Map<ChunkKind, Integer> counts = new EnumMap<>(ChunkKind.class);
        for (SchemaChunk chunk : chunks) {
            counts.merge(chunk.kind(), 1, Integer::sum);
        }

        log.info("=== Ingestion Complete ===");
        log.info("Namespace: {}", namespace);
        log.info("Total chunks: {}", chunks.size());
        for (Map.Entry<ChunkKind, Integer> entry : counts.entrySet()) {
            log.info("  {}: {}", entry.getKey(), entry.getValue());
        }
        log.info("Time elapsed: {} ms", elapsedMs);
    }

    private static String parseArg(String[] args, String prefix, String defaultValue) {
        for (String arg : args) {
            if (arg.startsWith(prefix + "=")) {
                return arg.substring(prefix.length() + 1);
            }
        }
        return defaultValue;
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equals(flag)) {
                return true;
            }
        }
        return false;
    }
}
