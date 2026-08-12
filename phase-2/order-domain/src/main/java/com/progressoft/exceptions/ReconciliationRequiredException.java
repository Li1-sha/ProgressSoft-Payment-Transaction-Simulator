package com.progressoft.exceptions;

import com.progressoft.domain.Order;

public class ReconciliationRequiredException extends Exception {
    private final Order order;

    public ReconciliationRequiredException(String message, Order order) {
        super(message);
        this.order = order;
    }
    public ReconciliationRequiredException(String message, Order order, Throwable cause) {
        super(message, cause);
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}