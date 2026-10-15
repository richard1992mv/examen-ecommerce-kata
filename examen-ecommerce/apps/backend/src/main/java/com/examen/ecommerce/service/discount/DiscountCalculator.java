package com.examen.ecommerce.service.discount;

import com.examen.ecommerce.config.DiscountProperties;
import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Motor de descuentos acumulativos en cascada (multiplicativa), redondeo HALF_UP a 2 decimales por paso:
 * <ol>
 *   <li>Categoría: por línea, line_subtotal × % de su categoría (Tecnología 10 %).</li>
 *   <li>Volumen: si subtotal_1 &gt; 100.00, subtotal_1 × 5 %.</li>
 *   <li>Cupón: si es válido, subtotal_2 × % del cupón (WELCOME2026 15 %).</li>
 *   <li>Tope: el descuento total nunca supera el 35 % del subtotal original.</li>
 * </ol>
 */
@Component
public class DiscountCalculator {

    private final BigDecimal volumePct;
    private final BigDecimal volumeThreshold;
    private final BigDecimal capPct;

    public DiscountCalculator(DiscountProperties properties) {
        this.volumePct = properties.volume().pct();
        this.volumeThreshold = properties.volume().threshold();
        this.capPct = MoneyUtils.round(properties.capPct());
    }

    /**
     * @param couponPct porcentaje del cupón ya validado; vacío si no hay cupón
     */
    public DiscountResult calculate(List<DiscountLine> lines, Optional<BigDecimal> couponPct) {
        List<DiscountResult.LineResult> lineResults = lines.stream().map(this::applyCategory).toList();
        BigDecimal original = MoneyUtils.sum(lines.stream().map(DiscountLine::lineSubtotal).toList());

        BigDecimal category = MoneyUtils.sum(
                lineResults.stream().map(DiscountResult.LineResult::categoryDiscount).toList());
        BigDecimal subtotal1 = MoneyUtils.subtract(original, category);

        BigDecimal volume = MoneyUtils.isGreaterThan(subtotal1, volumeThreshold)
                ? MoneyUtils.percentOf(subtotal1, volumePct)
                : MoneyUtils.ZERO;
        BigDecimal subtotal2 = MoneyUtils.subtract(subtotal1, volume);

        BigDecimal coupon = couponPct.map(pct -> MoneyUtils.percentOf(subtotal2, pct)).orElse(MoneyUtils.ZERO);

        BigDecimal gross = MoneyUtils.sum(List.of(category, volume, coupon));
        BigDecimal cap = MoneyUtils.percentOf(original, capPct);
        boolean capApplied = MoneyUtils.isGreaterThan(gross, cap);
        BigDecimal discountTotal = capApplied ? cap : gross;
        BigDecimal adjustment = MoneyUtils.subtract(gross, discountTotal);

        return new DiscountResult(lineResults, original, category, volume, coupon, adjustment, discountTotal,
                MoneyUtils.ratioAsPercent(discountTotal, original), capPct, capApplied,
                MoneyUtils.subtract(original, discountTotal));
    }

    private DiscountResult.LineResult applyCategory(DiscountLine line) {
        BigDecimal discount = MoneyUtils.percentOf(line.lineSubtotal(), line.categoryDiscountPct());
        return new DiscountResult.LineResult(line, discount, MoneyUtils.subtract(line.lineSubtotal(), discount));
    }
}
