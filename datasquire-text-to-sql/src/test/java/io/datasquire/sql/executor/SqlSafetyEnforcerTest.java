package io.datasquire.sql.executor;

import io.datasquire.core.exception.SqlExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SqlSafetyEnforcer. Verifies that only SELECT queries pass validation
 * and DDL/DML operations are rejected.
 */
class SqlSafetyEnforcerTest {

    private SqlSafetyEnforcer enforcer;

    @BeforeEach
    void setUp() {
        enforcer = new SqlSafetyEnforcer();
    }

    @ParameterizedTest(name = "rejects unsafe SQL: {0}")
    @ValueSource(strings = {
            "INSERT INTO users (name) VALUES ('test')",
            "UPDATE users SET name = 'x' WHERE id = 1",
            "DELETE FROM users WHERE id = 1",
            "DROP TABLE users",
            "ALTER TABLE users ADD COLUMN age INT",
            "TRUNCATE TABLE users",
            "CREATE TABLE evil (id INT)",
            "MERGE INTO users USING src ON users.id = src.id WHEN MATCHED THEN UPDATE SET name = 'x'",
            "GRANT ALL ON users TO public",
            "REVOKE SELECT ON users FROM public",
            "REINDEX TABLE users"
    })
    void validate_rejectsUnsafeStatements(String sql) {
        SqlExecutionException ex = assertThrows(SqlExecutionException.class, () -> enforcer.validate(sql));
        assertTrue(ex.getMessage().contains("Unsafe SQL operation detected"));
    }

    @ParameterizedTest(name = "rejects case-insensitive unsafe SQL: {0}")
    @ValueSource(strings = {
            "drop table users",
            "DROP TABLE users",
            "  DROP TABLE users",
            "insert into users values (1)",
            "  update users set x = 1"
    })
    void validate_rejectsCaseInsensitive(String sql) {
        assertThrows(SqlExecutionException.class, () -> enforcer.validate(sql));
    }

    @Test
    void validate_acceptsSelectQuery() {
        String result = enforcer.validate("SELECT * FROM users");
        assertEquals("SELECT * FROM users", result);
    }

    @Test
    void validate_acceptsWithCte() {
        String sql = "WITH cte AS (SELECT id FROM users) SELECT * FROM cte";
        String result = enforcer.validate(sql);
        assertEquals(sql, result);
    }

    @Test
    void validate_stripsTrailingSemicolons() {
        String result = enforcer.validate("SELECT 1;;;");
        assertEquals("SELECT 1", result);
    }

    @Test
    void validate_stripsTrailingSemicolonWithWhitespace() {
        String result = enforcer.validate("SELECT 1 ;  ;  ");
        assertEquals("SELECT 1", result);
    }

    @Test
    void validate_throwsOnNull() {
        assertThrows(SqlExecutionException.class, () -> enforcer.validate(null));
    }

    @Test
    void validate_throwsOnBlank() {
        assertThrows(SqlExecutionException.class, () -> enforcer.validate("   "));
    }

    @Test
    void validate_acceptsSelectWithSubquery() {
        String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)";
        String result = enforcer.validate(sql);
        assertEquals(sql, result);
    }

    @Test
    void validate_acceptsExplainSelect() {
        // EXPLAIN is not in blocked list, so it passes
        String sql = "EXPLAIN SELECT * FROM users";
        String result = enforcer.validate(sql);
        assertEquals(sql, result);
    }

    // --- CTE-based DML bypass tests (C1 fix) ---

    @ParameterizedTest(name = "rejects CTE-based DML bypass: {0}")
    @ValueSource(strings = {
            "WITH del AS (DELETE FROM users RETURNING *) SELECT * FROM del",
            "WITH upd AS (UPDATE users SET name = 'x' RETURNING *) SELECT * FROM upd",
            "WITH ins AS (INSERT INTO users (name) VALUES ('x') RETURNING *) SELECT * FROM ins",
            "WITH t AS (TRUNCATE TABLE users) SELECT 1",
            "SELECT * FROM (DELETE FROM users RETURNING *) sub"
    })
    void validate_rejectsCteDmlBypass(String sql) {
        SqlExecutionException ex = assertThrows(SqlExecutionException.class, () -> enforcer.validate(sql));
        assertTrue(ex.getMessage().contains("Unsafe SQL operation detected"));
    }

    @ParameterizedTest(name = "rejects extended unsafe commands: {0}")
    @ValueSource(strings = {
            "COPY users TO '/tmp/out.csv'",
            "CALL some_procedure()",
            "DO $$ BEGIN PERFORM 1; END $$",
            "EXECUTE some_plan",
            "PREPARE stmt AS DELETE FROM users",
            "LISTEN channel_name",
            "NOTIFY channel_name",
            "VACUUM users"
    })
    void validate_rejectsExtendedUnsafeCommands(String sql) {
        assertThrows(SqlExecutionException.class, () -> enforcer.validate(sql));
    }

    @Test
    void validate_rejectsSetWithoutSearchPath() {
        assertThrows(SqlExecutionException.class, () -> enforcer.validate("SET role = 'admin'"));
    }

    @Test
    void validate_allowsSetSearchPath() {
        String sql = "SET search_path TO public";
        // SET search_path is the only allowed SET variant
        String result = enforcer.validate(sql);
        assertEquals(sql, result);
    }

    @Test
    void validate_allowsDmlKeywordsInsideStringLiterals() {
        // DML keywords inside string literals should NOT trigger rejection
        String sql = "SELECT * FROM logs WHERE message = 'DELETE FROM users failed'";
        String result = enforcer.validate(sql);
        assertEquals(sql, result);
    }

    @Test
    void validate_allowsDmlKeywordsInsideEscapedStringLiterals() {
        String sql = "SELECT * FROM logs WHERE msg = 'can''t INSERT data'";
        String result = enforcer.validate(sql);
        assertEquals(sql, result);
    }

    @Test
    void validate_rejectsDmlOutsideLiteralEvenWhenLiteralPresent() {
        // Keyword outside the string literal triggers rejection
        String sql = "SELECT 'safe text' FROM users; DELETE FROM users";
        assertThrows(SqlExecutionException.class, () -> enforcer.validate(sql));
    }

    // --- Matched keyword in error message tests ---

    @Test
    void validate_errorMessageIncludesMatchedKeyword_delete() {
        SqlExecutionException ex = assertThrows(SqlExecutionException.class,
                () -> enforcer.validate("WITH del AS (DELETE FROM users RETURNING *) SELECT * FROM del"));
        assertTrue(ex.getMessage().contains("DELETE"), "Should include matched keyword DELETE");
    }

    @Test
    void validate_errorMessageIncludesMatchedKeyword_insert() {
        SqlExecutionException ex = assertThrows(SqlExecutionException.class,
                () -> enforcer.validate("INSERT INTO users VALUES (1)"));
        assertTrue(ex.getMessage().contains("INSERT"), "Should include matched keyword INSERT");
    }

    @Test
    void validate_errorMessageIncludesMatchedKeyword_reindex() {
        SqlExecutionException ex = assertThrows(SqlExecutionException.class,
                () -> enforcer.validate("REINDEX TABLE users"));
        assertTrue(ex.getMessage().contains("REINDEX"), "Should include matched keyword REINDEX");
    }

    @Test
    void validate_errorMessageIncludesSet() {
        SqlExecutionException ex = assertThrows(SqlExecutionException.class,
                () -> enforcer.validate("SET role = 'admin'"));
        assertTrue(ex.getMessage().contains("SET"), "Should include SET in message");
    }

    @Test
    void validate_allowsDeletedStatusInLiteral() {
        // Critical: DELETED inside string literal must NOT trigger rejection
        String sql = "SELECT * FROM orders WHERE status = 'DELETED'";
        String result = enforcer.validate(sql);
        assertEquals(sql, result);
    }

    @Test
    void validate_rejectsReindexInCte() {
        assertThrows(SqlExecutionException.class,
                () -> enforcer.validate("WITH r AS (REINDEX INDEX idx_users) SELECT 1"));
    }
}
