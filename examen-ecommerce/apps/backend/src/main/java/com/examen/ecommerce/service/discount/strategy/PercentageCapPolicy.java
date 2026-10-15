package com.examen.ecommerce.service.discount.strategy;

import com.examen.ecommerce.service.discount.model.CapResult;
import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.Objects;

/** R4: tope = subtotal_original × capPct. Si bruto > tope se recorta y cap_applied = true. */
public final class PercentageCapPolicy implements DiscountCapPolicy {

    private final BigDecimal capPct;

    public PercentageCapPolicy(BigDecimal capPct) {
        this.capPct = MoneyUtils.round(Objects.requireNonNull(capPct, "capPct"));
    }

    @Override
    public CapResult apply(BigDecimal original, BigDecimal grossDiscount) {
        BigDecimal cap = MoneyUtils.percentOf(original, capPct);
        BigDecimal gross = MoneyUtils.nonNegative(grossDiscount);
        if (MoneyUtils.isGreaterThan(gross, cap)) {
            return new CapResult(capPct, cap, gross, MoneyUtils.subtract(gross, cap), cap, true);
        }
        return new CapResult(capPct, cap, gross, MoneyUtils.ZERO, gross, false);
    }
}
