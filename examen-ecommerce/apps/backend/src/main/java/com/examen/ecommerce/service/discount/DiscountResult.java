package com.examen.ecommerce.service.discount;

import java.math.BigDecimal;
import java.util.List;

/** Desglose completo de la cascada; lo usan /cart/quote y /orders. */
public record DiscountResult(
        List<LineResult> lines,
        BigDecimal subtotalOriginal,
        BigDecimal categoryDiscount,
        BigDecimal volumeDiscount,
        BigDecimal couponDiscount,
        BigDecimal capAdjustment,
        BigDecimal discountTotal,
        BigDecimal effectiveDiscountPct,
        BigDecimal discountCapPct,
        boolean capApplied,
        BigDecimal total) {

    public DiscountResult {
        lines = List.copyOf(lines);
    }

    /** Línea con su descuento de categoría (el único descuento que se reparte por producto). */
    public record LineResult(DiscountLine line, BigDecimal categoryDiscount, BigDecimal lineTotal) {
    }
}
