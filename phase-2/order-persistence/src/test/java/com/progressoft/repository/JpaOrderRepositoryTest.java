package com.progressoft.repository;

import com.progressoft.domain.Order;
import com.progressoft.repository.jpa.JpaOrderRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JpaOrderRepositoryTest {

    private static EntityManagerFactory emf;
    private static JpaOrderRepository repository;

    @BeforeAll
    static void setUp() {
        emf = Persistence.createEntityManagerFactory("order-pu");
        repository = new JpaOrderRepository(emf);

        repository.deleteAll(repository.findAll().stream().map(Order::getId).collect(Collectors.toList()));

        repository.save(new Order("A", 50, "USD"));
        repository.save(new Order("B", 150, "USD"));
        repository.save(new Order("C", 250, "USD"));
    }

    @AfterAll
    static void tearDown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    void jpqlAndCriteriaReturnSameResults() {
        double threshold = 100.0;

        List<Order> jpqlResult = repository.findByAmountGreaterThanJPQL(threshold);
        List<Order> criteriaResult = repository.findByAmountGreaterThanCriteria(threshold);

        assertEquals(jpqlResult.size(), criteriaResult.size());
        // Check that both contain the same order IDs (order might not be guaranteed, but we can compare sets)
        List<Long> jpqlIds = jpqlResult.stream().map(Order::getId).sorted().collect(Collectors.toList());
        List<Long> criteriaIds = criteriaResult.stream().map(Order::getId).sorted().collect(Collectors.toList());
        assertEquals(jpqlIds, criteriaIds);
    }
}
