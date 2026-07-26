package io.datasquire.core.exception;

/**
 * Thrown when the orchestration layer encounters an unrecoverable error
 * during agent coordination, routing, or pipeline execution.
 */
public class OrchestrationException extends DataSquireException {

    public OrchestrationException(String message) {
        super("ORCHESTRATION_ERROR", message);
    }

    public OrchestrationException(String code, String message) {
        super(code, message);
    }

    public OrchestrationException(String message, Throwable cause) {
        super("ORCHESTRATION_ERROR", message, cause);
    }
}
