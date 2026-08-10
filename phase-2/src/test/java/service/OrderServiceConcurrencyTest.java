package service;

import com.progressoft.domain.Order;
import com.progressoft.repository.inmemory.InMemoryOrderRepository;
import com.progressoft.service.OrderService;
import com.progressoft.validation.Validators;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceConcurrencyTest {

    private static final int BATCH_SIZE = 500;
    private static final int POOL_SIZE = 8;
    private static final int REPEAT_COUNT = 20;
    private static final double DELTA = 1e-9;  // tolerance for double comparisons

    private OrderService service;
    private List<Order> testOrders;

    @BeforeEach
    void setUp() {
        service = new OrderService(
                new InMemoryOrderRepository(),
                order -> {},
                Validators.positiveAmount()
                        .and(Validators.maxLimit(10000.0))
                        .and(Validators.currencyCheck("USD", "EUR", "GBP", "OMR")),
                Validators.defaultCurrency("OMR")
                        .andThen(Validators.timestampEnricher()),
                POOL_SIZE
        );

        testOrders = IntStream.range(0, BATCH_SIZE)
                .mapToObj(i -> {
                    double amount = Math.random() * 200 - 50;
                    String currency = (i % 3 == 0) ? "USD" : (i % 3 == 1) ? "EUR" : "GBP";
                    if (i % 7 == 0) currency = "XYZ";
                    Order order = new Order();
                    order.setCustomerName("Test" + i);
                    order.setAmount(amount);
                    order.setCurrency(currency);
                    return order;
                })
                .collect(Collectors.toList());
    }

    @AfterEach
    void tearDown() {
        service.shutdownExecutor();
    }

    @RepeatedTest(REPEAT_COUNT)
    void concurrentProcessingMustBeThreadSafe() throws Exception {
        OrderService.ProcessingResult sequential = service.processBatchWithStreams(testOrders);
        OrderService.ProcessingResult concurrent = service.processBatchConcurrently(testOrders);

        // Compare totals with delta
        assertMapsEqual(sequential.getTotalsByCurrency(),
                concurrent.getTotalsByCurrency(),
                DELTA);

        // Partition sizes must match
        assertEquals(sequential.getPartitioned().get(true).size(),
                concurrent.getPartitioned().get(true).size());
        assertEquals(sequential.getPartitioned().get(false).size(),
                concurrent.getPartitioned().get(false).size());

        // All rejected orders must carry a reason
        concurrent.getPartitioned().get(false)
                .forEach(p -> assertNotNull(p.getRejectionReason()));
    }

    @Test
    void sequentialAndConcurrentResultsMustBeIdentical() throws Exception {
        OrderService.ProcessingResult seq = service.processBatchWithStreams(testOrders);
        OrderService.ProcessingResult conc = service.processBatchConcurrently(testOrders);

        assertMapsEqual(seq.getTotalsByCurrency(), conc.getTotalsByCurrency(), DELTA);
        assertEquals(seq.getPartitioned().get(true).size(),
                conc.getPartitioned().get(true).size());
        assertEquals(seq.getPartitioned().get(false).size(),
                conc.getPartitioned().get(false).size());
    }

    // Helper to compare maps with a tolerance
    private static void assertMapsEqual(Map<String, Double> expected,
                                        Map<String, Double> actual,
                                        double delta) {
        assertEquals(expected.keySet(), actual.keySet(),
                "Currency sets differ");
        for (String key : expected.keySet()) {
            assertEquals(expected.get(key), actual.get(key), delta,
                    "Difference for currency " + key);
        }
    }
}