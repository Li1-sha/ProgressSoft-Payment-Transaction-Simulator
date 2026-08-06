package com.tdd;

public class OrderService {
    private final PaymentGateway gateway;

    public OrderService(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public boolean placeOrder(double amount) {
        if (amount <= 0) {
            return false;          // invalid order, never touch gateway
        }
        return gateway.charge(amount);
    }
}
