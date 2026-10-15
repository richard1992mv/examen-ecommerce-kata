package com.examen.ecommerce.config;

import com.examen.ecommerce.dto.validation.Percentage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parámetros de las reglas de descuento (prefijo {@code discount}).
 * Ningún porcentaje ni umbral queda hardcodeado en el código.
 */
@Validated
@ConfigurationProperties(prefix = "discount")
public record DiscountProperties(
        @Valid @NotNull Volume volume,
        @NotNull @Percentage BigDecimal capPct) {

    /** discount.volume.pct y discount.volume.threshold. */
    public record Volume(
            @NotNull @Percentage BigDecimal pct,
            @NotNull @DecimalMin("0.00") BigDecimal threshold) {
    }
}
