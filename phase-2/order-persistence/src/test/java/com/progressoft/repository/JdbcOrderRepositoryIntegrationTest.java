package com.progressoft.repository;

import com.progressoft.domain.Order;
import com.progressoft.repository.jdbc.JdbcOrderRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
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
            stmt.execute("DROP TABLE IF EXISTS orders");
            stmt.execute("CREATE TABLE orders (" +
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

    private Order createOrder(String name, double amount, String currency) {
        Order order = new Order();
        order.setCustomerName(name);
        order.setAmount(amount);
        order.setCurrency(currency);
        return order;
    }

    @Test
    void saveAndFindById_shouldWork() {
        Order order = createOrder("IntegrationTest", 99.99, "EUR");
        Order saved = repository.save(order);
        assertNotNull(saved.getId());

        Optional<Order> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("IntegrationTest", found.get().getCustomerName());
        assertEquals(99.99, found.get().getAmount(), 0.001);
        assertEquals("EUR", found.get().getCurrency());
    }

    @Test
    void updateOrder_shouldWork() {
        Order order = createOrder("Initial", 100.00, "USD");
        Order saved = repository.save(order);

        saved.setCustomerName("Updated");
        saved.setAmount(150.00);
        repository.save(saved);

        Optional<Order> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Updated", found.get().getCustomerName());
        assertEquals(150.00, found.get().getAmount(), 0.001);
        assertEquals("USD", found.get().getCurrency());
    }

    @Test
    void findAll_shouldReturnAllOrders() {
        repository.save(createOrder("A", 10, "USD"));
        repository.save(createOrder("B", 20, "EUR"));
        repository.save(createOrder("C", 30, "GBP"));

        List<Order> all = repository.findAll();
        assertEquals(3, all.size());
        assertTrue(all.stream().anyMatch(o -> "A".equals(o.getCustomerName())));
        assertTrue(all.stream().anyMatch(o -> "B".equals(o.getCustomerName())));
        assertTrue(all.stream().anyMatch(o -> "C".equals(o.getCustomerName())));
    }

    @Test
    void deleteById_shouldRemoveOrder() {
        Order saved = repository.save(createOrder("ToDelete", 100, "USD"));
        assertTrue(repository.existsById(saved.getId()));

        repository.deleteById(saved.getId());
        assertFalse(repository.existsById(saved.getId()));
        assertEquals(0, repository.count());
    }

    @Test
    void deleteAll_shouldRemoveMultipleOrders() {
        Order o1 = repository.save(createOrder("A", 10, "USD"));
        Order o2 = repository.save(createOrder("B", 20, "EUR"));

        repository.deleteAll(Arrays.asList(o1.getId(), o2.getId()));
        assertEquals(0, repository.count());
        assertFalse(repository.existsById(o1.getId()));
        assertFalse(repository.existsById(o2.getId()));
    }

    @Test
    void existsById_shouldReturnTrueWhenExists() {
        Order saved = repository.save(createOrder("Exists", 50, "GBP"));
        assertTrue(repository.existsById(saved.getId()));
        assertFalse(repository.existsById(999L));
    }

    @Test
    void count_shouldReturnCorrectNumber() {
        assertEquals(0, repository.count());
        repository.save(createOrder("A", 10, "USD"));
        assertEquals(1, repository.count());
        repository.save(createOrder("B", 20, "EUR"));
        assertEquals(2, repository.count());
    }
}