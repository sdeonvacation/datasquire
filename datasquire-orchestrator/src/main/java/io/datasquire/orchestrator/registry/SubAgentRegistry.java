package io.datasquire.orchestrator.registry;

import io.datasquire.core.agent.AgentCapability;
import io.datasquire.core.agent.SubAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry that auto-discovers and manages sub-agent beans.
 * Provides lookup, health filtering, and capabilities documentation.
 */
@Component
public class SubAgentRegistry {

    private static final Logger log = LoggerFactory.getLogger(SubAgentRegistry.class);

    private final Map<String, SubAgent> agents = new ConcurrentHashMap<>();

    /**
     * Constructs the registry, auto-registering all discovered SubAgent beans.
     *
     * @param discoveredAgents sub-agent beans injected by Spring
     */
    public SubAgentRegistry(List<SubAgent> discoveredAgents) {
        discoveredAgents.forEach(this::register);
        log.info("SubAgentRegistry initialized with {} agent(s): {}",
                agents.size(), agents.keySet());
    }

    /**
     * Registers a sub-agent. Overwrites any existing agent with the same name.
     *
     * @param agent the sub-agent to register
     */
    public void register(SubAgent agent) {
        agents.put(agent.getName(), agent);
        log.debug("Registered sub-agent: {}", agent.getName());
    }

    /**
     * Removes a sub-agent by name.
     *
     * @param name the agent name to unregister
     */
    public void unregister(String name) {
        SubAgent removed = agents.remove(name);
        if (removed != null) {
            log.debug("Unregistered sub-agent: {}", name);
        }
    }

    /**
     * Retrieves a sub-agent by name.
     *
     * @param name the agent name
     * @return the agent, or empty if not found
     */
    public Optional<SubAgent> getAgent(String name) {
        return Optional.ofNullable(agents.get(name));
    }

    /**
     * Returns all registered agents that report healthy status.
     *
     * @return list of healthy sub-agents
     */
    public List<SubAgent> getHealthyAgents() {
        return agents.values().stream()
                .filter(SubAgent::isHealthy)
                .toList();
    }

    /**
     * Generates a markdown document listing all registered agents and their capabilities.
     *
     * @return markdown-formatted capabilities documentation
     */
    public String generateCapabilitiesDoc() {
        if (agents.isEmpty()) {
            return "No agents registered.";
        }

        var sb = new StringBuilder("# Registered Sub-Agents\n\n");
        for (SubAgent agent : agents.values()) {
            sb.append("## ").append(agent.getName()).append("\n");
            sb.append("**Description:** ").append(agent.getDescription()).append("\n\n");

            var capabilities = agent.getCapabilities();
            if (!capabilities.isEmpty()) {
                sb.append("**Capabilities:**\n");
                for (AgentCapability cap : capabilities) {
                    sb.append("- **").append(cap.name()).append("**: ")
                            .append(cap.description()).append("\n");
                }
                sb.append("\n");
            }

            var keywords = agent.getKeywords();
            if (!keywords.isEmpty()) {
                sb.append("**Keywords:** ")
                        .append(String.join(", ", keywords))
                        .append("\n\n");
            }
        }
        return sb.toString();
    }
}
