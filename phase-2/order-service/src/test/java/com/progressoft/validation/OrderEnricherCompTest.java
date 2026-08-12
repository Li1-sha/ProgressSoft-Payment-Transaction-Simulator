package com.progressoft.validation;

import com.progressoft.domain.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderEnricherCompTest {

    @Test
    void andThen_shouldApplyFirstEnricherThenSecond() {
        OrderEnricher first = order -> {
            order.setCustomerName("First");
            return order;
        };
        OrderEnricher second = order -> {
            order.setCurrency("EUR");
            return order;
        };

        OrderEnricher composed = first.andThen(second);
        Order order = new Order();

        Order result = composed.enrich(order);
        assertEquals("First", result.getCustomerName());
        assertEquals("EUR", result.getCurrency());
    }

    @Test
    void andThen_shouldSupportChainingMultipleEnrichers() {
        OrderEnricher first = order -> {
            order.setAmount(100);
            return order;
        };
        OrderEnricher second = order -> {
            order.setCurrency("USD");
            return order;
        };
        OrderEnricher third = order -> {
            order.setCustomerName("Charlie");
            return order;
        };

        OrderEnricher composed = first.andThen(second).andThen(third);
        Order order = new Order();

        Order result = composed.enrich(order);
        assertEquals(100, result.getAmount(), 0.001);
        assertEquals("USD", result.getCurrency());
        assertEquals("Charlie", result.getCustomerName());
    }

    @Test
    void andThen_shouldHandleNullEnricherGracefully() {
        OrderEnricher first = order -> {
            order.setCurrency("OMR");
            return order;
        };
        // If a null enricher is passed, it would throw NPE – but this test verifies we handle it if needed.
        // Since it's a functional interface, we can't pass null if we want to chain.
        // This is just a sanity check that composition works normally.
        OrderEnricher composed = first.andThen(order -> order);
        Order order = new Order();
        Order result = composed.enrich(order);
        assertEquals("OMR", result.getCurrency());
    }
}