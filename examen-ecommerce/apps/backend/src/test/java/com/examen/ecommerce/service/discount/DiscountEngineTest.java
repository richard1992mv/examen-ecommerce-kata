package com.examen.ecommerce.service.discount;

import static com.examen.ecommerce.support.TestData.line;
import static com.examen.ecommerce.support.TestData.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.examen.ecommerce.service.discount.model.CapResult;
import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.model.DiscountResult;
import com.examen.ecommerce.service.discount.model.DiscountStepResult;
import com.examen.ecommerce.service.discount.model.DiscountType;
import com.examen.ecommerce.service.discount.strategy.DiscountCapPolicy;
import com.examen.ecommerce.service.discount.strategy.DiscountRule;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** DiscountEngine con reglas mock: verifica orden por order() y encadenamiento (cascada). */
class DiscountEngineTest {

    private final DiscountContext ctx = new DiscountContext(List.of(line(3, 1), line(6, 1)), Optional.empty());

    @Test
    void appliesRulesSortedByOrderAndChainsTheSubtotal() {
        DiscountRule first = rule(DiscountType.CATEGORY, 1, "10.00");
        DiscountRule second = rule(DiscountType.VOLUME, 2, "5.00");
        DiscountRule third = rule(DiscountType.COUPON, 3, "1.00");
        DiscountCapPolicy cap = (original, gross) ->
                new CapResult(money("35.00"), money("38.50"), gross, money("0.00"), gross, false);

        DiscountResult result = new DiscountEngine(cap).calculate(ctx, List.of(third, first, second));

        InOrder order = inOrder(first, second, third);
        order.verify(first).apply(any(), argThat(amountIs("110.00")));
        order.verify(second).apply(any(), argThat(amountIs("100.00")));
        order.verify(third).apply(any(), argThat(amountIs("95.00")));
        assertThat(result.steps()).extracting(DiscountStepResult::type)
                .containsExactly(DiscountType.CATEGORY, DiscountType.VOLUME, DiscountType.COUPON);
        assertThat(result.discountTotal()).isEqualByComparingTo("16.00");
        assertThat(result.total()).isEqualByComparingTo("94.00");
    }

    @Test
    void delegatesTheCapToThePolicy() {
        DiscountRule huge = rule(DiscountType.COUPON, 1, "100.00");
        DiscountCapPolicy cap = (original, gross) ->
                new CapResult(money("35.00"), money("38.50"), gross, money("61.50"), money("38.50"), true);

        DiscountResult result = new DiscountEngine(cap).calculate(ctx, List.of(huge));

        assertThat(result.capApplied()).isTrue();
        assertThat(result.total()).isEqualByComparingTo("71.50");
        assertThat(result.effectiveDiscountPct()).isEqualByComparingTo("35.00");
    }

    @Test
    void withoutRulesThereIsNoDiscount() {
        DiscountCapPolicy cap = (original, gross) ->
                new CapResult(money("35.00"), money("38.50"), gross, money("0.00"), gross, false);

        DiscountResult result = new DiscountEngine(cap).calculate(ctx, List.of());

        assertThat(result.total()).isEqualByComparingTo("110.00");
        assertThat(result.steps()).isEmpty();
    }

    private static DiscountRule rule(DiscountType type, int order, String amount) {
        DiscountRule rule = mock(DiscountRule.class);
        when(rule.type()).thenReturn(type);
        when(rule.order()).thenReturn(order);
        when(rule.apply(any(), any())).thenReturn(DiscountStepResult.of(type, money(amount)));
        return rule;
    }

    private static org.mockito.ArgumentMatcher<BigDecimal> amountIs(String expected) {
        return actual -> actual != null && actual.compareTo(money(expected)) == 0;
    }
}
