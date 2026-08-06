package com.tdd;

public class SimplePaymentGateway implements PaymentGateway {
    @Override
    public boolean charge(double amount) {
        System.out.println("Charging amount: " + amount);
        return true;   // assume success
    }
}
