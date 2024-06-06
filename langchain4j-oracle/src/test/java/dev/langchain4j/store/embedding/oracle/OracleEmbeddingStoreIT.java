package dev.langchain4j.store.embedding.oracle;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import oracle.jdbc.pool.OracleDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class OracleEmbeddingStoreIT {
    @Container
    static OracleContainer oracleContainer = new OracleContainer("gvenzl/oracle-free:23.4-slim-faststart")
            .withUsername("testuser")
            .withPassword(("testpwd"));
    static OracleEmbeddingStore store;
    static DataSource dataSource;
    private static EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
    private static String table = "vector_store";
    private static Metadata m3 = new Metadata();

    static {
        m3.put("author", "J. R. R. Tolkien");
        m3.put("age", 81);
    }

    private static String c1 = "hello, world!";
    private static String c2 = "There are 50 states in the USA.";
    private static String c3 = "The Hobbit is one of Tolkien's many works.";

    private static Embedding e1 = embeddingModel.embed(c1).content();
    private static Embedding e2 = embeddingModel.embed(c2).content();
    private static Embedding e3 = embeddingModel.embed(c3).content();

    private static TextSegment ts3 = new TextSegment(c3, m3);

    @BeforeAll
    static void setup() throws SQLException {
        oracleContainer.start();
        OracleDataSource ds = new OracleDataSource();
        ds.setUser(oracleContainer.getUsername());
        ds.setPassword(oracleContainer.getPassword());
        ds.setURL(oracleContainer.getJdbcUrl());
        dataSource = ds;
        store = new OracleEmbeddingStore(ds, table, 384, null, null, null, true, true, true);
    }

    @Test
    void addAndRemove() {
        String id1 = store.add(e1);
        assertThat(id1).isNotNull();
        store.remove(id1);
        String id2 = store.add(e1);
        assertThat(id2).isNotNull();
        assertThat(id2).isNotEqualTo(id1);
    }

    @Test
    void addCollectionRemoveCollection() {
        List<String> ids = store.addAll(listOf(e1, e2, e3));
        store.removeAll(ids);
    }

    public static Stream<Arguments> addRemoveFilters() {
        return Stream.of(
                Arguments.of(e3, ts3, MetadataFilterBuilder.metadataKey("author").isEqualTo("J. R. R. Tolkien")),
                Arguments.of(e3, ts3, MetadataFilterBuilder.metadataKey("age").isEqualTo(81)),
                Arguments.of(e3, ts3, MetadataFilterBuilder.metadataKey("age").isGreaterThan(50))
        );
    }

    @ParameterizedTest
    @MethodSource("addRemoveFilters")
    void addRemoveWithFilter(Embedding embedding, TextSegment textSegment, Filter filter) throws SQLException {
        String id = store.add(embedding, textSegment);
        assertPresent(id);
        store.removeAll(filter);
        assertNotPresent(id);
    }

    @Test
    void addWithId() {
        store.add(UUID.randomUUID().toString(), e2);
    }

    @Test
    void addWithSegment() {
        String id3 = store.add(e3, ts3);
        assertThat(id3).isNotNull();
    }

    private List<Embedding> listOf(Embedding... embedding) {
        return Arrays.asList(embedding);
    }

    private void assertPresent(String id) {
        assertPresence(id, true);
    }

    private void assertNotPresent(String id) {
        assertPresence(id, false);
    }

    private void assertPresence(String id, boolean exists) {
        try (Connection connection = dataSource.getConnection(); Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(String.format("select * from %s where id = '%s'", table, id));
            System.out.println(rs.isBeforeFirst());
            assertThat(rs.isBeforeFirst()).isEqualTo(exists);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
