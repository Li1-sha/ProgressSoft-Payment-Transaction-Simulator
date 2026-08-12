package com.progressoft.repository.inmemory;

import com.progressoft.domain.Order;
import com.progressoft.repository.TransactionalOrderRepository;

import java.util.concurrent.atomic.AtomicLong;

public class InMemoryOrderRepository
        extends InMemoryRepository<Order, Long>
        implements TransactionalOrderRepository {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    public InMemoryOrderRepository() {
        super(ID_GENERATOR::getAndIncrement); // Generates 1, 2, 3, ...
    }
}