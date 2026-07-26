package io.datasquire.starter.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.datasquire.core.session.SessionState;
import io.datasquire.core.session.SessionStore;
import io.datasquire.starter.config.DataSquireProperties;

import java.time.Duration;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * In-memory session store backed by Caffeine cache with configurable size and TTL.
 */
public class CaffeineSessionStore implements SessionStore {

    private final Cache<String, SessionState> cache;

    public CaffeineSessionStore(DataSquireProperties properties) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getSession().getMaxSize())
                .expireAfterAccess(Duration.ofMinutes(properties.getSession().getTtlMinutes()))
                .build();
    }

    @Override
    public Optional<SessionState> get(String sessionId) {
        return Optional.ofNullable(cache.getIfPresent(sessionId));
    }

    @Override
    public void save(SessionState state) {
        cache.put(state.sessionId(), state);
    }

    /**
     * Atomically updates a session by applying the given operator.
     * If no session exists for the given ID, the operator receives null
     * and may return a new state or null to skip.
     *
     * @param sessionId the session identifier
     * @param updater   function that transforms the current state into the new state
     * @return the updated session state, or empty if updater returned null
     */
    public Optional<SessionState> update(String sessionId, UnaryOperator<SessionState> updater) {
        SessionState result = cache.asMap().compute(sessionId, (key, current) -> updater.apply(current));
        return Optional.ofNullable(result);
    }

    @Override
    public void delete(String sessionId) {
        cache.invalidate(sessionId);
    }

    @Override
    public int getActiveCount() {
        cache.cleanUp();
        return (int) cache.estimatedSize();
    }
}
