package com.progressoft.validation;

import com.progressoft.domain.Order;
import com.progressoft.exception.ValidationFailedException;

public final class Validators {

    private Validators() {}

    public static PaymentValidator positiveAmount() {
        return order -> {
            if (order.getAmount() <= 0) {
                throw new ValidationFailedException(
                        "amount",
                        order.getAmount(),
                        "Amount must be positive"
                );
            }
        };
    }

    public static PaymentValidator maxLimit(double limit) {
        return order -> {
            if (order.getAmount() > limit) {
                throw new ValidationFailedException(
                        "amount",
                        order.getAmount(),
                        "Amount exceeds maximum limit of " + limit
                );
            }
        };
    }

    public static PaymentValidator currencyCheck(String... allowedCurrencies) {
        return order -> {
            String currency = order.getCurrency();
            if (currency == null) {
                throw new ValidationFailedException(
                        "currency",
                        null,
                        "Currency cannot be null"
                );
            }
            for (String allowed : allowedCurrencies) {
                if (allowed.equalsIgnoreCase(currency)) {
                    return;
                }
            }
            throw new ValidationFailedException(
                    "currency",
                    currency,
                    "Currency not allowed. Allowed: " + String.join(", ", allowedCurrencies)
            );
        };
    }

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
            // For now, just return the order
            return order;
        };
    }
}
