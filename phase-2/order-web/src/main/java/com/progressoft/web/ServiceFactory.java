package com.progressoft.web;

import com.progressoft.service.OrderService;
import com.progressoft.repository.jdbc.JdbcOrderRepository;
import com.progressoft.repository.jpa.JpaOrderRepository;
import com.progressoft.validation.*;
import com.progressoft.payment.PaymentGateway;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class ServiceFactory {
    private static OrderService instance;
    private static HikariDataSource dataSource;
    private static String repoType = "jdbc"; // default, set by RepositoryChooser

    // Called by AppInitializer after creating the DataSource
    public static synchronized void setDataSource(HikariDataSource ds) {
        dataSource = ds;
    }

    // Called by RepositoryChooser at startup
    public static synchronized void setRepoType(String type) {
        repoType = type;
    }

    public static synchronized OrderService getOrderService() {
        if (instance == null) {
            if (dataSource == null) {
                throw new IllegalStateException("DataSource not initialized. AppInitializer must run first.");
            }

            // Choose repository based on repoType
            Object repository;
            if ("jpa".equalsIgnoreCase(repoType)) {
                repository = new JpaOrderRepository(); // uses EntityManagerFactoryProvider
            } else {
                repository = new JdbcOrderRepository(dataSource);
            }

            PaymentGateway gateway = order -> { /* no‑op for API demo */ };

            PaymentValidator validator = Validators.positiveAmount()
                    .and(Validators.maxLimit(10000.0))
                    .and(Validators.currencyCheck("OMR", "EUR", "USD", "GBP"));

            OrderEnricher enricher = Validators.defaultCurrency("OMR")
                    .andThen(Validators.timestampEnricher());

            // OrderService expects TransactionalOrderRepository – both Jdbc and Jpa implement it
            instance = new OrderService(
                    (com.progressoft.repository.TransactionalOrderRepository) repository,
                    gateway,
                    validator,
                    enricher,
                    dataSource
            );
        }
        return instance;
    }

    public static synchronized void shutdown() {
        if (dataSource != null) {
            try {
                dataSource.close();
                System.out.println("ServiceFactory: DataSource closed.");
            } catch (Exception e) {
                System.err.println("Error closing DataSource: " + e.getMessage());
            }
        }
        // Also close EntityManagerFactory via its own provider
        com.progressoft.repository.jpa.EntityManagerFactoryProvider.shutdown();
        instance = null; // allow re‑initialisation if needed
    }
}