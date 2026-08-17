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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.persistence.EntityManager;
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
    private EntityManagerFactory emf; // for JPA only
    private String repoName;

    static Stream<Arguments> repositoryProvider() {
        return Stream.of(
                Arguments.of("InMemory"),
                Arguments.of("Jdbc"),
                Arguments.of("Jpa")
        );
    }

    @BeforeEach
    void setUp(String name) {
        this.repoName = name;
        switch (name) {
            case "InMemory":
                repository = new InMemoryOrderRepository();
                dataSource = null;
                emf = null;
                break;
            case "Jdbc":
                dataSource = TestDataSourceFactory.createHikariDataSource();
                repository = new JdbcOrderRepository(dataSource);
                // Clear table
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM orders");
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to clear table", e);
                }
                break;
            case "Jpa":
                emf = Persistence.createEntityManagerFactory("order-pu");
                repository = new JpaOrderRepository(emf);
                // Clear table (or rely on create-drop)
                EntityManager em = emf.createEntityManager();
                em.getTransaction().begin();
                em.createQuery("DELETE FROM Order").executeUpdate();
                em.getTransaction().commit();
                em.close();
                break;
        }
        // Build service
        PaymentGateway gateway = order -> {};
        this.service = new OrderService(
                repository,
                gateway,
                Validators.positiveAmount(),
                Validators.defaultCurrency("OMR"),
                dataSource // may be null for InMemory and JPA
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

    // ---------- Tests ----------
    @ParameterizedTest
    @MethodSource("repositoryProvider")
    void testCrudRoundTrip(String name) throws Exception {
        setUp(name);
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
        tearDown();
    }

    @ParameterizedTest
    @MethodSource("repositoryProvider")
    void testExistsAndCount(String name) throws Exception {
        setUp(name);
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
        tearDown();
    }

    @ParameterizedTest
    @MethodSource("repositoryProvider")
    void testRollbackOnFailure(String name) throws Exception {
        setUp(name);
        if ("Jdbc".equals(name)) {
            JdbcOrderRepository realRepo = (JdbcOrderRepository) repository;
            JdbcOrderRepository spyRepo = spy(realRepo);
            // Rebuild service with spy
            PaymentGateway gateway = order -> {};
            OrderService spyService = new OrderService(
                    spyRepo,
                    gateway,
                    Validators.positiveAmount(),
                    Validators.defaultCurrency("OMR"),
                    dataSource
            );

            doThrow(new SQLException("Forced failure"))
                    .when(spyRepo).saveWithConnection(any(Order.class), any(Connection.class));

            Order order = new Order();
            order.setCustomerName("RollbackTest");
            order.setAmount(100);
            order.setCurrency("USD");

            assertThrows(ReconciliationRequiredException.class, () -> spyService.placeOrder(order));
            assertEquals(0, spyRepo.findAll().size());

        } else if ("Jpa".equals(name)) {
            JpaOrderRepository jpaRepo = (JpaOrderRepository) repository;
            assertThrows(UnsupportedOperationException.class, () ->
                    jpaRepo.saveWithConnection(new Order(), null));

            Order order = new Order();
            order.setCustomerName("JpaFallback");
            order.setAmount(200);
            order.setCurrency("EUR");
            Order placed = service.placeOrder(order);
            assertNotNull(placed.getId());

        } else {
            Order order = new Order();
            order.setCustomerName("InMemoryTest");
            order.setAmount(50);
            order.setCurrency("GBP");
            Order placed = service.placeOrder(order);
            assertNotNull(placed.getId());
        }
        tearDown();
    }
}