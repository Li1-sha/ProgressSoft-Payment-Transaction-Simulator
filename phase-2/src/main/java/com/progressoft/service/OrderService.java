package com.progressoft.service;

import com.progressoft.domain.Order;
import com.progressoft.exception.GatewayTimeoutException;
import com.progressoft.exception.InsufficientFundsException;
import com.progressoft.exception.ValidationFailedException;
import com.progressoft.payment.PaymentGateway;
import com.progressoft.repository.OrderRepository;
import com.progressoft.validation.OrderEnricher;
import com.progressoft.validation.PaymentValidator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new RuntimeException("Order with ID " + id + " not found"));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public static class ProcessingResult {
        private final Map<Boolean, List<ProcessedOrder>> partitioned;
        private final Map<String, Double> totalsByCurrency;

        public ProcessingResult(Map<Boolean, List<ProcessedOrder>> partitioned,
                                Map<String, Double> totalsByCurrency) {
            this.partitioned = partitioned;
            this.totalsByCurrency = totalsByCurrency;
        }

        public Map<Boolean, List<ProcessedOrder>> getPartitioned() {
            return partitioned;
        }

        public Map<String, Double> getTotalsByCurrency() {
            return totalsByCurrency;
        }

        public static class ProcessedOrder {
            private final Order order;
            private final String rejectionReason; // null means approved

            public ProcessedOrder(Order order, String rejectionReason) {
                this.order = order;
                this.rejectionReason = rejectionReason;
            }

            public Order getOrder() {
                return order;
            }

            public String getRejectionReason() {
                return rejectionReason;
            }

            public boolean isApproved() {
                return rejectionReason == null;
            }

        }
    }

    public ProcessingResult processBatchWithStreams(List<Order> orders) {
        // 1. Enrich and validate, capturing any rejection reason
        List<ProcessingResult.ProcessedOrder> processed = orders.stream()
                .map(orderEnricher::enrich)                 // apply default currency & timestamp
                .map(order -> {
                    try {
                        paymentValidator.validate(order);
                        return new ProcessingResult.ProcessedOrder(order, null);
                    } catch (Exception e) {                 // catches ValidationFailedException or PaymentValidationException
                        return new ProcessingResult.ProcessedOrder(order, e.getMessage());
                    }
                })
                .collect(Collectors.toList());              // intermediate list to reuse

        // 2. Partition into approved / rejected – preserves the reason
        Map<Boolean, List<ProcessingResult.ProcessedOrder>> partitioned =
                processed.stream()
                        .collect(Collectors.partitioningBy(ProcessingResult.ProcessedOrder::isApproved));

        // 3. Group approved orders by currency and sum amounts (using summingDouble)
        Map<String, Double> totalsByCurrency = partitioned.get(true).stream()
                .map(ProcessingResult.ProcessedOrder::getOrder)
                .collect(Collectors.groupingBy(
                        Order::getCurrency,
                        Collectors.summingDouble(Order::getAmount)
                ));

        return new ProcessingResult(partitioned, totalsByCurrency);
    }
}
