package com.examen.ecommerce.service.discount;

import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.List;

/**
 * Estrategia concreta – Regla 1, Categoría: por línea, line_subtotal × % de su categoría
 * (Tecnología 10 %, leído de {@code categories.discount_pct}). El total es la suma de los descuentos por línea.
 */
final class CategoryDiscountRule implements DiscountRule {

    @Override
    public DiscountType type() {
        return DiscountType.CATEGORY;
    }

    /** Ignora {@code subtotal}: es el primer paso y se calcula línea por línea. */
    @Override
    public BigDecimal calculate(List<DiscountLine> lines, BigDecimal subtotal) {
        return MoneyUtils.sum(lines.stream().map(CategoryDiscountRule::discountFor).toList());
    }

    /** Descuento de categoría de una línea; también lo usa el calculador para el desglose por producto. */
    static BigDecimal discountFor(DiscountLine line) {
        return MoneyUtils.percentOf(line.lineSubtotal(), line.categoryDiscountPct());
    }
}
