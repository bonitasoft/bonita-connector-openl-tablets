package com.bonitasoft.connectors.openl;

import lombok.extern.slf4j.Slf4j;

/**
 * Connector for executing an OpenL Tablets rule via REST API.
 * URL pattern: {httpMethod} {baseUrl}/REST/{serviceName}/{methodName}
 */
@Slf4j
public class ExecuteRuleConnector extends AbstractOpenLConnector {

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
    static final String INPUT_METHOD_NAME = "methodName";
    static final String INPUT_HTTP_METHOD = "httpMethod";
    static final String INPUT_REQUEST_BODY = "requestBody";

    // Output parameter name constants
    static final String OUTPUT_RESPONSE_BODY = "responseBody";
    static final String OUTPUT_STATUS_CODE = "statusCode";

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
                .methodName(readStringInput(INPUT_METHOD_NAME))
                .httpMethod(readStringInput(INPUT_HTTP_METHOD, "POST"))
                .requestBody(readStringInput(INPUT_REQUEST_BODY))
                .build();
    }

    @Override
    protected void validateConfiguration(OpenLConfiguration config) {
        super.validateConfiguration(config);
        if (config.getServiceName() == null || config.getServiceName().isBlank()) {
            throw new IllegalArgumentException("serviceName is mandatory");
        }
        if (config.getMethodName() == null || config.getMethodName().isBlank()) {
            throw new IllegalArgumentException("methodName is mandatory");
        }
    }

    @Override
    protected void doExecute() throws OpenLException {
        log.info("Executing OpenL rule: {}/{} via {}",
                configuration.getServiceName(), configuration.getMethodName(),
                configuration.getHttpMethod());

        OpenLClient.ExecuteRuleResult result = client.executeRule(
                configuration.getServiceName(),
                configuration.getMethodName(),
                configuration.getHttpMethod(),
                configuration.getRequestBody()
        );

        setOutputParameter(OUTPUT_RESPONSE_BODY, result.responseBody());

        log.info("OpenL rule executed successfully");
    }
}
