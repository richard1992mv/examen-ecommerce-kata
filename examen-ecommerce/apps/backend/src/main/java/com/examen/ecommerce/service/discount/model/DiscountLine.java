package com.examen.ecommerce.service.discount.model;

import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Línea del carrito sin referencias JPA (inmutable).
 *
 * @param categoryDiscountPct porcentaje de la categoría del producto (0.00 – 100.00)
 */
public record DiscountLine(
        Integer productId,
        String productName,
        String categoryName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal categoryDiscountPct) {

    public DiscountLine {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(unitPrice, "unitPrice");
        categoryDiscountPct = categoryDiscountPct == null ? MoneyUtils.ZERO : categoryDiscountPct;
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity debe ser > 0");
        }
    }

    /** unit_price × quantity. */
    public BigDecimal lineSubtotal() {
        return MoneyUtils.multiply(unitPrice, quantity);
    }
}
