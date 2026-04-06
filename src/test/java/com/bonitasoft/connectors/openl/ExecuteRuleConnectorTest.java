package com.bonitasoft.connectors.openl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ExecuteRuleConnectorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private OpenLClient mockClient;

    private ExecuteRuleConnector connector;

    @BeforeEach
    void setUp() {
        connector = new ExecuteRuleConnector();
    }

    @Test
    void shouldExecuteSuccessfully() throws Exception {
        // Given
        Map<String, Object> inputs = validInputs();
        connector.setInputParameters(inputs);
        connector.validateInputParameters();

        injectMockClient();

        var resultNode = MAPPER.readTree("{\"status\":\"ok\"}");
        when(mockClient.executeRule("MyService", "calculateRate", "POST", "{\"age\":30}"))
                .thenReturn(new OpenLClient.ExecuteRuleResult("{\"status\":\"ok\"}", resultNode));

        // When
        connector.executeBusinessLogic();

        // Then
        Map<String, Object> outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("responseBody")).isEqualTo("{\"status\":\"ok\"}");
    }

    @Test
    void shouldFailValidationWhenBaseUrlMissing() {
        Map<String, Object> inputs = validInputs();
        inputs.remove("baseUrl");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("baseUrl");
    }

    @Test
    void shouldFailValidationWhenServiceNameMissing() {
        Map<String, Object> inputs = validInputs();
        inputs.remove("serviceName");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("serviceName");
    }

    @Test
    void shouldFailValidationWhenMethodNameMissing() {
        Map<String, Object> inputs = validInputs();
        inputs.remove("methodName");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("methodName");
    }

    @Test
    void shouldFailValidationWhenUsernameBlankForBasicAuth() {
        Map<String, Object> inputs = validInputs();
        inputs.put("username", "");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("username");
    }

    @Test
    void shouldFailValidationWhenBearerTokenMissingForBearerAuth() {
        Map<String, Object> inputs = validInputs();
        inputs.put("authMode", "BEARER");
        inputs.remove("username");
        inputs.remove("password");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("bearerToken");
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        Map<String, Object> inputs = validInputs();
        connector.setInputParameters(inputs);
        connector.validateInputParameters();

        injectMockClient();

        when(mockClient.executeRule(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new OpenLException("API error 401: Unauthorized", 401, false));

        connector.executeBusinessLogic();

        Map<String, Object> outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("401");
    }

    @Test
    void shouldApplyDefaultsForNullOptionalInputs() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("baseUrl", "http://localhost:8080");
        inputs.put("username", "admin");
        inputs.put("serviceName", "Svc");
        inputs.put("methodName", "run");
        connector.setInputParameters(inputs);
        connector.validateInputParameters();

        // Should have default authMode=BASIC, httpMethod=POST, etc.
        assertThat(connector.configuration.getAuthMode()).isEqualTo("BASIC");
        assertThat(connector.configuration.getHttpMethod()).isEqualTo("POST");
        assertThat(connector.configuration.getConnectTimeout()).isEqualTo(30000);
        assertThat(connector.configuration.getReadTimeout()).isEqualTo(60000);
    }

    private Map<String, Object> validInputs() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("authMode", "BASIC");
        inputs.put("baseUrl", "http://localhost:8080");
        inputs.put("username", "admin");
        inputs.put("password", "secret");
        inputs.put("serviceName", "MyService");
        inputs.put("methodName", "calculateRate");
        inputs.put("httpMethod", "POST");
        inputs.put("requestBody", "{\"age\":30}");
        return inputs;
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractOpenLConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }
}
