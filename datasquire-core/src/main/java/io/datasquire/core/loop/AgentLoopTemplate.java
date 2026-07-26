package io.datasquire.core.loop;

/**
 * SPI for the agent execution loop. Implementations orchestrate iterative LLM calls
 * with tool use, validation, and retry logic.
 */
public interface AgentLoopTemplate {

    /**
     * Executes the agent loop with the given context.
     *
     * @param context the loop execution context
     * @return the loop result including response and iteration count
     */
    LoopResult execute(LoopContext context);
}
