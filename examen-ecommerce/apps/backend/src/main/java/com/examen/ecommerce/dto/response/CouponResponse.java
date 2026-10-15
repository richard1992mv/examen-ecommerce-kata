package com.examen.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
        Integer id,
        String code,
        BigDecimal discountPct,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        boolean active) {
}
