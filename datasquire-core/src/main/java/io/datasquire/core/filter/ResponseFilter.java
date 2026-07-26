package io.datasquire.core.filter;

import io.datasquire.core.agent.RequestContext;

/**
 * SPI for filtering or blocking agent responses before they reach the user.
 * Filters can sanitize, redact, or entirely block responses based on content and context.
 * Multiple filters are applied in order determined by {@link #getOrder()}.
 */
public interface ResponseFilter {

    /**
     * Applies this filter to a response.
     *
     * @param response the response content to filter
     * @param context  the request context for policy evaluation
     * @return the filter result (pass-through, modified, or blocked)
     */
    FilterResult filter(String response, RequestContext context);

    /**
     * Returns the execution order for this filter. Lower values execute first.
     *
     * @return the filter order (default 0)
     */
    default int getOrder() {
        return 0;
    }
}
