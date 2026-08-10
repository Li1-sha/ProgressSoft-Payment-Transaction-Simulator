package service;

import com.progressoft.domain.Order;
import com.progressoft.repository.inmemory.InMemoryOrderRepository;
import com.progressoft.service.OrderService;
import com.progressoft.validation.Validators;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class OrderServiceBatchTest {

    private final OrderService service = // initialize as in your existing tests
            new OrderService(
                    new InMemoryOrderRepository(),
                    order -> {},   // dummy gateway (not used)
                    Validators.positiveAmount()
                            .and(Validators.maxLimit(10000))
                            .and(Validators.currencyCheck("USD", "EUR", "GBP", "OMR")),
                    Validators.defaultCurrency("OMR")
                            .andThen(Validators.timestampEnricher())
            );

    @Test   // no throws clause
    void testStreamPipeline() {
        // Prepare a list of orders with mixed validity
        Order order1 = new Order();
        order1.setAmount(100);
        order1.setCurrency("USD");
        order1.setCustomerName("Alice");

        Order order2 = new Order();
        order2.setAmount(-50);   // invalid (negative)
        order2.setCurrency("EUR");
        order2.setCustomerName("Bob");

        Order order3 = new Order();
        order3.setAmount(200);
        order3.setCurrency("USD");
        order3.setCustomerName("Charlie");

        List<Order> batch = Arrays.asList(order1, order2, order3);

        // Process
        OrderService.ProcessingResult result = service.processBatchWithStreams(batch);

        // Assertions
        assertEquals(2, result.getPartitioned().get(true).size());  // two approved
        assertEquals(1, result.getPartitioned().get(false).size()); // one rejected

        // Rejected order must have reason
        OrderService.ProcessingResult.ProcessedOrder rejected =
                result.getPartitioned().get(false).get(0);
        assertNotNull(rejected.getRejectionReason());
        assertTrue(rejected.getRejectionReason().contains("positive"));

        // Totals by currency
        Map<String, Double> totals = result.getTotalsByCurrency();
        assertEquals(300.0, totals.get("USD"), 0.001);
    }
}