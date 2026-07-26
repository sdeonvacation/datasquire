package io.datasquire.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for the schema RAG retrieval module.
 */
@ConfigurationProperties(prefix = "datasquire.schema-rag")
public class SchemaRagProperties {

    private boolean enabled = true;
    private int topK = 8;
    private List<String> alwaysIncludeKinds = List.of("overview", "enum");
    private boolean failOnError = false;
    private String namespace = "default";
    private int embeddingDimension = 1536;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public List<String> getAlwaysIncludeKinds() {
        return alwaysIncludeKinds;
    }

    public void setAlwaysIncludeKinds(List<String> alwaysIncludeKinds) {
        this.alwaysIncludeKinds = alwaysIncludeKinds;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(int embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }
}
