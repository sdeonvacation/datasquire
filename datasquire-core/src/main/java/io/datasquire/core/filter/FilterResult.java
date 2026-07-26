package io.datasquire.core.filter;

/**
 * The result of applying a response filter.
 *
 * @param content     the (possibly modified) response content
 * @param blocked     whether the response was blocked entirely
 * @param blockReason explanation if blocked (null otherwise)
 */
public record FilterResult(String content, boolean blocked, String blockReason) {

    /**
     * Creates a result that passes content through unchanged.
     *
     * @param content the original response content
     * @return a pass-through filter result
     */
    public static FilterResult passThrough(String content) {
        return new FilterResult(content, false, null);
    }

    /**
     * Creates a result with modified content.
     *
     * @param content the modified response content
     * @return a modified filter result
     */
    public static FilterResult modified(String content) {
        return new FilterResult(content, false, null);
    }

    /**
     * Creates a result indicating the response was blocked.
     *
     * @param reason explanation of why the response was blocked
     * @return a blocked filter result
     */
    public static FilterResult blocked(String reason) {
        return new FilterResult(null, true, reason);
    }
}
