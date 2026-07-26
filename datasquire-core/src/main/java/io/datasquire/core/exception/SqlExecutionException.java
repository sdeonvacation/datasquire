package io.datasquire.core.exception;

/**
 * Thrown when SQL execution fails due to syntax errors, permission issues,
 * or database connectivity problems.
 */
public class SqlExecutionException extends DataSquireException {

    public SqlExecutionException(String message) {
        super("SQL_EXECUTION_ERROR", message);
    }

    public SqlExecutionException(String message, Throwable cause) {
        super("SQL_EXECUTION_ERROR", message, cause);
    }
}
