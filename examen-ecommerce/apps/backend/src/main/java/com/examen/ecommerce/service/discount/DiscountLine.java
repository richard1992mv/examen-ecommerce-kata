package com.examen.ecommerce.service.discount;

import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;

/**
 * Línea de entrada del cálculo (datos ya leídos de la BD).
 *
 * @param categoryDiscountPct porcentaje de la categoría del producto (Tecnología = 10.00)
 */
public record DiscountLine(
        Integer productId,
        String productName,
        String categoryName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal categoryDiscountPct) {

    /** unit_price × quantity. */
    public BigDecimal lineSubtotal() {
        return MoneyUtils.multiply(unitPrice, quantity);
    }
}
