package com.progressoft.service;

import com.progressoft.domain.Order;
import com.progressoft.payment.PaymentGateway;
import com.progressoft.repository.OrderRepository;

public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public OrderService(OrderRepository orderRepository,
                        PaymentGateway paymentGateway) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
    }

    public Order placeOrder(Order order) {
        // 1. Business logic
        paymentGateway.charge(order);

        // abstraction not implementation
        return orderRepository.save(order);
    }

    public Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order with ID " + id + " not found"));
    }

    public boolean isOrderExist(Long id) {
        return orderRepository.existsById(id);
    }

    public long getTotalOrders() {
        return orderRepository.count();
    }
}
