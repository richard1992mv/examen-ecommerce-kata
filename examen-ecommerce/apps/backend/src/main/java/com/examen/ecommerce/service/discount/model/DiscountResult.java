package com.examen.ecommerce.service.discount.model;

import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Desglose completo e inmutable producido por el motor. */
public record DiscountResult(
        List<DiscountLine> lines,
        Optional<CouponSnapshot> coupon,
        BigDecimal subtotalOriginal,
        List<DiscountStepResult> steps,
        CapResult cap,
        BigDecimal total,
        BigDecimal effectiveDiscountPct) {

    public DiscountResult {
        lines = List.copyOf(lines);
        steps = List.copyOf(steps);
        coupon = coupon == null ? Optional.empty() : coupon;
    }

    /** Suma de los pasos de un tipo (0 si no se ejecutó). */
    public BigDecimal amountOf(DiscountType type) {
        return MoneyUtils.sum(steps.stream()
                .filter(step -> step.type() == type)
                .map(DiscountStepResult::amount)
                .toList());
    }

    /** Descuento asignado a una línea por los pasos que reparten por producto. */
    public BigDecimal lineDiscount(Integer productId) {
        return MoneyUtils.sum(steps.stream().map(step -> step.lineAmount(productId)).toList());
    }

    public BigDecimal lineTotal(DiscountLine line) {
        return MoneyUtils.subtract(line.lineSubtotal(), lineDiscount(line.productId()));
    }

    public BigDecimal discountTotal() {
        return cap.discountTotal();
    }

    public boolean capApplied() {
        return cap.capApplied();
    }
}
