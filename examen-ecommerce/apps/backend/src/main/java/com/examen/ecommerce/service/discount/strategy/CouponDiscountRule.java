package com.examen.ecommerce.service.discount.strategy;

import com.examen.ecommerce.service.discount.model.CouponSnapshot;
import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.model.DiscountStepResult;
import com.examen.ecommerce.service.discount.model.DiscountType;
import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.Objects;

/** R3: % del cupón (ya validado) sobre el subtotal actual (subtotal_2). */
public final class CouponDiscountRule implements DiscountRule {

    private static final int EXECUTION_ORDER = 3;

    private final CouponSnapshot coupon;

    public CouponDiscountRule(CouponSnapshot coupon) {
        this.coupon = Objects.requireNonNull(coupon, "coupon");
    }

    @Override
    public DiscountType type() {
        return DiscountType.COUPON;
    }

    @Override
    public int order() {
        return EXECUTION_ORDER;
    }

    @Override
    public DiscountStepResult apply(DiscountContext ctx, BigDecimal currentSubtotal) {
        return DiscountStepResult.of(type(), MoneyUtils.percentOf(currentSubtotal, coupon.discountPct()));
    }
}
