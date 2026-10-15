package com.examen.ecommerce.event;

import java.math.BigDecimal;

/** Orden persistida. Los observadores la reciben solo tras el COMMIT. */
public record OrderConfirmedEvent(Integer orderId, String orderNumber, BigDecimal total) implements CartEvent {
}
