package com.progressoft.repository.inmemory;

import com.progressoft.repository.Identifiable;
import com.progressoft.repository.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class InMemoryRepository<T extends Identifiable<ID>, ID>
        implements Repository<T, ID> {

    private final Map<ID, T> storage = new ConcurrentHashMap<>();
    private final Supplier<ID> idGenerator;

    public InMemoryRepository(Supplier<ID> idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public T save(T entity) {
        ID id = entity.getId();
        if (id == null) {
            id = idGenerator.get();
            entity.setId(id); // Guaranteed by the Identifiable contract
        }
        storage.put(id, entity);
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void deleteById(ID id) {
        storage.remove(id);
    }

    @Override
    public void deleteAll(Collection<? extends ID> ids) {
        ids.forEach(storage::remove);
    }

    @Override
    public boolean existsById(ID id) {
        return storage.containsKey(id);
    }

    @Override
    public long count() {
        return storage.size();
    }
}