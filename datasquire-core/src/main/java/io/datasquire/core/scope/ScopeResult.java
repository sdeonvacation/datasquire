package io.datasquire.core.scope;

/**
 * The result of a scope enforcement check.
 *
 * @param inScope          whether the query is within the allowed scope
 * @param rejectionMessage explanation when the query is out of scope (null when in scope)
 */
public record ScopeResult(boolean inScope, String rejectionMessage) {

    /**
     * Creates a result indicating the query is allowed.
     *
     * @return a passing scope result
     */
    public static ScopeResult allowed() {
        return new ScopeResult(true, null);
    }

    /**
     * Creates a result indicating the query is rejected with a reason.
     *
     * @param message explanation of why the query is out of scope
     * @return a rejecting scope result
     */
    public static ScopeResult rejected(String message) {
        return new ScopeResult(false, message);
    }
}
