package com.progressoft.payment;

import com.progressoft.domain.Order;
import com.progressoft.exceptions.InsufficientFundsException;

public interface PaymentGateway {
    void charge(Order order) throws InsufficientFundsException;
}