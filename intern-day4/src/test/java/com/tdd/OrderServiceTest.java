package com.tdd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private PaymentGateway mockGateway;          // fully controlled fake

    @Test
    void testPlaceOrder_gatewayApproves_shouldReturnTrue() {
        when(mockGateway.charge(anyDouble())).thenReturn(true);
        OrderService service = new OrderService(mockGateway);

        boolean result = service.placeOrder(100.0);

        assertTrue(result);
        verify(mockGateway, times(1)).charge(100.0);
    }

    @Test
    void testPlaceOrder_gatewayDeclines_shouldReturnFalse() {
        when(mockGateway.charge(anyDouble())).thenReturn(false);

        OrderService service = new OrderService(mockGateway);

        boolean result = service.placeOrder(50.0);

        assertFalse(result);
        verify(mockGateway, times(1)).charge(50.0);
    }

    @Test
    void testPlaceOrder_invalidAmount_shouldReturnFalseAndNeverCallGateway() {
        OrderService service = new OrderService(mockGateway);

        boolean result = service.placeOrder(-5.0);

        assertFalse(result);
        verify(mockGateway, never()).charge(anyDouble());
    }

    @Test
    void testPlaceOrder_withSpy_realChargeExecutes() {
        // Real gateway
        SimplePaymentGateway realGateway = new SimplePaymentGateway();

        // using a lambda – tracks if charge() was called
        boolean[] chargeCalled = { false };
        PaymentGateway spyGateway = amount -> {
            chargeCalled[0] = true;
            return realGateway.charge(amount);
        };

        OrderService service = new OrderService(spyGateway);

        boolean result = service.placeOrder(75.0);

        assertTrue(result);                            // real charge() returned true
        assertTrue(chargeCalled[0]);                   // verify that charge was called

        // Console will show: "Charging amount: 75.0" – proof of real execution.
    }
}
