package dev.langchain4j.model.oci;

import java.io.IOException;
import java.nio.file.Paths;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.auth.okeworkloadidentity.OkeWorkloadIdentityAuthenticationDetailsProvider;
import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import com.oracle.bmc.generativeaiinference.model.DedicatedServingMode;
import com.oracle.bmc.generativeaiinference.model.OnDemandServingMode;
import com.oracle.bmc.generativeaiinference.model.ServingMode;
import com.oracle.bmc.retrier.RetryConfiguration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


/**
 * Client Builder for OCI authentication.
 * Supports file, simple, instance principal, and workload identity authentication.
 */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
class OCIClientProvider {
    @Builder.Default
    private AuthenticationType authenticationType = AuthenticationType.FILE;
    @Builder.Default
    private ServingModeType servingModeType = ServingModeType.ON_DEMAND;
    private String model;
    protected String compartmentId;
    @Builder.Default
    private String region = "us-chicago-1";
    @Builder.Default
    private String configFile = Paths.get(System.getProperty("user.home"), ".oci", "config").toString();
    @Builder.Default
    private String profile = "DEFAULT";
    private String tenantId;
    private String userId;
    private String fingerprint;
    private String privateKey;
    private String passPhrase;
    private String endpoint;

    private boolean hasRegion() {
        return region != null && !region.isEmpty();
    }

    private boolean hasEndpoint() {
        return endpoint != null && !endpoint.isEmpty();
    }

    private ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .retryConfiguration(RetryConfiguration.SDK_DEFAULT_RETRY_CONFIGURATION)
                .build();
    }

    protected BasicAuthenticationDetailsProvider authenticationProvider() {
        switch (authenticationType) {
            case FILE:
                try {
                    return new ConfigFileAuthenticationDetailsProvider(configFile, profile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            case INSTANCE_PRINCIPAL:
                return InstancePrincipalsAuthenticationDetailsProvider.builder().build();
            case WORKLOAD_IDENTITY:
                return OkeWorkloadIdentityAuthenticationDetailsProvider.builder().build();
            case SIMPLE:
                return SimpleAuthenticationDetailsProvider.builder()
                        .userId(userId)
                        .tenantId(tenantId)
                        .fingerprint(fingerprint)
                        .privateKeySupplier(new SimplePrivateKeySupplier(privateKey))
                        .passPhrase(passPhrase)
                        .region(Region.valueOf(region))
                        .build();
            default:
                throw new IllegalArgumentException("Unsupported authentication type: " + authenticationType);
        }
    }

    protected GenerativeAiInferenceClient generativeAiInferenceClient() {
        GenerativeAiInferenceClient.Builder builder = GenerativeAiInferenceClient.builder()
                .configuration(clientConfiguration());
        if (hasRegion()) {
            builder.region(Region.valueOf(region));
        }
        if (hasEndpoint()) {
            builder.endpoint(endpoint);
        }
        return builder.build(authenticationProvider());
    }

    protected ServingMode servingMode() {
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
