package dev.langchain4j.model.oci;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import com.oracle.bmc.generativeaiinference.model.EmbedTextDetails;
import com.oracle.bmc.generativeaiinference.model.ServingMode;
import com.oracle.bmc.generativeaiinference.requests.EmbedTextRequest;
import com.oracle.bmc.generativeaiinference.responses.EmbedTextResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class OCIEmbeddingModel extends OCIClientProvider implements EmbeddingModel {
    /**
     * OCI GenAI accepts a maximum of 96 inputs per embedding request. If the Langchain input is greater
     * than 96 segments, the input will be split into chunks of this size.
     */
    private static final int EMBEDDING_BATCH_SIZE = 96;

    private GenerativeAiInferenceClient aiClient;
    private ServingMode servingMode;

    /**
     * OCI GenAi accepts a maximum of 512 tokens per embedding. If the number of tokens exceeds this amount,
     * and the embedding truncation value is set to None (default), an error will be received.
     * <p>
     * If truncate is set to START, embeddings will be truncated to 512 tokens from the start of the input.
     * If truncate is set to END, embeddings will be truncated to 512 tokens from the end of the input.
     */
    private EmbedTextDetails.Truncate truncate;

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        List<Embedding> embeddings = new ArrayList<>();
        List<List<TextSegment>> batches = toBatches(textSegments);
        for (List<TextSegment> batch : batches) {
            EmbedTextRequest embedTextRequest = toEmbedTextRequest(batch);
            EmbedTextResponse response = getGenerativeAIClient().embedText(embedTextRequest);
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
                .servingMode(getServingMode())
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

    private GenerativeAiInferenceClient getGenerativeAIClient() {
        if (aiClient == null) {
            aiClient = generativeAiInferenceClient();
        }
        return aiClient;
    }

    private ServingMode getServingMode() {
        if (servingMode == null) {
            servingMode = super.servingMode();
        }
        return servingMode;
    }
}
