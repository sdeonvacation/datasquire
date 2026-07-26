package io.datasquire.sql.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the text-to-sql executor.
 */
@ConfigurationProperties(prefix = "datasquire.sql-executor")
public class TextToSqlProperties {

    private int maxRows = 50;
    private int maxColumns = 20;
    private int maxOutputChars = 16000;
    private int maxCellChars = 200;
    private int statementTimeoutSeconds = 30;
    private boolean readOnly = true;
    private int maxIterations = 5;

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public int getMaxColumns() {
        return maxColumns;
    }

    public void setMaxColumns(int maxColumns) {
        this.maxColumns = maxColumns;
    }

    public int getMaxOutputChars() {
        return maxOutputChars;
    }

    public void setMaxOutputChars(int maxOutputChars) {
        this.maxOutputChars = maxOutputChars;
    }

    public int getMaxCellChars() {
        return maxCellChars;
    }

    public void setMaxCellChars(int maxCellChars) {
        this.maxCellChars = maxCellChars;
    }

    public int getStatementTimeoutSeconds() {
        return statementTimeoutSeconds;
    }

    public void setStatementTimeoutSeconds(int statementTimeoutSeconds) {
        this.statementTimeoutSeconds = statementTimeoutSeconds;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }
}
