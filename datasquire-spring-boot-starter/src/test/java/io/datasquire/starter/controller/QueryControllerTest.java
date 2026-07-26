package io.datasquire.starter.controller;

import io.datasquire.core.session.SessionState;
import io.datasquire.core.session.SessionStore;
import io.datasquire.orchestrator.registry.SubAgentRegistry;
import io.datasquire.starter.config.DataSquireProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QueryController verifying graceful degradation when
 * optional dependencies (ReActOrchestrator, DataSquireMetrics) are absent.
 */
class QueryControllerTest {

    private SessionStore sessionStore;
    private SubAgentRegistry subAgentRegistry;
    private DataSquireProperties properties;

    @BeforeEach
    void setUp() {
        sessionStore = new InMemorySessionStore();
        subAgentRegistry = new SubAgentRegistry(List.of());
        properties = new DataSquireProperties();
    }

    @Test
    void constructor_acceptsNullOrchestratorAndMetrics() {
        // Should not throw - both orchestrator and metrics are @Nullable
        QueryController controller = new QueryController(
                null, sessionStore, subAgentRegistry, properties, null);
        assertNotNull(controller);
    }

    @Test
    void query_returnsEmitterWhenOrchestratorIsNull() {
        QueryController controller = new QueryController(
                null, sessionStore, subAgentRegistry, properties, null);

        QueryRequest request = new QueryRequest("What is revenue?", null, Map.of());
        SseEmitter emitter = controller.query(request, "tenant1", "user1");

        // Should return an emitter (not throw NPE)
        assertNotNull(emitter);
    }

    @Test
    void listAgents_worksWithoutOrchestrator() {
        QueryController controller = new QueryController(
                null, sessionStore, subAgentRegistry, properties, null);

        // With empty registry, returns empty list
        List<AgentInfoDto> agents = controller.listAgents();
        assertNotNull(agents);
        assertTrue(agents.isEmpty());
    }

    @Test
    void getSession_returnsNotFoundForMissingSession() {
        QueryController controller = new QueryController(
                null, sessionStore, subAgentRegistry, properties, null);

        var response = controller.getSession("nonexistent", "default");
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void deleteSession_returnsNoContent() {
        QueryController controller = new QueryController(
                null, sessionStore, subAgentRegistry, properties, null);

        var response = controller.deleteSession("any-id", "default");
        assertEquals(204, response.getStatusCode().value());
    }

    /**
     * Minimal in-memory SessionStore for testing without Spring context.
     */
    private static class InMemorySessionStore implements SessionStore {
        private final java.util.Map<String, SessionState> store = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public Optional<SessionState> get(String sessionId) {
            return Optional.ofNullable(store.get(sessionId));
        }

        @Override
        public void save(SessionState state) {
            store.put(state.sessionId(), state);
        }

        @Override
        public void delete(String sessionId) {
            store.remove(sessionId);
        }

        @Override
        public int getActiveCount() {
            return store.size();
        }
    }
}
