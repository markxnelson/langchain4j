package dev.langchain4j.store.embedding.oracle;

import javax.sql.DataSource;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import oracle.jdbc.OracleType;
import oracle.sql.VECTOR;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.Utils.isNullOrEmpty;
import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.internal.ValidationUtils.ensureTrue;
import static java.util.Collections.singletonList;

public class OracleEmbeddingStore implements EmbeddingStore<TextSegment> {
    private static final Logger log = LoggerFactory.getLogger(OracleEmbeddingStore.class);
    private static final Integer DEFAULT_DIMENSIONS = -1;
    private static final Integer DEFAULT_ACCURACY = -1;
    private static final DistanceType DEFAULT_DISTANCE_TYPE = DistanceType.COSINE;
    private static final IndexType DEFAULT_INDEX_TYPE = IndexType.IVF;

    private final String table;
    private final DataSource dataSource;
    private final Integer accuracy;
    private final DistanceType distanceType;
    private final IndexType indexType;


    public OracleEmbeddingStore(DataSource dataSource,
                                String table,
                                Integer dimension,
                                Integer accuracy,
                                DistanceType distanceType,
                                IndexType indexType,
                                Boolean useIndex,
                                Boolean createTable,
                                Boolean dropTableFirst
    ) {
        this.dataSource = ensureNotNull(dataSource, "dataSource");
        this.table = ensureNotBlank(table, "table");
        this.accuracy = getOrDefault(accuracy, DEFAULT_ACCURACY);
        this.distanceType = getOrDefault(distanceType, DEFAULT_DISTANCE_TYPE);
        this.indexType = getOrDefault(indexType, DEFAULT_INDEX_TYPE);

        useIndex = getOrDefault(useIndex, false);
        createTable = getOrDefault(createTable, true);
        dropTableFirst = getOrDefault(dropTableFirst, false);
        dimension = getOrDefault(dimension, DEFAULT_DIMENSIONS);

        initTable(dropTableFirst, createTable, useIndex, dimension);
    }


    /**
     * Adds a given embedding to the store.
     *
     * @param embedding
     *     The embedding to be added to the store.
     * @return The auto-generated ID associated with the added embedding.
     */
    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        addInternal(id, embedding, null);
        return id;
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
        addInternal(id, embedding, null);
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
        String id = UUID.randomUUID().toString();
        addInternal(id, embedding, null);
        return id;
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
        List<String> ids = createIds(embeddings);
        addAllInternal(ids, embeddings, null);
        return ids;
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
        List<String> ids = createIds(embeddings);
        addAllInternal(ids, embeddings, embedded);
        return ids;
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

    private void addInternal(String id, Embedding embedding, TextSegment embedded) {
        addAllInternal(
                singletonList(id),
                singletonList(embedding),
                embedded == null ? null : singletonList(embedded));
    }

    private void addAllInternal(List<String> ids, List<Embedding> embeddings, List<TextSegment> segments) {
        if (isNullOrEmpty(ids) || isNullOrEmpty(embeddings)) {
            log.info("Empty embeddings - none added");
            return;
        }
        ensureTrue(ids.size() == embeddings.size(), "ids and embeddings have different size");
        ensureTrue(segments == null || segments.size() == embeddings.size(), "segments and embeddings have different size");

        String upsert = String.format("merge into %s target using (values(?, ?, ?, ?)) source (id, content, metadata, embedding) on (target.id = source.id)\n" +
                "when matched then update set target.content = source.content, target.metadata = source.metadata, target.embedding = source.embedding\n" +
                "when not matched then insert (target.id, target.content, target.metadata, target.embedding) values (source.id, source.content, source.metadata, source.embedding)",
                table);
        try (Connection connection = dataSource.getConnection(); PreparedStatement stmt = connection.prepareStatement(upsert)) {
            for (int i = 0; i < ids.size(); i++) {
                stmt.setString(1, ids.get(i));
                if (segments != null && segments.get(i) != null) {
                    TextSegment textSegment = segments.get(i);
                    stmt.setString(2, textSegment.text());
                    OracleJsonObject ojson = toJSON(textSegment.metadata().toMap());
                    stmt.setObject(3, ojson, OracleType.JSON.getVendorTypeNumber());
                } else {
                    stmt.setString(2, "");
                    stmt.setObject(3, toJSON(null), OracleType.JSON.getVendorTypeNumber());
                }
                stmt.setObject(4, toVECTOR(embeddings.get(i)), OracleType.VECTOR.getVendorTypeNumber());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> createIds(List<Embedding> embeddings) {
        return embeddings.stream()
                .map(e -> UUID.randomUUID().toString())
                .collect(Collectors.toList());
    }

    private VECTOR toVECTOR(Embedding embedding) throws SQLException {
        return VECTOR.ofFloat32Values(embedding.vector());
    }

    private OracleJsonObject toJSON(Map<String, Object> metadata) {
        OracleJsonObject ojson = new OracleJsonFactory().createObject();
        if (metadata == null) {
            return ojson;
        }
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            final Object o = entry.getValue();
            if (o instanceof String) {
                ojson.put(entry.getKey(), (String) o);
            }
            else if (o instanceof Integer) {
                ojson.put(entry.getKey(), (Integer) o);
            }
            else if (o instanceof Float) {
                ojson.put(entry.getKey(), (Float) o);
            }
            else if (o instanceof Double) {
                ojson.put(entry.getKey(), (Double) o);
            }
            else if (o instanceof Boolean) {
                ojson.put(entry.getKey(), (Boolean) o);
            }
            ojson.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return ojson;
    }

    protected void initTable(Boolean dropTableFirst, Boolean createTable, Boolean useIndex, Integer dimension) {
        String query = "init";
        try (Connection connection = dataSource.getConnection(); Statement stmt = connection.createStatement()) {
            if (dropTableFirst) {
                stmt.executeUpdate(String.format("drop table if exists %s purge", query));
            }
            if (createTable) {
                stmt.executeUpdate(String.format("create table if not exists %s (\n" +
                                "id        varchar2(36) default sys_guid() primary key,\n" +
                                "content   clob,\n" +
                                "metadata  json,\n" +
                                "embedding vector(%s,FLOAT64) annotations(Distance '%s', IndexType '%s'))",
                        table, getDimensionString(dimension), distanceType.name(), indexType.name()));
            }
            if (useIndex) {
                switch (indexType) {
                    case IVF:
                        stmt.executeUpdate(String.format("create vector index if not exists vector_index_%s on %s (embedding)\n" +
                                "organization neighbor partitions\n" +
                                "distance %s\n" +
                                "with target accuracy %d\n" +
                                "parameters (type IVF, neighbor partitions 10)",
                                table, table, distanceType.name(), getAccuracy()));
                        break;

                    /*
                     * TODO: Enable for 23.5 case HNSW:
                     * this.jdbcTemplate.execute(String.format(""" create vector index if not
                     * exists vector_index_%s on %s (embedding) organization inmemory neighbor
                     * graph distance %s with target accuracy %d parameters (type HNSW,
                     * neighbors 40, efconstruction 500)""", tableName, tableName,
                     * distanceType.name(), searchAccuracy == DEFAULT_SEARCH_ACCURACY ? 95 :
                     * searchAccuracy)); break;
                     */
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Could not connect to database: %s", query), e);
        }
    }

    private String getDimensionString(Integer dimension) {
        return dimension.equals(DEFAULT_DIMENSIONS) ? "*" : String.valueOf(dimension);
    }

    private int getAccuracy() {
        return accuracy.equals(DEFAULT_ACCURACY) ? 95 : accuracy;
    }

    public enum DistanceType {
        /**
         * Default metric. It calculates the cosine distane between two vectors.
         */
        COSINE,

        /**
         * Also called the inner product, calculates the negated dot product of two
         * vectors.
         */
        DOT,

        /**
         * Also called L2_DISTANCE, calculates the Euclidean distance between two vectors.
         */
        EUCLIDEAN,

        /**
         * Also called L2_SQUARED is the Euclidean distance without taking the square
         * root.
         */
        EUCLIDEAN_SQUARED,

        /*
         * Calculates the hamming distance between two vectors. Requires INT8 element
         * type.
         */
        // TODO: add HAMMING support,

        /**
         * Also called L1_DISTANCE or taxicab distance, calculates the Manhattan distance.
         */
        MANHATTAN
    }

    public enum IndexType {

        /**
         * Performs exact nearest neighbor search.
         */
        NONE,

        /**
         * </p>
         * The default type of index created for an In-Memory Neighbor Graph vector index
         * is Hierarchical Navigable Small World (HNSW).
         * </p>
         *
         * <p>
         * With Navigable Small World (NSW), the idea is to build a proximity graph where
         * each vector in the graph connects to several others based on three
         * characteristics:
         * <ul>
         * <li>The distance between vectors</li>
         * <li>The maximum number of closest vector candidates considered at each step of
         * the search during insertion (EFCONSTRUCTION)</li>
         * <li>Within the maximum number of connections (NEIGHBORS) permitted per
         * vector</li>
         * </ul>
         * </p>
         *
         * @see <a href=
         * "https://docs.oracle.com/en/database/oracle/oracle-database/23/vecse/understand-hierarchical-navigable-small-world-indexes.html">Oracle
         * Database documentation</a>
         */
        HNSW,

        /**
         * <p>
         * The default type of index created for a Neighbor Partition vector index is
         * Inverted File Flat (IVF) vector index. The IVF index is a technique designed to
         * enhance search efficiency by narrowing the search area through the use of
         * neighbor partitions or clusters.
         * </p>
         *
         * * @see <a href=
         * "https://docs.oracle.com/en/database/oracle/oracle-database/23/vecse/understand-inverted-file-flat-vector-indexes.html">Oracle
         * Database documentation</a>
         */
        IVF;

    }

}