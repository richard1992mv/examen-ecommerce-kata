package com.examen.ecommerce.service.discount;

import static com.examen.ecommerce.support.TestData.line;
import static com.examen.ecommerce.support.TestData.money;
import static com.examen.ecommerce.support.TestData.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

import com.examen.ecommerce.service.discount.factory.DiscountRuleFactory;
import com.examen.ecommerce.service.discount.model.CouponSnapshot;
import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.model.DiscountLine;
import com.examen.ecommerce.service.discount.model.DiscountResult;
import com.examen.ecommerce.service.discount.model.DiscountType;
import com.examen.ecommerce.service.discount.strategy.PercentageCapPolicy;
import com.examen.ecommerce.support.TestData;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Motor sin Spring ni BD: los casos del seed deben coincidir al centavo. */
class DiscountEngineSeedCasesTest {

    private final DiscountRuleFactory factory = new DiscountRuleFactory(money("5.00"), money("100.00"));
    private final DiscountEngine engine = new DiscountEngine(new PercentageCapPolicy(money("35.00")));

    static Stream<Arguments> seedCases() {
        return Stream.of(
                Arguments.of("(a) Novela×2 + Mouse×1", List.of(line(8, 2), line(2, 1)), null,
                        "50.00", "2.00", "0.00", "0.00", "0.00", "2.00", "48.00", "4.00", false),
                Arguments.of("(b) Teclado×1 + Lámpara×1", List.of(line(3, 1), line(6, 1)), null,
                        "110.00", "8.00", "5.10", "0.00", "0.00", "13.10", "96.90", "11.91", false),
                Arguments.of("(c) Audífonos×2 + Libro×1 + WELCOME2026", List.of(line(1, 2), line(9, 1)),
                        snapshot(TestData.welcome2026()),
                        "130.00", "9.00", "6.05", "17.24", "0.00", "32.29", "97.71", "24.84", false),
                Arguments.of("(d) Monitor + Smartwatch + Balón×2 + TESTCAP30",
                        List.of(line(4, 1), line(12, 1), line(10, 2)), snapshot(TestData.testCap30()),
                        "320.00", "27.00", "14.65", "83.51", "13.16", "112.00", "208.00", "35.00", true));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("seedCases")
    void reproducesSeedOrdersToTheCent(String name, List<DiscountLine> lines, CouponSnapshot coupon,
                                       String subtotal, String category, String volume, String couponAmount,
                                       String adjustment, String discountTotal, String total,
                                       String effective, boolean capApplied) {
        DiscountResult result = calculate(lines, coupon);

        assertThat(result.subtotalOriginal()).isEqualByComparingTo(subtotal);
        assertThat(result.amountOf(DiscountType.CATEGORY)).isEqualByComparingTo(category);
        assertThat(result.amountOf(DiscountType.VOLUME)).isEqualByComparingTo(volume);
        assertThat(result.amountOf(DiscountType.COUPON)).isEqualByComparingTo(couponAmount);
        assertThat(result.cap().adjustment()).isEqualByComparingTo(adjustment);
        assertThat(result.discountTotal()).isEqualByComparingTo(discountTotal);
        assertThat(result.total()).isEqualByComparingTo(total);
        assertThat(result.effectiveDiscountPct()).isEqualByComparingTo(effective);
        assertThat(result.capApplied()).isEqualTo(capApplied);
        assertThat(result.total().add(result.discountTotal())).isEqualByComparingTo(result.subtotalOriginal());
    }

    @Test
    @DisplayName("Caso (d): líneas con descuento de categoría por línea")
    void caseDLineDetail() {
        DiscountResult result = calculate(List.of(line(4, 1), line(12, 1), line(10, 2)),
                snapshot(TestData.testCap30()));

        assertThat(result.lineDiscount(4)).isEqualByComparingTo("15.00");
        assertThat(result.lineDiscount(12)).isEqualByComparingTo("12.00");
        assertThat(result.lineDiscount(10)).isEqualByComparingTo("0.00");
        assertThat(result.lineTotal(result.lines().get(0))).isEqualByComparingTo("135.00");
        assertThat(result.cap().grossDiscount()).isEqualByComparingTo("125.16");
        assertThat(result.cap().capAmount()).isEqualByComparingTo("112.00");
    }

    @Nested
    @DisplayName("Bordes")
    class Edges {

        @Test
        @DisplayName("subtotal_1 = 100.00 exacto → sin volumen")
        void exactlyAtThresholdHasNoVolume() {
            DiscountResult result = calculate(List.of(lineOf("100.00", "0.00")), null);
            assertThat(result.amountOf(DiscountType.VOLUME)).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("subtotal_1 = 100.01 → con volumen")
        void justAboveThresholdHasVolume() {
            DiscountResult result = calculate(List.of(lineOf("100.01", "0.00")), null);
            assertThat(result.amountOf(DiscountType.VOLUME)).isEqualByComparingTo("5.00");
            assertThat(result.total()).isEqualByComparingTo("95.01");
        }

        @Test
        @DisplayName("Bruto exactamente igual al 35 % → sin ajuste")
        void grossEqualToCapIsNotAdjusted() {
            DiscountResult result = calculate(List.of(lineOf("100.00", "35.00")), null);
            assertThat(result.cap().grossDiscount()).isEqualByComparingTo("35.00");
            assertThat(result.cap().adjustment()).isEqualByComparingTo("0.00");
            assertThat(result.capApplied()).isFalse();
            assertThat(result.discountTotal()).isEqualByComparingTo("35.00");
        }

        @Test
        @DisplayName("Categoría con 0 % → sin descuento de categoría")
        void zeroPercentCategory() {
            DiscountResult result = calculate(List.of(line(8, 2)), null);
            assertThat(result.amountOf(DiscountType.CATEGORY)).isEqualByComparingTo("0.00");
            assertThat(result.total()).isEqualByComparingTo("30.00");
        }

        @Test
        @DisplayName("Cascada ≠ suma de porcentajes")
        void cascadeIsNotTheSumOfPercentages() {
            DiscountResult result = calculate(List.of(line(1, 2), line(9, 1)), snapshot(TestData.welcome2026()));
            BigDecimal cascade = result.amountOf(DiscountType.VOLUME).add(result.amountOf(DiscountType.COUPON));
            BigDecimal summedPercentages = money("121.00").multiply(money("0.20"));
            assertThat(cascade).isEqualByComparingTo("23.29");
            assertThat(cascade).isNotEqualByComparingTo(summedPercentages);
        }

        @Test
        @DisplayName("Carrito de subtotal 0 → porcentaje efectivo 0")
        void zeroSubtotal() {
            DiscountResult result = calculate(List.of(lineOf("0.00", "10.00")), null);
            assertThat(result.effectiveDiscountPct()).isEqualByComparingTo("0.00");
            assertThat(result.total()).isEqualByComparingTo("0.00");
        }
    }

    private DiscountResult calculate(List<DiscountLine> lines, CouponSnapshot coupon) {
        DiscountContext ctx = new DiscountContext(lines, Optional.ofNullable(coupon));
        return engine.calculate(ctx, factory.createRules(ctx.coupon()));
    }

    private static DiscountLine lineOf(String price, String pct) {
        return new DiscountLine(99, "Producto", "Categoría", money(price), 1, money(pct));
    }
}
