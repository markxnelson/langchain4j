package dev.langchain4j.data.document.source.oci.os;

import java.io.IOException;
import java.io.InputStream;

import dev.langchain4j.data.document.DocumentSource;
import dev.langchain4j.data.document.Metadata;

import static java.lang.String.format;

/**
 * OCI Object Storage implementation of Langchain4j DocumentSource
 * Document InputStream represents an object in an OCI Object Storage bucket.
 */
public class OCIObjectStorageSource implements DocumentSource {
    public static final String SOURCE = "source";

    private final InputStream inputStream;
    private final String namespace;
    private final String region;
    private final String bucket;
    private final String key;

    public OCIObjectStorageSource(InputStream inputStream, String namespace, String region, String bucket, String key) {
        this.inputStream = inputStream;
        this.namespace = namespace;
        this.region = region;
        this.bucket = bucket;
        this.key = key;
    }

    @Override
    public InputStream inputStream() throws IOException {
        return inputStream;
    }

    @Override
    public Metadata metadata() {
        Metadata metadata = new Metadata();
        // Object Storage has the following native format for object URIs:
        // https://objectstorage.region.oraclecloud.com/n/namespace-string/b/bucket/o/filename
        metadata.put(SOURCE, format("https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s", region, namespace, bucket, key));
        return metadata;
    }
}
