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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceConcurrencyTest {

    private static final int BATCH_SIZE = 500;
    private static final int POOL_SIZE = 8;
    private static final int REPEAT_COUNT = 20;

    private OrderService service;
    private List<Order> testOrders;

    @BeforeEach
    void setUp() {
        service = new OrderService(
                new InMemoryOrderRepository(),
                order -> {},   // dummy gateway
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
                    if (i % 7 == 0) currency = "XYZ"; // invalid
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
        service.shutdownExecutor();  // prevents thread leaks
    }

    @RepeatedTest(REPEAT_COUNT)
    void concurrentProcessingMustBeThreadSafe() throws Exception {
        OrderService.ProcessingResult sequential = service.processBatchWithStreams(testOrders);
        OrderService.ProcessingResult concurrent = service.processBatchConcurrently(testOrders);

        assertEquals(sequential.getTotalsByCurrency(),
                concurrent.getTotalsByCurrency(),
                "Totals differ by currency");

        assertEquals(sequential.getPartitioned().get(true).size(),
                concurrent.getPartitioned().get(true).size());
        assertEquals(sequential.getPartitioned().get(false).size(),
                concurrent.getPartitioned().get(false).size());

        concurrent.getPartitioned().get(false)
                .forEach(p -> assertNotNull(p.getRejectionReason()));
    }

    @Test
    void sequentialAndConcurrentResultsMustBeIdentical() throws Exception {
        OrderService.ProcessingResult seq = service.processBatchWithStreams(testOrders);
        OrderService.ProcessingResult conc = service.processBatchConcurrently(testOrders);

        assertEquals(seq.getTotalsByCurrency(), conc.getTotalsByCurrency());
        assertEquals(seq.getPartitioned().get(true).size(),
                conc.getPartitioned().get(true).size());
        assertEquals(seq.getPartitioned().get(false).size(),
                conc.getPartitioned().get(false).size());
    }
}
