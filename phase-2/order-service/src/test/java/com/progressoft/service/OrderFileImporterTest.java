package com.progressoft.service;

import com.progressoft.repository.TestDataSourceFactory;
import com.progressoft.repository.inmemory.InMemoryOrderRepository;
import com.progressoft.service.OrderFileImporter;
import com.progressoft.service.OrderService;
import com.progressoft.validation.Validators;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class OrderFileImporterTest {

    @Test
    void shouldImportValidOrdersAndSkipMalformed() throws Exception {
        Path testFile = Paths.get("src/test/resources/sample_orders.csv");
        OrderFileImporter importer = new OrderFileImporter();
        OrderFileImporter.ImportResult result = importer.importOrders(testFile);

        assertEquals(5, result.getValidCount());
        assertEquals(2, result.getSkippedCount());

        // reasons
        assertTrue(result.getSkipped().stream()
                .anyMatch(s -> s.getReason().contains("Customer name cannot be empty")));
        assertTrue(result.getSkipped().stream()
                .anyMatch(s -> s.getReason().contains("Invalid amount")));

        DataSource dataSource = TestDataSourceFactory.createHikariDataSource();

        OrderService service = new OrderService(
                new InMemoryOrderRepository(),
                order -> {},
                Validators.positiveAmount()
                        .and(Validators.maxLimit(10000.0))
                        .and(Validators.currencyCheck("USD", "EUR", "GBP", "OMR")),
                Validators.defaultCurrency("OMR")
                        .andThen(Validators.timestampEnricher()),
                dataSource
        );

        OrderService.ProcessingResult processingResult =
                service.processBatchWithStreams(result.getOrders());

        assertNotNull(processingResult);

        // Approved: 4 (orders 1,2,4,5), Rejected: 1 (order 3, negative)
        assertEquals(4, processingResult.getPartitioned().get(true).size());
        assertEquals(1, processingResult.getPartitioned().get(false).size());
        assertEquals(400.0, processingResult.getTotalsByCurrency().get("USD"), 0.001);
        assertEquals(250.0, processingResult.getTotalsByCurrency().get("EUR"), 0.001);
        assertEquals(75.0, processingResult.getTotalsByCurrency().get("GBP"), 0.001);
    }
}