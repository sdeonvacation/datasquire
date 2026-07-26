package io.datasquire.core.exception;

/**
 * Thrown when schema retrieval fails due to missing files, parsing errors,
 * or unavailable schema sources.
 */
public class SchemaRetrievalException extends DataSquireException {

    public SchemaRetrievalException(String message) {
        super("SCHEMA_RETRIEVAL_ERROR", message);
    }

    public SchemaRetrievalException(String message, Throwable cause) {
        super("SCHEMA_RETRIEVAL_ERROR", message, cause);
    }
}
