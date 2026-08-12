package com.progressoft.service;

import com.progressoft.domain.Order;
import com.progressoft.exceptions.*;
import com.progressoft.payment.PaymentGateway;
import com.progressoft.repository.OrderRepository;
import com.progressoft.repository.jdbc.JdbcOrderRepository;
import com.progressoft.validation.OrderEnricher;
import com.progressoft.validation.PaymentValidator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;

public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentValidator paymentValidator;
    private final OrderEnricher orderEnricher;
    private final ExecutorService executor;
    private final DataSource dataSource;

    // Constructor with custom pool size
    public OrderService(OrderRepository orderRepository,
                        PaymentGateway paymentGateway,
                        PaymentValidator paymentValidator,
                        OrderEnricher orderEnricher,
                        int poolSize,
                        DataSource dataSource) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.paymentValidator = paymentValidator;
        this.orderEnricher = orderEnricher;
        this.executor = Executors.newFixedThreadPool(poolSize);
        this.dataSource = dataSource;
    }

    public OrderService(OrderRepository orderRepository,
                        PaymentGateway paymentGateway,
                        PaymentValidator paymentValidator,
                        OrderEnricher orderEnricher) {
        this(orderRepository, paymentGateway, paymentValidator, orderEnricher,
                Runtime.getRuntime().availableProcessors(),
                null);
    }
    public OrderService(OrderRepository orderRepository,
                        PaymentGateway paymentGateway,
                        PaymentValidator paymentValidator,
                        OrderEnricher orderEnricher,
                        DataSource dataSource) {
        this(orderRepository, paymentGateway, paymentValidator, orderEnricher,
                Runtime.getRuntime().availableProcessors(), dataSource);
    }

    public Order placeOrder(Order order) throws ValidationFailedException,
            InsufficientFundsException, GatewayTimeoutException,
            ReconciliationRequiredException {
        Order enriched = orderEnricher.enrich(order);
        paymentValidator.validate(enriched);
        paymentGateway.charge(enriched);

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            Order saved = ((JdbcOrderRepository) orderRepository).saveWithConnection(enriched, conn);
            conn.commit();
            return saved;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            throw new ReconciliationRequiredException(
                    "Order charged but DB write failed", enriched, e
            );
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }
    }
        public Order findOrder (Long id){
            return orderRepository.findById(id)
                    .orElseThrow(() -> new OrderNotFoundException(id));
        }

        public List<Order> getAllOrders () {
            return orderRepository.findAll();
        }

        // Stream Pipeline
        public ProcessingResult processBatchWithStreams (List < Order > orders) {
            List<ProcessingResult.ProcessedOrder> processed = orders.stream()
                    .map(orderEnricher::enrich)
                    .map(order -> {
                        try {
                            paymentValidator.validate(order);
                            return new ProcessingResult.ProcessedOrder(order, null);
                        } catch (Exception e) {
                            return new ProcessingResult.ProcessedOrder(order, e.getMessage());
                        }
                    })
                    .collect(Collectors.toList());

            Map<Boolean, List<ProcessingResult.ProcessedOrder>> partitioned =
                    processed.stream()
                            .collect(Collectors.partitioningBy(ProcessingResult.ProcessedOrder::isApproved));

            Map<String, Double> totalsByCurrency = partitioned.get(true).stream()
                    .map(ProcessingResult.ProcessedOrder::getOrder)
                    .collect(Collectors.groupingBy(
                            Order::getCurrency,
                            Collectors.summingDouble(Order::getAmount)
                    ));

            return new ProcessingResult(partitioned, totalsByCurrency);
        }

        // Concurrent Processing
        public ProcessingResult processBatchConcurrently (List < Order > orders)
            throws InterruptedException, ExecutionException {

            AtomicLong processedCount = new AtomicLong(0);
            ConcurrentHashMap<String, DoubleAdder> currencyTotals = new ConcurrentHashMap<>();

            List<CompletableFuture<ProcessingResult.ProcessedOrder>> futures = orders.stream()
                    .map(order -> CompletableFuture.supplyAsync(() -> {
                        Order enriched = orderEnricher.enrich(order);
                        try {
                            paymentValidator.validate(enriched);
                            processedCount.incrementAndGet();
                            currencyTotals.computeIfAbsent(enriched.getCurrency(),
                                            k -> new DoubleAdder())
                                    .add(enriched.getAmount());
                            return new ProcessingResult.ProcessedOrder(enriched, null);
                        } catch (Exception e) {
                            return new ProcessingResult.ProcessedOrder(enriched, e.getMessage());
                        }
                    }, executor))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

            List<ProcessingResult.ProcessedOrder> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            Map<Boolean, List<ProcessingResult.ProcessedOrder>> partitioned =
                    results.stream()
                            .collect(Collectors.partitioningBy(ProcessingResult.ProcessedOrder::isApproved));

            Map<String, Double> totals = new HashMap<>();
            currencyTotals.forEach((currency, adder) -> totals.put(currency, adder.sum()));

            return new ProcessingResult(partitioned, totals);
        }

        // Shutdown
        public void shutdownExecutor () {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Result
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
                private final String rejectionReason;

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
    }
