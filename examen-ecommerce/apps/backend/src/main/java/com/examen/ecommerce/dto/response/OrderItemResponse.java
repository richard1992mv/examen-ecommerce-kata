package com.examen.ecommerce.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Integer id,
        Integer productId,
        String productName,
        String categoryName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineSubtotal,
        BigDecimal categoryDiscount,
        BigDecimal lineTotal) {
}
