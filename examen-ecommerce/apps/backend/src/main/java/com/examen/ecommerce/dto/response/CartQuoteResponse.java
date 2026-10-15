package com.examen.ecommerce.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Desglose de /cart/quote (no persiste ni toca stock). */
public record CartQuoteResponse(
        BigDecimal subtotalOriginal,
        BigDecimal categoryDiscount,
        BigDecimal volumeDiscount,
        BigDecimal couponDiscount,
        BigDecimal capAdjustment,
        BigDecimal discountTotal,
        BigDecimal effectiveDiscountPct,
        BigDecimal discountCapPct,
        boolean capApplied,
        String message,
        BigDecimal total,
        String currency,
        String couponCode,
        List<CartLineResponse> items) {
}
