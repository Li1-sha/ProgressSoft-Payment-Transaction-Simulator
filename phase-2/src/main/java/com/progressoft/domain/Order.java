package com.progressoft.domain;

import com.progressoft.repository.Identifiable;

public class Order implements Identifiable<Long> {
    private Long id;
    private String customerName;
    private double amount;

    // --- Identifiable Contract ---
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    // --- Regular Getters and Setters ---
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}