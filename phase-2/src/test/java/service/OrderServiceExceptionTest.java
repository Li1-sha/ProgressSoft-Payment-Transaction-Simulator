package service;

import com.progressoft.domain.Order;
import com.progressoft.exception.GatewayTimeoutException;
import com.progressoft.exception.InsufficientFundsException;
import com.progressoft.exception.ValidationFailedException;
import com.progressoft.payment.PaymentGateway;
import com.progressoft.repository.OrderRepository;
import com.progressoft.repository.inmemory.InMemoryOrderRepository;
import com.progressoft.service.OrderService;
import com.progressoft.validation.OrderEnricher;
import com.progressoft.validation.PaymentValidator;
import com.progressoft.validation.Validators;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OrderServiceExceptionTest {

    private final OrderRepository repository = new InMemoryOrderRepository();
    private final PaymentValidator validator = Validators.positiveAmount()
            .and(Validators.maxLimit(1000))
            .and(Validators.currencyCheck("USD"));

    private final OrderEnricher enricher = Validators.defaultCurrency("USD");

    @Test
    void placeOrder_throwsValidationFailed_whenAmountNegative() {
        PaymentGateway gateway = order -> {};
        OrderService service = new OrderService(repository, gateway, validator, enricher);

        Order order = new Order();
        order.setCustomerName("John");
        order.setAmount(-10.0);
        order.setCurrency("USD");

        assertThrows(ValidationFailedException.class, () -> service.placeOrder(order));
    }

    @Test
    void placeOrder_throwsInsufficientFunds_whenGatewayFailsWithThat() throws Exception {
        PaymentGateway failingGateway = order -> {
            throw new InsufficientFundsException(100.0, 20.0);
        };
        OrderService service = new OrderService(repository, failingGateway, validator, enricher);

        Order order = new Order();
        order.setCustomerName("Jane");
        order.setAmount(50.0);
        order.setCurrency("USD");

        InsufficientFundsException ex =
                assertThrows(InsufficientFundsException.class, () -> service.placeOrder(order));
        assertEquals(50.0, ex.getRequiredAmount()); // The required amount from the order
        assertEquals(20.0, ex.getAvailableAmount());
    }

    @Test
    void placeOrder_throwsGatewayTimeout_whenGatewayTimesOut() {
        PaymentGateway timeoutGateway = order -> {
            throw new GatewayTimeoutException("api.payments.com", 3000L, "auth");
        };
        OrderService service = new OrderService(repository, timeoutGateway, validator, enricher);

        Order order = new Order();
        order.setCustomerName("Bob");
        order.setAmount(200.0);
        order.setCurrency("USD");

        GatewayTimeoutException ex =
                assertThrows(GatewayTimeoutException.class, () -> service.placeOrder(order));
        assertEquals("api.payments.com", ex.getEndpoint());
        assertEquals(3000L, ex.getTimeoutMillis());
    }
}