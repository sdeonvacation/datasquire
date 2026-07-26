package io.datasquire.core.agent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * SPI contract for a sub-agent that can handle specific types of queries.
 * Implementations are discovered and orchestrated by the routing layer.
 */
public interface SubAgent {

    /**
     * @return the unique name identifying this agent
     */
    String getName();

    /**
     * @return a human-readable description of this agent's purpose
     */
    String getDescription();

    /**
     * @return the set of capabilities this agent provides
     */
    Set<AgentCapability> getCapabilities();

    /**
     * @return keywords that help the router match queries to this agent
     */
    Set<String> getKeywords();

    /**
     * @return example queries that this agent can handle, useful for documentation and testing
     */
    List<String> getExampleQueries();

    /**
     * Checks whether this agent is operational and ready to process requests.
     *
     * @return {@code true} if the agent is healthy, {@code false} otherwise
     */
    default boolean isHealthy() {
        return true;
    }

    /**
     * Processes a request asynchronously.
     *
     * @param request the incoming sub-agent request
     * @return a future that completes with the agent's response
     */
    CompletableFuture<SubAgentResponse> process(SubAgentRequest request);
}
