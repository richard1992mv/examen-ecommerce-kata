package com.examen.ecommerce.service.discount;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.examen.ecommerce.config.DiscountProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Patrón Factory: la fábrica decide qué reglas aplican y en qué orden de la cascada. Prueba unitaria pura. */
@DisplayName("Factory · Creación de las reglas de descuento")
class DiscountRuleFactoryTest {

    private DiscountRuleFactory factory;

    @BeforeEach
    void setUp() {
        DiscountProperties properties = new DiscountProperties(
                new DiscountProperties.Volume(new BigDecimal("5.00"), new BigDecimal("100.00")),
                new BigDecimal("35.00"));
        factory = new DiscountRuleFactory(properties);
    }

    @Test
    @DisplayName("Sin cupón crea Categoría → Volumen")
    void sinCuponCreaCategoriaYVolumen() {
        List<DiscountRule> rules = factory.createRules(Optional.empty());

        assertEquals(List.of(DiscountType.CATEGORY, DiscountType.VOLUME), types(rules));
    }

    @Test
    @DisplayName("Con cupón crea Categoría → Volumen → Cupón, en ese orden")
    void conCuponAgregaLaReglaDeCuponAlFinal() {
        List<DiscountRule> rules = factory.createRules(Optional.of(new BigDecimal("15.00")));

        assertEquals(List.of(DiscountType.CATEGORY, DiscountType.VOLUME, DiscountType.COUPON), types(rules));
    }

    @Test
    @DisplayName("La regla de cupón usa el porcentaje recibido en cada petición")
    void reglaDeCuponUsaElPorcentajeDeLaPeticion() {
        DiscountRule coupon = factory.createRules(Optional.of(new BigDecimal("15.00"))).get(2);

        assertEquals(new BigDecimal("15.00"), coupon.calculate(List.of(), new BigDecimal("100.00")));
    }

    private static List<DiscountType> types(List<DiscountRule> rules) {
        return rules.stream().map(DiscountRule::type).toList();
    }
}
