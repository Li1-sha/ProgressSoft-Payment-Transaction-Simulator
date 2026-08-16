package com.progressoft.web;

import com.progressoft.domain.Order;
import com.progressoft.service.OrderService;
import com.progressoft.validation.OrderEnricher;
import com.progressoft.validation.PaymentValidator;
import com.progressoft.validation.Validators;
import com.progressoft.payment.PaymentGateway;
import com.progressoft.repository.jdbc.JdbcOrderRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@WebServlet("/orders")
public class OrderListServlet extends HttpServlet {

    private OrderService orderService;
    private HikariDataSource dataSource;   // ✅ stored as a field

    @Override
    public void init() {
        try {
            // H2 DataSource
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:h2:~/orders;DB_CLOSE_DELAY=-1");
            config.setUsername("sa");
            config.setPassword("");
            config.setDriverClassName("org.h2.Driver");
            config.setMaximumPoolSize(10);
            dataSource = new HikariDataSource(config);   // ✅ assign to field

            // Create schema if needed
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "customer_name VARCHAR(255) NOT NULL, " +
                        "amount DECIMAL(19,4) NOT NULL, " +
                        "currency VARCHAR(10) NOT NULL)");
            }

            // Repository
            JdbcOrderRepository repository = new JdbcOrderRepository(dataSource);

            // Gateway (dummy, because web only reads)
            PaymentGateway paymentGateway = order -> {};

            // Validator & Enricher
            PaymentValidator composedValidator = Validators.positiveAmount()
                    .and(Validators.maxLimit(10000.0))
                    .and(Validators.currencyCheck("OMR", "EUR", "USD", "GBP"));
            OrderEnricher enricher = Validators.defaultCurrency("OMR")
                    .andThen(Validators.timestampEnricher());

            // Service
            orderService = new OrderService(
                    repository,
                    paymentGateway,
                    composedValidator,
                    enricher,
                    dataSource
            );

            System.out.println("OrderListServlet initialized successfully.");

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize OrderService", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain");
        try {
            List<Order> orders = orderService.getAllOrders();
            PrintWriter out = resp.getWriter();
            if (orders.isEmpty()) {
                out.println("No orders found.");
            } else {
                for (Order order : orders) {
                    out.println(order);
                }
            }
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Error retrieving orders: " + e.getMessage());
        }
    }

    @Override
    public void destroy() {
        // ✅ Now we can close the DataSource properly
        if (dataSource != null) {
            try {
                dataSource.close();
                System.out.println("OrderListServlet destroyed and DataSource closed.");
            } catch (Exception e) {
                System.err.println("Error closing DataSource: " + e.getMessage());
            }
        } else {
            System.out.println("OrderListServlet destroyed (no DataSource to close).");
        }
    }
}