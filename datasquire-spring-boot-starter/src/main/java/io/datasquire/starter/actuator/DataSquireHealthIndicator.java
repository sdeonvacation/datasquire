package io.datasquire.starter.actuator;

import io.datasquire.core.session.SessionStore;
import io.datasquire.orchestrator.registry.SubAgentRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Spring Boot Actuator health indicator for DataSquire.
 * Reports agent health and session store accessibility.
 */
public class DataSquireHealthIndicator implements HealthIndicator {

    private final SubAgentRegistry subAgentRegistry;
    private final SessionStore sessionStore;

    public DataSquireHealthIndicator(SubAgentRegistry subAgentRegistry, SessionStore sessionStore) {
        this.subAgentRegistry = subAgentRegistry;
        this.sessionStore = sessionStore;
    }

    @Override
    public Health health() {
        var allAgents = subAgentRegistry.getHealthyAgents();
        int healthyCount = allAgents.size();
        int activeSessions;

        try {
            activeSessions = sessionStore.getActiveCount();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "Session store inaccessible: " + e.getMessage())
                    .build();
        }

        // Determine total agent count by checking both healthy and all registered
        // SubAgentRegistry only exposes healthy agents list, so healthy count = available
        var healthBuilder = (healthyCount > 0) ? Health.up() : Health.down();

        return healthBuilder
                .withDetail("agentCount", healthyCount)
                .withDetail("healthyAgentCount", healthyCount)
                .withDetail("activeSessions", activeSessions)
                .build();
    }
}
