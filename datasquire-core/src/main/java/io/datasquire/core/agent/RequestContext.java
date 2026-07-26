package io.datasquire.core.agent;

import java.util.Map;

/**
 * Contextual information about the request origin, including tenant and user identity.
 *
 * @param tenantId   identifier for the tenant making the request
 * @param userId     identifier for the user making the request
 * @param attributes additional context attributes (e.g., roles, preferences)
 */
public record RequestContext(String tenantId, String userId, Map<String, Object> attributes) {

    /**
     * Compact constructor ensuring defensive copy of attributes.
     */
    public RequestContext {
        attributes = Map.copyOf(attributes);
    }
}
