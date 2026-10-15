package com.examen.ecommerce.service.discount.strategy;

import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.model.DiscountStepResult;
import com.examen.ecommerce.service.discount.model.DiscountType;
import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.Objects;

/** R2: pct sobre el subtotal actual si es ESTRICTAMENTE mayor que el umbral. */
public final class VolumeDiscountRule implements DiscountRule {

    private static final int EXECUTION_ORDER = 2;

    private final BigDecimal pct;
    private final BigDecimal threshold;

    public VolumeDiscountRule(BigDecimal pct, BigDecimal threshold) {
        this.pct = Objects.requireNonNull(pct, "pct");
        this.threshold = Objects.requireNonNull(threshold, "threshold");
    }

    @Override
    public DiscountType type() {
        return DiscountType.VOLUME;
    }

    @Override
    public int order() {
        return EXECUTION_ORDER;
    }

    @Override
    public DiscountStepResult apply(DiscountContext ctx, BigDecimal currentSubtotal) {
        if (currentSubtotal == null || !MoneyUtils.isGreaterThan(currentSubtotal, threshold)) {
            return DiscountStepResult.notApplied(type());
        }
        return DiscountStepResult.of(type(), MoneyUtils.percentOf(currentSubtotal, pct));
    }
}
