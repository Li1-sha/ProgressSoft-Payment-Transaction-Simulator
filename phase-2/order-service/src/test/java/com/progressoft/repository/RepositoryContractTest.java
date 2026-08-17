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

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RepositoryContractTest {

    private TransactionalOrderRepository repository;
    private OrderService service;        // ✅ stored as a field
    private DataSource dataSource;

    // ---------- Provider ----------
    static Stream<Arguments> repositoryProvider() {
        return Stream.of(
                Arguments.of("InMemory", new InMemoryOrderRepository(), null),
                Arguments.of("Jdbc", createJdbcRepo(), null),
                Arguments.of("Jpa", createJpaRepo(), null)
        );
    }

    private static TransactionalOrderRepository createJpaRepo() {
        // Create a fresh EntityManagerFactory for the test
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("order-pu");
        // Store it to close later? We'll close in tearDown via a helper.
        return new JpaOrderRepository(emf);
    }

    private static TransactionalOrderRepository createJdbcRepo() {
        DataSource ds = TestDataSourceFactory.createHikariDataSource();
        return new JdbcOrderRepository(ds);
    }

    // Cleanup after each test
    @AfterEach
    void tearDown() throws Exception {
        if (dataSource instanceof AutoCloseable) {
            ((AutoCloseable) dataSource).close();
        }
    }

    // Helper to set up service and clear data
    private void init(String name, TransactionalOrderRepository repo, DataSource ds) {
        this.repository = repo;
        this.dataSource = ds;
        // Clear any existing data
        clearData(repo);
        // Build service
        PaymentGateway gateway = order -> {};
        this.service = new OrderService(
                repo,
                gateway,
                Validators.positiveAmount(),
                Validators.defaultCurrency("OMR"),
                ds // may be null
        );
    }

    private void clearData(TransactionalOrderRepository repo) {
        try {
            repo.deleteAll(repo.findAll().stream().map(Order::getId).collect(java.util.stream.Collectors.toList()));
        } catch (Exception e) {
            // ignore if not supported
        }
    }

    // ---------- Test: CRUD ----------
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

    // ---------- Test: exists / count ----------
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

    // ---------- Test: rollback / fallback ----------
    @ParameterizedTest
    @MethodSource("repositoryProvider")
    void testRollbackOnFailure(String name, TransactionalOrderRepository repo, DataSource ds) throws Exception {
        init(name, repo, ds);

        if ("Jdbc".equals(name)) {
            // Use a spy to force SQLException
            JdbcOrderRepository realRepo = (JdbcOrderRepository) repo;
            JdbcOrderRepository spyRepo = spy(realRepo);
            // Rebuild service with spy
            PaymentGateway gateway = order -> {};
            OrderService spyService = new OrderService(
                    spyRepo,
                    gateway,
                    Validators.positiveAmount(),
                    Validators.defaultCurrency("OMR"),
                    ds
            );

            doThrow(new SQLException("Forced failure"))
                    .when(spyRepo).saveWithConnection(any(Order.class), any(Connection.class));

            Order order = new Order();
            order.setCustomerName("RollbackTest");
            order.setAmount(100);
            order.setCurrency("USD");

            assertThrows(ReconciliationRequiredException.class, () -> spyService.placeOrder(order));
            // Ensure no row persisted
            assertEquals(0, spyRepo.findAll().size());

        } else if ("Jpa".equals(name)) {
            // Test that saveWithConnection throws UnsupportedOperationException
            JpaOrderRepository jpaRepo = (JpaOrderRepository) repo;
            assertThrows(UnsupportedOperationException.class, () ->
                    jpaRepo.saveWithConnection(new Order(), null));

            // Also test that placeOrder works (falls back to save)
            Order order = new Order();
            order.setCustomerName("JpaFallback");
            order.setAmount(200);
            order.setCurrency("EUR");
            Order placed = service.placeOrder(order);   // ✅ uses the field
            assertNotNull(placed.getId());

        } else {
            // InMemory: normal save works
            Order order = new Order();
            order.setCustomerName("InMemoryTest");
            order.setAmount(50);
            order.setCurrency("GBP");
            Order placed = service.placeOrder(order);
            assertNotNull(placed.getId());
        }
    }
}