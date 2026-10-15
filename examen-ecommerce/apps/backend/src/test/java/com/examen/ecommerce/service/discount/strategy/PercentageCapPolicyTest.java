package com.examen.ecommerce.service.discount.strategy;

import static com.examen.ecommerce.support.TestData.money;
import static org.assertj.core.api.Assertions.assertThat;

import com.examen.ecommerce.service.discount.model.CapResult;
import org.junit.jupiter.api.Test;

class PercentageCapPolicyTest {

    private final PercentageCapPolicy policy = new PercentageCapPolicy(money("35"));

    @Test
    void trimsGrossDiscountAboveTheCap() {
        CapResult result = policy.apply(money("320.00"), money("125.16"));
        assertThat(result.capApplied()).isTrue();
        assertThat(result.adjustment()).isEqualByComparingTo("13.16");
        assertThat(result.discountTotal()).isEqualByComparingTo("112.00");
        assertThat(result.capPct()).isEqualByComparingTo("35.00");
    }

    @Test
    void keepsGrossDiscountEqualToTheCap() {
        CapResult result = policy.apply(money("100.00"), money("35.00"));
        assertThat(result.capApplied()).isFalse();
        assertThat(result.adjustment()).isEqualByComparingTo("0.00");
        assertThat(result.discountTotal()).isEqualByComparingTo("35.00");
    }

    @Test
    void keepsGrossDiscountBelowTheCap() {
        CapResult result = policy.apply(money("130.00"), money("32.29"));
        assertThat(result.capApplied()).isFalse();
        assertThat(result.capAmount()).isEqualByComparingTo("45.50");
        assertThat(result.discountTotal()).isEqualByComparingTo("32.29");
    }
}
