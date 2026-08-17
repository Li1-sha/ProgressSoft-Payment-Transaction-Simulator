package com.progressoft.service;

import com.progressoft.domain.Order;
import com.progressoft.exceptions.ReconciliationRequiredException;
import com.progressoft.repository.TestDataSourceFactory;
import com.progressoft.repository.TransactionalOrderRepository;

import com.progressoft.payment.PaymentGateway;
import com.progressoft.validation.Validators;
import com.progressoft.repository.inmemory.InMemoryOrderRepository;
import com.progressoft.repository.jdbc.JdbcOrderRepository;
import com.progressoft.repository.jpa.JpaOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryContractTest {

    private TransactionalOrderRepository repository;
    private OrderService service;
    private String repoName;

    // We need to be able to clean up resources after each test
    private AutoCloseable resourceCloser;

    @BeforeEach
    void setUp() {
        // Will be set by the parameterized test via custom setup
    }

    // ---------- Provider: returns Arguments for each repository ----------
    static Stream<Arguments> repositoryProvider() {
        return Stream.of(
                Arguments.of("InMemory", createInMemoryRepo(), null),
                Arguments.of("Jdbc", createJdbcRepo(), null),
                Arguments.of("Jpa", createJpaRepo(), null)
        );
    }

    private static TransactionalOrderRepository createInMemoryRepo() {
        return new InMemoryOrderRepository();
    }

    private static TransactionalOrderRepository createJdbcRepo() {
        DataSource ds = TestDataSourceFactory.createHikariDataSource();
        JdbcOrderRepository repo = new JdbcOrderRepository(ds);
        // We'll need to close the pool after test; we'll handle it via a cleaner
        return repo;
    }

    private static TransactionalOrderRepository createJpaRepo() {
        // JPA repository uses the EntityManagerFactory from the provider
        return new JpaOrderRepository();
    }

    // This method sets up the repository and service before each test
    private void init(String repoName, TransactionalOrderRepository repo) {
        this.repoName = repoName;
        this.repository = repo;
        // Build OrderService with a dummy gateway, validators, and a DataSource (for JDBC path)
        // For JPA, we pass null DataSource; OrderService will use the JPA-specific path.
        PaymentGateway dummyGateway = order -> {};
        this.service = new OrderService(
                repository,
                dummyGateway,
                Validators.positiveAmount(),
                Validators.defaultCurrency("OMR"),
                null // DataSource not needed for JPA, and for InMemory/Jdbc we'll use the repository's own connection
        );
    }

    // Clean up after each test – close pools if needed
    @AfterEach
    void tearDown() throws Exception {
        if (resourceCloser != null) {
            resourceCloser.close();
        }
    }

    // ---------- Test: CRUD round-trip ----------
    @ParameterizedTest
    @MethodSource("repositoryProvider")
    void testCrudRoundTrip(String repoName, TransactionalOrderRepository repo) {
        init(repoName, repo);

        Order order = new Order();
        order.setCustomerName("ContractTest");
        order.setAmount(50.0);
        order.setCurrency("USD");

        // Save
        Order saved = repository.save(order);
        assertNotNull(saved.getId());

        // Find
        Order found = repository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals("ContractTest", found.getCustomerName());

        // Update
        found.setAmount(75.0);
        repository.save(found);
        Order updated = repository.findById(saved.getId()).orElse(null);
        assertEquals(75.0, updated.getAmount(), 0.001);

        // Delete
        repository.deleteById(saved.getId());
        assertFalse(repository.existsById(saved.getId()));
        assertEquals(0, repository.count());
    }

    // ---------- Test: existsById / count ----------
    @ParameterizedTest
    @MethodSource("repositoryProvider")
    void testExistsAndCount(String repoName, TransactionalOrderRepository repo) {
        init(repoName, repo);

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

    // ---------- Test: forced failure rollback ----------
    @ParameterizedTest
    @MethodSource("repositoryProvider")
    void testRollbackOnFailure(String repoName, TransactionalOrderRepository repo) throws Exception {
        init(repoName, repo);

        // For Jdbc and Jpa we handle differently.
        if ("Jdbc".equals(repoName)) {
            // Use a spy to force SQLException on saveWithConnection
            JdbcOrderRepository realRepo = (JdbcOrderRepository) repo;
            JdbcOrderRepository spyRepo = spy(realRepo);
            // Rebuild service with spy
            service = new OrderService(
                    spyRepo,
                    order -> {},
                    Validators.positiveAmount(),
                    Validators.defaultCurrency("OMR"),
                    TestDataSourceFactory.createHikariDataSource()
            );

            // Force failure after charge
            doThrow(new SQLException("Forced DB failure"))
                    .when(spyRepo).saveWithConnection(any(Order.class), any(Connection.class));

            Order order = new Order();
            order.setCustomerName("RollbackTest");
            order.setAmount(100);
            order.setCurrency("USD");

            assertThrows(ReconciliationRequiredException.class, () -> service.placeOrder(order));
            // Verify no row persisted
            assertEquals(0, spyRepo.findAll().size());

        } else if ("Jpa".equals(repoName)) {
            // JPA: saveWithConnection throws UnsupportedOperationException,
            // and OrderService should fallback to plain save.
            // We test that placeOrder succeeds and persists the order.
            Order order = new Order();
            order.setCustomerName("JpaFallback");
            order.setAmount(200);
            order.setCurrency("EUR");

            Order placed = service.placeOrder(order);
            assertNotNull(placed.getId());
            // Verify it's in the database
            Order found = repository.findById(placed.getId()).orElse(null);
            assertNotNull(found);
            assertEquals("JpaFallback", found.getCustomerName());

            // Also explicitly test that saveWithConnection throws
            JpaOrderRepository jpaRepo = (JpaOrderRepository) repo;
            assertThrows(UnsupportedOperationException.class, () ->
                    jpaRepo.saveWithConnection(new Order(), null));

        } else {
            // InMemory: no transaction support; just test that it saves normally
            Order order = new Order();
            order.setCustomerName("InMemoryTest");
            order.setAmount(50);
            order.setCurrency("GBP");
            Order placed = service.placeOrder(order);
            assertNotNull(placed.getId());
        }
    }
}