package com.bonitasoft.connectors.openl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;

/**
 * HTTP client facade for OpenL Tablets Rule Services REST API.
 * Uses java.net.http.HttpClient with Basic Auth or Bearer Token authentication.
 * All methods use the RetryPolicy for automatic exponential backoff on 429/5xx.
 */
@Slf4j
public class OpenLClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OpenLConfiguration configuration;
    private final HttpClient httpClient;
    private final RetryPolicy retryPolicy;

    public OpenLClient(OpenLConfiguration configuration) throws OpenLException {
        this.configuration = configuration;
        this.retryPolicy = new RetryPolicy(configuration.getMaxRetries());
        this.httpClient = buildHttpClient(configuration);
        log.debug("OpenLClient initialized with authMode={}", configuration.getAuthMode());
    }

    // Visible for testing
    OpenLClient(OpenLConfiguration configuration, HttpClient httpClient, RetryPolicy retryPolicy) {
        this.configuration = configuration;
        this.httpClient = httpClient;
        this.retryPolicy = retryPolicy;
    }

    private HttpClient buildHttpClient(OpenLConfiguration config) throws OpenLException {
        try {
            HttpClient.Builder builder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(config.getConnectTimeout()));

            if (config.isTrustAllCertificates()) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
                builder.sslContext(sslContext);
            } else if (config.getCustomTrustStorePath() != null && !config.getCustomTrustStorePath().isBlank()) {
                SSLContext sslContext = buildCustomTrustStoreContext(config);
                builder.sslContext(sslContext);
            }

            return builder.build();
        } catch (OpenLException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenLException("Failed to build HTTP client: " + e.getMessage(), e);
        }
    }

    private SSLContext buildCustomTrustStoreContext(OpenLConfiguration config) throws OpenLException {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] password = config.getCustomTrustStorePassword() != null
                    ? config.getCustomTrustStorePassword().toCharArray()
                    : null;
            try (FileInputStream fis = new FileInputStream(config.getCustomTrustStorePath())) {
                trustStore.load(fis, password);
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new OpenLException("Failed to load custom truststore: " + e.getMessage(), e);
        }
    }

    // === Operations ===

    /**
     * Execute a rule by calling {httpMethod} {baseUrl}/REST/{serviceName}/{methodName}.
     */
    public ExecuteRuleResult executeRule(String serviceName, String methodName,
                                         String httpMethod, String requestBody) throws OpenLException {
        return retryPolicy.execute(() -> {
            String path = "/REST/" + serviceName + "/" + methodName;
            JsonNode response;

            if ("GET".equalsIgnoreCase(httpMethod)) {
                response = doGet(path);
            } else {
                response = doRequest(httpMethod.toUpperCase(), path, requestBody);
            }

            String responseJson = MAPPER.writeValueAsString(response);
            return new ExecuteRuleResult(responseJson, response);
        });
    }

    /**
     * List all deployed services via GET {baseUrl}/admin/services/.
     */
    public ListServicesResult listServices() throws OpenLException {
        return retryPolicy.execute(() -> {
            JsonNode response = doGet("/admin/services/");
            String servicesJson = MAPPER.writeValueAsString(response);
            int count = response.isArray() ? response.size() : 0;
            return new ListServicesResult(servicesJson, count);
        });
    }

    /**
     * List methods for a service via GET {baseUrl}/admin/services/{serviceName}/methods/.
     */
    public ListMethodsResult listMethods(String serviceName) throws OpenLException {
        return retryPolicy.execute(() -> {
            JsonNode response = doGet("/admin/services/" + serviceName + "/methods/");
            String methodsJson = MAPPER.writeValueAsString(response);
            int count = response.isArray() ? response.size() : 0;
            return new ListMethodsResult(methodsJson, count);
        });
    }

    // === HTTP Methods ===

    private JsonNode doGet(String path) throws OpenLException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(configuration.getBaseUrl() + path))
                    .headers(authHeaders())
                    .header("Accept", "application/json")
                    .GET()
                    .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response);
        } catch (OpenLException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new OpenLException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private JsonNode doRequest(String method, String path, String body) throws OpenLException {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(configuration.getBaseUrl() + path))
                    .headers(authHeaders())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(configuration.getReadTimeout()));

            String requestBody = (body != null && !body.isBlank()) ? body : "{}";
            builder.method(method, HttpRequest.BodyPublishers.ofString(requestBody));

            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response);
        } catch (OpenLException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new OpenLException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private String[] authHeaders() throws OpenLException {
        if ("BEARER".equalsIgnoreCase(configuration.getAuthMode())) {
            String token = configuration.getBearerToken();
            if (token == null || token.isBlank()) {
                throw new OpenLException("Bearer token is required when authMode=BEARER");
            }
            return new String[]{"Authorization", "Bearer " + token};
        } else {
            // Default: BASIC
            String username = configuration.getUsername();
            String password = configuration.getPassword();
            if (username == null || username.isBlank()) {
                throw new OpenLException("Username is required when authMode=BASIC");
            }
            String credentials = Base64.getEncoder()
                    .encodeToString((username + ":" + (password != null ? password : ""))
                            .getBytes(StandardCharsets.UTF_8));
            return new String[]{"Authorization", "Basic " + credentials};
        }
    }

    private JsonNode handleResponse(HttpResponse<String> response) throws OpenLException {
        int statusCode = response.statusCode();
        String responseBody = response.body();

        if (statusCode >= 200 && statusCode < 300) {
            try {
                if (responseBody == null || responseBody.isBlank()) {
                    return MAPPER.createObjectNode();
                }
                return MAPPER.readTree(responseBody);
            } catch (JsonProcessingException e) {
                throw new OpenLException("Failed to parse response: " + e.getMessage(), e);
            }
        }

        boolean retryable = RetryPolicy.isRetryableStatusCode(statusCode);
        String errorMessage = buildErrorMessage(statusCode, responseBody);
        throw new OpenLException(errorMessage, statusCode, retryable);
    }

    private String buildErrorMessage(int statusCode, String responseBody) {
        try {
            JsonNode error = MAPPER.readTree(responseBody);
            String message = getTextOrNull(error, "message");
            if (message != null) {
                return String.format("OpenL API error %d: %s", statusCode, message);
            }
        } catch (JsonProcessingException ignored) {
            // Fall through to generic message
        }
        return "OpenL API error " + statusCode + ": " + responseBody;
    }

    private String getTextOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return null;
        return node.get(field).asText();
    }

    // === Trust-all TLS manager ===

    private static class TrustAllManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // Trust all
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // Trust all
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    // === Result records ===

    public record ExecuteRuleResult(String responseBody, JsonNode responseJson) {}

    public record ListServicesResult(String servicesJson, int serviceCount) {}

    public record ListMethodsResult(String methodsJson, int methodCount) {}
}
