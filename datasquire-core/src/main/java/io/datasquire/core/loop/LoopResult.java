package io.datasquire.core.loop;

import java.util.Map;

/**
 * The result of an agent loop execution.
 *
 * @param response       the final response produced by the loop
 * @param iterationsUsed number of iterations consumed
 * @param complete       whether the loop completed successfully (vs. hitting max iterations)
 * @param metadata       additional execution metadata (e.g., tool calls made, tokens used)
 */
public record LoopResult(String response, int iterationsUsed, boolean complete, Map<String, Object> metadata) {

    /**
     * Compact constructor ensuring defensive copy of metadata.
     */
    public LoopResult {
        metadata = Map.copyOf(metadata);
    }
}
