package com.progressoft.validation;

import com.progressoft.domain.Order;

@FunctionalInterface
public interface OrderEnricher {
    Order enrich(Order order);

    default OrderEnricher andThen(OrderEnricher after) {
        return order -> after.enrich(this.enrich(order));
    }
}