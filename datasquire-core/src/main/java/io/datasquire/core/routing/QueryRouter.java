package io.datasquire.core.routing;

import io.datasquire.core.agent.SubAgent;

import java.util.List;

/**
 * SPI for determining which sub-agent(s) should handle a given query.
 * Implementations may use keyword matching, embeddings, or LLM-based classification.
 */
public interface QueryRouter {

    /**
     * Routes a query to one or more sub-agents.
     *
     * @param query  the user's natural language query
     * @param agents the available sub-agents to route to
     * @return the routing decision with selected agents and confidence
     */
    RoutingDecision route(String query, List<SubAgent> agents);

    /**
     * Returns the minimum confidence threshold below which routing falls back to LLM.
     *
     * @return the confidence threshold (default 0.7)
     */
    default double getConfidenceThreshold() {
        return 0.7;
    }
}
