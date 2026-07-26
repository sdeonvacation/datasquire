package io.datasquire.starter.session;

import io.datasquire.core.session.ConversationMessage;
import io.datasquire.core.session.Role;
import io.datasquire.core.session.SessionState;
import io.datasquire.starter.config.DataSquireProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CaffeineSessionStore, focusing on atomic update behavior (H3 fix).
 */
class CaffeineSessionStoreTest {

    private CaffeineSessionStore store;

    @BeforeEach
    void setUp() {
        DataSquireProperties props = new DataSquireProperties();
        props.getSession().setMaxSize(100);
        props.getSession().setTtlMinutes(30);
        store = new CaffeineSessionStore(props);
    }

    @Test
    void save_and_get_roundtrips() {
        SessionState state = new SessionState("s1", List.of(), Map.of(), Instant.now());
        store.save(state);

        Optional<SessionState> retrieved = store.get("s1");
        assertTrue(retrieved.isPresent());
        assertEquals("s1", retrieved.get().sessionId());
    }

    @Test
    void get_returnsEmpty_whenNotFound() {
        assertTrue(store.get("nonexistent").isEmpty());
    }

    @Test
    void delete_removesSession() {
        SessionState state = new SessionState("s1", List.of(), Map.of(), Instant.now());
        store.save(state);
        store.delete("s1");
        assertTrue(store.get("s1").isEmpty());
    }

    @Test
    void update_atomicallyModifiesSession() {
        SessionState initial = new SessionState("s1", List.of(), Map.of(), Instant.now());
        store.save(initial);

        Optional<SessionState> result = store.update("s1", current -> {
            assertNotNull(current);
            return current.withMessage(new ConversationMessage(Role.USER, "hello", Instant.now()));
        });

        assertTrue(result.isPresent());
        assertEquals(1, result.get().history().size());
        assertEquals("hello", result.get().history().get(0).content());
    }

    @Test
    void update_returnsEmpty_whenUpdaterReturnsNull() {
        SessionState initial = new SessionState("s1", List.of(), Map.of(), Instant.now());
        store.save(initial);

        Optional<SessionState> result = store.update("s1", current -> null);
        assertTrue(result.isEmpty());
        // Session should be removed when compute returns null
        assertTrue(store.get("s1").isEmpty());
    }

    @Test
    void update_onMissingSession_receivesNull() {
        Optional<SessionState> result = store.update("missing", current -> {
            assertNull(current);
            return new SessionState("missing", List.of(), Map.of(), Instant.now());
        });

        assertTrue(result.isPresent());
        assertEquals("missing", result.get().sessionId());
    }

    @Test
    void update_concurrentUpdates_noLostMessages() throws InterruptedException {
        String sessionId = "concurrent";
        SessionState initial = new SessionState(sessionId, List.of(), Map.of(), Instant.now());
        store.save(initial);

        int threadCount = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);

        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                exec.submit(() -> {
                    try {
                        store.update(sessionId, current -> {
                            if (current == null) {
                                return new SessionState(sessionId, List.of(), Map.of(), Instant.now());
                            }
                            return current.withMessage(
                                    new ConversationMessage(Role.USER, "msg-" + idx, Instant.now()));
                        });
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await();
        assertEquals(0, errors.get());

        Optional<SessionState> finalState = store.get(sessionId);
        assertTrue(finalState.isPresent());
        // All messages should be present - no lost updates
        assertEquals(threadCount, finalState.get().history().size());
    }

    @Test
    void getActiveCount_returnsCorrectSize() {
        assertEquals(0, store.getActiveCount());
        store.save(new SessionState("s1", List.of(), Map.of(), Instant.now()));
        store.save(new SessionState("s2", List.of(), Map.of(), Instant.now()));
        assertEquals(2, store.getActiveCount());
    }
}
