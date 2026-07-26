package io.datasquire.core.loop;

import io.datasquire.core.session.ConversationMessage;

import java.util.List;

/**
 * Context provided to an agent loop iteration, containing all inputs needed for execution.
 *
 * @param systemPrompt  the system-level prompt defining agent behavior
 * @param userQuery     the user's original query
 * @param history       conversation history for context
 * @param maxIterations maximum number of loop iterations allowed
 */
public record LoopContext(
        String systemPrompt,
        String userQuery,
        List<ConversationMessage> history,
        int maxIterations
) {

    /**
     * Compact constructor ensuring defensive copy of history.
     */
    public LoopContext {
        history = List.copyOf(history);
    }
}
