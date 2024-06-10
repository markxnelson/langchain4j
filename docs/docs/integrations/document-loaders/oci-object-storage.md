---
sidebar_position: 5
---

# Oracle Cloud Infrastructure (OCI) Object Storage Document Loader

The [OCI Object Storage](https://docs.oracle.com/en-us/iaas/Content/Object/Concepts/objectstorageoverview.htm) Document Loader can be used to stream objects from an OCI Object Storage bucket as Langchain4j Documents.

### Project setup

To install langchain4j with OCI Object Storage to your project, add the following dependencies:

For Maven project `pom.xml`

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-document-loader-oci-object-storage</artifactId>
    <version>{your-version}</version> <!-- Specify your version here -->
</dependency>
```

For Gradle project `build.gradle`

```groovy
implementation 'dev.langchain4j:langchain4j-document-loader-oci-object-storage:{your-version}'
```

### Load a single document from a bucket

```java
OCIObjectStorageDocumentLoader documentLoader = new OCIObjectStorageDocumentLoader(
        tenancyNamespace,
        region,
        objectStorageClient
);
Document document = documentloader.loadDocument(bucketName, objectKey, new TextDocumentParser());
```

### Load all documents in a bucket as a stream

```java
OCIObjectStorageDocumentLoader documentLoader = new OCIObjectStorageDocumentLoader(
        tenancyNamespace,
        region,
        objectStorageClient
);
Stream<Document> documents = documentloader.streamDocuments(bucketName, new TextDocumentParser());
```

### Load all documents from a given bucket prefix as a stream

```java
OCIObjectStorageDocumentLoader documentLoader = new OCIObjectStorageDocumentLoader(
        tenancyNamespace,
        region,
        objectStorageClient
);
Stream<Document> documents = documentloader.streamDocuments(bucketName, prefixName, new TextDocumentParser());
```
