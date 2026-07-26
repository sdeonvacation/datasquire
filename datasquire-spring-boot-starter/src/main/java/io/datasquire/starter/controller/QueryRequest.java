package io.datasquire.starter.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Inbound request payload for the query endpoint.
 *
 * @param query     the natural language question (required)
 * @param sessionId optional session identifier for conversational context
 * @param options   optional execution options
 */
public record QueryRequest(
        @NotBlank(message = "Query must not be blank")
        @Size(max = 10000, message = "Query must not exceed 10,000 characters")
        String query,
        @Size(max = 100, message = "Session ID must not exceed 100 characters")
        String sessionId,
        Map<String, Object> options
) {

    public QueryRequest {
        if (options == null) {
            options = Map.of();
        }
    }
}
