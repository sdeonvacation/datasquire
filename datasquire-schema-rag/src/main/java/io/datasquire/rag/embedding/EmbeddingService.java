package io.datasquire.rag.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps Spring AI's EmbeddingModel to produce float[] vectors for text inputs.
 */
@Component
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Embeds a single text string into a float vector.
     *
     * @param text the text to embed
     * @return embedding vector, or empty array if input is null/blank
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            log.debug("Skipping embedding for null/blank text");
            return new float[0];
        }
        EmbeddingResponse response = embeddingModel.call(
                new org.springframework.ai.embedding.EmbeddingRequest(
                        List.of(text), null));
        return response.getResult().getOutput();
    }

    /**
     * Embeds a batch of texts into float vectors.
     *
     * @param texts the texts to embed
     * @return list of embedding vectors (preserves input order); empty list if input is null/empty
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            log.debug("Skipping embedding for null/empty batch");
            return List.of();
        }

        List<String> nonBlank = new ArrayList<>();
        int[] indexMap = new int[texts.size()];
        int mappedIdx = 0;

        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i) != null && !texts.get(i).isBlank()) {
                indexMap[i] = mappedIdx++;
                nonBlank.add(texts.get(i));
            } else {
                indexMap[i] = -1;
            }
        }

        if (nonBlank.isEmpty()) {
            return texts.stream().map(t -> new float[0]).toList();
        }

        EmbeddingResponse response = embeddingModel.call(
                new org.springframework.ai.embedding.EmbeddingRequest(nonBlank, null));

        List<float[]> embeddings = response.getResults().stream()
                .map(r -> r.getOutput())
                .toList();

        List<float[]> result = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i++) {
            if (indexMap[i] >= 0) {
                result.add(embeddings.get(indexMap[i]));
            } else {
                result.add(new float[0]);
            }
        }
        return result;
    }
}
