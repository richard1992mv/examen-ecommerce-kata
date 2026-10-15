package com.examen.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Tabla products. El stock solo se descuenta con UPDATE atómico (ProductRepository). */
@Entity
@Table(name = "products")
public class Product extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, columnDefinition = UNSIGNED_INT)
    private Category category;

    @Column(name = "stock", nullable = false, columnDefinition = UNSIGNED_INT)
    private Integer stock;

    protected Product() {
    }

    public String getName() {
        return name;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Category getCategory() {
        return category;
    }

    public Integer getStock() {
        return stock;
    }

    @Override
    public String toString() {
        return "Product{id=" + getId() + ", name='" + name + "', unitPrice=" + unitPrice + ", stock=" + stock + '}';
    }
}
