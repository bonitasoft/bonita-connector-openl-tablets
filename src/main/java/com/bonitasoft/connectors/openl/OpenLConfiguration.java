package com.bonitasoft.connectors.openl;

import lombok.Builder;
import lombok.Data;

/**
 * Immutable configuration for OpenL Tablets connector operations.
 * Holds all connection, auth, and operation-specific parameters.
 */
@Data
@Builder
public class OpenLConfiguration {

    // === Connection / Auth parameters ===
    @Builder.Default
    private String authMode = "BASIC";

    private String baseUrl;
    private String username;
    private String password;
    private String bearerToken;

    @Builder.Default
    private int connectTimeout = 30000;

    @Builder.Default
    private int readTimeout = 60000;

    // === TLS/Truststore ===
    @Builder.Default
    private boolean trustAllCertificates = false;

    private String customTrustStorePath;
    private String customTrustStorePassword;

    // === Execute Rule parameters ===
    private String serviceName;
    private String methodName;
    @Builder.Default
    private String httpMethod = "POST";
    private String requestBody;

    // === Retry ===
    @Builder.Default
    private int maxRetries = 5;
}
