package dev.langchain4j.store.embedding.oracle;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;

import java.util.Collection;
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

    /**
     * Removes a single embedding from the store by ID.
     *
     * @param id The unique ID of the embedding to be removed.
     */
    @Override
    public void remove(String id) {

    }

    /**
     * Removes all embeddings that match the specified IDs from the store.
     *
     * @param ids A collection of unique IDs of the embeddings to be removed.
     */
    @Override
    public void removeAll(Collection<String> ids) {

    }

    /**
     * Removes all embeddings that match the specified {@link Filter} from the store.
     *
     * @param filter The filter to be applied to the {@link Metadata} of the {@link TextSegment} during removal.
     *               Only embeddings whose {@code TextSegment}'s {@code Metadata}
     *               match the {@code Filter} will be removed.
     */
    @Override
    public void removeAll(Filter filter) {
        
    }

    /**
     * Removes all embeddings from the store.
     */
    @Override
    public void removeAll() {

    }

    /**
     * Searches for the most similar (closest in the embedding space) {@link Embedding}s.
     * <br>
     * All search criteria are defined inside the {@link EmbeddingSearchRequest}.
     * <br>
     * {@link EmbeddingSearchRequest#filter()} can be used to filter by user/memory ID.
     * Please note that not all {@link EmbeddingStore} implementations support {@link Filter}ing.
     *
     * @param request A request to search in an {@link EmbeddingStore}. Contains all search criteria.
     * @return An {@link EmbeddingSearchResult} containing all found {@link Embedding}s.
     */
    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        return null;
    }


}