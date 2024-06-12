package dev.langchain4j.model.oci;

import lombok.Getter;

/**
 * Authentication Type for OCIClientProvider builder.
 */
@Getter
public enum AuthenticationType {

    FILE("file"),
    INSTANCE_PRINCIPAL("instance-principal"),
    WORKLOAD_IDENTITY("workload-identity"),
    SIMPLE("simple");

    private final String authType;

    AuthenticationType(String authType) {
        this.authType = authType;
    }

}
