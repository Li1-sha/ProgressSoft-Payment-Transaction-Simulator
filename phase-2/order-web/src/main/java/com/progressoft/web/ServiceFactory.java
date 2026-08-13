package com.progressoft.web;

import com.progressoft.service.OrderService;
import com.progressoft.repository.jdbc.JdbcOrderRepository;
import com.progressoft.validation.*;
import com.progressoft.payment.PaymentGateway;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class ServiceFactory {
    private static OrderService instance;

    public static synchronized OrderService getOrderService() {
        if (instance == null) {
            try {
                DataSource dataSource = createDataSource();
                createSchema(dataSource);

                JdbcOrderRepository repository = new JdbcOrderRepository(dataSource);

                // Dummy gateway for the API (you can replace with real one)
                PaymentGateway gateway = order -> {
                    // For demo, we don't actually charge; but you could add logic.
                };

                PaymentValidator validator = Validators.positiveAmount()
                        .and(Validators.maxLimit(10000.0))
                        .and(Validators.currencyCheck("OMR", "EUR", "USD", "GBP"));

                OrderEnricher enricher = Validators.defaultCurrency("OMR")
                        .andThen(Validators.timestampEnricher());

                instance = new OrderService(repository, gateway, validator, enricher, dataSource);

            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize OrderService", e);
            }
        }
        return instance;
    }

    private static DataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:~/orders;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }

    private static void createSchema(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "customer_name VARCHAR(255) NOT NULL, " +
                    "amount DECIMAL(19,4) NOT NULL, " +
                    "currency VARCHAR(10) NOT NULL)");
        } catch (SQLException e) {
            // ignore if table exists
        }
    }
}