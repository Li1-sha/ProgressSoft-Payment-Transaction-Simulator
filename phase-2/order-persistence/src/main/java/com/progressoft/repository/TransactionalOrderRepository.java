package com.progressoft.repository;

import com.progressoft.domain.Order;
import java.sql.Connection;
import java.sql.SQLException;

public interface TransactionalOrderRepository extends OrderRepository {
    default Order saveWithConnection(Order entity, Connection conn) throws SQLException {
        throw new UnsupportedOperationException(
                "This repository does not support transactional save with a provided connection"
        );
    }
}