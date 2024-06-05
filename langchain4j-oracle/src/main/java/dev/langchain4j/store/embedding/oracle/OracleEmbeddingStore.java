package dev.langchain4j.store.embedding.oracle;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.List;

public class OracleEmbeddingStore implements EmbeddingStore<TextSegment> {

    /**
     * Adds a given embedding to the store.
     *
     * @param embedding
     *     The embedding to be added to the store.
     * @return The auto-generated ID associated with the added embedding.
     */
    @Override
    public String add(Embedding embedding) {
        return null;
    }

    /**
     * Adds a given embedding to the store.
     *
     * @param id
     *     The unique identifier for the embedding to be added.
     * @param embedding
     *     The embedding to be added to the store.
     */
    @Override
    public void add(String id, Embedding embedding) {

    }

    /**
     * Adds a given embedding and the corresponding content that has been embedded
     * to the store.
     *
     * @param embedding
     *     The embedding to be added to the store.
     * @param textSegment
     *     Original content that was embedded.
     * @return The auto-generated ID associated with the added embedding.
     */
    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        return null;
    }

    /**
     * Adds multiple embeddings to the store.
     *
     * @param embeddings
     *     A list of embeddings to be added to the store.
     * @return A list of auto-generated IDs associated with the added embeddings.
     */
    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return null;
    }

    /**
     * Adds multiple embeddings and their corresponding contents that have been
     * embedded to the store.
     *
     * @param embeddings
     *     A list of embeddings to be added to the store.
     * @param embedded
     *     A list of original contents that were embedded.
     * @return A list of auto-generated IDs associated with the added embeddings.
     */
    @Override
    public List<String> addAll(List<Embedding> embeddings,
        List<TextSegment> embedded) {
        return null;
    }
}