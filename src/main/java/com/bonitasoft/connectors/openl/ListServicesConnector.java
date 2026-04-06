package com.bonitasoft.connectors.openl;

import lombok.extern.slf4j.Slf4j;

/**
 * Connector for listing deployed services on OpenL Tablets.
 * URL: GET {baseUrl}/admin/services/
 */
@Slf4j
public class ListServicesConnector extends AbstractOpenLConnector {

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

    // Output parameter name constants
    static final String OUTPUT_SERVICES_JSON = "servicesJson";
    static final String OUTPUT_SERVICE_COUNT = "serviceCount";

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
                .build();
    }

    @Override
    protected void doExecute() throws OpenLException {
        log.info("Listing OpenL Tablets services");

        OpenLClient.ListServicesResult result = client.listServices();

        setOutputParameter(OUTPUT_SERVICES_JSON, result.servicesJson());
        setOutputParameter(OUTPUT_SERVICE_COUNT, result.serviceCount());

        log.info("Listed {} OpenL Tablets services", result.serviceCount());
    }
}
