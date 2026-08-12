package com.progressoft.domain;

import com.progressoft.repository.Identifiable;

import java.util.Objects;

public class Order implements Identifiable<Long> {
    private Long id;
    private String customerName;
    private double amount;
    private String currency;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        if (id != null && order.id != null) {
            return id.equals(order.id);
        }
        return Double.compare(order.amount, amount) == 0 &&
                Objects.equals(customerName, order.customerName) &&
                Objects.equals(currency, order.currency);
    }

    @Override
    public int hashCode() {
        if (id != null) return id.hashCode();
        return Objects.hash(customerName, amount, currency);
    }

    @Override
    public String toString() {
        return String.format("Order{id=%s, customerName='%s', amount=%.2f, currency='%s'}",
                id, customerName, amount, currency);
    }
}