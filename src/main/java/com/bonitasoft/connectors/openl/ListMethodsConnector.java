package com.bonitasoft.connectors.openl;

import lombok.extern.slf4j.Slf4j;

/**
 * Connector for listing methods of a deployed service on OpenL Tablets.
 * URL: GET {baseUrl}/admin/services/{serviceName}/methods/
 */
@Slf4j
public class ListMethodsConnector extends AbstractOpenLConnector {

    // Input parameter name constants
    static final String INPUT_AUTH_MODE = "authMode";
    static final String INPUT_BASE_URL = "baseUrl";
    static final String INPUT_USERNAME = "username";
    static final String INPUT_PASSWORD = "password";
    static final String INPUT_BEARER_TOKEN = "bearerToken";
    static final String INPUT_CONNECT_TIMEOUT = "connectTimeout";
    static final String INPUT_READ_TIMEOUT = "readTimeout";
    static final String INPUT_TRUST_ALL_CERTIFICATES = "trustAllCertificates";
    static final String INPUT_CUSTOM_TRUST_STORE_PATH = "customTrustStorePath";
    static final String INPUT_CUSTOM_TRUST_STORE_PASSWORD = "customTrustStorePassword";
    static final String INPUT_SERVICE_NAME = "serviceName";

    // Output parameter name constants
    static final String OUTPUT_METHODS_JSON = "methodsJson";
    static final String OUTPUT_METHOD_COUNT = "methodCount";

    @Override
    protected OpenLConfiguration buildConfiguration() {
        return OpenLConfiguration.builder()
                .authMode(readStringInput(INPUT_AUTH_MODE, "BASIC"))
                .baseUrl(readStringInput(INPUT_BASE_URL))
                .username(readStringInput(INPUT_USERNAME))
                .password(readStringInput(INPUT_PASSWORD))
                .bearerToken(readStringInput(INPUT_BEARER_TOKEN))
                .connectTimeout(readIntegerInput(INPUT_CONNECT_TIMEOUT, 30000))
                .readTimeout(readIntegerInput(INPUT_READ_TIMEOUT, 60000))
                .trustAllCertificates(readBooleanInput(INPUT_TRUST_ALL_CERTIFICATES, false))
                .customTrustStorePath(readStringInput(INPUT_CUSTOM_TRUST_STORE_PATH))
                .customTrustStorePassword(readStringInput(INPUT_CUSTOM_TRUST_STORE_PASSWORD))
                .serviceName(readStringInput(INPUT_SERVICE_NAME))
                .build();
    }

    @Override
    protected void validateConfiguration(OpenLConfiguration config) {
        super.validateConfiguration(config);
        if (config.getServiceName() == null || config.getServiceName().isBlank()) {
            throw new IllegalArgumentException("serviceName is mandatory for listing methods");
        }
    }

    @Override
    protected void doExecute() throws OpenLException {
        log.info("Listing methods for OpenL service: {}", configuration.getServiceName());

        OpenLClient.ListMethodsResult result = client.listMethods(configuration.getServiceName());

        setOutputParameter(OUTPUT_METHODS_JSON, result.methodsJson());
        setOutputParameter(OUTPUT_METHOD_COUNT, result.methodCount());

        log.info("Listed {} methods for service {}", result.methodCount(), configuration.getServiceName());
    }
}
