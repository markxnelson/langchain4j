package dev.langchain4j.model.oci;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.generativeaiinference.model.EmbedTextDetails;
import com.oracle.bmc.model.BmcException;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static dev.langchain4j.model.oci.OCIConfiguration.OCI_COMPARTMENT_ID;
import static dev.langchain4j.model.oci.OCIConfiguration.OCI_COMPARTMENT_ID_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = OCI_COMPARTMENT_ID_KEY, matches = ".+")
public class OCIEmbeddingModelIT {
    private static final String EMBEDDING_MODEL_V2 = "cohere.embed-english-light-v2.0";
    private static final String EMBEDDING_MODEL_V3 = "cohere.embed-english-v3.0";
    private static BasicAuthenticationDetailsProvider authenticationDetailsProvider;

    private final List<TextSegment> simpleTexts = toSegments(Arrays.asList(
            "The USA has 50 states.",
            "Canada has 10 provinces."
    ));
    private final String tooManyTokensContent = "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur? Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur? Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?";

    @BeforeAll
    static void setUp() throws IOException {
        String configFile = Paths.get(System.getProperty("user.home"), ".oci", "config").toString();
        authenticationDetailsProvider = new ConfigFileAuthenticationDetailsProvider(configFile, "DEFAULT");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            EMBEDDING_MODEL_V2,
            EMBEDDING_MODEL_V3
    })
    void embedAll_simpleTexts(String model) {
        EmbeddingModel embeddingModel = getEmbeddingModel(model)
                .build();
        List<Embedding> embeddings = embeddingModel.embedAll(simpleTexts).content();
        assertThat(embeddings).hasSize(2);
    }

    @Test
    void embed_noTruncate() {
        EmbeddingModel embeddingModel = getEmbeddingModel(EMBEDDING_MODEL_V2).build();
        BmcException e = assertThrows(
                BmcException.class,
                () -> embeddingModel.embed(tooManyTokensContent)
        );
        assertThat(e.getMessage()).contains("too long");
    }

    @Test
    void embed_truncate() {
        EmbeddingModel embeddingModel = getEmbeddingModel(EMBEDDING_MODEL_V2)
                .truncate(EmbedTextDetails.Truncate.End)
                .build();
        Embedding embedding = embeddingModel.embed(tooManyTokensContent).content();
        assertThat(embedding).isNotNull();
    }

    private List<TextSegment> toSegments(List<String> inputs) {
        return inputs.stream()
                .map(i -> new TextSegment(i, new Metadata()))
                .collect(Collectors.toList());
    }

    private OCIEmbeddingModel.OCIEmbeddingModelBuilder getEmbeddingModel(String model) {
        return OCIEmbeddingModel.builder()
                .model(model)
                .authenticationDetailsProvider(authenticationDetailsProvider)
                .compartmentId(OCI_COMPARTMENT_ID);
    }
}
