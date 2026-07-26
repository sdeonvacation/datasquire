package io.datasquire.ingest.introspection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates schema reference markdown by introspecting PostgreSQL information_schema.
 */
@Component
@ConditionalOnBean(JdbcTemplate.class)
public class SchemaIntrospector {

    private static final Logger log = LoggerFactory.getLogger(SchemaIntrospector.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaIntrospector(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Introspects the public schema and generates a markdown reference document.
     *
     * @return markdown string describing all tables, columns, types, and row counts
     */
    public String introspect() {
        log.info("Introspecting database schema...");

        List<String> tables = fetchTables();
        Map<String, Long> rowCounts = fetchRowCounts();

        StringBuilder md = new StringBuilder();
        md.append("# Database Schema Reference\n\n");
        md.append("Auto-generated from database introspection.\n\n");

        for (String table : tables) {
            long rowCount = rowCounts.getOrDefault(table, -1L);
            md.append("## ").append(table).append("\n\n");

            if (rowCount >= 0) {
                md.append("Estimated row count: ~").append(rowCount).append("\n\n");
            }

            List<ColumnInfo> columns = fetchColumns(table);
            if (!columns.isEmpty()) {
                md.append("| Column | Type | Nullable | Default |\n");
                md.append("| --- | --- | --- | --- |\n");
                for (ColumnInfo col : columns) {
                    md.append("| ").append(col.name)
                            .append(" | ").append(col.dataType)
                            .append(" | ").append(col.nullable ? "YES" : "NO")
                            .append(" | ").append(col.defaultValue != null ? col.defaultValue : "")
                            .append(" |\n");
                }
            }

            md.append("\n");
        }

        log.info("Introspection complete: {} tables found", tables.size());
        return md.toString();
    }

    private List<String> fetchTables() {
        return jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """, String.class);
    }

    private Map<String, Long> fetchRowCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT relname, n_live_tup
                FROM pg_stat_user_tables
                WHERE schemaname = 'public'
                """, rs -> {
            counts.put(rs.getString("relname"), rs.getLong("n_live_tup"));
        });
        return counts;
    }

    private List<ColumnInfo> fetchColumns(String tableName) {
        return jdbcTemplate.query("""
                SELECT column_name, data_type, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                ORDER BY ordinal_position
                """, (rs, rowNum) -> new ColumnInfo(
                rs.getString("column_name"),
                rs.getString("data_type"),
                "YES".equals(rs.getString("is_nullable")),
                rs.getString("column_default")
        ), tableName);
    }

    private record ColumnInfo(String name, String dataType, boolean nullable, String defaultValue) {
    }
}
