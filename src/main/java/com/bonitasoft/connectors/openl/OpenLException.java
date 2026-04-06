package com.bonitasoft.connectors.openl;

/**
 * Typed exception for OpenL Tablets connector.
 */
public class OpenLException extends Exception {

    private final int statusCode;
    private final boolean retryable;

    public OpenLException(String message) {
        super(message);
        this.statusCode = -1;
        this.retryable = false;
    }

    public OpenLException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.retryable = false;
    }

    public OpenLException(String message, int statusCode, boolean retryable) {
        super(message);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public OpenLException(String message, int statusCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
