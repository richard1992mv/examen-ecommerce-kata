package com.examen.ecommerce.service.discount;

import com.examen.ecommerce.config.DiscountProperties;
import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
 *
 * <p><b>Uso de los patrones:</b> el calculador es el <i>contexto</i> del patrón Strategy. Pide las reglas 1–3 a
 * {@link DiscountRuleFactory} (Factory) y las recorre a través de la interfaz {@link DiscountRule} sin conocer
 * cada fórmula; cada regla recibe el subtotal que dejó la anterior. El tope (paso 4) no es una regla de la
 * cascada sino un límite sobre la suma de las tres, por eso se resuelve aquí.
 */
@Component
public class DiscountCalculator {

    private final DiscountRuleFactory ruleFactory;
    private final BigDecimal capPct;

    /** @param ruleFactory fábrica inyectada: el calculador depende de ella sin construirla */
    public DiscountCalculator(DiscountRuleFactory ruleFactory, DiscountProperties properties) {
        this.ruleFactory = ruleFactory;
        this.capPct = MoneyUtils.round(properties.capPct());
    }

    /**
     * @param couponPct porcentaje del cupón ya validado; vacío si no hay cupón
     */
    public DiscountResult calculate(List<DiscountLine> lines, Optional<BigDecimal> couponPct) {
        List<DiscountResult.LineResult> lineResults = lines.stream().map(DiscountCalculator::toLineResult).toList();
        BigDecimal original = MoneyUtils.sum(lines.stream().map(DiscountLine::lineSubtotal).toList());

        // Cascada: cada estrategia descuenta sobre el subtotal que dejó la anterior.
        Map<DiscountType, BigDecimal> amounts = new EnumMap<>(DiscountType.class);
        BigDecimal subtotal = original;
        for (DiscountRule rule : ruleFactory.createRules(couponPct)) {
            BigDecimal amount = rule.calculate(lines, subtotal);
            amounts.put(rule.type(), amount);
            subtotal = MoneyUtils.subtract(subtotal, amount);
        }
        BigDecimal category = amounts.getOrDefault(DiscountType.CATEGORY, MoneyUtils.ZERO);
        BigDecimal volume = amounts.getOrDefault(DiscountType.VOLUME, MoneyUtils.ZERO);
        BigDecimal coupon = amounts.getOrDefault(DiscountType.COUPON, MoneyUtils.ZERO);

        BigDecimal gross = MoneyUtils.sum(List.of(category, volume, coupon));
        BigDecimal cap = MoneyUtils.percentOf(original, capPct);
        boolean capApplied = MoneyUtils.isGreaterThan(gross, cap);
        BigDecimal discountTotal = capApplied ? cap : gross;
        BigDecimal adjustment = MoneyUtils.subtract(gross, discountTotal);

        return new DiscountResult(lineResults, original, category, volume, coupon, adjustment, discountTotal,
                MoneyUtils.ratioAsPercent(discountTotal, original), capPct, capApplied,
                MoneyUtils.subtract(original, discountTotal));
    }

    private static DiscountResult.LineResult toLineResult(DiscountLine line) {
        BigDecimal discount = CategoryDiscountRule.discountFor(line);
        return new DiscountResult.LineResult(line, discount, MoneyUtils.subtract(line.lineSubtotal(), discount));
    }
}
