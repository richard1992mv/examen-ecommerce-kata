package com.examen.ecommerce.service.discount;

import com.examen.ecommerce.config.DiscountProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <b>Patrón Factory</b>: único lugar donde se instancian las reglas de descuento ({@link DiscountRule}).
 *
 * <p>Centraliza dos decisiones que no deben quedar repartidas por el código:
 * <ul>
 *   <li><b>Qué reglas aplican</b> en cada cálculo: categoría y volumen siempre; cupón solo si hay uno válido.
 *       La regla de cupón depende del porcentaje de cada petición, por eso se crea por cálculo y no puede ser un
 *       objeto único compartido.</li>
 *   <li><b>En qué orden</b> se aplican (la cascada es multiplicativa, el orden cambia el resultado):
 *       Categoría → Volumen → Cupón.</li>
 * </ul>
 *
 * <p>Los parámetros de volumen salen de {@code application.properties} ({@code discount.volume.*}); los
 * porcentajes de categoría y cupón vienen de la BD. Las reglas sin estado por petición se crean una sola vez.
 */
public class DiscountRuleFactory {

    private final DiscountRule categoryRule;
    private final DiscountRule volumeRule;

    public DiscountRuleFactory(DiscountProperties properties) {
        this.categoryRule = new CategoryDiscountRule();
        this.volumeRule = new VolumeDiscountRule(properties.volume().pct(), properties.volume().threshold());
    }

    /**
     * @param couponPct porcentaje del cupón ya validado; vacío si no hay cupón
     * @return reglas en el orden de la cascada
     */
    public List<DiscountRule> createRules(Optional<BigDecimal> couponPct) {
        List<DiscountRule> rules = new ArrayList<>(List.of(categoryRule, volumeRule));
        couponPct.map(CouponDiscountRule::new).ifPresent(rules::add);
        return List.copyOf(rules);
    }
}
