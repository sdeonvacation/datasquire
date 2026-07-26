package io.datasquire.core.agent;

import java.util.Set;

/**
 * Describes a specific capability offered by a sub-agent.
 *
 * @param name        short identifier for the capability
 * @param description human-readable description of what this capability does
 * @param keywords    terms that help match user queries to this capability
 */
public record AgentCapability(String name, String description, Set<String> keywords) {

    /**
     * Compact constructor ensuring defensive copy of keywords.
     */
    public AgentCapability {
        keywords = Set.copyOf(keywords);
    }
}
