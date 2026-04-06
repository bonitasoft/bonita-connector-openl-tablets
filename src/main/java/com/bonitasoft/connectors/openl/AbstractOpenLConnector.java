package com.bonitasoft.connectors.openl;

import lombok.extern.slf4j.Slf4j;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import java.util.Map;

/**
 * Abstract base connector for OpenL Tablets operations.
 * Handles lifecycle: validate -> connect -> execute -> disconnect.
 * Subclasses implement buildConfiguration() and doExecute().
 */
@Slf4j
public abstract class AbstractOpenLConnector extends AbstractConnector {

    // Output parameter constants
    protected static final String OUTPUT_SUCCESS = "success";
    protected static final String OUTPUT_ERROR_MESSAGE = "errorMessage";

    protected OpenLConfiguration configuration;
    protected OpenLClient client;

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        try {
            this.configuration = buildConfiguration();
            validateConfiguration(this.configuration);
        } catch (IllegalArgumentException e) {
            throw new ConnectorValidationException(this, e.getMessage());
        }
    }

    @Override
    public void connect() throws ConnectorException {
        try {
            this.client = new OpenLClient(this.configuration);
            log.info("OpenL Tablets connector connected successfully");
        } catch (OpenLException e) {
            throw new ConnectorException("Failed to connect: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() throws ConnectorException {
        this.client = null;
    }

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        try {
            doExecute();
            setOutputParameter(OUTPUT_SUCCESS, true);
        } catch (OpenLException e) {
            log.error("OpenL Tablets connector execution failed: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in OpenL Tablets connector: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, "Unexpected error: " + e.getMessage());
        }
    }

    protected abstract void doExecute() throws OpenLException;

    protected abstract OpenLConfiguration buildConfiguration();

    /**
     * Validates the configuration. Checks auth mode requirements.
     */
    protected void validateConfiguration(OpenLConfiguration config) {
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            throw new IllegalArgumentException("baseUrl is mandatory");
        }

        String authMode = config.getAuthMode();
        if ("BASIC".equalsIgnoreCase(authMode)) {
            if (config.getUsername() == null || config.getUsername().isBlank()) {
                throw new IllegalArgumentException("username is mandatory when authMode=BASIC");
            }
        } else if ("BEARER".equalsIgnoreCase(authMode)) {
            if (config.getBearerToken() == null || config.getBearerToken().isBlank()) {
                throw new IllegalArgumentException("bearerToken is mandatory when authMode=BEARER");
            }
        } else if (authMode != null && !authMode.isBlank()) {
            throw new IllegalArgumentException("Invalid authMode: " + authMode + ". Must be BASIC or BEARER");
        }
    }

    /** Helper: read a String input, returning null if not set. */
    protected String readStringInput(String name) {
        Object value = getInputParameter(name);
        return value != null ? value.toString() : null;
    }

    /** Helper: read a String input with a default value. */
    protected String readStringInput(String name, String defaultValue) {
        String value = readStringInput(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    /** Helper: read a Boolean input with a default value. */
    protected Boolean readBooleanInput(String name, boolean defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? (Boolean) value : defaultValue;
    }

    /** Helper: read an Integer input with a default value. */
    protected Integer readIntegerInput(String name, int defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? ((Number) value).intValue() : defaultValue;
    }

    /**
     * Exposes output parameters for testing. Package-visible.
     */
    Map<String, Object> getOutputs() {
        return getOutputParameters();
    }
}
