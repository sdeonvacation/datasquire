package io.datasquire.core.schema;

/**
 * A discrete chunk of schema documentation that can be individually embedded and retrieved.
 *
 * @param chunkId   unique identifier for this chunk
 * @param kind      the category of schema content this chunk represents
 * @param title     human-readable title for display purposes
 * @param content   the full documentation content of this chunk
 * @param embedText the text representation used for embedding/similarity search
 */
public record SchemaChunk(String chunkId, ChunkKind kind, String title, String content, String embedText) {
}
