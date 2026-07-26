package io.datasquire.core.scope;

import io.datasquire.core.agent.RequestContext;

/**
 * SPI for enforcing query scope boundaries. Implementations define what queries
 * are permissible based on configuration and request context.
 */
public interface ScopeEnforcer {

    /**
     * Determines whether a query is within the allowed scope for the given context.
     *
     * @param query   the user's natural language query
     * @param context the request context (tenant, user, attributes)
     * @return the scope check result
     */
    ScopeResult isInScope(String query, RequestContext context);

    /**
     * Returns a human-readable description of the enforced scope boundaries.
     *
     * @return description of what is in and out of scope
     */
    String getScopeDescription();
}
