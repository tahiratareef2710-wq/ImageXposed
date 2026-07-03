package displayPackage.core;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract base class for all domain entities in the system.
 * Provides common fields (id, timestamps) and utility methods
 * that every entity shares.
 *
 * Subclasses: User, Image, Scan, Report, Feedback
 */
public abstract class BaseEntity {

    protected String id;
    protected Date createdAt;
    protected Date updatedAt;

    /**
     * Default constructor — auto-generates a UUID and sets timestamps.
     */
    protected BaseEntity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    /**
     * Constructor with a specific ID (useful when loading from DB).
     *
     * @param id the entity's unique identifier
     */
    protected BaseEntity(String id) {
        this.id = id;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    /**
     * Validates the entity's data integrity.
     * Each subclass must define its own validation rules.
     *
     * @return true if the entity is valid, false otherwise
     */
    public abstract boolean validate();

    /**
     * Converts the entity's fields into a Map for easy serialization
     * or passing to the DatabaseHandler for persistence.
     *
     * @return a map of field names to their values
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Call this whenever the entity is modified to refresh the updatedAt timestamp.
     */
    protected void touch() {
        this.updatedAt = new Date();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id='" + id + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
