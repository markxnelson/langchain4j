package dev.langchain4j.data.document.loader.oci.os;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentLoader;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.source.oci.os.OCIObjectStorageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OCIObjectStorageDocumentLoader {
    private static final Logger log = LoggerFactory.getLogger(OCIObjectStorageDocumentLoader.class);
    private static final Integer DEFAULT_LIMIT = -1;

    // Tenancy-unique Object Storage namespace
    private final String namespace;
    private final String region;
    private final ObjectStorageClient objectStorageClient;
    // If set, the document loader will skip object loading errors when processing multiple documents from a bucket.
    private final boolean skipObjectLoadOnError;
    private final Integer limit;

    public OCIObjectStorageDocumentLoader(String namespace, String region, ObjectStorageClient objectStorageClient) {
        this(namespace, region, objectStorageClient, true, DEFAULT_LIMIT);
    }

    public OCIObjectStorageDocumentLoader(String namespace, String region, ObjectStorageClient objectStorageClient, boolean skipObjectLoadOnError) {
        this(namespace, region, objectStorageClient, skipObjectLoadOnError, DEFAULT_LIMIT);
    }

    public OCIObjectStorageDocumentLoader(String namespace, String region, ObjectStorageClient objectStorageClient, boolean skipObjectLoadOnError, Integer limit) {
        this.namespace = namespace;
        this.region = region;
        this.objectStorageClient = objectStorageClient;
        this.skipObjectLoadOnError = skipObjectLoadOnError;
        this.limit = limit;
    }

    /**
     * Loads a document from an OCI Object Storage bucket.
     *
     * @param bucket The Object Storage bucket name to load from.
     * @param key The key of the object to load within the bucket.
     * @param parser The parser used when parsing the object text.
     * @return A document containing the content of the Object Storage object.
     */
    public Document loadDocument(String bucket, String key, DocumentParser parser) {
        GetObjectRequest request = GetObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucket)
                .objectName(key)
                .build();
        GetObjectResponse response = objectStorageClient.getObject(request);
        OCIObjectStorageSource source = new OCIObjectStorageSource(
                response.getInputStream(),
                namespace,
                region,
                bucket,
                key
        );
        return DocumentLoader.load(source, parser);
    }

    /**
     * Load documents in an Object Storage bucket for a given prefix.
     *
     * @param bucket The Object Storage bucket name to load from.
     * @param prefix The prefix objects should start with.
     * @param parser The parser used when parsing the object text.
     * @return A list of documents from the bucket matching the prefix.
     */
    public List<Document> loadDocuments(String bucket, String prefix, DocumentParser parser) {
        ListObjectsRequest.Builder requestBuilder = ListObjectsRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucket)
                .prefix(prefix);
        if (!limit.equals(DEFAULT_LIMIT)) {
            requestBuilder.limit(limit);
        }
        ListObjectsResponse response = objectStorageClient.listObjects(requestBuilder.build());
        List<Document> documents = new ArrayList<>(toDocuments(bucket, parser, response));
        while (response.getListObjects().getNextStartWith() != null) {
            requestBuilder = ListObjectsRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucket)
                    .prefix(prefix)
                    .start(response.getListObjects().getNextStartWith());
            if (!limit.equals(DEFAULT_LIMIT)) {
                requestBuilder.limit(limit);
            }
            response = objectStorageClient.listObjects(requestBuilder.build());
            documents.addAll(toDocuments(bucket, parser, response));
        }
        return documents;
    }

    /**
     * Loads all documents from an Object Storage bucket.
     *
     * @param bucket The Object Storage bucket name to load from.
     * @param parser The parser used when parsing the object text.
     * @return A list of documents from the bucket.
     */
    public List<Document> loadDocuments(String bucket, DocumentParser parser) {
        return loadDocuments(bucket, null, parser);
    }

    private List<Document> toDocuments(String bucket, DocumentParser parser, ListObjectsResponse response) {
        return response.getListObjects().getObjects()
                .stream()
                // ignore empty objects and directories
                .filter(o -> !o.getName().endsWith("/"))
                // convert to langchain document
                .flatMap((o -> {
                    try {
                        return Stream.of(loadDocument(bucket, o.getName(), parser));
                    } catch (Exception e) {
                        if (skipObjectLoadOnError) {
                            log.warn("Failed to load object {}/{}, skipping", bucket, o.getName(), e);
                            return Stream.empty();
                        }
                        throw e;
                    }
                }))
                .collect(Collectors.toList());
    }
}
