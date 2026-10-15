package com.examen.ecommerce.service.discount.model;

/** Tipos de descuento soportados. Agregar uno nuevo = constante + DiscountRule + caso en la Factory. */
public enum DiscountType {
    CATEGORY,
    VOLUME,
    COUPON
}
