package com.examen.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.Objects;
import org.hibernate.Hibernate;

/**
 * PK INT UNSIGNED AUTO_INCREMENT común. equals/hashCode por id (sin recorrer relaciones)
 * y seguros con proxies de Hibernate.
 */
@MappedSuperclass
public abstract class IdentifiedEntity {

    protected static final String UNSIGNED_INT = "INT UNSIGNED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = UNSIGNED_INT)
    private Integer id;

    public Integer getId() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        IdentifiedEntity that = (IdentifiedEntity) other;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
