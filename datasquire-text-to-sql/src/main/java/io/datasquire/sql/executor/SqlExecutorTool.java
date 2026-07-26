package io.datasquire.sql.executor;

import io.datasquire.core.exception.SqlExecutionException;
import io.datasquire.sql.config.TextToSqlProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI tool that executes read-only SQL queries and returns results as markdown tables.
 * Enforces safety constraints: row limits, column limits, cell truncation, and output size caps.
 */
public class SqlExecutorTool {

    private static final Logger log = LoggerFactory.getLogger(SqlExecutorTool.class);

    private final JdbcTemplate jdbcTemplate;
    private final TextToSqlProperties properties;
    private final SqlSafetyEnforcer safetyEnforcer;

    public SqlExecutorTool(JdbcTemplate jdbcTemplate,
                           TextToSqlProperties properties,
                           SqlSafetyEnforcer safetyEnforcer) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.safetyEnforcer = safetyEnforcer;
    }

    @Tool(description = "Execute a read-only SQL query against the database. " +
            "Returns results as a markdown table. Only SELECT statements are allowed.")
    public String executeSql(@ToolParam(description = "The SQL SELECT query to execute") String sql) {
        try {
            String validatedSql = safetyEnforcer.validate(sql);
            log.debug("Executing SQL: {}", validatedSql);

            int timeout = properties.getStatementTimeoutSeconds();
            List<String[]> rows = new ArrayList<>();
            String[] headers = jdbcTemplate.execute((Connection conn) -> {
                try (Statement timeoutStmt = conn.createStatement()) {
                    timeoutStmt.execute("SET LOCAL statement_timeout = '" + timeout + "s'");
                }
                try (PreparedStatement stmt = conn.prepareStatement(validatedSql)) {
                    stmt.setQueryTimeout(timeout);
                    try (ResultSet rs = stmt.executeQuery()) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int columnCount = Math.min(meta.getColumnCount(), properties.getMaxColumns());
                        String[] cols = new String[columnCount];
                        for (int i = 1; i <= columnCount; i++) {
                            cols[i - 1] = meta.getColumnLabel(i);
                        }

                        int rowCount = 0;
                        while (rs.next() && rowCount < properties.getMaxRows()) {
                            String[] row = new String[columnCount];
                            for (int i = 1; i <= columnCount; i++) {
                                Object value = rs.getObject(i);
                                row[i - 1] = value != null ? value.toString() : "NULL";
                            }
                            rows.add(row);
                            rowCount++;
                        }
                        return cols;
                    }
                }
            });

            if (headers == null || headers.length == 0) {
                return "(empty result set)";
            }

            return renderMarkdown(headers, rows);
        } catch (SqlExecutionException e) {
            log.warn("SQL safety violation: {}", e.getMessage());
            return "[sql error] " + e.getMessage();
        } catch (Exception e) {
            log.warn("SQL execution failed: {}", e.getMessage());
            return "[sql error] " + e.getMessage();
        }
    }

    private String renderMarkdown(String[] headers, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();

        // Header row
        sb.append("| ");
        for (String header : headers) {
            sb.append(truncateCell(header)).append(" | ");
        }
        sb.append("\n");

        // Separator row
        sb.append("| ");
        for (int i = 0; i < headers.length; i++) {
            sb.append("---").append(" | ");
        }
        sb.append("\n");

        // Data rows
        int renderedRows = 0;
        for (String[] row : rows) {
            sb.append("| ");
            for (String cell : row) {
                sb.append(truncateCell(cell)).append(" | ");
            }
            sb.append("\n");
            renderedRows++;

            if (sb.length() > properties.getMaxOutputChars()) {
                sb.append("\n... (output truncated, showing ")
                        .append(renderedRows).append(" of ").append(rows.size()).append(" rows)\n");
                break;
            }
        }

        // Row count footer
        sb.append("\n_").append(rows.size()).append(" row(s) returned");
        if (rows.size() >= properties.getMaxRows()) {
            sb.append(" (limit reached)");
        }
        sb.append("_");

        return sb.toString();
    }

    private String truncateCell(String value) {
        if (value == null) {
            return "NULL";
        }
        if (value.length() <= properties.getMaxCellChars()) {
            return value;
        }
        int remaining = value.length() - properties.getMaxCellChars();
        return value.substring(0, properties.getMaxCellChars()) + "...[" + remaining + " more]";
    }
}
