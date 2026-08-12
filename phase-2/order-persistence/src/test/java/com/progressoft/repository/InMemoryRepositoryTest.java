package com.progressoft.repository;

import com.progressoft.repository.inmemory.InMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRepositoryTest {

    private InMemoryRepository<TestEntity, Long> repository;
    private final AtomicLong idGenerator = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepository<>(idGenerator::getAndIncrement);
    }

    @Test
    void save_shouldGenerateId_whenEntityHasNullId() {
        TestEntity entity = new TestEntity(null, "test");
        TestEntity saved = repository.save(entity);
        assertNotNull(saved.getId());
        assertEquals(1L, saved.getId());
        assertEquals("test", saved.getName());
    }

    @Test
    void save_shouldUpdateExistingEntity_whenIdAlreadySet() {
        TestEntity entity = new TestEntity(100L, "existing");
        repository.save(entity);
        TestEntity updated = new TestEntity(100L, "updated");
        repository.save(updated);

        Optional<TestEntity> found = repository.findById(100L);
        assertTrue(found.isPresent());
        assertEquals("updated", found.get().getName());
        assertEquals(100L, found.get().getId());
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        Optional<TestEntity> found = repository.findById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    void findAll_shouldReturnAllSavedEntities() {
        repository.save(new TestEntity(null, "one"));
        repository.save(new TestEntity(null, "two"));
        List<TestEntity> all = repository.findAll();
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(e -> "one".equals(e.getName())));
        assertTrue(all.stream().anyMatch(e -> "two".equals(e.getName())));
    }

    @Test
    void deleteById_shouldRemoveEntity() {
        TestEntity saved = repository.save(new TestEntity(null, "toDelete"));
        assertTrue(repository.existsById(saved.getId()));

        repository.deleteById(saved.getId());
        assertFalse(repository.existsById(saved.getId()));
        assertEquals(0, repository.count());
    }

    @Test
    void deleteAll_shouldRemoveMultipleEntities() {
        TestEntity e1 = repository.save(new TestEntity(null, "a"));
        TestEntity e2 = repository.save(new TestEntity(null, "b"));
        repository.deleteAll(Arrays.asList(e1.getId(), e2.getId()));

        assertEquals(0, repository.count());
        assertFalse(repository.existsById(e1.getId()));
        assertFalse(repository.existsById(e2.getId()));
    }

    @Test
    void existsById_shouldReturnTrue_whenEntityExists() {
        TestEntity saved = repository.save(new TestEntity(null, "exists"));
        assertTrue(repository.existsById(saved.getId()));
    }

    @Test
    void count_shouldReturnCorrectNumberOfEntities() {
        assertEquals(0, repository.count());
        repository.save(new TestEntity(null, "one"));
        assertEquals(1, repository.count());
        repository.save(new TestEntity(null, "two"));
        assertEquals(2, repository.count());
    }

    // Helper test entity
    private static class TestEntity implements Identifiable<Long> {
        private Long id;
        private final String name;

        TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() { return id; }

        @Override
        public void setId(Long id) { this.id = id; }

        String getName() { return name; }
    }
}