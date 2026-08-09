package com.progressoft.validation;

import com.progressoft.domain.Order;
import com.progressoft.exception.PaymentValidationException;

public final class Validators {

    private Validators() {} // Utility class

    public static PaymentValidator positiveAmount() {
        return order -> {
            if (order.getAmount() <= 0) {
                throw new PaymentValidationException(
                        "Amount must be positive. Provided: " + order.getAmount()
                );
            }
        };
    }

    public static PaymentValidator maxLimit(double limit) {
        return order -> {
            if (order.getAmount() > limit) {
                throw new PaymentValidationException(
                        "Amount exceeds maximum limit of " + limit +
                                ". Provided: " + order.getAmount()
                );
            }
        };
    }

    public static PaymentValidator currencyCheck(String... allowedCurrencies) {
        return order -> {
            String currency = order.getCurrency();
            if (currency == null) {
                throw new PaymentValidationException("Currency cannot be null");
            }
            for (String allowed : allowedCurrencies) {
                if (allowed.equalsIgnoreCase(currency)) {
                    return; // Valid
                }
            }
            throw new PaymentValidationException(
                    "Currency " + currency + " is not allowed. Allowed: " +
                            String.join(", ", allowedCurrencies)
            );
        };
    }

    // An example of using OrderEnricher (second FI)
    public static OrderEnricher defaultCurrency(String defaultCurrency) {
        return order -> {
            if (order.getCurrency() == null) {
                order.setCurrency(defaultCurrency);
            }
            return order;
        };
    }

    public static OrderEnricher timestampEnricher() {
        return order -> {
            // In real life, set a timestamp.
            // For now, just return the order (we just need to show it composes).
            return order;
        };
    }
}
