package exception;

import com.progressoft.exception.GatewayTimeoutException;
import com.progressoft.exception.InsufficientFundsException;
import com.progressoft.exception.ValidationFailedException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExceptionDataTest {

    @Test
    void validationExceptionCarriesStructuredData() {
        ValidationFailedException ex =
                new ValidationFailedException("amount", -5.0, "Must be positive");

        assertEquals("amount", ex.getFieldName());
        assertEquals(-5.0, ex.getRejectedValue());
        assertEquals("Must be positive", ex.getReason());
        assertEquals("VAL-001", ex.getErrorCode());
    }

    @Test
    void insufficientFundsCarriesAmounts() {
        InsufficientFundsException ex =
                new InsufficientFundsException(100.00, 45.50);

        assertEquals(100.00, ex.getRequiredAmount());
        assertEquals(45.50, ex.getAvailableAmount());
        assertTrue(ex.getMessage().contains("100.00"));
        assertTrue(ex.getMessage().contains("45.50"));
    }

    @Test
    void gatewayTimeoutCarriesEndpointAndTimeout() {
        GatewayTimeoutException ex =
                new GatewayTimeoutException("https://payments.progressoft.com/charge", 5000L, "charge");

        assertEquals("https://payments.progressoft.com/charge", ex.getEndpoint());
        assertEquals(5000L, ex.getTimeoutMillis());
        assertEquals("charge", ex.getOperation());
    }
}