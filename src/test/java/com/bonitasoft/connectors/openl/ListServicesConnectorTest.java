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
class ListServicesConnectorTest {

    @Mock
    private OpenLClient mockClient;

    private ListServicesConnector connector;

    @BeforeEach
    void setUp() {
        connector = new ListServicesConnector();
    }

    @Test
    void shouldListServicesSuccessfully() throws Exception {
        Map<String, Object> inputs = validInputs();
        connector.setInputParameters(inputs);
        connector.validateInputParameters();

        injectMockClient();

        when(mockClient.listServices())
                .thenReturn(new OpenLClient.ListServicesResult("[\"Service1\",\"Service2\"]", 2));

        connector.executeBusinessLogic();

        Map<String, Object> outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("servicesJson")).isEqualTo("[\"Service1\",\"Service2\"]");
        assertThat(outputs.get("serviceCount")).isEqualTo(2);
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

        when(mockClient.listServices())
                .thenThrow(new OpenLException("Connection refused", -1, false));

        connector.executeBusinessLogic();

        Map<String, Object> outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("Connection refused");
    }

    private Map<String, Object> validInputs() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("authMode", "BASIC");
        inputs.put("baseUrl", "http://localhost:8080");
        inputs.put("username", "admin");
        inputs.put("password", "secret");
        return inputs;
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractOpenLConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }
}
