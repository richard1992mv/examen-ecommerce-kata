package com.examen.ecommerce.service.discount.model;

import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Resultado de un paso de la cascada.
 *
 * @param amount      monto del paso (≥ 0, 2 decimales)
 * @param lineAmounts reparto opcional por productId (lo usa la regla de categoría)
 */
public record DiscountStepResult(DiscountType type, BigDecimal amount, Map<Integer, BigDecimal> lineAmounts) {

    public DiscountStepResult {
        Objects.requireNonNull(type, "type");
        amount = MoneyUtils.nonNegative(amount);
        lineAmounts = lineAmounts == null ? Map.of() : Map.copyOf(lineAmounts);
    }

    public static DiscountStepResult notApplied(DiscountType type) {
        return new DiscountStepResult(type, MoneyUtils.ZERO, Map.of());
    }

    public static DiscountStepResult of(DiscountType type, BigDecimal amount) {
        return new DiscountStepResult(type, amount, Map.of());
    }

    public static DiscountStepResult ofLines(DiscountType type, Map<Integer, BigDecimal> lineAmounts) {
        return new DiscountStepResult(type, MoneyUtils.sum(lineAmounts.values()), lineAmounts);
    }

    public BigDecimal lineAmount(Integer productId) {
        return lineAmounts.getOrDefault(productId, MoneyUtils.ZERO);
    }
}
