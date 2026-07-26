package io.datasquire.orchestrator.react;

import io.datasquire.core.agent.SubAgentResponse;

import java.util.List;

/**
 * Immutable snapshot of the ReAct loop state at a given iteration.
 *
 * @param iteration    current iteration number (1-based)
 * @param observations accumulated sub-agent responses from all iterations
 * @param currentPlan  the LLM's current reasoning/plan for what information is still needed
 * @param qualityScore self-assessed quality of the accumulated answer (0.0 to 1.0)
 */
public record ReActState(
        int iteration,
        List<SubAgentResponse> observations,
        String currentPlan,
        double qualityScore
) {

    public ReActState {
        observations = List.copyOf(observations);
    }
}
