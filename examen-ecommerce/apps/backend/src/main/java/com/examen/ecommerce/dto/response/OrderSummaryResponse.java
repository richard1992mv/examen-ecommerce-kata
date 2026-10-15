package com.examen.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila del listado paginado de órdenes (sin ítems: evita N+1). */
public record OrderSummaryResponse(
        Integer id,
        String orderNumber,
        LocalDateTime createdAt,
        String couponCode,
        BigDecimal subtotalOriginal,
        BigDecimal discountTotal,
        BigDecimal total,
        boolean capApplied,
        String currency) {
}
