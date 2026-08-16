package com.progressoft.service;

import com.progressoft.domain.Order;
import com.progressoft.exceptions.ReconciliationRequiredException;
import com.progressoft.payment.PaymentGateway;
import com.progressoft.repository.jpa.JpaOrderRepository;
import com.progressoft.validation.Validators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import static org.junit.jupiter.api.Assertions.*;

class JpaOrderRepositoryRollbackTest {

    private EntityManagerFactory emf;
    private JpaOrderRepository realRepo;
    private JpaOrderRepository spyRepo;

    @BeforeEach
    void setUp() {
        // 1. Create a fresh EntityManagerFactory for each test
        emf = Persistence.createEntityManagerFactory("order-pu");
        realRepo = new JpaOrderRepository(emf);
        spyRepo = Mockito.spy(realRepo);

        // 2. Clear the table to ensure a clean state
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Order").executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    @Test
    void rollbackOnDbFailureAfterCharge() throws Exception {
        // 1. Force saveWithEntityManager to throw (simulate DB failure)
        Mockito.doThrow(new RuntimeException("Forced DB failure"))
                .when(spyRepo).saveWithEntityManager(Mockito.any(Order.class), Mockito.any(EntityManager.class));

        // 2. Build OrderService with the spy repository
        PaymentGateway dummyGateway = order -> {};
        OrderService service = new OrderService(
                spyRepo,
                dummyGateway,
                Validators.positiveAmount(),
                Validators.defaultCurrency("OMR"),
                null // DataSource not needed – JPA uses its own
        );

        // 3. Place an order – should throw ReconciliationRequiredException
        Order order = new Order();
        order.setCustomerName("RollbackTest");
        order.setAmount(100);
        order.setCurrency("USD");

        assertThrows(ReconciliationRequiredException.class, () -> service.placeOrder(order));

        // 4. Verify no row was persisted
        assertEquals(0, spyRepo.findAll().size());

        // 5. Clean up
        emf.close();
    }
}