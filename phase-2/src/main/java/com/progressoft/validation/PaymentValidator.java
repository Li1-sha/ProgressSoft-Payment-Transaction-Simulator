package com.progressoft.validation;

import com.progressoft.domain.Order;
import com.progressoft.exception.PaymentValidationException;
import com.progressoft.exception.ValidationFailedException;

@FunctionalInterface
public interface PaymentValidator {

    void validate(Order order) throws PaymentValidationException, ValidationFailedException;

    /**
     * Composes two validators into one.
     * Acts exactly like Predicate.and():
     * - First validates 'this'.
     * - If it passes, validates 'other'.
     * - If 'this' throws, 'other' is never invoked.
     */
    default PaymentValidator and(PaymentValidator other) {
        return order -> {
            this.validate(order); // breaks if this throws
            other.validate(order);
        };
    }
}
