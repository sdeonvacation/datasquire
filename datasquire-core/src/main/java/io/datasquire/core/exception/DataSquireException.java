package io.datasquire.core.exception;

/**
 * Base exception for all DataSquire runtime errors.
 * Carries a machine-readable error code for programmatic handling.
 */
public class DataSquireException extends RuntimeException {

    private final String code;

    /**
     * @param code    machine-readable error code
     * @param message human-readable error description
     */
    public DataSquireException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * @param code    machine-readable error code
     * @param message human-readable error description
     * @param cause   the underlying cause
     */
    public DataSquireException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * @return the machine-readable error code
     */
    public String getCode() {
        return code;
    }
}
