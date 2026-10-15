package com.examen.ecommerce.dto.response;

import java.math.BigDecimal;

public record CategoryResponse(Integer id, String name, BigDecimal discountPct) {
}
