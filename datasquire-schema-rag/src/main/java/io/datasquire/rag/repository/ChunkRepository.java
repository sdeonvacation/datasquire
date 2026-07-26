package io.datasquire.rag.repository;

import io.datasquire.core.schema.ChunkKind;
import io.datasquire.core.schema.SchemaChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Repository for schema chunks stored with pgvector embeddings.
 */
@Component
public class ChunkRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<SchemaChunk> CHUNK_ROW_MAPPER = (rs, rowNum) -> mapRow(rs);

    public ChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds chunks whose kind is in the always-include list for the given namespace.
     */
    public List<SchemaChunk> findAlwaysIncluded(String namespace, List<String> kinds) {
        if (kinds == null || kinds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", Collections.nCopies(kinds.size(), "?"));
        String sql = """
                SELECT chunk_id, kind, title, content, embed_text
                FROM schema_chunks
                WHERE namespace = ? AND LOWER(kind) IN (%s)
                ORDER BY kind, title
                """.formatted(placeholders);

        Object[] params = new Object[kinds.size() + 1];
        params[0] = namespace;
        for (int i = 0; i < kinds.size(); i++) {
            params[i + 1] = kinds.get(i).toLowerCase();
        }

        return jdbcTemplate.query(sql, CHUNK_ROW_MAPPER, params);
    }

    /**
     * Finds chunks by cosine similarity to the query vector, excluding specified kinds.
     */
    public List<SchemaChunk> findByEmbeddingSimilarity(String namespace, float[] queryVector,
                                                       int topK, List<String> excludeKinds) {
        String vectorLiteral = toVectorLiteral(queryVector);

        String excludeClause = "";
        Object[] params;

        if (excludeKinds != null && !excludeKinds.isEmpty()) {
            String placeholders = String.join(",", Collections.nCopies(excludeKinds.size(), "?"));
            excludeClause = "AND LOWER(kind) NOT IN (%s)".formatted(placeholders);

            params = new Object[excludeKinds.size() + 2];
            params[0] = namespace;
            for (int i = 0; i < excludeKinds.size(); i++) {
                params[i + 1] = excludeKinds.get(i).toLowerCase();
            }
            params[params.length - 1] = topK;
        } else {
            params = new Object[]{namespace, topK};
        }

        String sql = """
                SELECT chunk_id, kind, title, content, embed_text
                FROM schema_chunks
                WHERE namespace = ? %s
                ORDER BY embedding <=> '%s'::vector
                LIMIT ?
                """.formatted(excludeClause, vectorLiteral);

        return jdbcTemplate.query(sql, CHUNK_ROW_MAPPER, params);
    }

    /**
     * Upserts a schema chunk with its embedding vector.
     */
    public void saveChunk(SchemaChunk chunk, float[] embedding, String namespace) {
        String vectorLiteral = toVectorLiteral(embedding);

        String sql = """
                INSERT INTO schema_chunks (chunk_id, namespace, kind, title, content, embed_text, embedding)
                VALUES (?, ?, ?, ?, ?, ?, ?::vector)
                ON CONFLICT (chunk_id, namespace)
                DO UPDATE SET kind = EXCLUDED.kind,
                              title = EXCLUDED.title,
                              content = EXCLUDED.content,
                              embed_text = EXCLUDED.embed_text,
                              embedding = EXCLUDED.embedding
                """;

        jdbcTemplate.update(sql, chunk.chunkId(), namespace, chunk.kind().name(),
                chunk.title(), chunk.content(), chunk.embedText(), vectorLiteral);
    }

    /**
     * Deletes all chunks for the given namespace.
     */
    public void deleteByNamespace(String namespace) {
        jdbcTemplate.update("DELETE FROM schema_chunks WHERE namespace = ?", namespace);
    }

    private static SchemaChunk mapRow(ResultSet rs) throws SQLException {
        return new SchemaChunk(
                rs.getString("chunk_id"),
                ChunkKind.valueOf(rs.getString("kind").toUpperCase()),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("embed_text")
        );
    }

    private static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
