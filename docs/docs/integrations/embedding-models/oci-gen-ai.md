---
sidebar_position: 12
---

# Oracle Cloud Infrastructure (OCI) GenAI

https://www.oracle.com/artificial-intelligence/generative-ai/generative-ai-service/[OCI GenAI Service] offers text embedding with on-demand models, or dedicated AI clusters.

The https://docs.oracle.com/en-us/iaas/Content/generative-ai/embed-models.htm[OCI Embedding Models Page] and https://docs.oracle.com/en-us/iaas/Content/generative-ai/use-playground-embed.htm[OCI Text Embeddings Page] provide detailed information about using and hosting embedding models on OCI.

### Project setup

To install langchain4j with OCI GenAI to your project, add the following dependencies:

For Maven project `pom.xml`

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-oci-genai</artifactId>
    <version>{your-version}</version> <!-- Specify your version here -->
</dependency>
```

For Gradle project `build.gradle`

```groovy
implementation 'dev.langchain4j:langchain4j:{your-version}'
implementation 'dev.langchain4j:langchain4j-oci-genai:{your-version}'
```
## OCI Embedding Model Configuration

The following examples demonstrate common ways to instantiate the OCI Embedding Model.

#### Config File Authentication

```java
String configFile = Paths.get(System.getProperty("user.home"), ".oci", "config").toString();
String profile = "DEFAULT";
BasicAuthenticationDetailsProvider authenticationDetailsProvider = new ConfigFileAuthenticationDetailsProvider(configFile, profile);
EmbeddingModel embeddingModel = OCIEmbeddingModel.builder()
    .model(System.getenv("OCI_EMBEDDING_MODEL"))
    .compartmentId(System.getenv("OCI_COMPARTMENT_ID"))
    .authenticationDetailsProvider(authenticationDetailsProvider)
    .build();
```

#### Input Token Truncation

OCI GenAI accepts at most 512 tokens per text embedding. If your embeddings may exceed this amount, configure the embedding model to truncate inputs:

```java
EmbeddingModel embeddingModel = OCIEmbeddingModel.builder()
    .model(System.getenv("OCI_EMBEDDING_MODEL"))
    .compartmentId(System.getenv("OCI_COMPARTMENT_ID"))
    .truncate(EmbedTextDetails.Truncate.End) // Defaults to None. Can be None, Start, or End.
    .authenticationDetailsProvider(authenticationDetailsProvider)
    .build();
```

#### Dedicated AI Clusters

If you are using an OCI Dedicated AI cluster, configure the embedding model to use dedicated serving mode:

```java
EmbeddingModel embeddingModel = OCIEmbeddingModel.builder()
    .model(System.getenv("OCI_DEDICATED_AI_CLUSTER_ENDPOINT"))
    .compartmentId(System.getenv("OCI_COMPARTMENT_ID"))
    .servingModeType(ServingModeType.DEDICATED) // Defaults to ON_DEMAND serving mode.
    .authenticationDetailsProvider(authenticationDetailsProvider)
    .build();
```

## Embedding Model Usage

The following example class demonstrates how the OCI GenAI embeddings model can be used embed text and store it in a langchain4j embedding store:

```java
import java.util.List;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;


public class HelloWorld {
    public static void main(String[] args) {
        String configFile = Paths.get(System.getProperty("user.home"), ".oci", "config").toString();
        String profile = "DEFAULT";
        BasicAuthenticationDetailsProvider authenticationDetailsProvider = new ConfigFileAuthenticationDetailsProvider(configFile, profile);
        EmbeddingModel embeddingModel = OCIEmbeddingModel.builder()
                .model(System.getenv("OCI_EMBEDDING_MODEL"))
                .compartmentId(System.getenv("OCI_COMPARTMENT_ID"))
                .authenticationDetailsProvider(authenticationDetailsProvider)
                .build();

        // For simplicity, this example uses an in-memory store, but you can choose any external compatible store for production environments.
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        TextSegment segment1 = TextSegment.from("I like football.");
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        embeddingStore.add(embedding1, segment1);

        TextSegment segment2 = TextSegment.from("The weather is good today.");
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        embeddingStore.add(embedding2, segment2);

        String userQuery = "What is your favourite sport?";
        Embedding queryEmbedding = embeddingModel.embed(userQuery).content();
        int maxResults = 1;
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults).build();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();
        EmbeddingMatch<TextSegment> embeddingMatch = matches.getFirst();

        System.out.println("Question: " + userQuery); // What is your favourite sport?
        System.out.println("Response: " + embeddingMatch.embedded().text()); // I like football.
    }
}
```
For this example, we'll add 2 text segments, but LangChain4j offers built-in support for loading documents from various sources:
File System, URL, Amazon S3, Azure Blob Storage, GitHub, Tencent COS.
Additionally, LangChain4j supports parsing multiple document types:
text, pdf, doc, xls, ppt.

The output will be similar to this:

```plaintext
Question: What is your favourite sport?
Response: I like football.
```

### More examples
If you want to check more examples, you can find them in the [langchain4j-examples](https://github.com/langchain4j/langchain4j-examples) project.
