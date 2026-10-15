package com.examen.ecommerce.dto.request;

import com.examen.ecommerce.dto.validation.Percentage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Entrada de categorías (sin id ni timestamps: anti asignación masiva). */
public record CategoryRequest(
        @Schema(example = "Tecnología") @NotBlank @Size(max = 100) String name,
        @Schema(example = "10.00") @NotNull @Percentage BigDecimal discountPct) {
}
