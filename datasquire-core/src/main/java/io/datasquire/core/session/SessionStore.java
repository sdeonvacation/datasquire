package io.datasquire.core.session;

import java.util.Optional;

/**
 * SPI for session persistence. Implementations may store sessions in-memory, in a database,
 * or in any other backing store.
 */
public interface SessionStore {

    /**
     * Retrieves a session by its identifier.
     *
     * @param sessionId the session identifier
     * @return the session state, or empty if not found
     */
    Optional<SessionState> get(String sessionId);

    /**
     * Persists or updates a session state.
     *
     * @param state the session state to save
     */
    void save(SessionState state);

    /**
     * Removes a session from the store.
     *
     * @param sessionId the session identifier to delete
     */
    void delete(String sessionId);

    /**
     * Returns the number of currently active (stored) sessions.
     *
     * @return active session count
     */
    int getActiveCount();
}
