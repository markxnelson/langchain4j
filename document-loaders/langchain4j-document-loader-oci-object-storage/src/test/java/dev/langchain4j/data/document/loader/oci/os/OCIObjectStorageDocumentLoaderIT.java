package dev.langchain4j.data.document.loader.oci.os;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BiConsumer;

import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.CreateBucketDetails;
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest;
import com.oracle.bmc.objectstorage.requests.DeleteBucketRequest;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test will create a bucket in your OCI tenancy, populating it with test data.
 * After the test runs, the bucket and its data will be deleted.
 * <p>
 * To run the test the following conditions must be met:
 * 1. Set the "OCI_NAMESPACE" environment variable to your tenancy's namespace.
 * 2. Set the "OCI_COMPARTMENT" environment variable to the compartment OCID you wish to run the test in.
 * 3. Have a valid .oci/config file in your user's home directory.
 * 4. Ensure your OCI user can manage OCI Object Storage buckets and their contents,
 * at least for a bucket named "langchain4j-oci-test-bucket".
 * <p>
 * On error, the test may result in an orphaned bucket. If this occurs, it is recommended to check your OCI account and do
 * any required cleanup (deleting the "langchain4j-oci-test-bucket" Object storage bucket).
 */
@EnabledIfEnvironmentVariables({
        @EnabledIfEnvironmentVariable(named = OCIObjectStorageDocumentLoaderIT.OCI_NAMESPACE, matches = ".+"),
        @EnabledIfEnvironmentVariable(named = OCIObjectStorageDocumentLoaderIT.OCI_COMPARTMENT, matches = ".+")
})
public class OCIObjectStorageDocumentLoaderIT {
    private static final Logger log = LoggerFactory.getLogger(OCIObjectStorageDocumentLoader.class);

    public static final String OCI_BUCKET_NAME = "langchain4j-oci-test-bucket";
    public static final String OCI_NAMESPACE = "OCI_NAMESPACE";
    public static final String OCI_COMPARTMENT = "OCI_COMPARTMENT";

    private static final String NAMESPACE_NAME = System.getenv(OCI_NAMESPACE);
    private static final String COMPARTMENT_OCID = System.getenv(OCI_COMPARTMENT);

    private static OCIObjectStorageDocumentLoader documentLoader;
    private static ObjectStorageClient objectStorageClient;
    private static DocumentParser parser = new TextDocumentParser();

    @BeforeAll
    static void setup() throws IOException {
        ObjectStorageClientProvider.ClientConfiguration client = ObjectStorageClientProvider.get();
        documentLoader = new OCIObjectStorageDocumentLoader(NAMESPACE_NAME, client.region, client.objectStorageClient, true, 1);
        objectStorageClient = client.objectStorageClient;

        deleteBucket(); // try to clean up the bucket if it already exists

        CreateBucketDetails createBucketDetails = CreateBucketDetails.builder()
                .compartmentId(COMPARTMENT_OCID)
                .name(OCI_BUCKET_NAME)
                .build();
        CreateBucketRequest request = CreateBucketRequest.builder()
                .createBucketDetails(createBucketDetails)
                .namespaceName(NAMESPACE_NAME)
                .build();
        objectStorageClient.createBucket(request);
        log.info("Created test bucket");
        walkObjects(OCIObjectStorageDocumentLoaderIT::createBucketObject);
        log.info("Populated test data");
    }

    @AfterAll
    static void teardown() throws IOException {
        deleteBucket();
    }

    static void deleteBucket() throws IOException {
        try {
            walkObjects(OCIObjectStorageDocumentLoaderIT::deleteBucketObject);
            DeleteBucketRequest request = DeleteBucketRequest.builder()
                    .bucketName(OCI_BUCKET_NAME)
                    .namespaceName(NAMESPACE_NAME)
                    .build();
            objectStorageClient.deleteBucket(request);
            log.info("Deleted test bucket");
        } catch (BmcException e) {
            if (!e.getServiceCode().equals("BucketNotFound")) {
                log.info("Did not clean up test bucket", e);
                throw e;
            }
        }
    }

    static void walkObjects(BiConsumer<File, String> fileAction) throws IOException {
        File objectsDir = new File("src/test/resources/objects");
        String pathPrefix = "^" + objectsDir.getAbsolutePath();
        Files.walk(Paths.get(objectsDir.toURI()))
                .map(Path::toFile)
                .filter(f -> !f.isDirectory())
                .forEach(f -> fileAction.accept(f, f.getPath().replaceFirst(pathPrefix + "/", "")));
    }

    static void deleteBucketObject(File f, String objectName) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucketName(OCI_BUCKET_NAME)
                    .namespaceName(NAMESPACE_NAME)
                    .objectName(objectName)
                    .build();
            objectStorageClient.deleteObject(request);
            log.info("Deleted object {}", objectName);
        } catch (BmcException e) {
            if (!e.getServiceCode().equals("BucketNotFound")) {
                throw e;
            }
        }
    }

    static void createBucketObject(File f, String objectName) {
        try {
            // trim the local
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucketName(OCI_BUCKET_NAME)
                    .namespaceName(NAMESPACE_NAME)
                    .objectName(objectName)
                    .contentLength(f.length())
                    .putObjectBody(Files.newInputStream(f.toPath()))
                    .build();
            objectStorageClient.putObject(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void loadDocument() {
        Document document = documentLoader.loadDocument(OCI_BUCKET_NAME, "hello.txt", parser);
        assertThat(document.text()).contains("Hello, world!");
    }

    @Test
    void loadDocumentsWithPrefix() {
        List<Document> documents = documentLoader.loadDocuments(OCI_BUCKET_NAME, "d1", parser);
        assertThat(documents).hasSize(2);
    }

    @Test
    void loadAllDocuments() {
        List<Document> documents = documentLoader.loadDocuments(OCI_BUCKET_NAME, parser);
        assertThat(documents).hasSize(3);
    }
}
