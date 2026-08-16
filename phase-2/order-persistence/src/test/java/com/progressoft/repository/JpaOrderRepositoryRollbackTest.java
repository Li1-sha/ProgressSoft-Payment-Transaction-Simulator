package com.progressoft.repository;

import com.progressoft.domain.Order;
import com.progressoft.exceptions.ReconciliationRequiredException;
import com.progressoft.repository.jpa.JpaOrderRepository;
import com.progressoft.service.OrderService;
import com.progressoft.payment.PaymentGateway;
import com.progressoft.validation.Validators;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import static org.junit.jupiter.api.Assertions.*;

class JpaOrderRepositoryRollbackTest {

    @Test
    void rollbackOnDbFailureAfterCharge() throws Exception {
        // Use a real JPA repository with a mock/spy
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("order-pu");
        JpaOrderRepository realRepo = new JpaOrderRepository(emf);
        JpaOrderRepository spyRepo = org.mockito.Mockito.spy(realRepo);

        // Force saveWithEntityManager to throw
        org.mockito.Mockito.doThrow(new RuntimeException("Forced DB failure"))
                .when(spyRepo).saveWithEntityManager(
                        org.mockito.ArgumentMatchers.any(Order.class),
                        org.mockito.ArgumentMatchers.any(EntityManager.class)
                );

        OrderService service = new OrderService(
                spyRepo,
                order -> {},
                Validators.positiveAmount(),
                Validators.defaultCurrency("OMR"),
                null // DataSource not needed for JPA test
        );

        Order order = new Order();
        order.setCustomerName("RollbackTest");
        order.setAmount(100);
        order.setCurrency("USD");

        assertThrows(ReconciliationRequiredException.class, () -> service.placeOrder(order));

        // Verify no rows were persisted
        assertEquals(0, spyRepo.findAll().size());

        emf.close();
    }
}