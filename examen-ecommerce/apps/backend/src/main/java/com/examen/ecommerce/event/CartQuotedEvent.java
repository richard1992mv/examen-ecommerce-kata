package com.examen.ecommerce.event;

import java.math.BigDecimal;

/** Se publicó una cotización. items = número de líneas del carrito. */
public record CartQuotedEvent(int items, BigDecimal discountTotal, boolean capApplied) implements CartEvent {
}
