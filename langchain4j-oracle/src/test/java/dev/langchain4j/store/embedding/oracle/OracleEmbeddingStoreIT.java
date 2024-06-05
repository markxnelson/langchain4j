package dev.langchain4j.store.embedding.oracle;

import java.sql.SQLException;

import oracle.jdbc.pool.OracleDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

@Testcontainers
public class OracleEmbeddingStoreIT {
    @Container
    static OracleContainer oracleContainer = new OracleContainer("gvenzl/oracle-free:23.4-slim-faststart")
            .withDatabaseName("pdb1")
            .withUsername("testuser")
            .withPassword(("testpwd"));

    static OracleEmbeddingStore store;

    @BeforeAll
    static void setup() throws SQLException {
        oracleContainer.start();
        OracleDataSource ds = new OracleDataSource();
        ds.setUser(oracleContainer.getUsername());
        ds.setPassword(oracleContainer.getPassword());
        ds.setURL(oracleContainer.getJdbcUrl());

        store = new OracleEmbeddingStore(ds, "vector_store", 1024, null, null, null, true, true, true);
    }

    @Test
    void add() {

    }
}
