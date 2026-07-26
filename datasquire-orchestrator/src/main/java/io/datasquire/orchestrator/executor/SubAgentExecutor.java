package io.datasquire.orchestrator.executor;

import io.datasquire.core.agent.SubAgent;
import io.datasquire.core.agent.SubAgentRequest;
import io.datasquire.core.agent.SubAgentResponse;
import io.datasquire.orchestrator.config.OrchestratorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executes sub-agents in parallel or sequential mode with timeout handling.
 */
@Component
public class SubAgentExecutor {

    private static final Logger log = LoggerFactory.getLogger(SubAgentExecutor.class);

    private final OrchestratorProperties properties;

    public SubAgentExecutor(OrchestratorProperties properties) {
        this.properties = properties;
    }

    /**
     * Executes agents based on the configured execution mode.
     *
     * @param agents  the sub-agents to execute
     * @param request the request to process
     * @return list of responses from all agents
     */
    public List<SubAgentResponse> execute(List<SubAgent> agents, SubAgentRequest request) {
        if (agents.isEmpty()) {
            return List.of();
        }
        return "parallel".equalsIgnoreCase(properties.getExecutionMode())
                ? executeParallel(agents, request)
                : executeSequential(agents, request);
    }

    /**
     * Executes all agents in parallel with timeout.
     *
     * @param agents  the sub-agents to execute concurrently
     * @param request the request to process
     * @return list of responses (includes failures for timed-out agents)
     */
    public List<SubAgentResponse> executeParallel(List<SubAgent> agents, SubAgentRequest request) {
        long timeoutSeconds = properties.getSubagentTimeoutSeconds();

        List<CompletableFuture<SubAgentResponse>> futures = agents.stream()
                .map(agent -> executeWithTimeout(agent, request, timeoutSeconds))
                .toList();

        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(CompletableFuture[]::new));

        try {
            allOf.get(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Global timeout reached during parallel execution");
        } catch (Exception e) {
            log.error("Error during parallel execution", e);
        }

        return futures.stream()
                .map(this::getResponseSafely)
                .toList();
    }

    /**
     * Executes agents one-by-one in sequence with individual timeouts.
     *
     * @param agents  the sub-agents to execute sequentially
     * @param request the request to process
     * @return list of responses in execution order
     */
    public List<SubAgentResponse> executeSequential(List<SubAgent> agents, SubAgentRequest request) {
        long timeoutSeconds = properties.getSubagentTimeoutSeconds();
        List<SubAgentResponse> responses = new ArrayList<>();

        for (SubAgent agent : agents) {
            SubAgentResponse response = getResponseSafely(
                    executeWithTimeout(agent, request, timeoutSeconds));
            responses.add(response);
        }

        return responses;
    }

    private CompletableFuture<SubAgentResponse> executeWithTimeout(
            SubAgent agent, SubAgentRequest request, long timeoutSeconds) {
        return agent.process(request)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    String reason = ex instanceof TimeoutException
                            ? "Agent timed out after " + timeoutSeconds + "s"
                            : "Agent execution failed: " + ex.getMessage();
                    log.warn("Sub-agent '{}' failed: {}", agent.getName(), reason);
                    return SubAgentResponse.failure(agent.getName(), reason);
                });
    }

    private SubAgentResponse getResponseSafely(CompletableFuture<SubAgentResponse> future) {
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            return SubAgentResponse.failure("unknown", "Failed to retrieve response: " + e.getMessage());
        }
    }
}
