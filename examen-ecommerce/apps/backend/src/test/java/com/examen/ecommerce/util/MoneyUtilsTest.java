package com.examen.ecommerce.util;

import static com.examen.ecommerce.support.TestData.money;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyUtilsTest {

    @ParameterizedTest(name = "{0} × {1} % = {2}")
    @CsvSource({"278.35, 30, 83.51", "114.95, 15, 17.24", "102.00, 5, 5.10", "20.00, 10, 2.00",
        "10.00, 0, 0.00", "-10.00, 10, 0.00"})
    void percentOfRoundsHalfUpToTwoDecimals(String amount, String pct, String expected) {
        assertThat(MoneyUtils.percentOf(money(amount), money(pct))).isEqualByComparingTo(expected);
    }

    @ParameterizedTest(name = "{0} / {1} = {2} %")
    @CsvSource({"13.10, 110.00, 11.91", "32.29, 130.00, 24.84", "112.00, 320.00, 35.00", "5.00, 0.00, 0.00"})
    void ratioAsPercent(String part, String whole, String expected) {
        assertThat(MoneyUtils.ratioAsPercent(money(part), money(whole))).isEqualByComparingTo(expected);
    }

    @Test
    void arithmeticKeepsScaleTwo() {
        assertThat(MoneyUtils.multiply(money("12.50"), 3)).isEqualByComparingTo("37.50");
        assertThat(MoneyUtils.add(money("1.005"), money("0"))).isEqualByComparingTo("1.01");
        assertThat(MoneyUtils.subtract(money("10"), money("2.5")).scale()).isEqualTo(2);
        assertThat(MoneyUtils.sum(List.of(money("1.10"), money("2.20")))).isEqualByComparingTo("3.30");
        assertThat(MoneyUtils.nonNegative(money("-1"))).isEqualByComparingTo("0.00");
        assertThat(MoneyUtils.round(null)).isEqualByComparingTo("0.00");
        assertThat(MoneyUtils.isGreaterThan(money("100.01"), money("100.00"))).isTrue();
        assertThat(MoneyUtils.isPositive(null)).isFalse();
    }
}
