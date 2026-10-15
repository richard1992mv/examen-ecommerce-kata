package com.examen.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;

/** created_at / updated_at los gestiona MySQL (DEFAULT CURRENT_TIMESTAMP / ON UPDATE). */
@MappedSuperclass
public abstract class AuditableEntity extends IdentifiedEntity {

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
