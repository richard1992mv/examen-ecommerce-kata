package com.examen.ecommerce.service.discount;

import java.math.BigDecimal;
import java.util.List;

/**
 * <b>Patrón Strategy</b>: contrato común de cada tipo de descuento de la cascada.
 *
 * <p>Cada regla (categoría, volumen, cupón) tiene su propia fórmula y condición, pero todas se usan igual:
 * reciben el subtotal acumulado hasta ese paso y devuelven el monto a descontar. Así
 * {@link DiscountCalculator} recorre las reglas sin {@code if/switch} por tipo, y agregar un descuento nuevo
 * es crear otra implementación sin modificar el calculador (principio abierto/cerrado).
 *
 * <p>Implementaciones: {@link CategoryDiscountRule}, {@link VolumeDiscountRule}, {@link CouponDiscountRule}.
 * Las instancia {@link DiscountRuleFactory}.
 */
public interface DiscountRule {

    /** Campo del desglose ({@link DiscountResult}) al que corresponde el monto de esta regla. */
    DiscountType type();

    /**
     * Calcula el descuento de esta regla, redondeado HALF_UP a 2 decimales.
     *
     * @param lines    líneas del carrito (solo la regla de categoría las necesita)
     * @param subtotal subtotal tras aplicar las reglas anteriores de la cascada
     * @return monto a descontar; {@code 0.00} si la regla no aplica
     */
    BigDecimal calculate(List<DiscountLine> lines, BigDecimal subtotal);
}
