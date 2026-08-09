package com.progressoft.payment;

import com.progressoft.domain.Order;

public interface PaymentGateway {
    void charge(Order order);
}