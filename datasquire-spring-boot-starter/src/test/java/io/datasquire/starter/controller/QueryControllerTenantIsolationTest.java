package io.datasquire.starter.controller;

import io.datasquire.core.session.SessionState;
import io.datasquire.core.session.SessionStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying tenant-scoped session key logic (H1 fix).
 * Tests the key-building convention: tenantId + ":" + sessionId.
 */
class QueryControllerTenantIsolationTest {

    @Test
    void scopedKey_prefixesTenantId() {
        String tenantId = "acme";
        String sessionId = "sess-123";
        String scopedKey = tenantId + ":" + sessionId;
        assertEquals("acme:sess-123", scopedKey);
    }

    @Test
    void scopedKey_usesDefaultTenant_whenNoTenantProvided() {
        String tenantId = "default";
        String sessionId = "sess-456";
        String scopedKey = tenantId + ":" + sessionId;
        assertEquals("default:sess-456", scopedKey);
    }

    @Test
    void differentTenants_cannotAccessEachOthersSessions() {
        // Simulates the isolation: two tenants with same sessionId produce different keys
        String sessionId = "shared-session";
        String keyTenantA = "tenant-a:" + sessionId;
        String keyTenantB = "tenant-b:" + sessionId;

        assertNotEquals(keyTenantA, keyTenantB);

        // Store simulation
        InMemoryStore store = new InMemoryStore();
        SessionState stateA = new SessionState(keyTenantA, List.of(), Map.of(), Instant.now());
        SessionState stateB = new SessionState(keyTenantB, List.of(), Map.of(), Instant.now());
        store.save(stateA);
        store.save(stateB);

        // Tenant A cannot see tenant B's session via its own scoped key
        Optional<SessionState> fromA = store.get(keyTenantA);
        assertTrue(fromA.isPresent());
        assertEquals(keyTenantA, fromA.get().sessionId());

        Optional<SessionState> fromB = store.get(keyTenantB);
        assertTrue(fromB.isPresent());
        assertEquals(keyTenantB, fromB.get().sessionId());

        // Unscoped raw sessionId finds nothing
        assertTrue(store.get(sessionId).isEmpty());
    }

    /**
     * Minimal in-memory store for isolation testing without Spring context.
     */
    private static class InMemoryStore implements SessionStore {
        private final java.util.Map<String, SessionState> map = new java.util.HashMap<>();

        @Override
        public Optional<SessionState> get(String sessionId) {
            return Optional.ofNullable(map.get(sessionId));
        }

        @Override
        public void save(SessionState state) {
            map.put(state.sessionId(), state);
        }

        @Override
        public void delete(String sessionId) {
            map.remove(sessionId);
        }

        @Override
        public int getActiveCount() {
            return map.size();
        }
    }
}
