package dev.langchain4j.model.oci;

public class OCIConfiguration {
    public static final String OCI_COMPARTMENT_ID_KEY = "OCI_COMPARTMENT_ID";
    public static final String OCI_COMPARTMENT_ID = System.getenv(OCI_COMPARTMENT_ID_KEY);
}
