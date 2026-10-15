package com.examen.ecommerce.dto.response;

import java.math.BigDecimal;

public record ProductResponse(
        Integer id,
        String name,
        BigDecimal unitPrice,
        Integer stock,
        Integer categoryId,
        String categoryName) {
}
