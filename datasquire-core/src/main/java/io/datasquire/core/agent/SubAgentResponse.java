package io.datasquire.core.agent;

import java.util.Set;

/**
 * The result of a sub-agent processing a request.
 *
 * @param agentName    name of the agent that produced this response
 * @param response     the response content (null on failure)
 * @param success      whether the processing completed successfully
 * @param errorMessage error details if processing failed (null on success)
 * @param artifacts    set of artifact identifiers produced during processing
 */
public record SubAgentResponse(
        String agentName,
        String response,
        boolean success,
        String errorMessage,
        Set<String> artifacts
) {

    /**
     * Compact constructor ensuring defensive copy of artifacts.
     */
    public SubAgentResponse {
        artifacts = artifacts != null ? Set.copyOf(artifacts) : Set.of();
    }

    /**
     * Creates a successful response with the given artifacts.
     *
     * @param agentName name of the responding agent
     * @param response  the response content
     * @param artifacts artifact identifiers produced
     * @return a successful SubAgentResponse
     */
    public static SubAgentResponse success(String agentName, String response, Set<String> artifacts) {
        return new SubAgentResponse(agentName, response, true, null, artifacts);
    }

    /**
     * Creates a failure response with an error message.
     *
     * @param agentName    name of the responding agent
     * @param errorMessage description of what went wrong
     * @return a failed SubAgentResponse
     */
    public static SubAgentResponse failure(String agentName, String errorMessage) {
        return new SubAgentResponse(agentName, null, false, errorMessage, Set.of());
    }
}
