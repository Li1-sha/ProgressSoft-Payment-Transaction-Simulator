package com.progressoft.validation;

import com.progressoft.domain.Order;
import com.progressoft.exceptions.ValidationFailedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentValidatorCompTest {

    @Test
    void and_shouldShortCircuit_whenFirstValidatorFails() {
        // First always fails
        PaymentValidator first = order -> {
            throw new ValidationFailedException("field", "value", "first failed");
        };

        // Second – counts invocations
        int[] secondCalled = {0};
        PaymentValidator second = order -> {
            secondCalled[0]++;
            // would validate normally, but should never be called
        };

        PaymentValidator composed = first.and(second);

        Order order = new Order();
        assertThrows(ValidationFailedException.class, () -> composed.validate(order));
        assertEquals(0, secondCalled[0], "Second validator should NOT be invoked");
    }

    @Test
    void and_shouldCallBothValidators_whenFirstPasses() {
        // First passes
        PaymentValidator first = order -> { /* no exception */ };
        // Second passes
        PaymentValidator second = order -> { /* no exception */ };

        PaymentValidator composed = first.and(second);
        Order order = new Order();

        // Should not throw
        assertDoesNotThrow(() -> composed.validate(order));
    }

    @Test
    void and_shouldPropagateExceptionFromSecondValidator() {
        PaymentValidator first = order -> {};
        PaymentValidator second = order -> {
            throw new ValidationFailedException("amount", 0, "second failed");
        };

        PaymentValidator composed = first.and(second);
        Order order = new Order();

        ValidationFailedException ex =
                assertThrows(ValidationFailedException.class, () -> composed.validate(order));
        assertEquals("second failed", ex.getReason());
    }
}