package io.datasquire.orchestrator.consolidator;

import io.datasquire.core.agent.SubAgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Consolidates multiple sub-agent responses into a unified answer.
 * Uses pass-through for single responses and LLM-based merging for multiple.
 */
@Component
public class ResponseConsolidator {

    private static final Logger log = LoggerFactory.getLogger(ResponseConsolidator.class);

    private static final String CONSOLIDATION_TEMPLATE = """
            Consolidate these agent responses into a unified answer for the user's question.
            Remove duplicates, resolve conflicts, and present a coherent response.
            
            User's question: %s
            
            Agent responses:
            %s
            
            Provide a unified, well-structured answer:
            """;

    private final ChatModel chatModel;

    public ResponseConsolidator(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Consolidates a list of sub-agent responses for the original query.
     *
     * @param responses     the sub-agent responses to consolidate
     * @param originalQuery the user's original query
     * @return the consolidated response text
     */
    public String consolidate(List<SubAgentResponse> responses, String originalQuery) {
        if (responses.isEmpty()) {
            return "No responses were generated for your query.";
        }

        List<SubAgentResponse> successful = responses.stream()
                .filter(SubAgentResponse::success)
                .toList();

        if (successful.isEmpty()) {
            return buildErrorSummary(responses);
        }

        // Single successful response - pass through without LLM call
        if (successful.size() == 1) {
            log.debug("Single successful response from '{}', passing through",
                    successful.getFirst().agentName());
            return successful.getFirst().response();
        }

        // Multiple responses - use LLM to consolidate
        return consolidateWithLlm(successful, originalQuery);
    }

    private String consolidateWithLlm(List<SubAgentResponse> responses, String originalQuery) {
        String responsesText = responses.stream()
                .map(r -> "[" + r.agentName() + "]: " + r.response())
                .collect(Collectors.joining("\n\n"));

        String prompt = CONSOLIDATION_TEMPLATE.formatted(originalQuery, responsesText);

        try {
            ChatResponse chatResponse = chatModel.call(new Prompt(prompt));
            return chatResponse.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("LLM consolidation failed, falling back to concatenation", e);
            return fallbackConsolidate(responses);
        }
    }

    private String fallbackConsolidate(List<SubAgentResponse> responses) {
        return responses.stream()
                .map(r -> "**" + r.agentName() + ":**\n" + r.response())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private String buildErrorSummary(List<SubAgentResponse> responses) {
        String errors = responses.stream()
                .map(r -> "- " + r.agentName() + ": " + r.errorMessage())
                .collect(Collectors.joining("\n"));
        return "All agents failed to process your query:\n" + errors;
    }
}
