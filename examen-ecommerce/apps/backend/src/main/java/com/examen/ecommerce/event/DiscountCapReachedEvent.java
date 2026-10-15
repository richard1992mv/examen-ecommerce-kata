package com.examen.ecommerce.event;

import java.math.BigDecimal;

/** El descuento bruto superó el tope y fue recortado (HU4). */
public record DiscountCapReachedEvent(BigDecimal subtotalOriginal, BigDecimal grossDiscount, BigDecimal capAdjustment)
        implements CartEvent {
}
