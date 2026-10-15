package com.examen.ecommerce.event;

/** OBSERVER: jerarquía cerrada de eventos del carrito (records inmutables). */
public sealed interface CartEvent permits CartQuotedEvent, DiscountCapReachedEvent, OrderConfirmedEvent {
}
