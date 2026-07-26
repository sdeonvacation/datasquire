package io.datasquire.core.routing;

import java.util.List;

/**
 * The outcome of a routing decision, identifying which agents should handle a query.
 *
 * @param targetAgentNames   ordered list of agent names selected to handle the query
 * @param confidence         confidence score for this routing decision (0.0 to 1.0)
 * @param reasoning          human-readable explanation of why these agents were selected
 * @param requiresLlmFallback whether the router determined an LLM fallback is needed
 */
public record RoutingDecision(
        List<String> targetAgentNames,
        double confidence,
        String reasoning,
        boolean requiresLlmFallback
) {

    /**
     * Compact constructor ensuring defensive copy of target agent names.
     */
    public RoutingDecision {
        targetAgentNames = List.copyOf(targetAgentNames);
    }
}
