package com.cupqueue.config.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.time.Instant;

/**
 * Base mapping for entities whose creation and modification timestamps are managed by PostgreSQL.
 *
 * <p>Both attributes are read-only to JPA. An entity must be reloaded or refreshed when code
 * needs a timestamp changed by a database trigger during the current persistence context.</p>
 */
@MappedSuperclass
public abstract class DatabaseTimestampedEntity {

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    /**
     * Creates the timestamp base state for a JPA entity.
     */
    public DatabaseTimestampedEntity() {
    }

    /**
     * Returns the instant at which the row was created.
     *
     * @return the database-managed creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the instant at which the row was last modified.
     *
     * @return the database-managed modification timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
