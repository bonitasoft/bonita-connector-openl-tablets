package com.bonitasoft.connectors.openl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ListMethodsConnectorTest {

    @Mock
    private OpenLClient mockClient;

    private ListMethodsConnector connector;

    @BeforeEach
    void setUp() {
        connector = new ListMethodsConnector();
    }

    @Test
    void shouldListMethodsSuccessfully() throws Exception {
        Map<String, Object> inputs = validInputs();
        connector.setInputParameters(inputs);
        connector.validateInputParameters();

        injectMockClient();

        when(mockClient.listMethods("MyService"))
                .thenReturn(new OpenLClient.ListMethodsResult("[\"method1\",\"method2\"]", 2));

        connector.executeBusinessLogic();

        Map<String, Object> outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("methodsJson")).isEqualTo("[\"method1\",\"method2\"]");
        assertThat(outputs.get("methodCount")).isEqualTo(2);
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
    void shouldFailValidationWhenBaseUrlMissing() {
        Map<String, Object> inputs = validInputs();
        inputs.remove("baseUrl");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("baseUrl");
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        Map<String, Object> inputs = validInputs();
        connector.setInputParameters(inputs);
        connector.validateInputParameters();

        injectMockClient();

        when(mockClient.listMethods("MyService"))
                .thenThrow(new OpenLException("Service not found", 404, false));

        connector.executeBusinessLogic();

        Map<String, Object> outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("not found");
    }

    private Map<String, Object> validInputs() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("authMode", "BASIC");
        inputs.put("baseUrl", "http://localhost:8080");
        inputs.put("username", "admin");
        inputs.put("password", "secret");
        inputs.put("serviceName", "MyService");
        return inputs;
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractOpenLConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }
}
