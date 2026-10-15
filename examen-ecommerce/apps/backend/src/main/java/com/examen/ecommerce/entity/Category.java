package com.examen.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Tabla categories. */
@Entity
@Table(name = "categories")
public class Category extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "discount_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPct;

    protected Category() {
    }

    public Category(String name, BigDecimal discountPct) {
        update(name, discountPct);
    }

    public final void update(String newName, BigDecimal newDiscountPct) {
        this.name = newName;
        this.discountPct = newDiscountPct;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getDiscountPct() {
        return discountPct;
    }

    @Override
    public String toString() {
        return "Category{id=" + getId() + ", name='" + name + "', discountPct=" + discountPct + '}';
    }
}
