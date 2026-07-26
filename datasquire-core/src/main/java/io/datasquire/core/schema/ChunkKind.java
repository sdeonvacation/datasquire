package io.datasquire.core.schema;

/**
 * Categorizes schema documentation chunks by their content type.
 */
public enum ChunkKind {
    /** High-level schema overview or summary */
    OVERVIEW,
    /** Table definition including columns and relationships */
    TABLE,
    /** Enum or lookup value documentation */
    ENUM,
    /** Example queries or usage patterns */
    EXAMPLE
}
