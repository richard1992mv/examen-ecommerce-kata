package com.examen.ecommerce.dto.request;

import com.examen.ecommerce.dto.validation.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Entrada de productos (HU1): unit_price > 0 y stock ≥ 0. */
public record ProductRequest(
        @Schema(example = "Teclado mecánico") @NotBlank @Size(max = 150) String name,
        @Schema(example = "80.00") @NotNull @Money BigDecimal unitPrice,
        @Schema(example = "1") @NotNull @Positive Integer categoryId,
        @Schema(example = "15") @NotNull @PositiveOrZero Integer stock) {
}
