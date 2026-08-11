package com.progressoft.service;

import com.progressoft.domain.Order;
import com.progressoft.exception.ReconciliationRequiredException;
import com.progressoft.repository.jdbc.JdbcOrderRepository;
import com.progressoft.testutil.TestDataSourceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("integration")
@ExtendWith(MockitoExtension.class)
class OrderServiceTransactionTest {

    private DataSource realDataSource;
    private JdbcOrderRepository realRepository;
    private JdbcOrderRepository spyRepository;

    private OrderService service;

    @BeforeEach
    void setUp() throws SQLException {
        // 1. Real H2 DataSource (for actual DB operations)
        realDataSource = TestDataSourceFactory.createHikariDataSource();
        realRepository = new JdbcOrderRepository(realDataSource);

        // 2. Create table (using real repository)
        try (Connection conn = realDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE orders (id BIGINT AUTO_INCREMENT PRIMARY KEY, customer_name VARCHAR(255), amount DECIMAL(19,4), currency VARCHAR(10))");
        }

        // 3. Spy on the repository to intercept saveWithConnection
        spyRepository = spy(realRepository);

        // 4. Build OrderService with the spy repository
        service = new OrderService(
                spyRepository,
                order -> {}, // gateway always succeeds
                order -> {}, // validator always passes
                order -> order, // no enrichment
                realDataSource // real DataSource, not failing
        );
    }

    @Test
    void rollbackOnDbFailureAfterCharge() throws Exception {
        // Given: an order
        Order order = new Order();
        order.setCustomerName("TestRollback");
        order.setAmount(100);
        order.setCurrency("USD");

        // When: saveWithConnection throws SQLException (simulate DB failure after charge)
        doThrow(new SQLException("Forced DB failure"))
                .when(spyRepository).saveWithConnection(any(Order.class), any(Connection.class));

        // Then: placeOrder must throw ReconciliationRequiredException
        assertThrows(ReconciliationRequiredException.class, () -> service.placeOrder(order));

        // And: no row should be persisted (rollback happened)
        assertEquals(0, realRepository.findAll().size());
    }
}