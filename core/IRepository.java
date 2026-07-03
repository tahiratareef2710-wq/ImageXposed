package displayPackage.core;

import java.util.List;

/**
 * Generic repository interface following the Repository pattern.
 * Provides standard CRUD operations for any entity type.
 * Concrete repositories (e.g., UserRepository, ScanRepository)
 * will implement this interface for type-safe data access.
 *
 * @param <T> the entity type this repository manages (must extend BaseEntity)
 */
public interface IRepository<T extends BaseEntity> {

    /**
     * Persists an entity to the data store.
     * If the entity already exists (same ID), it should be updated.
     *
     * @param entity the entity to save
     */
    void save(T entity);

    /**
     * Retrieves an entity by its unique identifier.
     *
     * @param id the unique identifier
     * @return the entity if found, or null if not present
     */
    T findById(String id);

    /**
     * Removes an entity from the data store by ID.
     *
     * @param id the unique identifier of the entity to delete
     */
    void delete(String id);

    /**
     * Retrieves all entities of this type from the data store.
     *
     * @return a list of all entities (empty list if none exist)
     */
    List<T> findAll();
}
