package io.datasquire.core.session;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of a conversation session's state.
 *
 * @param sessionId      unique identifier for this session
 * @param history        ordered list of conversation messages
 * @param metadata       arbitrary key-value attributes associated with the session
 * @param lastAccessTime when the session was last accessed
 */
public record SessionState(
        String sessionId,
        List<ConversationMessage> history,
        Map<String, Object> metadata,
        Instant lastAccessTime
) {

    /**
     * Compact constructor ensuring defensive copies of mutable collections.
     */
    public SessionState {
        history = List.copyOf(history);
        metadata = Map.copyOf(metadata);
    }

    /**
     * Returns a new {@code SessionState} with the given message appended to the history.
     *
     * @param message the message to append
     * @return a new session state with updated history and access time
     */
    public SessionState withMessage(ConversationMessage message) {
        var updated = new ArrayList<>(history);
        updated.add(message);
        return new SessionState(sessionId, updated, metadata, Instant.now());
    }

    /**
     * Returns a new {@code SessionState} with history trimmed to the most recent messages.
     *
     * @param maxSize the maximum number of messages to retain
     * @return a new session state with trimmed history
     */
    public SessionState trimHistory(int maxSize) {
        if (history.size() <= maxSize) {
            return this;
        }
        var trimmed = history.subList(history.size() - maxSize, history.size());
        return new SessionState(sessionId, trimmed, metadata, lastAccessTime);
    }
}
