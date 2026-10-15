package com.examen.ecommerce.service.discount.model;

import java.math.BigDecimal;
import java.util.Objects;

/** Datos del cupón ya validado, sin referencias JPA (inmutable). */
public record CouponSnapshot(Integer couponId, String code, BigDecimal discountPct) {

    public CouponSnapshot {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(discountPct, "discountPct");
    }
}
