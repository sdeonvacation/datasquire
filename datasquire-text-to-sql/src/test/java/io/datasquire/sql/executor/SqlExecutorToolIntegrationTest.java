package io.datasquire.sql.executor;

import io.datasquire.sql.config.TextToSqlProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for SqlExecutorTool using a real PostgreSQL (pgvector) container.
 * Validates SQL execution, markdown rendering, row capping, and safety enforcement.
 */
@Testcontainers
class SqlExecutorToolIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17")
            .withDatabaseName("datasquire_test")
            .withUsername("test")
            .withPassword("test");

    private static JdbcTemplate jdbcTemplate;
    private SqlExecutorTool tool;
    private TextToSqlProperties properties;

    @BeforeAll
    static void setupSchema() {
        DataSource ds = createDataSource();
        jdbcTemplate = new JdbcTemplate(ds);

        jdbcTemplate.execute("""
                CREATE TABLE employees (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    department VARCHAR(50) NOT NULL,
                    salary NUMERIC(10, 2) NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                INSERT INTO employees (name, department, salary) VALUES
                ('Alice Johnson', 'Engineering', 95000.00),
                ('Bob Smith', 'Engineering', 88000.00),
                ('Carol White', 'Marketing', 72000.00),
                ('David Brown', 'Marketing', 68000.00),
                ('Eve Davis', 'Sales', 76000.00)
                """);
    }

    @BeforeEach
    void setUp() {
        properties = new TextToSqlProperties();
        SqlSafetyEnforcer safetyEnforcer = new SqlSafetyEnforcer();
        tool = new SqlExecutorTool(jdbcTemplate, properties, safetyEnforcer);
    }

    @Test
    void executeSql_happyPath_returnsMarkdownTable() {
        String result = tool.executeSql("SELECT id, name, department, salary FROM employees ORDER BY id");

        // Verify markdown table structure
        assertTrue(result.contains("| id |"), "Should contain id header");
        assertTrue(result.contains("| name |"), "Should contain name header");
        assertTrue(result.contains("| --- |"), "Should contain separator row");

        // Verify all 5 rows present
        assertTrue(result.contains("Alice Johnson"));
        assertTrue(result.contains("Bob Smith"));
        assertTrue(result.contains("Carol White"));
        assertTrue(result.contains("David Brown"));
        assertTrue(result.contains("Eve Davis"));

        // Verify row count footer
        assertTrue(result.contains("_5 row(s) returned_"));
    }

    @Test
    void executeSql_withAggregation_returnsCorrectResult() {
        String result = tool.executeSql(
                "SELECT department, AVG(salary) AS avg_salary FROM employees GROUP BY department ORDER BY department");

        // Engineering avg: (95000 + 88000) / 2 = 91500
        assertTrue(result.contains("Engineering"));
        assertTrue(result.contains("91500"));

        // Marketing avg: (72000 + 68000) / 2 = 70000
        assertTrue(result.contains("Marketing"));
        assertTrue(result.contains("70000"));

        // Sales avg: 76000
        assertTrue(result.contains("Sales"));
        assertTrue(result.contains("76000"));

        // 3 departments = 3 rows
        assertTrue(result.contains("_3 row(s) returned_"));
    }

    @Test
    void executeSql_rejectsDdl_returnsError() {
        String result = tool.executeSql("DROP TABLE employees");

        assertTrue(result.startsWith("[sql error]"), "DDL should be rejected");
        assertTrue(result.contains("Unsafe SQL operation detected"),
                "Error message should mention unsafe operation");
    }

    @Test
    void executeSql_maxRowsCap_truncatesResults() {
        properties.setMaxRows(2);

        String result = tool.executeSql("SELECT id, name FROM employees ORDER BY id");

        // Should contain first 2 rows
        assertTrue(result.contains("Alice Johnson"));
        assertTrue(result.contains("Bob Smith"));

        // Should NOT contain row 3+
        assertFalse(result.contains("Carol White"));
        assertFalse(result.contains("David Brown"));
        assertFalse(result.contains("Eve Davis"));

        // Footer indicates limit reached
        assertTrue(result.contains("_2 row(s) returned (limit reached)_"));
    }

    @Test
    void executeSql_emptyResult_returnsTableWithZeroRows() {
        String result = tool.executeSql("SELECT * FROM employees WHERE salary > 999999");

        // Headers still present from metadata
        assertTrue(result.contains("| id |"));
        assertTrue(result.contains("| --- |"));
        // No data rows but footer shows 0
        assertTrue(result.contains("_0 row(s) returned_"));
        // Should NOT contain any employee data
        assertFalse(result.contains("Alice"));
    }

    @Test
    void executeSql_withCte_succeeds() {
        String result = tool.executeSql("""
                WITH dept_stats AS (
                    SELECT department, COUNT(*) AS cnt
                    FROM employees
                    GROUP BY department
                )
                SELECT * FROM dept_stats ORDER BY department
                """);

        assertTrue(result.contains("Engineering"));
        assertTrue(result.contains("Marketing"));
        assertTrue(result.contains("Sales"));
        assertFalse(result.startsWith("[sql error]"));
    }

    private static DataSource createDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(postgres.getJdbcUrl());
        ds.setUsername(postgres.getUsername());
        ds.setPassword(postgres.getPassword());
        return ds;
    }
}
