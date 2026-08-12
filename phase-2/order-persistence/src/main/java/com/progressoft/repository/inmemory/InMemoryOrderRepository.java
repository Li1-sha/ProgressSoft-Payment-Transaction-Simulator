package com.progressoft.repository.inmemory;

import com.progressoft.domain.Order;
import com.progressoft.repository.OrderRepository;
import com.progressoft.repository.TransactionalOrderRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryOrderRepository
        extends InMemoryRepository<Order, Long>
        implements TransactionalOrderRepository {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    public InMemoryOrderRepository() {
        super(ID_GENERATOR::getAndIncrement);
    }

    @Override
    public Order saveWithConnection(Order entity, Connection conn) throws SQLException {
        throw new UnsupportedOperationException("InMemoryRepository does not support transactional save");
    }
}