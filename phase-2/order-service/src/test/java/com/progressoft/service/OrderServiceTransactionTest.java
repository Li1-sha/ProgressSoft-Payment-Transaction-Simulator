package com.progressoft.service;

import com.progressoft.domain.Order;
import com.progressoft.exceptions.ReconciliationRequiredException;
import com.progressoft.repository.TestDataSourceFactory;
import com.progressoft.repository.jdbc.JdbcOrderRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Tag("integration")
@ExtendWith(MockitoExtension.class)
class OrderServiceTransactionTest {

    @BeforeAll
    static void enableExperimentalByteBuddy() {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private DataSource realDataSource;
    private JdbcOrderRepository realRepository;
    private JdbcOrderRepository spyRepository;
    private OrderService service;

    @BeforeEach
    void setUp() throws SQLException {
        realDataSource = TestDataSourceFactory.createHikariDataSource();
        realRepository = new JdbcOrderRepository(realDataSource);

        try (Connection conn = realDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS orders");
            stmt.execute("CREATE TABLE orders (id BIGINT AUTO_INCREMENT PRIMARY KEY, customer_name VARCHAR(255), amount DECIMAL(19,4), currency VARCHAR(10))");
        }

        spyRepository = spy(realRepository);

        service = new OrderService(
                spyRepository,
                order -> {},      // PaymentGateway
                order -> {},      // PaymentValidator
                order -> order,   // OrderEnricher
                realDataSource
        );
    }

    @Test
    void rollbackOnDbFailureAfterCharge() throws Exception {
        Order order = new Order();
        order.setCustomerName("TestRollback");
        order.setAmount(100);
        order.setCurrency("USD");

        doThrow(new SQLException("Forced DB failure"))
                .when(spyRepository).saveWithConnection(any(Order.class), any(Connection.class));

        assertThrows(ReconciliationRequiredException.class, () -> service.placeOrder(order));

        assertEquals(0, realRepository.findAll().size());
    }
}