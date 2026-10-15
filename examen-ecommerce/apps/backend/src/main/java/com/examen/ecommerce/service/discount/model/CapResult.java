package com.examen.ecommerce.service.discount.model;

import java.math.BigDecimal;

/**
 * Resultado de aplicar el tope máximo de descuento.
 *
 * @param capPct        snapshot del tope (35.00)
 * @param capAmount     subtotal_original × tope
 * @param grossDiscount categoría + volumen + cupón
 * @param adjustment    bruto − tope si se superó; 0 en otro caso
 * @param discountTotal descuento final aplicado
 * @param capApplied    true si se recortó el descuento (HU4)
 */
public record CapResult(
        BigDecimal capPct,
        BigDecimal capAmount,
        BigDecimal grossDiscount,
        BigDecimal adjustment,
        BigDecimal discountTotal,
        boolean capApplied) {
}
