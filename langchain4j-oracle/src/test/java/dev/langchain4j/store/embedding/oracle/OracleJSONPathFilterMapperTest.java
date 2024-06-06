package dev.langchain4j.store.embedding.oracle;

import java.util.stream.Stream;

import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

public class OracleJSONPathFilterMapperTest {
    final OracleJSONPathFilterMapper mapper = new OracleJSONPathFilterMapper();

    public static Stream<Arguments> filters() {
        return Stream.of(
                Arguments.of(MetadataFilterBuilder.metadataKey("key").isEqualTo(1), "where json_exists(metadata, '$?(@.key == 1)')"),
                Arguments.of(MetadataFilterBuilder.metadataKey("key").isEqualTo("value"), "where json_exists(metadata, '$?(@.key == \"value\")')")
        );
    }

    @ParameterizedTest
    @MethodSource("filters")
    void testMetadataFilter(Filter filter, String clause) {
        assertThat(mapper.whereClause(filter)).isEqualTo(clause);
    }
}
