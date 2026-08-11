package repository;

import com.progressoft.domain.Order;
import com.progressoft.repository.jdbc.JdbcOrderRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class JdbcOrderRepositoryIntegrationTest {

    private HikariDataSource dataSource;
    private JdbcOrderRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = TestDataSourceFactory.createHikariDataSource();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "customer_name VARCHAR(255) NOT NULL, " +
                    "amount DECIMAL(19,4) NOT NULL, " +
                    "currency VARCHAR(10) NOT NULL)");
        }
        repository = new JdbcOrderRepository(dataSource);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) dataSource.close();
    }

    @Test
    void saveAndFindById_shouldWork() {
        Order order = new Order();
        order.setCustomerName("IntegrationTest");
        order.setAmount(99.99);
        order.setCurrency("EUR");

        Order saved = repository.save(order);
        assertNotNull(saved.getId());

        Optional<Order> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("IntegrationTest", found.get().getCustomerName());
        assertEquals(99.99, found.get().getAmount(), 0.001);
        assertEquals("EUR", found.get().getCurrency());
    }

}