package com.examen.ecommerce.service.discount.model;

import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Entrada inmutable del motor: líneas + cupón opcional. Las reglas nunca lo mutan. */
public record DiscountContext(List<DiscountLine> lines, Optional<CouponSnapshot> coupon) {

    public DiscountContext {
        lines = List.copyOf(lines);
        coupon = coupon == null ? Optional.empty() : coupon;
    }

    /** Σ unit_price × quantity, sin descuentos. */
    public BigDecimal subtotalOriginal() {
        return MoneyUtils.sum(lines.stream().map(DiscountLine::lineSubtotal).toList());
    }
}
