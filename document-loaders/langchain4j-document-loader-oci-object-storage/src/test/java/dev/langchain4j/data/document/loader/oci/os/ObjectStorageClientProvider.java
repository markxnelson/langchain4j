package dev.langchain4j.data.document.loader.oci.os;

import java.io.IOException;
import java.nio.file.Paths;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;

/**
 * Utility for retrieving an object storage client configured with default OCI config file authentication.
 * <p>
 * The OCI config file should be located at .oci/config in the current user's home directory.
 */
public class ObjectStorageClientProvider {
    public static class ClientConfiguration {
        final String region;
        final ObjectStorageClient objectStorageClient;

        public ClientConfiguration(String region, ObjectStorageClient objectStorageClient) {
            this.region = region;
            this.objectStorageClient = objectStorageClient;
        }
    }

    public static ClientConfiguration get() throws IOException {
        String configFile = Paths.get(System.getProperty("user.home"), ".oci", "config").toString();
        ConfigFileAuthenticationDetailsProvider authProvider = new ConfigFileAuthenticationDetailsProvider(configFile, "DEFAULT");
        return new ClientConfiguration(authProvider.getRegion().getRegionCode(), ObjectStorageClient.builder().build(authProvider));
    }
}
