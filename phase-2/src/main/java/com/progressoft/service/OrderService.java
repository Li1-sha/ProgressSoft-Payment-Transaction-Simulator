package com.progressoft.service;

import com.progressoft.domain.Order;
import com.progressoft.exception.GatewayTimeoutException;
import com.progressoft.exception.InsufficientFundsException;
import com.progressoft.exception.OrderNotFoundException;
import com.progressoft.exception.ValidationFailedException;
import com.progressoft.payment.PaymentGateway;
import com.progressoft.repository.OrderRepository;
import com.progressoft.validation.OrderEnricher;
import com.progressoft.validation.PaymentValidator;

import java.util.List;

public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentValidator paymentValidator;
    private final OrderEnricher orderEnricher;

    public OrderService(OrderRepository orderRepository,
                        PaymentGateway paymentGateway, PaymentValidator paymentValidator, OrderEnricher orderEnricher) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.paymentValidator = paymentValidator;
        this.orderEnricher = orderEnricher;
    }

    public Order placeOrder(Order order) throws ValidationFailedException, InsufficientFundsException, GatewayTimeoutException {
        // 1. Enrich the order (apply default currency, timestamp, etc.)
        Order enrichedOrder = orderEnricher.enrich(order);

        // 2. Validate the order (composed of 3 rules, but looks like one call!)
        paymentValidator.validate(enrichedOrder);

        // 3. Business logic: Charge payment
        paymentGateway.charge(enrichedOrder);

        // 4. Persist the order
        return orderRepository.save(enrichedOrder);
    }

    public Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
