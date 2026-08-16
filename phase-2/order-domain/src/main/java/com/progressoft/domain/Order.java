package com.progressoft.domain;

import com.progressoft.repository.Identifiable;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "orders")
public class Order implements Identifiable<Long>, Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money money;

    // Constructors
    public Order() {
        this.money = new Money();
    }

    public Order(String customerName, double amount, String currency) {
        this.customerName = customerName;
        this.money = new Money(amount, currency);
    }

    // Getters/Setters
    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Money getMoney() { return money; }
    public void setMoney(Money money) { this.money = money; }

    // Convenience methods
    public double getAmount() { return money != null ? money.getAmount() : 0.0; }
    public void setAmount(double amount) {
        if (money == null) money = new Money();
        money.setAmount(amount);
    }

    public String getCurrency() {
        return money != null ? money.getCurrency() : null;
    }
    public void setCurrency(String currency) {
        if (money == null) money = new Money();
        money.setCurrency(currency);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        if (id != null && order.id != null) {
            return id.equals(order.id);
        }
        if (id == null ^ order.id == null) return false;
        return Objects.equals(customerName, order.customerName) &&
                Objects.equals(money, order.money);
    }

    @Override
    public int hashCode() {
        if (id != null) return id.hashCode();
        return Objects.hash(customerName, money);
    }

    @Override
    public String toString() {
        return String.format("Order{id=%s, customerName='%s', money=%s}",
                id, customerName, money);
    }
}