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
        // create table
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE orders (id BIGINT AUTO_INCREMENT PRIMARY KEY, customer_name VARCHAR(255), amount DECIMAL(19,4), currency VARCHAR(10))");
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
    }

    // Add more tests: update, delete, count, etc.
}