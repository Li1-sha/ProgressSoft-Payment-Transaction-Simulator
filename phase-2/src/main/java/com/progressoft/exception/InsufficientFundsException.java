package com.progressoft.exception;

public class InsufficientFundsException extends PaymentException {

    private final double requiredAmount;
    private final double availableAmount;

    public InsufficientFundsException(double requiredAmount, double availableAmount) {
        super(String.format("Insufficient funds: required %.2f, available %.2f",
                        requiredAmount, availableAmount),
                "INS-001");
        this.requiredAmount = requiredAmount;
        this.availableAmount = availableAmount;
    }

    public double getRequiredAmount() {
        return requiredAmount;
    }

    public double getAvailableAmount() {
        return availableAmount;
    }
}