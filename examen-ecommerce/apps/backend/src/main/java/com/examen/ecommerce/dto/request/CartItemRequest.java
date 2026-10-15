package com.examen.ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Línea del carrito: solo productId y cantidad; precios y % se leen de la BD. */
public record CartItemRequest(
        @Schema(example = "1") @NotNull @Positive Integer productId,
        @Schema(example = "2") @NotNull @Positive Integer quantity) {
}
