package com.examen.ecommerce.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parámetros de descuento (prefijo {@code discount}): volumen (5 % sobre &gt; 100.00) y tope (35 %).
 * Los porcentajes de categoría y cupón se leen de la BD.
 */
@Validated
@ConfigurationProperties(prefix = "discount")
public record DiscountProperties(
        @Valid @NotNull Volume volume,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal capPct) {

    /** discount.volume.pct y discount.volume.threshold. */
    public record Volume(
            @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal pct,
            @NotNull @DecimalMin("0.00") BigDecimal threshold) {
    }
}
