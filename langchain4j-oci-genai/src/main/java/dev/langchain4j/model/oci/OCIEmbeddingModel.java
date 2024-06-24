package dev.langchain4j.model.oci;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.generativeaiinference.GenerativeAiInference;
import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import com.oracle.bmc.generativeaiinference.model.DedicatedServingMode;
import com.oracle.bmc.generativeaiinference.model.EmbedTextDetails;
import com.oracle.bmc.generativeaiinference.model.OnDemandServingMode;
import com.oracle.bmc.generativeaiinference.model.ServingMode;
import com.oracle.bmc.generativeaiinference.requests.EmbedTextRequest;
import com.oracle.bmc.generativeaiinference.responses.EmbedTextResponse;
import com.oracle.bmc.retrier.RetryConfiguration;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

/**
 * OCI GenAI implementation of Langchain4j EmbeddingModel
 */
public class OCIEmbeddingModel implements EmbeddingModel {
    /**
     * OCI GenAI accepts a maximum of 96 inputs per embedding request. If the Langchain input is greater
     * than 96 segments, the input will be split into chunks of this size.
     */
    private static final int EMBEDDING_BATCH_SIZE = 96;

    private final String model;
    protected final String compartmentId;
    private final GenerativeAiInference aiClient;
    private final ServingMode servingMode;
    /**
     * OCI GenAi accepts a maximum of 512 tokens per embedding. If the number of tokens exceeds this amount,
     * and the embedding truncation value is set to None (default), an error will be received.
     * <p>
     * If truncate is set to START, embeddings will be truncated to 512 tokens from the start of the input.
     * If truncate is set to END, embeddings will be truncated to 512 tokens from the end of the input.
     */
    private final EmbedTextDetails.Truncate truncate;

    @Builder
    public OCIEmbeddingModel(BasicAuthenticationDetailsProvider authenticationDetailsProvider, ServingModeType servingModeType, String model, String compartmentId, EmbedTextDetails.Truncate truncate) {
        ensureNotNull(authenticationDetailsProvider, "authenticationDetailsProvider");
        ensureNotBlank(model, "model");
        ensureNotBlank(compartmentId, "compartmentId");

        this.model = model;
        this.compartmentId = compartmentId;
        this.truncate = truncate == null ? EmbedTextDetails.Truncate.None : truncate;
        servingMode = servingMode(servingModeType == null ? ServingModeType.ON_DEMAND : servingModeType);
        aiClient = generativeAiInferenceClient(authenticationDetailsProvider);
    }

    /**
     * Embeds the text content of a list of TextSegments.
     *
     * @param textSegments the text segments to embed.
     * @return the embeddings.
     */
    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        List<Embedding> embeddings = new ArrayList<>();
        List<List<TextSegment>> batches = toBatches(textSegments);
        for (List<TextSegment> batch : batches) {
            EmbedTextRequest embedTextRequest = toEmbedTextRequest(batch);
            EmbedTextResponse response = aiClient.embedText(embedTextRequest);
            embeddings.addAll(toEmbeddings(response));
        }
        return Response.from(embeddings);
    }

    private List<List<TextSegment>> toBatches(List<TextSegment> textSegments) {
        int size = textSegments.size();
        List<List<TextSegment>> batches = new ArrayList<>();
        for (int i = 0; i < textSegments.size(); i+=EMBEDDING_BATCH_SIZE) {
            batches.add(textSegments.subList(i, Math.min(i + EMBEDDING_BATCH_SIZE, size)));
        }
        return batches;
    }

    private EmbedTextRequest toEmbedTextRequest(List<TextSegment> batch) {
        EmbedTextDetails embedTextDetails = EmbedTextDetails.builder()
                .servingMode(servingMode)
                .compartmentId(compartmentId)
                .inputs(toInputs(batch))
                .truncate(getTruncateOrDefault())
                .build();
        return EmbedTextRequest.builder().embedTextDetails(embedTextDetails).build();
    }

    private List<String> toInputs(List<TextSegment> batch) {
        return batch.stream().map(TextSegment::text).collect(Collectors.toList());
    }

    private List<Embedding> toEmbeddings(EmbedTextResponse response) {
        return response.getEmbedTextResult()
                .getEmbeddings()
                .stream()
                .map(Embedding::from)
                .collect(Collectors.toList());
    }

    private EmbedTextDetails.Truncate getTruncateOrDefault() {
        if (truncate == null) {
            return EmbedTextDetails.Truncate.None;
        }
        return truncate;
    }

    private ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .retryConfiguration(RetryConfiguration.SDK_DEFAULT_RETRY_CONFIGURATION)
                .build();
    }

    private GenerativeAiInferenceClient generativeAiInferenceClient(BasicAuthenticationDetailsProvider authenticationDetailsProvider) {
        GenerativeAiInferenceClient.Builder builder = GenerativeAiInferenceClient.builder()
                .configuration(clientConfiguration());
        return builder.build(authenticationDetailsProvider);
    }

    private ServingMode servingMode(ServingModeType servingModeType) {
        switch (servingModeType) {
            case DEDICATED:
                return DedicatedServingMode.builder().endpointId(model).build();
            case ON_DEMAND:
                return OnDemandServingMode.builder().modelId(model).build();
            default:
                throw new IllegalArgumentException("Unsupported serving mode: " + servingModeType);
        }
    }
}
