package com.examen.ecommerce.service.discount;

import com.examen.ecommerce.service.discount.model.CapResult;
import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.model.DiscountResult;
import com.examen.ecommerce.service.discount.model.DiscountStepResult;
import com.examen.ecommerce.service.discount.strategy.DiscountCapPolicy;
import com.examen.ecommerce.service.discount.strategy.DiscountRule;
import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Orquesta la cascada multiplicativa: recorre las reglas por order(), encadena el
 * subtotal y aplica el tope. No contiene if/switch por tipo de descuento (OCP).
 * Java puro: sin Spring ni JPA.
 */
public final class DiscountEngine {

    private final DiscountCapPolicy capPolicy;

    public DiscountEngine(DiscountCapPolicy capPolicy) {
        this.capPolicy = Objects.requireNonNull(capPolicy, "capPolicy");
    }

    public DiscountResult calculate(DiscountContext ctx, List<DiscountRule> rules) {
        BigDecimal original = ctx.subtotalOriginal();
        List<DiscountStepResult> steps = applyInCascade(ctx, rules, original);
        BigDecimal gross = MoneyUtils.sum(steps.stream().map(DiscountStepResult::amount).toList());
        CapResult cap = capPolicy.apply(original, gross);
        BigDecimal total = MoneyUtils.subtract(original, cap.discountTotal());
        BigDecimal effectivePct = MoneyUtils.ratioAsPercent(cap.discountTotal(), original);
        return new DiscountResult(ctx.lines(), ctx.coupon(), original, steps, cap, total, effectivePct);
    }

    private List<DiscountStepResult> applyInCascade(DiscountContext ctx, List<DiscountRule> rules,
                                                    BigDecimal original) {
        List<DiscountStepResult> steps = new ArrayList<>();
        BigDecimal current = original;
        for (DiscountRule rule : sortedByOrder(rules)) {
            DiscountStepResult step = rule.apply(ctx, current);
            steps.add(step);
            current = MoneyUtils.subtract(current, step.amount());
        }
        return steps;
    }

    private static List<DiscountRule> sortedByOrder(List<DiscountRule> rules) {
        return rules.stream().sorted(Comparator.comparingInt(DiscountRule::order)).toList();
    }
}
