package com.progressoft.repository;

import com.progressoft.domain.Order;
import com.progressoft.exceptions.ReconciliationRequiredException;
import com.progressoft.payment.PaymentGateway;
import com.progressoft.repository.inmemory.InMemoryOrderRepository;
import com.progressoft.repository.jdbc.JdbcOrderRepository;
import com.progressoft.repository.jpa.JpaOrderRepository;
import com.progressoft.service.OrderService;
import com.progressoft.validation.Validators;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RepositoryContractTest {

    private TransactionalOrderRepository repository;
    private OrderService service;
    private DataSource dataSource;
    private EntityManagerFactory emf;

    static Stream<Arguments> repositoryProvider() {
        DataSource jdbcDs = TestDataSourceFactory.createHikariDataSource();
        JdbcOrderRepository jdbcRepo = new JdbcOrderRepository(jdbcDs);
        return Stream.of(
                Arguments.of("InMemory", new InMemoryOrderRepository(), null),
                Arguments.of("Jdbc", jdbcRepo, jdbcDs),
                Arguments.of("Jpa", null, null)
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dataSource instanceof AutoCloseable) {
            ((AutoCloseable) dataSource).close();
        }
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    private void init(String name, TransactionalOrderRepository repo, DataSource ds) {
        this.dataSource = ds;   // store for cleanup
        if ("Jpa".equals(name)) {
            emf = Persistence.createEntityManagerFactory("order-pu");
            this.repository = new JpaOrderRepository(emf);
        } else {
            this.repository = repo;
            if (ds != null) {
                createSchema(ds);
            }
        }

        // Clear existing data
        try {
            this.repository.deleteAll(this.repository.findAll().stream().map(Order::getId).collect(java.util.stream.Collectors.toList()));
        } catch (Exception ignored) {}

        PaymentGateway gateway = order -> {};
        this.service = new OrderService(
                this.repository,
                gateway,
                Validators.positiveAmount(),
                Validators.defaultCurrency("OMR"),
                ds   // DataSource may be null for InMemory / JPA
        );
    }

    private void createSchema(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "customer_name VARCHAR(255) NOT NULL, " +
                    "amount DECIMAL(19,4) NOT NULL, " +
                    "currency VARCHAR(10) NOT NULL)");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create schema", e);
        }
    }

    @ParameterizedTest
    @MethodSource("repositoryProvider")
    void testCrudRoundTrip(String name, TransactionalOrderRepository repo, DataSource ds) {
        init(name, repo, ds);

        Order order = new Order();
        order.setCustomerName("ContractTest");
        order.setAmount(50.0);
        order.setCurrency("USD");

        Order saved = repository.save(order);
        assertNotNull(saved.getId());

        Order found = repository.findById(saved.getId()).orElse(null);
        assertNotNull(found);

        found.setAmount(75.0);
        repository.save(found);
        Order updated = repository.findById(saved.getId()).orElse(null);
        assertEquals(75.0, updated.getAmount(), 0.001);

        repository.deleteById(saved.getId());
        assertFalse(repository.existsById(saved.getId()));
        assertEquals(0, repository.count());
    }

    @ParameterizedTest
    @MethodSource("repositoryProvider")
    void testExistsAndCount(String name, TransactionalOrderRepository repo, DataSource ds) {
        init(name, repo, ds);

        assertEquals(0, repository.count());

        Order o1 = new Order();
        o1.setCustomerName("A");
        o1.setAmount(10);
        o1.setCurrency("USD");
        repository.save(o1);
        assertEquals(1, repository.count());

        Order o2 = new Order();
        o2.setCustomerName("B");
        o2.setAmount(20);
        o2.setCurrency("EUR");
        Order saved2 = repository.save(o2);
        assertEquals(2, repository.count());

        assertTrue(repository.existsById(o1.getId()));
        assertTrue(repository.existsById(saved2.getId()));
        assertFalse(repository.existsById(999L));
    }

    @ParameterizedTest
    @MethodSource("repositoryProvider")
    void testRollbackOnFailure(String name, TransactionalOrderRepository repo, DataSource ds) throws Exception {
        init(name, repo, ds);

        if ("Jdbc".equals(name)) {
            // Create a fresh DataSource for the spy test to guarantee non-null
            DataSource testDs = TestDataSourceFactory.createHikariDataSource();
            JdbcOrderRepository realRepo = (JdbcOrderRepository) this.repository;
            JdbcOrderRepository spyRepo = spy(realRepo);
            PaymentGateway gateway = order -> {};
            OrderService spyService = new OrderService(
                    spyRepo,
                    gateway,
                    Validators.positiveAmount(),
                    Validators.defaultCurrency("OMR"),
                    testDs
            );

            doThrow(new SQLException("Forced failure"))
                    .when(spyRepo).saveWithConnection(any(Order.class), any(Connection.class));

            Order order = new Order();
            order.setCustomerName("RollbackTest");
            order.setAmount(100);
            order.setCurrency("USD");

            assertThrows(ReconciliationRequiredException.class, () -> spyService.placeOrder(order));
            assertEquals(0, spyRepo.findAll().size());

            // Close the test DataSource
            if (testDs instanceof AutoCloseable) {
                ((AutoCloseable) testDs).close();
            }

        } else if ("Jpa".equals(name)) {
            // ... unchanged
        } else {
            // ... unchanged
        }
    }
}