package io.datasquire.core.agent;

import io.datasquire.core.session.ConversationMessage;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates all information needed for a sub-agent to process a query.
 *
 * @param query               the user's natural language query
 * @param conversationHistory prior messages in this conversation for context
 * @param context             request origin context (tenant, user, attributes)
 * @param metadata            additional processing hints or parameters
 */
public record SubAgentRequest(
        String query,
        List<ConversationMessage> conversationHistory,
        RequestContext context,
        Map<String, Object> metadata
) {

    /**
     * Compact constructor ensuring defensive copies.
     */
    public SubAgentRequest {
        conversationHistory = List.copyOf(conversationHistory);
        metadata = Map.copyOf(metadata);
    }
}
