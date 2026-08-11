package service;

import com.progressoft.domain.Order;
import com.progressoft.exception.ReconciliationRequiredException;
import com.progressoft.repository.jdbc.JdbcOrderRepository;
import com.progressoft.service.OrderService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.TestDataSourceFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("integration")
@ExtendWith(MockitoExtension.class)
class OrderServiceTransactionTest {

    // 🔧 Enable Byte Buddy experimental mode for Java 25
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

        // Create table
        try (Connection conn = realDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE orders (id BIGINT AUTO_INCREMENT PRIMARY KEY, customer_name VARCHAR(255), amount DECIMAL(19,4), currency VARCHAR(10))");
        }

        // Spy on the real repository
        spyRepository = spy(realRepository);

        service = new OrderService(
                spyRepository,
                order -> {}, // gateway always succeeds
                order -> {}, // validator always passes
                order -> order, // no enrichment
                realDataSource
        );
    }

    @Test
    void rollbackOnDbFailureAfterCharge() throws Exception {
        Order order = new Order();
        order.setCustomerName("TestRollback");
        order.setAmount(100);
        order.setCurrency("USD");

        // Force SQLException on save
        doThrow(new SQLException("Forced DB failure"))
                .when(spyRepository).saveWithConnection(any(Order.class), any(Connection.class));

        assertThrows(ReconciliationRequiredException.class, () -> service.placeOrder(order));

        // Verify no rows persisted
        assertEquals(0, realRepository.findAll().size());
    }
}