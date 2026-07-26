package io.datasquire.ingest.enricher;

import io.datasquire.core.schema.ChunkKind;
import io.datasquire.core.schema.SchemaChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enriches TABLE chunks with sample rows queried from the live database.
 */
@Component
@ConditionalOnBean(JdbcTemplate.class)
public class SampleRowEnricher {

    private static final Logger log = LoggerFactory.getLogger(SampleRowEnricher.class);
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^##\\s+(.+)$", Pattern.MULTILINE);

    private final JdbcTemplate jdbcTemplate;
    private final int rowsPerTable;
    private final int maxCellChars;

    public SampleRowEnricher(JdbcTemplate jdbcTemplate,
                             @Value("${datasquire.ingest.rows-per-table:3}") int rowsPerTable,
                             @Value("${datasquire.ingest.max-cell-chars:120}") int maxCellChars) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowsPerTable = rowsPerTable;
        this.maxCellChars = maxCellChars;
    }

    /**
     * Enriches TABLE chunks with sample data rows from the database.
     * Non-TABLE chunks pass through unchanged.
     *
     * @param chunks input chunks
     * @return new list with TABLE chunks enriched
     */
    public List<SchemaChunk> enrich(List<SchemaChunk> chunks) {
        List<SchemaChunk> result = new ArrayList<>(chunks.size());
        for (SchemaChunk chunk : chunks) {
            if (chunk.kind() == ChunkKind.TABLE) {
                result.add(enrichTableChunk(chunk));
            } else {
                result.add(chunk);
            }
        }
        return result;
    }

    private SchemaChunk enrichTableChunk(SchemaChunk chunk) {
        String tableName = extractTableName(chunk.title());
        if (tableName == null) {
            return chunk;
        }

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT * FROM " + quoteIdentifier(tableName) + " LIMIT " + rowsPerTable);

            if (rows.isEmpty()) {
                return chunk;
            }

            String markdownTable = renderMarkdownTable(rows);
            String enrichedContent = chunk.content() + "\n\n### Sample rows\n\n" + markdownTable;

            return new SchemaChunk(chunk.chunkId(), chunk.kind(), chunk.title(),
                    enrichedContent, chunk.embedText());
        } catch (Exception e) {
            log.debug("Skipping sample rows for table '{}': {}", tableName, e.getMessage());
            return chunk;
        }
    }

    private String extractTableName(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        // Title is typically the table name from H2 heading
        String cleaned = title.replaceAll("[`\"']", "").trim();
        // Validate: only allow schema-qualified or simple identifiers
        if (cleaned.matches("[a-zA-Z_][a-zA-Z0-9_.]*")) {
            return cleaned;
        }
        return null;
    }

    private String renderMarkdownTable(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "";
        }

        List<String> columns = new ArrayList<>(rows.get(0).keySet());
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("| ");
        for (String col : columns) {
            sb.append(col).append(" | ");
        }
        sb.append("\n|");
        for (int i = 0; i < columns.size(); i++) {
            sb.append(" --- |");
        }
        sb.append("\n");

        // Rows
        for (Map<String, Object> row : rows) {
            sb.append("| ");
            for (String col : columns) {
                Object val = row.get(col);
                String cell = val == null ? "NULL" : val.toString();
                if (cell.length() > maxCellChars) {
                    cell = cell.substring(0, maxCellChars - 3) + "...";
                }
                // Escape pipe chars in cell content
                cell = cell.replace("|", "\\|");
                sb.append(cell).append(" | ");
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    private static String quoteIdentifier(String identifier) {
        // Double-quote to handle reserved words; prevent injection by rejecting non-identifiers
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
