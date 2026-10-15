package com.examen.ecommerce.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * ÚNICO lugar con aritmética monetaria y redondeo (DRY).
 * Regla: HALF_UP a 2 decimales al final de CADA paso, igual que 02_seed_data.sql.
 * Nunca se usa double/float.
 */
public final class MoneyUtils {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    public static final String DEFAULT_CURRENCY = "USD";

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private MoneyUtils() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value == null ? ZERO : value.setScale(SCALE, ROUNDING);
    }

    /** unit_price × quantity, redondeado. */
    public static BigDecimal multiply(BigDecimal unitPrice, int quantity) {
        return round(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    /** amount × pct / 100 con un único redondeo HALF_UP; nunca negativo. */
    public static BigDecimal percentOf(BigDecimal amount, BigDecimal pct) {
        if (!isPositive(amount) || !isPositive(pct)) {
            return ZERO;
        }
        return amount.multiply(pct).divide(ONE_HUNDRED, SCALE, ROUNDING);
    }

    /** part / whole × 100, redondeado; 0 si whole es 0. */
    public static BigDecimal ratioAsPercent(BigDecimal part, BigDecimal whole) {
        if (!isPositive(whole)) {
            return ZERO;
        }
        return part.multiply(ONE_HUNDRED).divide(whole, SCALE, ROUNDING);
    }

    public static BigDecimal add(BigDecimal left, BigDecimal right) {
        return round(left.add(right));
    }

    public static BigDecimal subtract(BigDecimal minuend, BigDecimal subtrahend) {
        return round(minuend.subtract(subtrahend));
    }

    public static BigDecimal sum(Collection<BigDecimal> values) {
        return round(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public static BigDecimal nonNegative(BigDecimal value) {
        return isPositive(value) ? round(value) : ZERO;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    public static boolean isGreaterThan(BigDecimal left, BigDecimal right) {
        return left.compareTo(right) > 0;
    }
}
