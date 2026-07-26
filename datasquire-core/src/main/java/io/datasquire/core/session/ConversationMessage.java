package io.datasquire.core.session;

import java.time.Instant;

/**
 * An immutable message within a conversation, capturing the role, content, and timestamp.
 *
 * @param role      the participant role (USER, ASSISTANT, or SYSTEM)
 * @param content   the message text
 * @param timestamp when the message was created
 */
public record ConversationMessage(Role role, String content, Instant timestamp) {
}
