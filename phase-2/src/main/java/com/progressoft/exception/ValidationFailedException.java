package com.progressoft.exception;

public class ValidationFailedException extends PaymentException {

    private final String fieldName;
    private final Object rejectedValue;
    private final String reason;

    public ValidationFailedException(String fieldName, Object rejectedValue, String reason) {
        super("Validation failed for field '" + fieldName + "': " + reason,
                "VAL-001");
        this.fieldName = fieldName;
        this.rejectedValue = rejectedValue;
        this.reason = reason;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }

    public String getReason() {
        return reason;
    }
}