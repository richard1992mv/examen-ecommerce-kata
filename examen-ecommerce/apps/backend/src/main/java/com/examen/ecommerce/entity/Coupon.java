package com.examen.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Tabla coupons (solo lectura). Vigencia inclusiva en ambos extremos. */
@Entity
@Table(name = "coupons")
public class Coupon extends AuditableEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "discount_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPct;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    /** TINYINT(1) UNSIGNED → boolean (necesario para ddl-auto=validate). */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected Coupon() {
    }

    /** valid_from ≤ instante ≤ valid_until (inclusive). */
    public boolean isValidAt(LocalDateTime instant) {
        return !instant.isBefore(validFrom) && !instant.isAfter(validUntil);
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getDiscountPct() {
        return discountPct;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "Coupon{id=" + getId() + ", code='" + code + "', discountPct=" + discountPct + ", active=" + active + '}';
    }
}
