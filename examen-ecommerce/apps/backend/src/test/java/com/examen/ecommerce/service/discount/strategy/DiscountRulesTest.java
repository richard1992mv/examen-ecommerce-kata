package com.examen.ecommerce.service.discount.strategy;

import static com.examen.ecommerce.support.TestData.line;
import static com.examen.ecommerce.support.TestData.money;
import static org.assertj.core.api.Assertions.assertThat;

import com.examen.ecommerce.service.discount.model.CouponSnapshot;
import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.model.DiscountStepResult;
import com.examen.ecommerce.service.discount.model.DiscountType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Cada Strategy por separado + contrato común (Liskov). */
class DiscountRulesTest {

    private static final CouponSnapshot WELCOME = new CouponSnapshot(1, "WELCOME2026", money("15.00"));

    static Stream<DiscountRule> allRules() {
        return Stream.of(new CategoryDiscountRule(), new VolumeDiscountRule(money("5.00"), money("100.00")),
                new CouponDiscountRule(WELCOME));
    }

    @Nested
    class CategoryRule {

        private final CategoryDiscountRule rule = new CategoryDiscountRule();

        @Test
        void discountsEachLineByItsCategoryPercentage() {
            DiscountContext ctx = new DiscountContext(List.of(line(1, 2), line(9, 1)), Optional.empty());
            DiscountStepResult step = rule.apply(ctx, money("130.00"));
            assertThat(step.amount()).isEqualByComparingTo("9.00");
            assertThat(step.lineAmount(1)).isEqualByComparingTo("9.00");
            assertThat(step.lineAmount(9)).isEqualByComparingTo("0.00");
        }

        @Test
        void doesNotApplyWhenAllCategoriesAreZero() {
            DiscountContext ctx = new DiscountContext(List.of(line(8, 2), line(10, 1)), Optional.empty());
            assertThat(rule.apply(ctx, money("55.00")).amount()).isEqualByComparingTo("0.00");
        }

        @Test
        void hasOrderOne() {
            assertThat(rule.order()).isEqualTo(1);
            assertThat(rule.type()).isEqualTo(DiscountType.CATEGORY);
        }
    }

    @Nested
    class VolumeRule {

        private final VolumeDiscountRule rule = new VolumeDiscountRule(money("5.00"), money("100.00"));
        private final DiscountContext ctx = new DiscountContext(List.of(line(3, 1)), Optional.empty());

        @ParameterizedTest(name = "subtotal {0} → volumen {1}")
        @CsvSource({"100.00, 0.00", "100.01, 5.00", "102.00, 5.10", "293.00, 14.65", "48.00, 0.00"})
        void appliesOnlyWhenStrictlyAboveThreshold(String subtotal, String expected) {
            assertThat(rule.apply(ctx, money(subtotal)).amount()).isEqualByComparingTo(expected);
        }

        @Test
        void returnsZeroForNullSubtotal() {
            assertThat(rule.apply(ctx, null).amount()).isEqualByComparingTo("0.00");
            assertThat(rule.order()).isEqualTo(2);
            assertThat(rule.type()).isEqualTo(DiscountType.VOLUME);
        }
    }

    @Nested
    class CouponRule {

        @ParameterizedTest(name = "{0} × {1} % = {2}")
        @CsvSource({"114.95, 15.00, 17.24", "278.35, 30.00, 83.51", "0.00, 15.00, 0.00"})
        void appliesCouponOverCurrentSubtotalWithHalfUp(String subtotal, String pct, String expected) {
            CouponDiscountRule rule = new CouponDiscountRule(new CouponSnapshot(1, "X", money(pct)));
            DiscountContext ctx = new DiscountContext(List.of(line(1, 1)), Optional.empty());
            assertThat(rule.apply(ctx, money(subtotal)).amount()).isEqualByComparingTo(expected);
        }

        @Test
        void hasOrderThree() {
            CouponDiscountRule rule = new CouponDiscountRule(WELCOME);
            assertThat(rule.order()).isEqualTo(3);
            assertThat(rule.type()).isEqualTo(DiscountType.COUPON);
        }
    }

    @Nested
    class CommonContract {

        @ParameterizedTest
        @MethodSource("com.examen.ecommerce.service.discount.strategy.DiscountRulesTest#allRules")
        void neverReturnsNegativeAndDoesNotMutateTheContext(DiscountRule rule) {
            DiscountContext ctx = new DiscountContext(List.of(line(1, 2), line(9, 1)), Optional.of(WELCOME));
            DiscountContext snapshot = new DiscountContext(List.copyOf(ctx.lines()), ctx.coupon());

            DiscountStepResult step = rule.apply(ctx, money("-5.00"));

            assertThat(step.amount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(step.amount().scale()).isEqualTo(2);
            assertThat(ctx).isEqualTo(snapshot);
        }
    }
}
