package io.datasquire.orchestrator.react;

import java.util.List;
import java.util.Map;

/**
 * Final result of the orchestration pipeline.
 *
 * @param response     consolidated response text
 * @param iterations   number of ReAct iterations performed
 * @param agentsUsed   names of sub-agents that contributed to the response
 * @param qualityScore final self-assessed quality score (0.0 to 1.0)
 * @param metadata     additional metadata about the orchestration run
 */
public record OrchestratorResult(
        String response,
        int iterations,
        List<String> agentsUsed,
        double qualityScore,
        Map<String, Object> metadata
) {

    public OrchestratorResult {
        agentsUsed = List.copyOf(agentsUsed);
        metadata = Map.copyOf(metadata);
    }
}
