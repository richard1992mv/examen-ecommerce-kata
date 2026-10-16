package com.examen.ecommerce.service.discount;

import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.List;

/**
 * Estrategia concreta – Regla 2, Volumen: si el subtotal tras la regla 1 es estrictamente mayor que el umbral
 * ({@code discount.volume.threshold} = 100.00), descuenta {@code discount.volume.pct} (5 %) de ese subtotal.
 */
final class VolumeDiscountRule implements DiscountRule {

    private final BigDecimal pct;
    private final BigDecimal threshold;

    VolumeDiscountRule(BigDecimal pct, BigDecimal threshold) {
        this.pct = pct;
        this.threshold = threshold;
    }

    @Override
    public DiscountType type() {
        return DiscountType.VOLUME;
    }

    @Override
    public BigDecimal calculate(List<DiscountLine> lines, BigDecimal subtotal) {
        return MoneyUtils.isGreaterThan(subtotal, threshold)
                ? MoneyUtils.percentOf(subtotal, pct)
                : MoneyUtils.ZERO;
    }
}
