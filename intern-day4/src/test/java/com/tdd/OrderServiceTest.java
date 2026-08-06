package com.tdd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private PaymentGateway mockGateway;          // fully controlled fake

    @Spy
    private SimplePaymentGateway spyGateway;     // real code, but we can watch it
}
