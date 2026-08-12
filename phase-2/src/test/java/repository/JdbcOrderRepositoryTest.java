package repository;

import com.progressoft.domain.Order;
import com.progressoft.repository.jdbc.JdbcOrderRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JdbcOrderRepositoryTest {

    private HikariDataSource dataSource;
    private JdbcOrderRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = TestDataSourceFactory.createHikariDataSource();

        // Clean and create table
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

    @Test
    void save_shouldInsertAndGenerateId() {
        Order order = new Order();
        order.setCustomerName("Alice");
        order.setAmount(100.50);
        order.setCurrency("USD");

        Order saved = repository.save(order);
        assertNotNull(saved.getId());
        assertEquals("Alice", saved.getCustomerName());
        assertEquals(100.50, saved.getAmount(), 0.001);
        assertEquals("USD", saved.getCurrency());

        Optional<Order> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void save_shouldUpdateExistingOrder() {
        Order order = new Order();
        order.setCustomerName("Bob");
        order.setAmount(200.00);
        order.setCurrency("EUR");
        Order saved = repository.save(order);

        saved.setCustomerName("Robert");
        saved.setAmount(250.00);
        repository.save(saved);

        Optional<Order> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Robert", found.get().getCustomerName());
        assertEquals(250.00, found.get().getAmount(), 0.001);
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        Optional<Order> found = repository.findById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    void findAll_shouldReturnAllOrders() {
        Order o1 = new Order(); o1.setCustomerName("A"); o1.setAmount(10); o1.setCurrency("USD");
        Order o2 = new Order(); o2.setCustomerName("B"); o2.setAmount(20); o2.setCurrency("EUR");
        repository.save(o1);
        repository.save(o2);

        List<Order> all = repository.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void deleteById_shouldRemoveOrder() {
        Order order = new Order();
        order.setCustomerName("ToDelete");
        order.setAmount(100);
        order.setCurrency("USD");
        Order saved = repository.save(order);

        assertTrue(repository.existsById(saved.getId()));
        repository.deleteById(saved.getId());
        assertFalse(repository.existsById(saved.getId()));
        assertEquals(0, repository.count());
    }

    @Test
    void deleteAll_shouldRemoveMultipleOrders() {
        Order o1 = new Order(); o1.setCustomerName("A"); o1.setAmount(10); o1.setCurrency("USD");
        Order o2 = new Order(); o2.setCustomerName("B"); o2.setAmount(20); o2.setCurrency("EUR");
        Order saved1 = repository.save(o1);
        Order saved2 = repository.save(o2);

        repository.deleteAll(Arrays.asList(saved1.getId(), saved2.getId()));
        assertEquals(0, repository.count());
        assertFalse(repository.existsById(saved1.getId()));
        assertFalse(repository.existsById(saved2.getId()));
    }

    @Test
    void existsById_shouldReturnTrueWhenExists() {
        Order order = new Order();
        order.setCustomerName("Exists");
        order.setAmount(50);
        order.setCurrency("GBP");
        Order saved = repository.save(order);

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

    private Order createOrder(String name, double amount, String currency) {
        Order o = new Order();
        o.setCustomerName(name);
        o.setAmount(amount);
        o.setCurrency(currency);
        return o;
    }
}