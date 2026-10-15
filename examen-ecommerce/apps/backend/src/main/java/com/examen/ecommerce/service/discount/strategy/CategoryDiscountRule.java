package com.examen.ecommerce.service.discount.strategy;

import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.model.DiscountLine;
import com.examen.ecommerce.service.discount.model.DiscountStepResult;
import com.examen.ecommerce.service.discount.model.DiscountType;
import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** R1: por línea, line_subtotal × categories.discount_pct, redondeado por línea. */
public final class CategoryDiscountRule implements DiscountRule {

    private static final int EXECUTION_ORDER = 1;

    @Override
    public DiscountType type() {
        return DiscountType.CATEGORY;
    }

    @Override
    public int order() {
        return EXECUTION_ORDER;
    }

    @Override
    public DiscountStepResult apply(DiscountContext ctx, BigDecimal currentSubtotal) {
        Map<Integer, BigDecimal> perLine = new LinkedHashMap<>();
        for (DiscountLine line : ctx.lines()) {
            perLine.put(line.productId(), MoneyUtils.percentOf(line.lineSubtotal(), line.categoryDiscountPct()));
        }
        return DiscountStepResult.ofLines(type(), perLine);
    }
}
