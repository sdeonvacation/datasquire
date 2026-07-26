package io.datasquire.core.schema;

import java.util.Set;

/**
 * SPI for retrieving database schema information. Implementations may load schema
 * from files, databases, or remote services.
 */
public interface SchemaProvider {

    /**
     * Retrieves schema context relevant to the given query.
     * Implementations should return only the most pertinent schema information.
     *
     * @param query the user query to find relevant schema for
     * @return schema context as a formatted string
     */
    String getSchemaContext(String query);

    /**
     * Returns the complete schema documentation.
     *
     * @return full schema as a formatted string
     */
    String getFullSchema();

    /**
     * Returns the set of chunk kinds available in this schema provider.
     *
     * @return available chunk kinds
     */
    Set<ChunkKind> getChunkKinds();
}
