package com.progressoft.repository.jpa;

import com.progressoft.domain.Order;
import com.progressoft.repository.TransactionalOrderRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class JpaOrderRepository implements TransactionalOrderRepository {

    private final EntityManagerFactory emf;

    public JpaOrderRepository() {
        this.emf = EntityManagerFactoryProvider.getEntityManagerFactory();
    }

    public JpaOrderRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public Order save(Order entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            if (entity.getId() == null) {
                em.persist(entity);
            } else {
                entity = em.merge(entity);
            }
            em.getTransaction().commit();
            return entity;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save order", e);
        } finally {
            em.close();
        }
    }

    @Override
    public Order saveWithConnection(Order entity, Connection conn) throws SQLException {
        // JPA doesn't work with external connections – delegate to save()
        // This is a limitation we accept; JPA manages its own transactions.
        return save(entity);
    }

    @Override
    public Optional<Order> findById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            Order order = em.find(Order.class, id);
            return Optional.ofNullable(order);
        } finally {
            em.close();
        }
    }

    @Override
    public List<Order> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT o FROM Order o", Order.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public void deleteById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Order order = em.find(Order.class, id);
            if (order != null) {
                em.remove(order);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete order", e);
        } finally {
            em.close();
        }
    }

    @Override
    public void deleteAll(Collection<? extends Long> ids) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (Long id : ids) {
                Order order = em.find(Order.class, id);
                if (order != null) {
                    em.remove(order);
                }
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete orders", e);
        } finally {
            em.close();
        }
    }

    @Override
    public boolean existsById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Order.class, id) != null;
        } finally {
            em.close();
        }
    }

    @Override
    public long count() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(o) FROM Order o", Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    // ---------- Part D: JPQL & Criteria ----------

    public List<Order> findByAmountGreaterThanJPQL(double amount) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT o FROM Order o WHERE o.money.amount > :amount", Order.class)
                    .setParameter("amount", amount)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Order> findByAmountGreaterThanCriteria(double amount) {
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Order> query = cb.createQuery(Order.class);
            Root<Order> root = query.from(Order.class);
            Path<Double> amountPath = root.get("money").get("amount");
            query.select(root).where(cb.gt(amountPath, amount));
            return em.createQuery(query).getResultList();
        } finally {
            em.close();
        }
    }
}