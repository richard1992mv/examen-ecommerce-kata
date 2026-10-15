package com.examen.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Orden confirmada con su desglose histórico (snapshots). */
public record OrderResponse(
        Integer id,
        String orderNumber,
        LocalDateTime createdAt,
        String couponCode,
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
        List<OrderItemResponse> items) {
}
