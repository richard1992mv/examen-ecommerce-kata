package com.examen.ecommerce.service.discount;

import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.List;

/**
 * Estrategia concreta – Regla 3, Cupón: descuenta el % del cupón (WELCOME2026 = 15 %) sobre el subtotal tras la
 * regla 2. El cupón ya llega validado por {@code CouponValidator}; esta clase solo aplica el porcentaje.
 */
final class CouponDiscountRule implements DiscountRule {

    private final BigDecimal pct;

    CouponDiscountRule(BigDecimal pct) {
        this.pct = pct;
    }

    @Override
    public DiscountType type() {
        return DiscountType.COUPON;
    }

    @Override
    public BigDecimal calculate(List<DiscountLine> lines, BigDecimal subtotal) {
        return MoneyUtils.percentOf(subtotal, pct);
    }
}
