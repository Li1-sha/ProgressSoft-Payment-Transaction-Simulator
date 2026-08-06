package com.tdd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private PaymentGateway mockGateway;          // fully controlled fake

    @Spy
    private SimplePaymentGateway spyGateway;     // real code, but we can watch it

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
}
