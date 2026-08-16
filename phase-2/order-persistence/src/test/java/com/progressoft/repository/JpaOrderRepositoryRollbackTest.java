package com.progressoft.repository;

import com.progressoft.domain.Order;
import com.progressoft.exceptions.ReconciliationRequiredException;
import com.progressoft.service.OrderService;
import com.progressoft.validation.Validators;
import com.progressoft.repository.jpa.JpaOrderRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JpaOrderRepositoryRollbackTest {

    @Test
    void rollbackOnDbFailureAfterCharge() throws Exception {
        // 1. Create a JpaOrderRepository with a spy that throws on save
        JpaOrderRepository realRepo = new JpaOrderRepository();
        JpaOrderRepository spyRepo = org.mockito.Mockito.spy(realRepo);

        // Force save to throw (simulate DB failure)
        org.mockito.Mockito.doThrow(new RuntimeException("Forced DB failure"))
                .when(spyRepo).save(org.mockito.ArgumentMatchers.any(Order.class));

        // 2. Build OrderService with the spy repository
        OrderService service = new OrderService(
                spyRepo,
                order -> {}, // dummy gateway
                Validators.positiveAmount(),
                Validators.defaultCurrency("OMR"),
                null // we don't need DataSource for this test (JPA uses its own)
        );

        // 3. Place an order – should throw ReconciliationRequiredException
        Order order = new Order();
        order.setCustomerName("RollbackTest");
        order.setAmount(100);
        order.setCurrency("USD");

        assertThrows(ReconciliationRequiredException.class, () -> service.placeOrder(order));

        // 4. Verify no row was persisted
        assertEquals(0, spyRepo.findAll().size());
    }
}