package com.progressoft.payment;

import com.progressoft.domain.Order;
import com.progressoft.exception.GatewayTimeoutException;
import com.progressoft.exception.InsufficientFundsException;

public interface PaymentGateway {
    void charge(Order order) throws InsufficientFundsException, GatewayTimeoutException;;
}