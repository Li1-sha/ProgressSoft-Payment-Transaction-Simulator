package com.tdd;

public interface PaymentGateway {
    boolean charge(double amount);
}
