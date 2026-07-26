package io.datasquire.sql.executor;

import io.datasquire.sql.config.TextToSqlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SqlExecutorTool verifying statement timeout enforcement
 * and proper Connection-level execution.
 */
@ExtendWith(MockitoExtension.class)
class SqlExecutorToolTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private Connection connection;
    @Mock
    private Statement timeoutStatement;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private ResultSet resultSet;
    @Mock
    private ResultSetMetaData metaData;

    private TextToSqlProperties properties;
    private SqlSafetyEnforcer safetyEnforcer;
    private SqlExecutorTool tool;

    @BeforeEach
    void setUp() {
        properties = new TextToSqlProperties();
        properties.setStatementTimeoutSeconds(15);
        safetyEnforcer = new SqlSafetyEnforcer();
        tool = new SqlExecutorTool(jdbcTemplate, properties, safetyEnforcer);
    }

    @SuppressWarnings("unchecked")
    @Test
    void executeSql_setsStatementTimeoutViaSetLocal() throws Exception {
        // Capture the ConnectionCallback passed to jdbcTemplate.execute
        ArgumentCaptor<ConnectionCallback<String[]>> captor = ArgumentCaptor.forClass(ConnectionCallback.class);

        when(jdbcTemplate.execute(captor.capture())).thenAnswer(invocation -> {
            ConnectionCallback<String[]> callback = captor.getValue();
            return callback.doInConnection(connection);
        });

        when(connection.createStatement()).thenReturn(timeoutStatement);
        when(connection.prepareStatement("SELECT 1")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("col1");
        when(resultSet.next()).thenReturn(false);

        tool.executeSql("SELECT 1");

        // Verify SET LOCAL statement_timeout was executed
        verify(timeoutStatement).execute("SET LOCAL statement_timeout = '15s'");
        // Verify JDBC query timeout also set
        verify(preparedStatement).setQueryTimeout(15);
    }

    @SuppressWarnings("unchecked")
    @Test
    void executeSql_usesConfiguredTimeout() throws Exception {
        properties.setStatementTimeoutSeconds(45);

        ArgumentCaptor<ConnectionCallback<String[]>> captor = ArgumentCaptor.forClass(ConnectionCallback.class);

        when(jdbcTemplate.execute(captor.capture())).thenAnswer(invocation -> {
            ConnectionCallback<String[]> callback = captor.getValue();
            return callback.doInConnection(connection);
        });

        when(connection.createStatement()).thenReturn(timeoutStatement);
        when(connection.prepareStatement("SELECT 1")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("col1");
        when(resultSet.next()).thenReturn(false);

        tool.executeSql("SELECT 1");

        verify(timeoutStatement).execute("SET LOCAL statement_timeout = '45s'");
        verify(preparedStatement).setQueryTimeout(45);
    }

    @Test
    void executeSql_rejectsSqlInjectionViaSafety() {
        String result = tool.executeSql("DELETE FROM users");
        assertTrue(result.startsWith("[sql error]"));
        assertTrue(result.contains("Unsafe SQL operation detected: DELETE"));
        // jdbcTemplate should never be called
        verifyNoInteractions(jdbcTemplate);
    }

    @SuppressWarnings("unchecked")
    @Test
    void executeSql_returnsMarkdownOnSuccess() throws Exception {
        ArgumentCaptor<ConnectionCallback<String[]>> captor = ArgumentCaptor.forClass(ConnectionCallback.class);

        when(jdbcTemplate.execute(captor.capture())).thenAnswer(invocation -> {
            ConnectionCallback<String[]> callback = captor.getValue();
            return callback.doInConnection(connection);
        });

        when(connection.createStatement()).thenReturn(timeoutStatement);
        when(connection.prepareStatement("SELECT name FROM users")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn("Alice");

        String result = tool.executeSql("SELECT name FROM users");

        assertTrue(result.contains("| name |"));
        assertTrue(result.contains("| --- |"));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("_1 row(s) returned_"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void executeSql_returnsErrorOnSqlException() throws Exception {
        when(jdbcTemplate.execute(any(ConnectionCallback.class)))
                .thenThrow(new RuntimeException("connection refused"));

        String result = tool.executeSql("SELECT 1");

        assertTrue(result.startsWith("[sql error]"));
        assertTrue(result.contains("connection refused"));
    }
}
