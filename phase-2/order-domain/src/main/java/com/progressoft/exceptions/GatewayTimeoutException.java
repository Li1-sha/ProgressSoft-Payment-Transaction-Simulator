package com.progressoft.exceptions;

public class GatewayTimeoutException extends RuntimeException {
    private final String endpoint;
    private final long timeoutMillis;
    private final String operation;
    private final String errorCode;

    public GatewayTimeoutException(String endpoint, long timeoutMillis, String operation) {
        super(String.format("Gateway timeout calling %s after %d ms (operation: %s)",
                endpoint, timeoutMillis, operation));
        this.endpoint = endpoint;
        this.timeoutMillis = timeoutMillis;
        this.operation = operation;
        this.errorCode = "TIM-001"; ;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public String getOperation() {
        return operation;
    }

    public String getErrorCode() {
        return errorCode;
    }
}