package dev.langchain4j.store.embedding.oracle;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import oracle.jdbc.pool.OracleDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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

    private EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

    private static Metadata m3 = new Metadata();

    static {
        m3.put("author", "J. R. R. Tolkien");
        m3.put("age", 81);
    }

    private String c1 = "hello, world!";
    private String c2 = "There are 50 states in the USA.";
    private String c3 = "The Hobbit is one of Tolkien's many works.";

    private Embedding e1 = embeddingModel.embed(c1).content();
    private Embedding e2 = embeddingModel.embed(c2).content();
    private Embedding e3 = embeddingModel.embed(c3).content();


    private TextSegment ts3 = new TextSegment(c3, m3);

    @BeforeAll
    static void setup() throws SQLException {
        oracleContainer.start();
        OracleDataSource ds = new OracleDataSource();
        ds.setUser(oracleContainer.getUsername());
        ds.setPassword(oracleContainer.getPassword());
        ds.setURL(oracleContainer.getJdbcUrl());
        dataSource = ds;

        store = new OracleEmbeddingStore(ds, "vector_store", 384, null, null, null, true, true, true);
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

    @Test
    void addCollectionRemoveWithFilter() {
        List<String> ids = store.addAll(listOf(e1, e2, e3));
        store.removeAll(new IsIn("id", ids));
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
}
