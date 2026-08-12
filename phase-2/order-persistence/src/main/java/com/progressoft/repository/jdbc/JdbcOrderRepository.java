package com.progressoft.repository.jdbc;

import com.progressoft.domain.Order;
import com.progressoft.repository.TransactionalOrderRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class JdbcOrderRepository implements TransactionalOrderRepository {

    private final DataSource dataSource;

    public JdbcOrderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Order save(Order entity) {
        if (entity.getId() == null) {
            return insert(entity);
        } else {
            return update(entity);
        }
    }

    private Order insert(Order entity) {
        String sql = "INSERT INTO orders (customer_name, amount, currency) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, entity.getCustomerName());
            stmt.setDouble(2, entity.getAmount());
            stmt.setString(3, entity.getCurrency());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    entity.setId(rs.getLong(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert order", e);
        }
    }

    private Order update(Order entity) {
        String sql = "UPDATE orders SET customer_name=?, amount=?, currency=? WHERE id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entity.getCustomerName());
            stmt.setDouble(2, entity.getAmount());
            stmt.setString(3, entity.getCurrency());
            stmt.setLong(4, entity.getId());
            stmt.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update order", e);
        }
    }

    @Override
    public Optional<Order> findById(Long id) {
        String sql = "SELECT id, customer_name, amount, currency FROM orders WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find order", e);
        }
    }

    @Override
    public List<Order> findAll() {
        String sql = "SELECT id, customer_name, amount, currency FROM orders";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<Order> orders = new ArrayList<>();
            while (rs.next()) orders.add(mapRow(rs));
            return orders;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all orders", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM orders WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete order", e);
        }
    }

    @Override
    public void deleteAll(Collection<? extends Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        String placeholders = String.join(",", ids.stream().map(id -> "?").collect(java.util.stream.Collectors.toList()));
        String sql = "DELETE FROM orders WHERE id IN (" + placeholders + ")";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Long id : ids) stmt.setLong(idx++, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete orders", e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT 1 FROM orders WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check existence", e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM orders";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count orders", e);
        }
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setCustomerName(rs.getString("customer_name"));
        order.setAmount(rs.getDouble("amount"));
        order.setCurrency(rs.getString("currency"));
        return order;
    }

    public Order saveWithConnection(Order entity, Connection conn) throws SQLException {
        if (entity.getId() == null) {
            String sql = "INSERT INTO orders (customer_name, amount, currency) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, entity.getCustomerName());
                stmt.setDouble(2, entity.getAmount());
                stmt.setString(3, entity.getCurrency());
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) entity.setId(rs.getLong(1));
                }
            }
        } else {
            String sql = "UPDATE orders SET customer_name=?, amount=?, currency=? WHERE id=?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, entity.getCustomerName());
                stmt.setDouble(2, entity.getAmount());
                stmt.setString(3, entity.getCurrency());
                stmt.setLong(4, entity.getId());
                stmt.executeUpdate();
            }
        }
        return entity;
    }
}