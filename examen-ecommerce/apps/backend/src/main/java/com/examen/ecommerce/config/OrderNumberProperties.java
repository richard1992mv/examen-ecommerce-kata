package com.examen.ecommerce.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración del número de orden (prefijo {@code order.number}).
 *
 * @param prefix      prefijo visible (ORD)
 * @param maxAttempts reintentos acotados ante colisión del UNIQUE uk_orders_order_number
 */
@Validated
@ConfigurationProperties(prefix = "order.number")
public record OrderNumberProperties(@NotBlank String prefix, @Min(1) int maxAttempts) {
}
