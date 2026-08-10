package com.progressoft.exception;

public class GatewayTimeoutException extends PaymentException {

    private final String endpoint;
    private final long timeoutMillis;
    private final String operation;

    public GatewayTimeoutException(String endpoint, long timeoutMillis, String operation) {
        super(String.format("Gateway timeout calling %s after %d ms (operation: %s)",
                        endpoint, timeoutMillis, operation),
                "TIM-001");
        this.endpoint = endpoint;
        this.timeoutMillis = timeoutMillis;
        this.operation = operation;
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
}