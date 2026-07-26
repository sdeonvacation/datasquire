package io.datasquire.sql.executor;

import io.datasquire.core.exception.SqlExecutionException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates SQL statements to ensure only read-only queries are executed.
 * Rejects DDL and DML operations that could mutate the database.
 * Scans the FULL query body (not just the first keyword) to block CTE-based DML bypass.
 */
public class SqlSafetyEnforcer {

    private static final Pattern UNSAFE_PATTERN = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|TRUNCATE|DROP|ALTER|CREATE|MERGE|REPLACE|GRANT|REVOKE" +
                    "|COPY|CALL|DO|EXECUTE|PREPARE|LISTEN|NOTIFY|VACUUM|REINDEX)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SET_PATTERN = Pattern.compile(
            "\\bSET\\b(?!\\s+search_path\\b)",
            Pattern.CASE_INSENSITIVE
    );

    // Matches single-quoted string literals (including escaped quotes '')
    private static final Pattern STRING_LITERAL = Pattern.compile("'([^']|'')*'");

    /**
     * Validates a SQL statement is safe to execute (read-only).
     * Strips trailing semicolons and checks for unsafe operations anywhere in the query body.
     * String literals are excluded from scanning to avoid false positives.
     *
     * @param sql the SQL statement to validate
     * @return the sanitized SQL (trailing semicolons removed)
     * @throws SqlExecutionException if the statement contains DDL/DML operations
     */
    public String validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new SqlExecutionException("SQL statement cannot be null or empty");
        }

        String sanitized = sql.stripTrailing();
        while (sanitized.endsWith(";")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1).stripTrailing();
        }

        // Strip string literals before scanning for unsafe keywords
        String strippedOfLiterals = STRING_LITERAL.matcher(sanitized).replaceAll("");

        Matcher unsafeMatcher = UNSAFE_PATTERN.matcher(strippedOfLiterals);
        if (unsafeMatcher.find()) {
            throw new SqlExecutionException(
                    "Unsafe SQL operation detected: " + unsafeMatcher.group(1).toUpperCase()
                            + ". Only SELECT queries are allowed.");
        }

        Matcher setMatcher = SET_PATTERN.matcher(strippedOfLiterals);
        if (setMatcher.find()) {
            throw new SqlExecutionException(
                    "Unsafe SQL operation detected: SET. Only SELECT queries are allowed.");
        }

        return sanitized;
    }
}
